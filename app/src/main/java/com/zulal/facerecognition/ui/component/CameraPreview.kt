package com.zulal.facerecognition.ui.component

import android.annotation.SuppressLint
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

// @SuppressLint("UnsafeOptInUsageError") //Android lint uyarısını kapat
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
                    Log.d("FaceDetection", "Yüz bulundu! Sayı: ${faces.size}")

                    val dummyEmbedding = FloatArray(128) { 0.5f }
                    faceViewModel.updateLastEmbedding(dummyEmbedding)
                    onFaceEmbeddingDetected(dummyEmbedding)
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}
