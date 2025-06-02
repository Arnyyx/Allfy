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
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessagingServiceImpl : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data
        val notification = remoteMessage.notification

        when (data["type"]) {
            "call" -> {
                val callerId = remoteMessage.data["callerId"] ?: return
                val conversationId = remoteMessage.data["conversationId"] ?: return
                showCallNotification(callerId, conversationId)
            }

            "message" -> {
                val title = notification?.title ?: data["senderUsername"] ?: "New Message"
                val body = notification?.body ?: "You have a new message"
                val conversationId = data["conversationId"] ?: return
                val otherUserId = data["otherUserId"] ?: return
                sendMessageNotification(title, body, conversationId, otherUserId)
            }

            else -> {
                val title = notification?.title ?: data["senderUsername"] ?: "New Message"
                val body = notification?.body ?: "You have a new message"
                sendMessageNotification(title, body)
            }
        }
    }

    private fun showCallNotification(callerId: String, conversationId: String) {
        val channelId = "call_channel"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Call Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("callerId", callerId)
            putExtra("conversationId", conversationId)
            putExtra("isIncomingCall", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_call)
            .setContentTitle("Cuộc gọi đến")
            .setContentText("Có cuộc gọi video từ room $conversationId")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }

    private fun sendMessageNotification(
        title: String,
        message: String,
        conversationId: String? = null,
        otherUserId: String? = null
    ) {
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

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("isChatNotification", true)
            putExtra("conversationId", conversationId)
            putExtra("otherUserId", otherUserId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        Log.d("FCM", "Message notification sent: $title - $message")
    }
}