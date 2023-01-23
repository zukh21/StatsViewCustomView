package kg.zim.statsview.ui

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.withStyledAttributes
import kg.zim.statsview.R
import kg.zim.statsview.utils.AndroidUtils
import kotlin.math.min
import kotlin.random.Random

class StatsView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : View(context, attributeSet, defStyleAttr, defStyleRes) {
    private var progress: Float = 0F
    private var rotate: Float = 0F
    private var valueAnimator: ValueAnimator? = null
    var data: List<Float> = emptyList()
        set(value) {
            field = value
            anim()
        }
    private var radius = 0F
    private var center = PointF()
    private var textSize = AndroidUtils.dp(context, 20).toFloat()
    private var lineWidth = AndroidUtils.dp(context, 5)
    private var oval = RectF()
    private val durationValue = 3000L
    private var colors = emptyList<Int>()
    private var filledType: FilledType? = null

    init {
        context.withStyledAttributes(attributeSet, R.styleable.StatsView) {
            textSize = getDimension(R.styleable.StatsView_textSize, textSize)
            lineWidth = getDimension(R.styleable.StatsView_lineWidth, lineWidth.toFloat()).toInt()
            filledType = FilledType.values()[getInt(R.styleable.StatsView_filledType, 1)]
            colors = listOf(
                getColor(R.styleable.StatsView_color1, generateRandomColor()),
                getColor(R.styleable.StatsView_color2, generateRandomColor()),
                getColor(R.styleable.StatsView_color3, generateRandomColor()),
                getColor(R.styleable.StatsView_color4, generateRandomColor())
            )
        }
    }

    private val paint = Paint(
        Paint.ANTI_ALIAS_FLAG,
    ).apply {
        strokeWidth = lineWidth.toFloat()
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.MITER
        strokeCap = Paint.Cap.ROUND
    }
    private val textPaint = Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        textSize = this@StatsView.textSize
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        radius = min(w, h) / 2F - AndroidUtils.dp(context = context, 5)
        center = PointF(w / 2F, h / 2F)
        oval = RectF(
            center.x - radius,
            center.y - radius,
            center.x + radius,
            center.y + radius
        )
    }

    override fun onDraw(canvas: Canvas) {
        if (data.isEmpty()) {
            return
        }
        val progressAngle = progress * 360F
        var starAngle = -90F
        val count: Float = data.sum() / data.sum() * .25F
        val max = (count * data.size) * 360F
        val angle = count * 360F
        if (filledType == FilledType.sequential) {
            if (progressAngle > max) {
                data.forEachIndexed { index, _ ->
                    paint.color = colors.getOrElse(index) { generateRandomColor() }
                    canvas.drawArc(oval, starAngle, angle, false, paint)
                    starAngle += angle
                }
            } else {
                var filled = 0F
                for ((index, _) in data.withIndex()) {
                    paint.color = colors.getOrElse(index) { generateRandomColor() }
                    canvas.drawArc(oval, starAngle, progressAngle - filled, false, paint)
                    starAngle += angle
                    filled += angle
                    if (filled > progressAngle) {
                        break
                    }
                }
            }
            canvas.drawText(
                "%.2f%%".format((count * data.size) * 100),
                center.x,
                center.y + textPaint.textSize / 4,
                textPaint
            )
        } else if (filledType == FilledType.parallel) {
            data.forEachIndexed { index, _ ->
                paint.color = colors.getOrElse(index) { generateRandomColor() }
                canvas.drawArc(oval, starAngle, angle * progress, false, paint)
                starAngle += angle
            }
            canvas.drawText(
                "%.2f%%".format((count * data.size) * 100),
                center.x,
                center.y + textPaint.textSize / 4,
                textPaint
            )
        }
    }

    private fun anim() {
        progress = 0F
        rotate = 0F
        valueAnimator = ValueAnimator.ofFloat(0F, 1F).apply {
            addUpdateListener { anim ->
                progress = anim.animatedValue as Float
            }
            interpolator = LinearInterpolator()
            duration = durationValue
        }.also {
            it.start()
        }

        ValueAnimator.ofFloat(360F).apply {
            addUpdateListener { anim ->
                rotate = anim.animatedValue as Float
                invalidate()
            }

            interpolator = LinearInterpolator()
            duration = durationValue
        }.also {
            it.start()
        }
    }

    private fun generateRandomColor() = Random.nextInt(0xFF000000.toInt(), 0xFFFFFFFF.toInt())
}

enum class FilledType {
    sequential,
    parallel
}