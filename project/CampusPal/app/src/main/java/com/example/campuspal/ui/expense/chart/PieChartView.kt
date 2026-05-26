package com.example.campuspal.ui.expense.chart

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View

class PieChartView(context: Context) : View(context) {

    private val defaultColors = intArrayOf(
        0xFFFF6B6B.toInt(), 0xFF4ECDC4.toInt(), 0xFFFFB347.toInt(),
        0xFF9B59B6.toInt(), 0xFF4A90D9.toInt(), 0xFF95A5A6.toInt(),
        0xFFF0775A.toInt(), 0xFF5B9ECF.toInt(), 0xFF6B9E8A.toInt(),
    )

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF333333.toInt(); textSize = sp(13f); typeface = Typeface.DEFAULT_BOLD }
    private val centerBigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF333333.toInt(); textSize = sp(16f); typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }
    private val centerSmallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF888888.toInt(); textSize = sp(10f); textAlign = Paint.Align.CENTER }
    private val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF333333.toInt(); textSize = sp(11f) }
    private val legendSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF888888.toInt(); textSize = sp(9f) }
    private val touchBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xDD333333.toInt() }
    private val touchTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = sp(11f); typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }
    private val slicePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val gapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.FILL }

    private var total = 0.0
    private var slices = mutableListOf<PieSliceData>()
    private var arcRects = mutableListOf<Float>() // startAngles
    private var sweepAngles = mutableListOf<Float>()
    private var arcBounds = RectF()
    private var selectedIndex = -1

    data class PieSliceData(val label: String, val value: Double, val color: Int)

    fun setData(slices: List<PieSliceData>) {
        this.slices = slices.toMutableList()
        total = slices.sumOf { it.value }
        selectedIndex = -1
        requestLayout()
        invalidate()
    }

    fun setDataFromChartSlices(chartSlices: List<PieSlice>) {
        val data = chartSlices.mapIndexed { i, s ->
            PieSliceData(s.label, s.value, defaultColors[i % defaultColors.size])
        }
        setData(data)
    }

    fun setTitle(t: String) { titlePaint.textSize = sp(13f); invalidate() }

    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val w = if (MeasureSpec.getMode(wSpec) == MeasureSpec.UNSPECIFIED) dp(320f).toInt()
        else MeasureSpec.getSize(wSpec)
        val h = if (MeasureSpec.getMode(hSpec) == MeasureSpec.UNSPECIFIED) dp(280f).toInt()
        else MeasureSpec.getSize(hSpec)
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (slices.isEmpty() || total <= 0) {
            canvas.drawText("暂无数据", width / 2f, height / 2f, centerSmallPaint)
            return
        }

        val w = width.toFloat(); val h = height.toFloat()
        val cx = w / 2f; val cy = h * 0.38f
        val radius = kotlin.math.min(w, h) * 0.22f
        val innerRadius = radius * 0.6f
        val gapDeg = 2f

        arcBounds = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        // 画扇形
        arcRects.clear(); sweepAngles.clear()
        var startAngle = -90f
        slices.forEachIndexed { i, slice ->
            val sweep = (slice.value / total * 360).toFloat() - gapDeg
            if (sweep > 0f) {
                slicePaint.color = slice.color
                val isSelected = selectedIndex == i
                val scale = if (isSelected) 1.06f else 1f
                val b = RectF(
                    cx - radius * scale, cy - radius * scale,
                    cx + radius * scale, cy + radius * scale,
                )
                canvas.drawArc(b, startAngle + gapDeg / 2, sweep, true, slicePaint)

                // 内圆遮罩（环形效果）
                gapPaint.color = 0xFFFFFFFF.toInt()
                canvas.drawCircle(cx, cy, innerRadius, gapPaint)

                arcRects.add(startAngle)
                sweepAngles.add(sweep + gapDeg)
            } else {
                arcRects.add(startAngle)
                sweepAngles.add(0f)
            }
            startAngle += (slice.value / total * 360).toFloat()
        }

        // 中心文字
        val totalStr = "¥%.0f".format(total)
        canvas.drawText(totalStr, cx, cy - sp(2f), centerBigPaint)
        canvas.drawText("总支出", cx, cy + sp(14f), centerSmallPaint)

        // 选中提示
        if (selectedIndex in slices.indices) {
            val s = slices[selectedIndex]
            val txt = "${s.label}: ¥%.0f".format(s.value)
            val tw = touchTextPaint.measureText(txt)
            val bw = tw + dp(14f); val bh = touchTextPaint.textSize + dp(10f)
            canvas.drawRoundRect(RectF(cx - bw / 2, cy + radius + dp(12f), cx + bw / 2, cy + radius + dp(12f) + bh), dp(6f), dp(6f), touchBgPaint)
            canvas.drawText(txt, cx, cy + radius + dp(12f) + bh / 2 + sp(3f), touchTextPaint)
        }

        // 图例（底部）
        val legendY = h - dp(16f) - (slices.size / 3 + if (slices.size % 3 > 0) 1 else 0) * dp(24f)
        val legendW = (w - dp(32f)) / 3f
        slices.forEachIndexed { i, slice ->
            val col = i % 3; val row = i / 3
            val lx = dp(16f) + col * legendW
            val ly = legendY + row * dp(24f)
            slicePaint.color = slice.color
            canvas.drawCircle(lx + dp(5f), ly + sp(5f), dp(4f), slicePaint)
            canvas.drawText(slice.label, lx + dp(14f), ly + sp(5f) + sp(3f), legendPaint)
            val pct = "${(slice.value / total * 100).toInt()}%"
            val pctW = legendSubPaint.measureText(pct)
            canvas.drawText(pct, lx + legendW - pctW, ly + sp(5f) + sp(3f), legendSubPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (slices.isEmpty() || total <= 0) return false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_UP -> {
                val cx = width / 2f; val cy = height * 0.38f
                val dx = event.x - cx; val dy = event.y - cy
                val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                val radius = kotlin.math.min(width, height) * 0.22f
                if (dist <= radius && dist >= radius * 0.6f) {
                    var angle = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    if (angle < -90f) angle += 360f
                    val touchAngle = (angle + 90f + 360f) % 360f
                    var cum = 0f
                    slices.forEachIndexed { i, s ->
                        cum += (s.value / total * 360).toFloat()
                        if (touchAngle <= cum) { selectedIndex = if (selectedIndex == i) -1 else i; invalidate(); return true }
                    }
                }
                selectedIndex = -1; invalidate()
            }
        }
        return true
    }

    private fun dp(v: Float) = v * resources.displayMetrics.density
    private fun sp(v: Float) = v * resources.displayMetrics.scaledDensity
}
