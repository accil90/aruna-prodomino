package id.aruna.domino

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Small user-controlled overlay. It stays hidden until a real recommendation
 * is supplied by the recognition/analyzer pipeline.
 */
class FloatingRecommendationWindow(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var panel: View? = null
    private var cardText: TextView? = null
    private var sideText: TextView? = null
    private var params: WindowManager.LayoutParams? = null

    fun showRecommendation(tile: String, sides: String) {
        if (panel == null) createPanel()
        cardText?.text = tile
        sideText?.text = sides
        panel?.visibility = View.VISIBLE
    }

    fun hide() {
        panel?.visibility = View.GONE
    }

    fun remove() {
        panel?.let { runCatching { windowManager.removeView(it) } }
        panel = null
        cardText = null
        sideText = null
        params = null
    }

    private fun createPanel() {
        val card = TextView(context).apply {
            textSize = 25f
            setTextColor(0xFF111111.toInt())
            gravity = Gravity.CENTER
            setPadding(18, 12, 18, 12)
            background = GradientDrawable().apply {
                setColor(0xFFFFFFFF.toInt())
                setStroke(3, 0xFF111111.toInt())
                cornerRadius = 12f
            }
        }
        val sides = TextView(context).apply {
            textSize = 11f
            setTextColor(0xFF222222.toInt())
            gravity = Gravity.CENTER
            setPadding(4, 2, 4, 4)
        }
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(4, 4, 4, 4)
            background = GradientDrawable().apply {
                setColor(0xEEFFFFFF.toInt())
                cornerRadius = 14f
            }
            elevation = 10f
            addView(card, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))
            addView(sides, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))
        }

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val layout = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 18
            y = 220
        }

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = layout.x
                    startY = layout.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layout.x = (startX + touchX - event.rawX).toInt().coerceAtLeast(0)
                    layout.y = (startY + event.rawY - touchY).toInt().coerceAtLeast(0)
                    runCatching { windowManager.updateViewLayout(container, layout) }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(container, layout)
        container.visibility = View.GONE
        panel = container
        cardText = card
        sideText = sides
        params = layout
    }
}
