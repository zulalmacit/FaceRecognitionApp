package com.zulal.facerecognition.ui.graphic

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import androidx.camera.core.CameraSelector

class GraphicOverlay(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val graphics: MutableList<Graphic> = mutableListOf()

    // ML Kit’in analiz ettiği görüntünün boyutu
    private var analysisImageWidth: Int = 0
    private var analysisImageHeight: Int = 0

    // (ön kamera için aynalama gerekiyor)
    private var cameraLensFacing: Int = CameraSelector.LENS_FACING_FRONT

    fun scaleX(x: Float): Float {
        if (analysisImageWidth == 0) return x
        return x * width / analysisImageWidth.toFloat()
    }

    fun scaleY(y: Float): Float {
        if (analysisImageHeight == 0) return y
        return y * height / analysisImageHeight.toFloat()
    }

    fun setCameraInfo(
        imageWidth: Int,
        imageHeight: Int,
        lensFacing: Int
    ) {
        analysisImageWidth = imageWidth
        analysisImageHeight = imageHeight
        cameraLensFacing = lensFacing
    }

    fun translateX(x: Float): Float {
        return if (cameraLensFacing == CameraSelector.LENS_FACING_FRONT) {
            width - scaleX(x)
        } else {
            scaleX(x)
        }
    }

    fun translateY(y: Float): Float = scaleY(y)

    abstract class Graphic(private val overlay: GraphicOverlay) {
        abstract fun draw(canvas: Canvas)
        fun getOverlay(): GraphicOverlay = overlay
    }

    fun clear() {
        graphics.clear()
        postInvalidate()
    }

    fun add(graphic: Graphic) {
        graphics.add(graphic)
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (analysisImageWidth == 0 || analysisImageHeight == 0) return

        for (graphic in graphics) {
            graphic.draw(canvas)
        }
    }
}
