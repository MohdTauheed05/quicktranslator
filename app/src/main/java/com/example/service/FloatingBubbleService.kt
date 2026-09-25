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
            // Rounded background
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
        var isDrag = false

        floatingBubbleView?.setOnTouchListener { _, event ->
            val params = layoutParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDrag = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 10 || abs(dy) > 10) {
                        isDrag = true
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager?.updateViewLayout(floatingBubbleView, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDrag) {
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
        // Read copied text from clipboard and open Instant Floating Translation dialog
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        var clipboardText: String? = null
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            val clipDesc = clipboard.primaryClipDescription
            if (clipDesc != null && clipDesc.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                val item = clipboard.primaryClip?.getItemAt(0)
                clipboardText = item?.text?.toString()?.trim()
            }
        }

        val intent = Intent(this, FloatingTranslateActivity::class.java).apply {
            action = FloatingTranslateActivity.ACTION_TRANSLATE_CLIPBOARD
            putExtra(FloatingTranslateActivity.EXTRA_TEXT, clipboardText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
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
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openAppIntent,
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
            .setContentText("Tap floating bubble anytime to translate WhatsApp messages")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingOpen)
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
