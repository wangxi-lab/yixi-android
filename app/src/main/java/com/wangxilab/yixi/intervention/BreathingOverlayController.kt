package com.wangxilab.yixi.intervention

import android.accessibilityservice.AccessibilityService
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.wangxilab.yixi.domain.InterventionResult
import kotlin.math.min

class BreathingOverlayController(
    private val service: AccessibilityService,
    private val onResolved: (InterventionResult, Int) -> Unit,
) {
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private var rootView: View? = null
    private var countdown: CountDownTimer? = null
    private var resolved = false
    private var startedAt = 0L

    val isShowing: Boolean get() = rootView != null

    fun show(appLabel: String): Boolean {
        if (isShowing) return false
        resolved = false
        startedAt = System.currentTimeMillis()

        val root = FrameLayout(service).apply {
            setBackgroundColor(Color.rgb(246, 243, 236))
            isClickable = true
            isFocusable = true
        }
        val content = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), dp(64), dp(28), dp(40))
        }
        root.addView(
            content,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ).apply { gravity = Gravity.CENTER },
        )

        val eyebrow = TextView(service).apply {
            text = "打开 $appLabel 之前"
            textSize = 16f
            setTextColor(Color.rgb(84, 85, 80))
            gravity = Gravity.CENTER
        }
        val phase = TextView(service).apply {
            text = "吸气"
            textSize = 34f
            setTextColor(Color.rgb(35, 64, 53))
            gravity = Gravity.CENTER
            setPadding(0, dp(20), 0, dp(10))
        }
        val circle = BreathCircleView(service)
        val remaining = TextView(service).apply {
            text = "10.0 秒"
            textSize = 20f
            setTextColor(Color.rgb(49, 91, 75))
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(24))
        }
        val hint = TextView(service).apply {
            text = "先给自己一息的时间"
            textSize = 15f
            setTextColor(Color.rgb(100, 100, 94))
            gravity = Gravity.CENTER
        }
        val actions = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            visibility = View.INVISIBLE
            setPadding(0, dp(32), 0, 0)
        }
        val abandon = actionButton("算了", filled = true).apply {
            setOnClickListener {
                resolve(InterventionResult.ABANDONED)
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            }
        }
        val proceed = actionButton("继续打开", filled = false).apply {
            isEnabled = false
            alpha = 0f
            setOnClickListener { resolve(InterventionResult.PROCEEDED) }
        }
        actions.addView(abandon, buttonLayoutParams())
        actions.addView(proceed, buttonLayoutParams().apply { topMargin = dp(12) })

        content.addView(eyebrow, wrapLayoutParams())
        content.addView(phase, wrapLayoutParams())
        content.addView(circle, LinearLayout.LayoutParams(dp(210), dp(210)))
        content.addView(remaining, wrapLayoutParams())
        content.addView(hint, wrapLayoutParams())
        content.addView(
            actions,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ),
        )

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            title = "一息呼吸提醒"
        }

        return runCatching {
            windowManager.addView(root, params)
            rootView = root
            startCountdown(phase, circle, remaining, hint, actions, proceed)
            true
        }.getOrElse {
            rootView = null
            false
        }
    }

    fun resolve(result: InterventionResult) {
        if (!isShowing || resolved) return
        resolved = true
        val elapsed = ((System.currentTimeMillis() - startedAt) / 1000L).toInt()
        removeView()
        onResolved(result, elapsed)
    }

    fun destroy() {
        if (isShowing && !resolved) {
            resolved = true
            val elapsed = ((System.currentTimeMillis() - startedAt) / 1000L).toInt()
            removeView()
            onResolved(InterventionResult.TIMEOUT, elapsed)
        } else {
            removeView()
        }
    }

    private fun startCountdown(
        phase: TextView,
        circle: BreathCircleView,
        remaining: TextView,
        hint: TextView,
        actions: View,
        proceed: View,
    ) {
        countdown = object : CountDownTimer(BREATH_DURATION_MS, 50L) {
            override fun onTick(millisUntilFinished: Long) {
                val elapsed = BREATH_DURATION_MS - millisUntilFinished
                remaining.text = String.format("%.1f 秒", millisUntilFinished / 1000f)
                when {
                    elapsed < 4_000L -> {
                        phase.text = "吸气"
                        circle.fraction = elapsed / 4_000f
                    }
                    elapsed < 6_000L -> {
                        phase.text = "屏息"
                        circle.fraction = 1f
                    }
                    else -> {
                        phase.text = "呼气"
                        circle.fraction = 1f - (elapsed - 6_000L) / 4_000f
                    }
                }
            }

            override fun onFinish() {
                phase.text = "现在，重新选择"
                remaining.text = "10.0 秒已完成"
                hint.text = "你还想打开它吗？"
                circle.fraction = 0.25f
                actions.visibility = View.VISIBLE
                vibrateOnce()
                proceed.animate().alpha(1f).setDuration(250L).setStartDelay(800L)
                    .withStartAction { proceed.isEnabled = true }
                    .start()
            }
        }.start()
    }

    private fun removeView() {
        countdown?.cancel()
        countdown = null
        rootView?.let { view -> runCatching { windowManager.removeViewImmediate(view) } }
        rootView = null
    }

    private fun vibrateOnce() {
        val vibrator = service.getSystemService(Vibrator::class.java)
        if (vibrator?.hasVibrator() == true) {
            vibrator.vibrate(VibrationEffect.createOneShot(45L, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    private fun actionButton(label: String, filled: Boolean) = Button(service).apply {
        text = label
        textSize = 17f
        isAllCaps = false
        setTextColor(if (filled) Color.WHITE else Color.rgb(49, 91, 75))
        setBackgroundColor(if (filled) Color.rgb(49, 91, 75) else Color.TRANSPARENT)
        minHeight = dp(52)
    }

    private fun buttonLayoutParams() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        dp(56),
    )

    private fun wrapLayoutParams() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    )

    private fun dp(value: Int): Int = (value * service.resources.displayMetrics.density).toInt()

    companion object {
        private const val BREATH_DURATION_MS = 10_000L
    }
}

private class BreathCircleView(context: android.content.Context) : View(context) {
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(126, 166, 145) }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(49, 91, 75)
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density * 2f
    }
    var fraction: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val maxRadius = min(width, height) * 0.43f
        val radius = maxRadius * (0.55f + 0.45f * fraction)
        canvas.drawCircle(width / 2f, height / 2f, radius, fillPaint)
        canvas.drawCircle(width / 2f, height / 2f, maxRadius, ringPaint)
    }

}
