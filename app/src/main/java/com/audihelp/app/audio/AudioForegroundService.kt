package com.audihelp.app.audio

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.audihelp.app.MainActivity
import com.audihelp.app.R

/**
 * Keeps the process alive while audio processing is active.
 * Shows a persistent notification so the user knows the engine is running
 * and can stop it from the notification shade.
 *
 * This service does NOT start/stop the engine — the ViewModel does that.
 * Its only job is to hold a foreground slot so Android won't kill the process
 * when the screen goes off or the user navigates away.
 */
class AudioForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForegroundWithNotification()
            ACTION_STOP  -> { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
            else         -> stopSelf() // Restarted by OS after kill — don't auto-resume
        }
        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification() {
        val stopPending = PendingIntent.getService(
            this, 0,
            Intent(this, AudioForegroundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val openAppPending = PendingIntent.getActivity(
            this, 1,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Audihelp is running")
            .setContentText("Audio enhancement active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppPending)
            .addAction(0, "Stop", stopPending)
            .setOngoing(true)
            .setSilent(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    companion object {
        const val CHANNEL_ID  = "audihelp_audio"
        const val CHANNEL_NAME = "Audio Processing"
        const val NOTIF_ID    = 1
        const val ACTION_START = "com.audihelp.app.START_SERVICE"
        const val ACTION_STOP  = "com.audihelp.app.STOP_SERVICE"

        fun startIntent(context: Context) =
            Intent(context, AudioForegroundService::class.java).apply { action = ACTION_START }

        fun stopIntent(context: Context) =
            Intent(context, AudioForegroundService::class.java).apply { action = ACTION_STOP }
    }
}
