package com.arny.allfy.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings.Global.putString
import android.util.Log
import androidx.core.app.NotificationCompat
import com.arny.allfy.MainActivity
import com.arny.allfy.R
import com.arny.allfy.receivers.CallResponseReceiver
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessagingServiceImpl : FirebaseMessagingService() {
    companion object {
        const val CALL_NOTIFICATION_ID = 1
    }

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    private val stopEffectsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            stopRingingAndVibrating()
        }
    }

    override fun onCreate() {
        super.onCreate()
        ringtone = RingtoneManager.getRingtone(
            this,
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        )
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            registerReceiver(
                stopEffectsReceiver,
                IntentFilter("STOP_CALL_EFFECTS"),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(stopEffectsReceiver, IntentFilter("STOP_CALL_EFFECTS"))
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(stopEffectsReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver was not registered or already unregistered
            // Safe to ignore this exception
        }
//        stopRingingAndVibrating() // Also clean up audio/vibration
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data
        val notification = remoteMessage.notification

        when (data["type"]) {
            "call_invitation" -> {
                val callerId = data["callerId"] ?: return
                val calleeId = data["calleeId"] ?: return
                val callId = data["callId"] ?: return
                showCallNotification(callerId, calleeId, callId)
                startRingingAndVibrating()
            }

            else -> {
                val title = notification?.title ?: data["senderUsername"] ?: "New Message"
                val body = notification?.body ?: "You have a new message"
                sendMessageNotification(title, body)
            }
        }
    }

    private fun showCallNotification(callerId: String, calleeId: String, callId: String) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "call_notifications"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Call Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            action = "INCOMING_CALL"
            putExtra("callerId", callerId)
            putExtra("calleeId", calleeId)
            putExtra("callId", callId)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val acceptIntent = Intent(this, CallResponseReceiver::class.java).apply {
            action = "ACCEPT_CALL"
            putExtra("callerId", callerId)
            putExtra("calleeId", calleeId)
            putExtra("callId", callId)
        }
        val rejectIntent = Intent(this, CallResponseReceiver::class.java).apply {
            action = "REJECT_CALL"
            putExtra("callerId", callerId)
            putExtra("calleeId", calleeId)
            putExtra("callId", callId)
        }
        val acceptPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val rejectPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle("Incoming Call")
            .setContentText("From $callerId")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(R.drawable.ic_call, "Accept", acceptPendingIntent)
            .addAction(R.drawable.ic_call_end, "Reject", rejectPendingIntent)
            .setAutoCancel(false)
            .build()

        notificationManager.notify(CALL_NOTIFICATION_ID, notification)
    }

    private fun startRingingAndVibrating() {
        ringtone?.play()
        vibrator?.vibrate(longArrayOf(0, 1000, 500, 1000), 0)
    }

    private fun stopRingingAndVibrating() {
        ringtone?.let { if (it.isPlaying) it.stop() }
        vibrator?.cancel()
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(CALL_NOTIFICATION_ID)
    }

    private fun sendMessageNotification(title: String, message: String) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "chat_notifications"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Chat Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        Log.d("FCM", "Message notification sent: $title - $message")
    }
}