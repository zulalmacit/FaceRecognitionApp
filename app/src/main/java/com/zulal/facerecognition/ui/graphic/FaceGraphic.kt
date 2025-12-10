package com.zulal.facerecognition.ui.graphic

import android.graphics.*
import com.google.mlkit.vision.face.Face

class FaceGraphic(
    overlay: GraphicOverlay,
    private val face: Face
) : GraphicOverlay.Graphic(overlay) {

    private val boxPaint: Paint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val textPaint: Paint = Paint().apply {
        color = Color.WHITE
        textSize = 50f
        textAlign = Paint.Align.LEFT
    }

    override fun draw(canvas: Canvas) {
        val boundingBox: Rect = face.boundingBox

        val xLeft = getOverlay().translateX(boundingBox.left.toFloat())
        val yTop = getOverlay().translateY(boundingBox.top.toFloat())
        val xRight = getOverlay().translateX(boundingBox.right.toFloat())
        val yBottom = getOverlay().translateY(boundingBox.bottom.toFloat())

        canvas.drawRect(
            minOf(xLeft, xRight),
            yTop,
            maxOf(xLeft, xRight),
            yBottom,
            boxPaint
        )

        face.trackingId?.let {
            canvas.drawText(
                "ID: $it",
                minOf(xLeft, xRight) + 5,
                yTop - 10,
                textPaint
            )
        }
    }
}

/*
 Ortadaki tarama alanını (guide box) çizen grafik.
 Box koordinatları ML Kit image koordinatlarında (RectF), overlay'e çeviriyo.
 */
class GuideRectGraphic(
    overlay: GraphicOverlay,
    private val box: RectF
) : GraphicOverlay.Graphic(overlay) {

    private val guidePaint: Paint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.STROKE
        strokeWidth = 6f
        pathEffect = DashPathEffect(floatArrayOf(20f, 10f), 0f) // kesik çizgi görünümü
    }

    override fun draw(canvas: Canvas) {
        val left = getOverlay().translateX(box.left)
        val top = getOverlay().translateY(box.top)
        val right = getOverlay().translateX(box.right)
        val bottom = getOverlay().translateY(box.bottom)

        canvas.drawRect(
            minOf(left, right),
            top,
            maxOf(left, right),
            bottom,
            guidePaint
        )
    }
}
