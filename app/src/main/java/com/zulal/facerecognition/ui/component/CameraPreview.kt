package com.zulal.facerecognition.ui.component

import android.annotation.SuppressLint
import android.graphics.*
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.zulal.facerecognition.ui.graphic.FaceGraphic
import com.zulal.facerecognition.ui.graphic.GraphicOverlay
import com.zulal.facerecognition.ui.graphic.GuideRectGraphic
import com.zulal.facerecognition.viewmodel.FaceViewModel
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner,
    faceViewModel: FaceViewModel,
    onFaceEmbeddingDetected: (FloatArray) -> Unit
) {
    val context = LocalContext.current

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            // Container: Preview + Overlay
            val container = FrameLayout(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            // 1) Kamera önizleme alanı
            val previewView = PreviewView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            container.addView(previewView)

            // 2) Çizim alanı (yüz kutusu + tarama kutusu)
            val graphicOverlay = GraphicOverlay(ctx, null).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            container.addView(graphicOverlay)

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val analysisUseCase = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetResolution(android.util.Size(640, 480))
                        .build()

                    val options = FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .build()

                    val detector = FaceDetection.getClient(options)

                    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                    val lensFacing = CameraSelector.LENS_FACING_FRONT

                    analysisUseCase.setAnalyzer(
                        ContextCompat.getMainExecutor(ctx)
                    ) { imageProxy ->
                        processImageProxy(
                            detector = detector,
                            imageProxy = imageProxy,
                            faceViewModel = faceViewModel,
                            onFaceEmbeddingDetected = onFaceEmbeddingDetected,
                            overlay = graphicOverlay,
                            cameraLensFacing = lensFacing
                        )
                    }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        analysisUseCase
                    )
                } catch (exc: Exception) {
                    Log.e("CameraPreview", "Camera yüklenemedi", exc)
                }
            }, ContextCompat.getMainExecutor(ctx))

            container
        }
    )
}

@OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processImageProxy(
    detector: com.google.mlkit.vision.face.FaceDetector,
    imageProxy: ImageProxy,
    faceViewModel: FaceViewModel,
    onFaceEmbeddingDetected: (FloatArray) -> Unit,
    overlay: GraphicOverlay,
    cameraLensFacing: Int
) {
    overlay.clear()

    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        // Overlay’e kamera boyutu, lens bilgisini verir
        overlay.setCameraInfo(image.width, image.height, cameraLensFacing)

        detector.process(image)
            .addOnSuccessListener { faces ->

                // 1) Ortada tarama kutusu
                val centerBox = android.graphics.RectF(
                    image.width * 0.2f,   // left
                    image.height * 0.2f,  // top
                    image.width * 0.8f,   // right
                    image.height * 0.8f   // bottom
                )
                // Bu kutuyu overlay’de göster
                overlay.add(GuideRectGraphic(overlay, centerBox))

                if (faces.isEmpty()) {
                    return@addOnSuccessListener
                }

                //  2) Tespit edilen yüzleri overlay’e çiz
                faces.forEach { face ->
                    overlay.add(FaceGraphic(overlay, face))
                }

                //  3) Sadece ortadaki kutunun içinde kalan tek yüzü kullan
                val facesInCenter = faces.filter { face ->
                    val box = face.boundingBox
                    val cx = box.exactCenterX()
                    val cy = box.exactCenterY()
                    cx >= centerBox.left && cx <= centerBox.right &&
                            cy >= centerBox.top && cy <= centerBox.bottom
                }

                // Eğer ortada 1 tane yüz yoksa embedding üretme
                if (facesInCenter.size != 1) {
                    Log.d("FaceDetection", "Merkez bölgedeki yüz sayısı: ${facesInCenter.size}")
                    return@addOnSuccessListener
                }

                val face = facesInCenter[0]

                // Yüzü kırp ve FaceNet’e uygun hale getir!!!!
                val bitmap = imageProxy.toBitmap() ?: return@addOnSuccessListener
                val box = face.boundingBox

                val cropped = try {
                    Bitmap.createBitmap(
                        bitmap,
                        box.left.coerceAtLeast(0),
                        box.top.coerceAtLeast(0),
                        box.width().coerceAtMost(bitmap.width - box.left),
                        box.height().coerceAtMost(bitmap.height - box.top)
                    )
                } catch (e: Exception) {
                    Log.e("FaceCrop", "Bitmap kırpma hatası: ${e.message}")
                    return@addOnSuccessListener
                }

                val inputArray = bitmapToFloatArray(cropped)

                faceViewModel.updateLastEmbedding(inputArray)
                onFaceEmbeddingDetected(inputArray)

                Log.d("FaceEmbedding", "Merkezdeki yüz için embedding üretildi")
            }
            .addOnFailureListener { e ->
                Log.e("FaceDetection", "Face detection failed", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

// Bitmap to FloatArray (FaceNet girişi)
private fun bitmapToFloatArray(bitmap: Bitmap): FloatArray {
    val inputImage = Bitmap.createScaledBitmap(bitmap, 160, 160, true)
    val floatArray = FloatArray(160 * 160 * 3)
    var index = 0

    for (y in 0 until 160) {
        for (x in 0 until 160) {
            val pixel = inputImage.getPixel(x, y)
            floatArray[index++] = ((pixel shr 16 and 0xFF) - 127.5f) / 128f // R
            floatArray[index++] = ((pixel shr 8 and 0xFF) - 127.5f) / 128f  // G
            floatArray[index++] = ((pixel and 0xFF) - 127.5f) / 128f        // B
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

    val yuvImage = YuvImage(
        nv21, ImageFormat.NV21, this.width, this.height, null
    )

    val out = ByteArrayOutputStream()
    try {
        yuvImage.compressToJpeg(
            Rect(0, 0, yuvImage.width, yuvImage.height), 75, out
        )
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    } finally {
        out.close()
    }
}

