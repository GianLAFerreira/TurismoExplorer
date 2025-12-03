package com.example.turismoexplorer.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.turismoexplorer.R
import com.example.turismoexplorer.map.PlacesMapActivity
import com.example.turismoexplorer.data.notifications.NotificationsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


object NotificationUtils {
    private const val CHANNEL_ID = "nearby_places_channel"
    private const val CHANNEL_NAME = "Pontos turísticos próximos"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // IMPORTANCE_HIGH para notificação mais visível (heads-up em muitos casos)
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos quando você estiver próximo a um ponto turístico"
                enableLights(true)
                lightColor = Color.BLUE
            }
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun canPostNotifications(context: Context): Boolean {
        val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (!enabled) return false
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun notifyNearby(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        targetLat: Double,
        targetLng: Double
    ) {
        ensureChannel(context)

        if (!canPostNotifications(context)) {
            Log.w("NotificationUtils", "Bloqueado: app sem permissão/permite notificações desativadas")
            // Mesmo sem permissão, salvamos no inbox interno
            CoroutineScope(Dispatchers.IO).launch {
                NotificationsRepository(context).add(title, message, targetLat, targetLng)
            }
            return
        }

        val intent = Intent(context, PlacesMapActivity::class.java).apply {
            putExtra("target_lat", targetLat)
            putExtra("target_lng", targetLng)
            putExtra("target_name", title.replace("Você está próximo do ponto turístico ", ""))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pending = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notif)
        } catch (se: SecurityException) {
            Log.w("NotificationUtils", "SecurityException ao notificar", se)
        } finally {
            // Sempre salva no inbox interno
            CoroutineScope(Dispatchers.IO).launch {
                NotificationsRepository(context).add(title, message, targetLat, targetLng)
            }
        }
    }
}