package com.zulal.facerecognition.ui.component

import android.annotation.SuppressLint
import android.graphics.*
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.zulal.facerecognition.ui.graphic.FaceGraphic
import com.zulal.facerecognition.ui.graphic.GraphicOverlay
import com.zulal.facerecognition.ui.graphic.GuideRectGraphic
import com.zulal.facerecognition.viewmodel.FaceViewModel
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner,
    faceViewModel: FaceViewModel,
    onFaceEmbeddingDetected: (FloatArray) -> Unit
) {
    val context = LocalContext.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }

    // Analyzer artık UI değil background thread’de çalışacak
    val analysisExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    // Callback stale olmasın diye
    val onFaceEmbeddingDetectedState by rememberUpdatedState(onFaceEmbeddingDetected)

    // Camera / ML kaynaklarını tutalım ki dispose’ta kapatalım
    val cameraProviderFuture = remember(context) { ProcessCameraProvider.getInstance(context) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var analysisUseCase by remember { mutableStateOf<ImageAnalysis?>(null) }
    var detector by remember { mutableStateOf<FaceDetector?>(null) }
    var overlayRef by remember { mutableStateOf<GraphicOverlay?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val container = FrameLayout(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            val previewView = PreviewView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            container.addView(previewView)

            val graphicOverlay = GraphicOverlay(ctx, null).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            container.addView(graphicOverlay)

            previewViewRef = previewView
            overlayRef = graphicOverlay

            container
        }
    )

    // Bind işi composable tarafında yönetilsin (dispose’ta da temizleyeceğiz)
    LaunchedEffect(lifecycleOwner) {
        try {
            val provider = cameraProviderFuture.get()
            cameraProvider = provider

            val previewView = previewViewRef ?: return@LaunchedEffect
            val overlay = overlayRef ?: return@LaunchedEffect

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(android.util.Size(640, 480))
                .build()

            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .build()

            val faceDetector = FaceDetection.getClient(options)
            detector = faceDetector
            analysisUseCase = analysis

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            val lensFacing = CameraSelector.LENS_FACING_FRONT

            analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                processImageProxy(
                    detector = faceDetector,
                    imageProxy = imageProxy,
                    faceViewModel = faceViewModel,
                    onFaceEmbeddingDetected = { arr -> onFaceEmbeddingDetectedState(arr) },
                    overlay = overlay,
                    cameraLensFacing = lensFacing,
                    mainExecutor = mainExecutor
                )
            }

            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                analysis
            )
        } catch (e: Exception) {
            Log.e("CameraPreview", "Camera bind failed", e)
        }
    }

    //ekran kapanınca her şeyi kapat
    DisposableEffect(Unit) {
        onDispose {
            try {
                analysisUseCase?.clearAnalyzer()
            } catch (_: Exception) { }

            try {
                cameraProvider?.unbindAll()
            } catch (_: Exception) { }

            try {
                detector?.close()
            } catch (_: Exception) { }

            try {
                analysisExecutor.shutdownNow()
            } catch (_: Exception) { }

            analysisUseCase = null
            cameraProvider = null
            detector = null
        }
    }
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processImageProxy(
    detector: FaceDetector,
    imageProxy: ImageProxy,
    faceViewModel: FaceViewModel,
    onFaceEmbeddingDetected: (FloatArray) -> Unit,
    overlay: GraphicOverlay,
    cameraLensFacing: Int,
    mainExecutor: java.util.concurrent.Executor
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }

    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
    val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

    // ML Kit task listener’larını ana threade alalım (overlay çizimleri için)
    detector.process(image)
        .addOnSuccessListener(mainExecutor) { faces ->
            overlay.clear()

            overlay.setCameraInfo(image.width, image.height, cameraLensFacing)

            val centerBox = android.graphics.RectF(
                image.width * 0.2f,
                image.height * 0.2f,
                image.width * 0.8f,
                image.height * 0.8f
            )
            overlay.add(GuideRectGraphic(overlay, centerBox))

            if (faces.isEmpty()) return@addOnSuccessListener

            faces.forEach { face -> overlay.add(FaceGraphic(overlay, face)) }

            val facesInCenter = faces.filter { face ->
                val box = face.boundingBox
                val cx = box.exactCenterX()
                val cy = box.exactCenterY()
                cx >= centerBox.left && cx <= centerBox.right &&
                        cy >= centerBox.top && cy <= centerBox.bottom
            }

            if (facesInCenter.size != 1) return@addOnSuccessListener

            val face = facesInCenter[0]
            val box = face.boundingBox

            val bitmap = imageProxy.toBitmap() ?: return@addOnSuccessListener

            val cropped = try {
                Bitmap.createBitmap(
                    bitmap,
                    box.left.coerceAtLeast(0),
                    box.top.coerceAtLeast(0),
                    box.width().coerceAtMost(bitmap.width - box.left),
                    box.height().coerceAtMost(bitmap.height - box.top)
                )
            } catch (e: Exception) {
                Log.e("FaceCrop", "Bitmap crop error: ${e.message}")
                return@addOnSuccessListener
            }

            val inputArray = bitmapToFloatArray(cropped)

            faceViewModel.updateLastEmbedding(inputArray)
            onFaceEmbeddingDetected(inputArray)
        }
        .addOnFailureListener { e ->
            Log.e("FaceDetection", "Face detection failed", e)
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}

private fun bitmapToFloatArray(bitmap: Bitmap): FloatArray {
    val inputImage = Bitmap.createScaledBitmap(bitmap, 160, 160, true)
    val floatArray = FloatArray(160 * 160 * 3)
    var index = 0

    for (y in 0 until 160) {
        for (x in 0 until 160) {
            val pixel = inputImage.getPixel(x, y)
            floatArray[index++] = ((pixel shr 16 and 0xFF) - 127.5f) / 128f
            floatArray[index++] = ((pixel shr 8 and 0xFF) - 127.5f) / 128f
            floatArray[index++] = ((pixel and 0xFF) - 127.5f) / 128f
        }
    }
    return floatArray
}

@SuppressLint("UnsafeOptInUsageError")
private fun ImageProxy.toBitmap(): Bitmap? {
    val mediaImage = this.image ?: return null
    val planes = mediaImage.planes
    val yBuffer: ByteBuffer = planes[0].buffer
    val uBuffer: ByteBuffer = planes[1].buffer
    val vBuffer: ByteBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)

    val out = ByteArrayOutputStream()
    return try {
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 75, out)
        val imageBytes = out.toByteArray()
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    } finally {
        out.close()
    }
}
