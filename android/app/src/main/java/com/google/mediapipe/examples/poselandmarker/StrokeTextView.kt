package com.google.mediapipe.examples.poselandmarker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class StrokeTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var strokeColor: Int = Color.BLACK
    private var strokeWidth: Float = 4f // Default stroke width in pixels

    private val strokePaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.StrokeTextView, 0, 0).apply {
            try {
                strokeColor = getColor(R.styleable.StrokeTextView_strokeColor, Color.BLACK)
                strokeWidth = getDimension(R.styleable.StrokeTextView_strokeWidth, 4f)
            } finally {
                recycle()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        val text = text.toString()

        // Calculate centered positions
        val xPos = (width / 2f)
        val yPos = (height / 2f) - (paint.descent() + paint.ascent()) / 2

        // Draw the stroke
        strokePaint.apply {
            color = strokeColor
            strokeWidth = this@StrokeTextView.strokeWidth
            textSize = paint.textSize
            typeface = paint.typeface
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(text, xPos, yPos, strokePaint)

        // Draw the fill text (restore original Paint settings)
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        paint.color = currentTextColor
        canvas.drawText(text, xPos, yPos, paint)
    }
}
