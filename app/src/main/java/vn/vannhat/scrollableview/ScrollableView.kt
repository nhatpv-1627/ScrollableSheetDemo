package vn.vannhat.scrollableview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.OverScroller
import kotlin.math.abs

class ScrollableView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var overScroller: OverScroller = OverScroller(context)
    private var minVelocity: Int = 0
    private var velocityTracker: VelocityTracker? = null
    private var lastTouchPoint = PointF()
    private var lastTouchId = -1
    private var isInteracting: Boolean = false
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
    }
    private var horizontalDividerRects = mutableListOf<Rect>()
    private var verticalDividerRects = mutableListOf<Rect>()

    // These configs should be the params for this view
    private var defaultSheetWidth = 2000
    private var defaultSheetHeight = 2000
    private val maxScrollDistance = 20
    private val horizontalDividerCount = 23
    private val verticalDividerCount = 30
    private val sheetPadding = 10
    private val cellHeight = 200
    private val cellWidth = 300
    private val scrollVelocity = 500


    init {
        overScrollMode = OVER_SCROLL_ALWAYS
        minVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity

        horizontalDividerRects = mutableListOf()
        for (i in 0 until horizontalDividerCount) {
            horizontalDividerRects.add(Rect())
        }
        verticalDividerRects = mutableListOf()
        for (i in 0 until verticalDividerCount) {
            verticalDividerRects.add(Rect())
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setHorizontalRects()
        setVerticalRects()
    }

    private fun setHorizontalRects() {
        for (i in horizontalDividerRects.indices) {
            val top: Int = sheetPadding + i * cellHeight
            val bottom: Int = top + cellHeight
            horizontalDividerRects[i].set(
                sheetPadding,
                top,
                verticalDividerRects.size * cellWidth + sheetPadding,
                bottom
            )
        }
        defaultSheetHeight = horizontalDividerRects.size * cellHeight + sheetPadding
    }

    private fun setVerticalRects() {
        for (i in verticalDividerRects.indices) {
            val start: Int = sheetPadding + i * cellWidth
            val end: Int = start + cellWidth
            verticalDividerRects[i].set(start, sheetPadding, end, defaultSheetHeight)
        }
        defaultSheetWidth = verticalDividerRects.size * cellWidth + sheetPadding
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (velocityTracker == null) velocityTracker = VelocityTracker.obtain()
        velocityTracker?.addMovement(event)
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!overScroller.isFinished.not()) overScroller.abortAnimation()
                lastTouchPoint.set(event.x, event.y)
                lastTouchId = event.getPointerId(0)
                isInteracting = true
            }
            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = event.findPointerIndex(lastTouchId)
                if (pointerIndex >= 0) {
                    val translatedX = event.getX(pointerIndex)
                    val translatedY = event.getY(pointerIndex)
                    this.overScrollBy(
                        (lastTouchPoint.x - translatedX).toInt(),
                        (lastTouchPoint.y - translatedY).toInt(),
                        scrollX,
                        scrollY,
                        defaultSheetWidth - width,
                        defaultSheetHeight - height,
                        maxScrollDistance,
                        maxScrollDistance,
                        true
                    )
                    lastTouchPoint.set(translatedX, translatedY)
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                val tracker = velocityTracker
                tracker?.computeCurrentVelocity(scrollVelocity)
                val initialXVelocity = tracker?.xVelocity ?: 0f
                val initialYVelocity = tracker?.yVelocity ?: 0f
                if (abs(initialXVelocity) + abs(initialYVelocity) > minVelocity) {
                    fling(-initialXVelocity.toInt(), -initialYVelocity.toInt())
                } else {
                    val isSpringBack = overScroller.springBack(
                        scrollX,
                        scrollY,
                        0,
                        defaultSheetWidth - width,
                        0,
                        defaultSheetHeight - height
                    )
                    if (isSpringBack) invalidate()
                    isInteracting = false
                }

                velocityTracker?.let {
                    it.recycle()
                    velocityTracker = null
                }
                lastTouchId = -1
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex: Int =
                    event.action and MotionEvent.ACTION_POINTER_INDEX_MASK shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
                val pointerId: Int = event.getPointerId(pointerIndex)
                if (pointerId == lastTouchId) {
                    val newPointIndex = if (pointerIndex == 0) 1 else 0
                    lastTouchPoint.set(event.getX(newPointIndex), event.getY(newPointIndex))
                    lastTouchId = event.getPointerId(newPointIndex)
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                val isSpringBack = overScroller.springBack(
                    scrollX, scrollY, 0, defaultSheetWidth - width, 0, defaultSheetHeight - height
                )
                if (isSpringBack) invalidate()
                lastTouchId = -1
            }
        }
        return true
    }

    override fun computeScroll() {
        if (overScroller.computeScrollOffset()) {
            val x = overScroller.currX
            val y = overScroller.currY
            if (scrollX != x || scrollY != y) {
                this.overScrollBy(
                    x - scrollX,
                    y - scrollY,
                    scrollX,
                    scrollY,
                    defaultSheetWidth - width,
                    defaultSheetHeight - height,
                    maxScrollDistance,
                    maxScrollDistance,
                    true
                )
            }
            if (overScroller.isFinished) isInteracting = false
            postInvalidate()
        }
    }

    private fun fling(velocityX: Int, velocityY: Int) {
        isInteracting = true
        overScroller.fling(
            scrollX,
            scrollY,
            velocityX,
            velocityY,
            0,
            defaultSheetWidth - width,
            0,
            defaultSheetHeight - height
        )
        invalidate()
    }

    override fun onOverScrolled(scrollX: Int, scrollY: Int, clampedX: Boolean, clampedY: Boolean) {
        if (!overScroller.isFinished && (clampedX || clampedY)) {
            overScroller.springBack(
                scrollX, scrollY, 0, defaultSheetWidth - width, 0, defaultSheetHeight - height
            )
        }
        super.scrollTo(scrollX, scrollY)
        awakenScrollBars()
    }

    override fun onDraw(canvas: Canvas?) {
        for (rect in horizontalDividerRects) {
            canvas?.drawRect(
                rect.left.toFloat(),
                rect.top.toFloat(),
                rect.right.toFloat(),
                rect.bottom.toFloat(),
                paint
            )
        }
        for (rect in verticalDividerRects) {
            canvas?.drawRect(
                rect.left.toFloat(),
                rect.top.toFloat(),
                rect.right.toFloat(),
                rect.bottom.toFloat(),
                paint
            )
        }
    }
}
