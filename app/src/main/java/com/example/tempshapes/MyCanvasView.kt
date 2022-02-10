package com.example.tempshapes

import android.content.Context
import android.graphics.*
import android.graphics.drawable.shapes.OvalShape
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

enum class TOOL {
    PENCIL, ARROW, RECTANGLE, ELLIPSE;
}

private const val TAG = "MyCanvasView"

private const val STROKE_WIDTH = 10f // has to be float

class MyCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var _tool = MutableLiveData<TOOL>()
    val tool: LiveData<TOOL>
        get() = _tool

    private lateinit var extraCanvas: Canvas
    private lateinit var extraBitmap: Bitmap

    private var motionTouchEventX = 0f
    private var motionTouchEventY = 0f

    private var currentX = 0f
    private var currentY = 0f

    private val backgroundColor = ResourcesCompat.getColor(resources, R.color.colorBackground, null)

    private val drawColor = ResourcesCompat.getColor(resources, R.color.colorPaint, null)

    private val touchTolerance = ViewConfiguration.get(context).scaledTouchSlop

    private var tempRect: RectF = RectF()

    // Set up the paint with which to draw.
    private val paint = Paint().apply {
        color = drawColor
        // Smooths out edges of what is drawn without affecting shape.
        isAntiAlias = true
        // Dithering affects how colors with higher-precision than the device are down-sampled.
        isDither = true
        style = Paint.Style.STROKE // default: FILL
        strokeJoin = Paint.Join.ROUND // default: MITER
        strokeCap = Paint.Cap.ROUND // default: BUTT
        strokeWidth = STROKE_WIDTH // default: Hairline-width (really thin)
    }

    private var path = Path()

    init {
        _tool.value = TOOL.PENCIL
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldheight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldheight)

        if (::extraBitmap.isInitialized) extraBitmap.recycle()
        extraBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        extraCanvas = Canvas(extraBitmap)
        extraCanvas.drawColor(backgroundColor)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(extraBitmap, 0f, 0f, null)
        // Draw a frame around the canvas.
        when (tool.value) {
            TOOL.RECTANGLE -> canvas.drawRect(tempRect, paint)
            TOOL.ARROW -> canvas.drawRect(tempRect, paint)
            TOOL.ELLIPSE -> canvas.drawOval(tempRect, paint)
            else -> Log.i(TAG, "onDraw: No shape")
        }
    }

    private fun touchStart(x: Float, y: Float) {
        path.reset()
        path.moveTo(motionTouchEventX, motionTouchEventY)
        tempRect.left = x
        tempRect.top = y
        tempRect.bottom = y
        tempRect.right = x
        currentX = motionTouchEventX
        currentY = motionTouchEventY
    }

    private fun touchMove() {
        val dx = Math.abs(motionTouchEventX - currentX)
        val dy = Math.abs(motionTouchEventY - currentY)
        if (dx >= touchTolerance || dy >= touchTolerance) {
            // QuadTo() adds a quadratic bezier from the last point,
            // approaching control point (x1,y1), and ending at (x2,y2).
            path.quadTo(currentX, currentY, (motionTouchEventX + currentX) / 2, (motionTouchEventY + currentY) / 2)
            currentX = motionTouchEventX
            currentY = motionTouchEventY
            // Draw the path in the extra bitmap to cache it.
            if(tool.value == TOOL.PENCIL)  extraCanvas.drawPath(path, paint)

            tempRect.bottom = (motionTouchEventY + currentY) / 2
            tempRect.right = (motionTouchEventX + currentX) / 2
        }
        invalidate()
    }

    private fun touchUp() {
        path.reset()
        Log.i(TAG, "touchUp: $tool")
        when (tool.value) {
            TOOL.RECTANGLE -> extraCanvas.drawRect(tempRect, paint)
            TOOL.ARROW -> extraCanvas.drawRect(tempRect, paint)
            TOOL.ELLIPSE -> extraCanvas.drawOval(tempRect, paint)
            else -> Log.i(TAG, "touchUp: No shape")
        }
        tempRect = RectF()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        motionTouchEventX = event.x
        motionTouchEventY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> touchStart(event.x, event.y)
            MotionEvent.ACTION_MOVE -> touchMove()
            MotionEvent.ACTION_UP -> touchUp()
        }
        return true
    }

    fun selectTool(toolParam: TOOL) {
        _tool.value = toolParam
    }

}