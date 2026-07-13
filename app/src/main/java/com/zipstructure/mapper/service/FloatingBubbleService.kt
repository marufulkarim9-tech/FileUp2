kotlin
package com.zipstructure.mapper.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.zipstructure.mapper.App
import com.zipstructure.mapper.MainActivity
import com.zipstructure.mapper.R
import com.zipstructure.mapper.ui.overlay.BubbleCollapsed
import com.zipstructure.mapper.ui.overlay.ExpandedExplorer
import com.zipstructure.mapper.ui.theme.ZipMapperTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.math.abs

class FloatingBubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private val lifecycleOwner = OverlayLifecycleOwner()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var explorer: ExplorerStateHolder

    private var bubbleView: ComposeView? = null
    private var expandedView: ComposeView? = null
    private lateinit var bubbleParams: WindowManager.LayoutParams

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        lifecycleOwner.onCreate()
        explorer = ExplorerStateHolder(this, serviceScope)
        startAsForeground()
        showBubble()
    }

    private fun startAsForeground() {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification: Notification = NotificationCompat.Builder(this, App.BUBBLE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bubble)
            .setContentTitle(getString(R.string.bubble_notification_title))
            .setContentText(getString(R.string.bubble_notification_text))
            .setContentIntent(pi)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    // -------------------- Collapsed bubble --------------------

    @SuppressLint("ClickableViewAccessibility")
    private fun showBubble() {
        removeExpanded()
        if (bubbleView != null) return

        bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 300
        }

        val view = createComposeView().apply {
            setContent { ZipMapperTheme { BubbleCollapsed() } }
        }

        var downX = 0f; var downY = 0f
        var startX = 0; var startY = 0
        var dragged = false

        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY
                    startX = bubbleParams.x; startY = bubbleParams.y
                    dragged = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (abs(dx) > 12 || abs(dy) > 12) dragged = true
                    bubbleParams.x = (startX + dx).toInt()
                    bubbleParams.y = (startY + dy).toInt()
                    windowManager.updateViewLayout(v, bubbleParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (dragged) snapToEdge(v) else showExpanded()
                    true
                }
                else -> false
            }
        }

        windowManager.addView(view, bubbleParams)
        bubbleView = view
    }

    /** Spring animation snapping the bubble to the nearest horizontal screen edge. */
    private fun snapToEdge(view: View) {
        val screenWidth = resources.displayMetrics.widthPixels
        val targetX = if (bubbleParams.x + view.width / 2 < screenWidth / 2) 0
                      else screenWidth - view.width
        SpringAnimation(FloatValueHolder(bubbleParams.x.toFloat())).apply {
            spring = SpringForce(targetX.toFloat()).apply {
                stiffness = SpringForce.STIFFNESS_LOW
                dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
            }
            addUpdateListener { _, value, _ ->
                bubbleParams.x = value.toInt()
                runCatching { windowManager.updateViewLayout(view, bubbleParams) }
            }
            start()
        }
    }

    // -------------------- Expanded explorer --------------------

    private fun showExpanded() {
        removeBubble()
        if (expandedView != null) return

        // Focusable so the search field can receive keyboard input while expanded.
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        val view = createComposeView().apply {
            setContent {
                ZipMapperTheme {
                    ExpandedExplorer(
                        holder = explorer,
                        onCollapse = { showBubble() },
                        onStopService = { stopSelf() }
                    )
                }
            }
        }
        windowManager.addView(view, params)
        expandedView = view
    }

    // -------------------- Compose view plumbing --------------------

    private fun createComposeView(): ComposeView = ComposeView(this).apply {
        setViewTreeLifecycleOwner(lifecycleOwner)
        setViewTreeViewModelStoreOwner(lifecycleOwner)
        setViewTreeSavedStateRegistryOwner(lifecycleOwner)
    }

    private fun removeBubble() {
        bubbleView?.let { runCatching { windowManager.removeView(it) } }
        bubbleView = null
    }

    private fun removeExpanded() {
        expandedView?.let { runCatching { windowManager.removeView(it) } }
        expandedView = null
    }

    override fun onDestroy() {