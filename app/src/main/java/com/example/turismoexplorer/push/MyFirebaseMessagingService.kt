package com.example.turismoexplorer.push

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.turismoexplorer.notify.NotificationUtils

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d("FCM", "onMessageReceived from=${message.from} data=${message.data}")
        val data = message.data
        val type = data["type"] ?: return

        if (type == "nearby") {
            val name = data["name"] ?: "Ponto turístico"
            val lat = data["lat"]?.toDoubleOrNull() ?: return
            val lng = data["lng"]?.toDoubleOrNull() ?: return

            NotificationUtils.notifyNearby(
                context = this,
                notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                title = "Você está próximo do ponto turístico $name",
                message = "Toque para abrir no mapa",
                targetLat = lat,
                targetLng = lng
            )
        }
    }

    override fun onNewToken(token: String) {
        Log.d("FCM", "Novo token: $token")
        // Envie esse token ao seu backend, se necessário.
    }
}