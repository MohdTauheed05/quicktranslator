package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
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
            setupFloatingBubble()
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
    private fun setupFloatingBubble() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 350
        }

        // Create bubble layout programmatically
        val bubbleContainer = FrameLayout(this).apply {
            val paddingPx = (10 * resources.displayMetrics.density).toInt()
            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
        }

        val iconView = ImageView(this).apply {
            val sizePx = (46 * resources.displayMetrics.density).toInt()
            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)
            setImageResource(R.mipmap.ic_launcher)
            contentDescription = "QuickTranslate Floating Bubble"
        }
        bubbleContainer.addView(iconView)
        floatingBubbleView = bubbleContainer

        // Dragging and Tap Handling
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var touchDownTime = 0L
        var isDrag = false
        val touchSlop = android.view.ViewConfiguration.get(this).scaledTouchSlop.coerceAtLeast(30)

        floatingBubbleView?.setOnTouchListener { _, event ->
            val params = layoutParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    touchDownTime = System.currentTimeMillis()
                    isDrag = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                        isDrag = true
                        params.x = initialX + dx
                        params.y = initialY + dy
                        try {
                            windowManager?.updateViewLayout(floatingBubbleView, params)
                        } catch (e: Exception) {
                            // ignore layout updates during drag
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val totalDx = abs(event.rawX - initialTouchX)
                    val totalDy = abs(event.rawY - initialTouchY)
                    val duration = System.currentTimeMillis() - touchDownTime
                    // If finger did not drag significantly OR duration was a quick tap (<350ms), trigger click!
                    if (!isDrag || (totalDx < touchSlop && totalDy < touchSlop) || duration < 350) {
                        onBubbleClicked()
                    }
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

    private fun onBubbleClicked() {
        try {
            floatingBubbleView?.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
        } catch (e: Exception) {
            // ignore
        }

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
            // Background clipboard access may be restricted on Android 10+; FloatingTranslateActivity will read on focus
        }

        val intent = Intent(this, FloatingTranslateActivity::class.java).apply {
            action = FloatingTranslateActivity.ACTION_TRANSLATE_CLIPBOARD
            putExtra("timestamp", System.currentTimeMillis())
            if (!clipboardText.isNullOrBlank()) {
                putExtra(FloatingTranslateActivity.EXTRA_TEXT, clipboardText)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        // Use PendingIntent first (bypasses Android 12-15 background start restrictions)
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
        } catch (e: Exception) {
            // PendingIntent send failed, fallback below
        }

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
                "QuickTranslate Floating Bubble",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows the QuickTranslate instant translation floating assistant"
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
            .setContentTitle("QuickTranslate Instant Assistant")
            .setContentText("Tap floating bubble or 'Translate Now' below")
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
            } catch (e: Exception) {
                // View might already be detached
            }
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
