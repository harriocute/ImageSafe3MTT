package com.safetry.privacy.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.safetry.privacy.model.DetectionResult

/**
 * Custom ImageView that draws red highlight boxes over detected sensitive regions.
 */
class PrivacyOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val detections = mutableListOf<DetectionResult>()
    private val boxPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    private val labelBgPaint = Paint().apply {
        color = Color.argb(180, 220, 0, 0)
        style = Paint.Style.FILL
    }
    private val labelTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 28f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }

    fun setDetections(results: List<DetectionResult>) {
        detections.clear()
        detections.addAll(results)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (detections.isEmpty()) return

        val drawable = drawable ?: return
        val imageWidth = drawable.intrinsicWidth.toFloat()
        val imageHeight = drawable.intrinsicHeight.toFloat()

        // Calculate actual image display bounds within the view
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        val scaleX = viewWidth / imageWidth
        val scaleY = viewHeight / imageHeight
        val scale = minOf(scaleX, scaleY)

        val offsetX = (viewWidth - imageWidth * scale) / 2f
        val offsetY = (viewHeight - imageHeight * scale) / 2f

        for (detection in detections) {
            val rect = detection.boundingBox

            // Transform normalized or pixel coords to view coords
            val left = rect.left * scale + offsetX
            val top = rect.top * scale + offsetY
            val right = rect.right * scale + offsetX
            val bottom = rect.bottom * scale + offsetY

            val displayRect = RectF(left, top, right, bottom)

            // Draw box
            canvas.drawRect(displayRect, boxPaint)

            // Draw label background
            val label = detection.label
            val labelWidth = labelTextPaint.measureText(label) + 16f
            val labelHeight = 40f
            val labelRect = RectF(left, top - labelHeight, left + labelWidth, top)
            canvas.drawRect(labelRect, labelBgPaint)

            // Draw label text
            canvas.drawText(label, left + 8f, top - 10f, labelTextPaint)
        }
    }
}
