package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.ui.screens.FloatingTranslateActivity
import kotlin.math.abs

class FloatingBubbleService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingBubbleView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        if (Settings.canDrawOverlays(this)) {
            setupSamsungEdgeHandle()
        } else {
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSamsungEdgeHandle() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Samsung-style sleek edge handle dimensions
        val touchWidth = (24 * displayMetrics.density).toInt()
        val touchHeight = (92 * displayMetrics.density).toInt()
        val barWidth = (6 * displayMetrics.density).toInt()
        val barHeight = (78 * displayMetrics.density).toInt()

        val minY = (40 * displayMetrics.density).toInt()
        val maxY = (screenHeight - touchHeight - (70 * displayMetrics.density).toInt()).coerceAtLeast(minY)

        val rightEdgeX = screenWidth - touchWidth
        val leftEdgeX = 0

        layoutParams = WindowManager.LayoutParams(
            touchWidth,
            touchHeight,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = rightEdgeX // default to right edge (like Samsung One UI)
            y = (screenHeight * 0.38f).toInt().coerceIn(minY, maxY)
        }

        // Outer touchable transparent container
        val handleContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(touchWidth, touchHeight)
            setBackgroundColor(Color.TRANSPARENT)
        }

        // Visible Samsung Edge Handle Bar (slender pill bar with frosted accent)
        val barDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 14 * displayMetrics.density
            setColor(0xE62563EB.toInt()) // Modern translucent royal blue
            setStroke((1.5f * displayMetrics.density).toInt(), 0xCCFFFFFF.toInt()) // Crisp white border
        }

        val barView = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(barWidth, barHeight).apply {
                gravity = Gravity.CENTER
            }
            background = barDrawable
            elevation = 14f
        }
        handleContainer.addView(barView)
        floatingBubbleView = handleContainer

        // Dragging and Edge Pull Handling
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var touchDownTime = 0L
        var isVerticalDrag = false
        val touchSlop = android.view.ViewConfiguration.get(this).scaledTouchSlop.coerceAtLeast(20)

        floatingBubbleView?.setOnTouchListener { _, event ->
            val params = layoutParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    touchDownTime = System.currentTimeMillis()
                    isVerticalDrag = false
                    try {
                        floatingBubbleView?.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                    } catch (e: Exception) {}
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()

                    // If user is dragging vertically along the edge to change handle height
                    if (abs(dy) > touchSlop && abs(dy) > abs(dx)) {
                        isVerticalDrag = true
                        params.y = (initialY + dy).coerceIn(minY, maxY)
                        try {
                            windowManager?.updateViewLayout(floatingBubbleView, params)
                        } catch (e: Exception) {}
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    val duration = System.currentTimeMillis() - touchDownTime
                    val isRightEdge = params.x > screenWidth / 2

                    // Check if user pulled inward (Samsung edge gesture) or tapped
                    val pulledInward = (isRightEdge && dx < -20) || (!isRightEdge && dx > 20)
                    val isTap = !isVerticalDrag && (abs(dx) < touchSlop && abs(dy) < touchSlop) && duration < 380

                    if (pulledInward || isTap) {
                        onHandlePulled()
                    }

                    // Snap firmly to edge (Left or Right)
                    val targetX = if (event.rawX < screenWidth / 2) leftEdgeX else rightEdgeX
                    animateHandleToX(targetX)
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(floatingBubbleView, layoutParams)
        } catch (e: Exception) {
            stopSelf()
        }
    }

    private fun animateHandleToX(targetX: Int) {
        val params = layoutParams ?: return
        val startX = params.x
        if (startX == targetX) return

        val animator = android.animation.ValueAnimator.ofInt(startX, targetX)
        animator.duration = 180
        animator.interpolator = android.view.animation.DecelerateInterpolator()
        animator.addUpdateListener { va ->
            params.x = va.animatedValue as Int
            try {
                windowManager?.updateViewLayout(floatingBubbleView, params)
            } catch (e: Exception) {}
        }
        animator.start()
    }

    private fun onHandlePulled() {
        try {
            floatingBubbleView?.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (e: Exception) {}

        var clipboardText: String? = null
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                val clip = clipboard.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    clipboardText = clip.getItemAt(0)?.coerceToText(this)?.toString()?.trim()
                }
            }
        } catch (e: Exception) {
            // Background clipboard may be restricted on Android 10+; FloatingTranslateActivity will read on focus
        }

        val intent = Intent(this, FloatingTranslateActivity::class.java).apply {
            action = FloatingTranslateActivity.ACTION_TRANSLATE_CLIPBOARD
            putExtra("timestamp", System.currentTimeMillis())
            if (!clipboardText.isNullOrBlank()) {
                putExtra(FloatingTranslateActivity.EXTRA_TEXT, clipboardText)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        var launched = false
        try {
            val pendingIntent = PendingIntent.getActivity(
                this,
                (System.currentTimeMillis() % 10000).toInt(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val options = android.app.ActivityOptions.makeBasic().apply {
                    pendingIntentBackgroundActivityStartMode = android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                }
                pendingIntent.send(this, 0, null, null, null, null, options.toBundle())
            } else {
                pendingIntent.send()
            }
            launched = true
        } catch (e: Exception) {}

        if (!launched) {
            try {
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "QuickTranslate Edge Handle",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows the Samsung-style Edge pull handle for instant translation"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val translateIntent = Intent(this, FloatingTranslateActivity::class.java).apply {
            action = FloatingTranslateActivity.ACTION_TRANSLATE_CLIPBOARD
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingTranslate = PendingIntent.getActivity(
            this, 2, translateIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, FloatingBubbleService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val pendingStop = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("QuickTranslate Edge Handle Active")
            .setContentText("Pull or tap the edge handle on the screen to translate")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingTranslate)
            .addAction(android.R.drawable.ic_dialog_info, "Translate Now", pendingTranslate)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Turn Off", pendingStop)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatingBubbleView != null) {
            try {
                windowManager?.removeView(floatingBubbleView)
            } catch (e: Exception) {}
            floatingBubbleView = null
        }
    }

    companion object {
        const val CHANNEL_ID = "quicktranslate_bubble_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_SERVICE = "com.example.ACTION_STOP_BUBBLE"

        fun start(context: Context) {
            val intent = Intent(context, FloatingBubbleService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingBubbleService::class.java)
            context.stopService(intent)
        }
    }
}
