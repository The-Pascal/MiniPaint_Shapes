package com.example.tempshapes

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin


enum class TOOL {
    PENCIL, ARROW, RECTANGLE, ELLIPSE;
}

data class Line(
    var startX: Float = 0f,
    var startY: Float = 0f,
    var stopX: Float = 0f,
    var stopY: Float = 0f,
)

private const val TAG = "MyCanvasView"

private const val STROKE_WIDTH = 10f

class MyCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var _tool = MutableLiveData<TOOL>()
    val tool: LiveData<TOOL>
        get() = _tool

    private var _showPaletteBool = MutableLiveData<Boolean>()
    val showPaletteBool: LiveData<Boolean>
        get() = _showPaletteBool

    private var _selectedColor = MutableLiveData<Int>()
    val selectedColor: LiveData<Int>
        get() = _selectedColor

    private lateinit var extraCanvas: Canvas
    private lateinit var extraBitmap: Bitmap

    private var motionTouchEventX = 0f
    private var motionTouchEventY = 0f

    private var currentX = 0f
    private var currentY = 0f

    private val backgroundColor = ResourcesCompat.getColor(resources, R.color.colorBackground, null)

    private val touchTolerance = ViewConfiguration.get(context).scaledTouchSlop

    private var tempRect: RectF = RectF()
    private var tempArrow = Line()

    // Set up the paint with which to draw.
    private val paint = Paint().apply {
        color = ResourcesCompat.getColor(resources, R.color.colorPaintBlack, null)
        // Smooths out edges of what is drawn without affecting shape.
        isAntiAlias = true
        // Dithering affects how colors with higher-precision than the device are down-sampled.
        isDither = true
        style = Paint.Style.STROKE // default: FILL
        strokeJoin = Paint.Join.ROUND // default: MITER
        strokeCap = Paint.Cap.ROUND // default: BUTT
        strokeWidth = STROKE_WIDTH // default: Hairline-width (really thin)
    }

    // Path for line
    private var path = Path()

    init {
        _tool.value = TOOL.PENCIL
        _showPaletteBool.value = false
        _selectedColor.value = ResourcesCompat.getColor(resources, R.color.colorPaintBlack, null)
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldheight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldheight)

        // recycle extra bitmap
        if (::extraBitmap.isInitialized) extraBitmap.recycle()
        extraBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        extraCanvas = Canvas(extraBitmap)
        extraCanvas.drawColor(backgroundColor)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(extraBitmap, 0f, 0f, null)

        // Handle different shapes
        when (tool.value) {
            TOOL.RECTANGLE -> canvas.drawRect(tempRect, paint)
            TOOL.ARROW -> drawArrowLine(canvas)
            TOOL.ELLIPSE -> canvas.drawOval(tempRect, paint)
            else -> Log.i(TAG, "onDraw: No shape")
        }
    }

    // draw arrow line - includes arrow head
    private fun drawArrowLine(canvas: Canvas) {
        val dx: Float = tempArrow.stopX - tempArrow.startX
        val dy: Float = tempArrow.stopY - tempArrow.startY
        val rad = atan2(dy.toDouble(), dx.toDouble()).toFloat()
        canvas.drawLine(
            tempArrow.startX,
            tempArrow.startY,
            tempArrow.stopX,
            tempArrow.stopY,
            paint
        )
        canvas.drawLine(
            tempArrow.stopX, tempArrow.stopY,
            (tempArrow.stopX + cos(rad + Math.PI * 0.75) * 20).toFloat(),
            (tempArrow.stopY + sin(rad + Math.PI * 0.75) * 20).toFloat(),
            paint
        )
        canvas.drawLine(
            tempArrow.stopX, tempArrow.stopY,
            (tempArrow.stopX + cos(rad - Math.PI * 0.75) * 20).toFloat(),
            (tempArrow.stopY + sin(rad - Math.PI * 0.75) * 20).toFloat(),
            paint
        )
    }

    // when user starts to draw
    private fun touchStart(x: Float, y: Float) {
        path.reset()
        path.moveTo(motionTouchEventX, motionTouchEventY)
        tempRect.apply {
            left = x
            top = y
            bottom = y
            right = x
        }
        tempArrow.apply {
            startX = x
            startY = y
            stopY = y
            stopX = x
        }
        currentX = motionTouchEventX
        currentY = motionTouchEventY
    }

    // when user is continuing drawing
    private fun touchMove() {
        val dx = abs(motionTouchEventX - currentX)
        val dy = abs(motionTouchEventY - currentY)
        if (dx >= touchTolerance || dy >= touchTolerance) {
            // QuadTo() adds a quadratic bezier from the last point,
            // approaching control point (x1,y1), and ending at (x2,y2).
            path.quadTo(
                currentX,
                currentY,
                (motionTouchEventX + currentX) / 2,
                (motionTouchEventY + currentY) / 2
            )
            currentX = motionTouchEventX
            currentY = motionTouchEventY

            // Draw the path in the extra bitmap to cache it.
            if (tool.value == TOOL.PENCIL) extraCanvas.drawPath(path, paint)

            tempRect.apply {
                bottom = (motionTouchEventY + currentY) / 2
                right = (motionTouchEventX + currentX) / 2
            }

            tempArrow.apply {
                stopY = (motionTouchEventY + currentY) / 2
                stopX = (motionTouchEventX + currentX) / 2
            }

        }
        invalidate()
    }

    // when user releases the touch
    private fun touchUp() {
        // cache on the extra canvas
        when (tool.value) {
            TOOL.RECTANGLE -> extraCanvas.drawRect(tempRect, paint)
            TOOL.ARROW -> drawArrowLine(extraCanvas)
            TOOL.ELLIPSE -> extraCanvas.drawOval(tempRect, paint)
            else -> Log.i(TAG, "touchUp: No shape")
        }
        // reset all previous references of drawings
        path.reset()
        tempRect = RectF()
        tempArrow = Line()
    }

    // Handle touch events
    @SuppressLint("ClickableViewAccessibility")
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

    fun selectColor(color: Int) {
        paint.color = color
        _selectedColor.value = color
        _showPaletteBool.value = false
    }

    fun showPalette() {
        _showPaletteBool.value = _showPaletteBool.value != true
    }

}