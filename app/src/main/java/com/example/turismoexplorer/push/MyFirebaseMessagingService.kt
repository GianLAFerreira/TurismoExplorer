package com.example.turismoexplorer.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.turismoexplorer.notify.NotificationUtils

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val type = data["type"] ?: return

        if (type == "nearby") {
            val name = data["name"] ?: "Ponto turístico"
            val lat = data["lat"]?.toDoubleOrNull() ?: return
            val lng = data["lng"]?.toDoubleOrNull() ?: return

            NotificationUtils.notifyNearby(
                context = this,
                notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                title = name,
                message = "Toque para abrir no mapa",
                targetLat = lat,
                targetLng = lng
            )
        }
    }

    override fun onNewToken(token: String) {
        // Envie o token para seu backend se necessário
    }
}