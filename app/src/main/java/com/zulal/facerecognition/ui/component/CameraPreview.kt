package com.zulal.facerecognition.ui.component

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import com.zulal.facerecognition.viewmodel.FaceViewModel
import java.nio.ByteBuffer

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
            val previewView = PreviewView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val analysisUseCase = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    val options = FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .build()

                    val detector = FaceDetection.getClient(options)

                    analysisUseCase.setAnalyzer(
                        ContextCompat.getMainExecutor(ctx)
                    ) { imageProxy ->
                        processImageProxy(detector, imageProxy, faceViewModel, onFaceEmbeddingDetected)
                    }

                    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
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

            previewView
        }
    )
}

@OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processImageProxy(
    detector: com.google.mlkit.vision.face.FaceDetector,
    imageProxy: ImageProxy,
    faceViewModel: FaceViewModel,
    onFaceEmbeddingDetected: (FloatArray) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    val face = faces[0] // ilk yüz
                    val bitmap = imageProxy.toBitmap() ?: return@addOnSuccessListener

                    // yüzü kırp
                    val box = face.boundingBox
                    val cropped = Bitmap.createBitmap(
                        bitmap,
                        box.left.coerceAtLeast(0),
                        box.top.coerceAtLeast(0),
                        box.width().coerceAtMost(bitmap.width - box.left),
                        box.height().coerceAtMost(bitmap.height - box.top)
                    )

                    val inputArray = bitmapToFloatArray(cropped)

                    // embedding update
                    faceViewModel.updateLastEmbedding(inputArray)
                    onFaceEmbeddingDetected(inputArray)

                    Log.d("FaceEmbedding", "Yüz embedding üretildi")
                }
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

// Bitmap -> FloatArray dönüştürme
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

// ImageProxy -> Bitmap dönüştürme
private fun ImageProxy.toBitmap(): Bitmap? {
    val planeProxy = planes.firstOrNull() ?: return null
    val buffer: ByteBuffer = planeProxy.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}
