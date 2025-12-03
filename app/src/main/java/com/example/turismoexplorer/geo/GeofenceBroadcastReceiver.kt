package com.example.turismoexplorer.geo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.turismoexplorer.notify.NotificationUtils
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            val code = event.errorCode
            val msg = GeofenceStatusCodes.getStatusCodeString(code)
            Log.e("GeofenceReceiver", "Erro no GeofencingEvent: $code ($msg)")
            return
        }

        val transition = event.geofenceTransition
        val geofences = event.triggeringGeofences.orEmpty()
        val ids = geofences.map { it.requestId }
        Log.d("GeofenceReceiver", "Transição=$transition geofences=$ids")

        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            transition == Geofence.GEOFENCE_TRANSITION_DWELL) {

            val loc = event.triggeringLocation
            if (loc != null && geofences.isNotEmpty()) {
                // Usa o primeiro geofence que disparou
                val requestId = geofences.first().requestId
                val (placeId, placeName) = parseRequestId(requestId)

                val title = if (placeName.isNullOrBlank())
                    "Você está próximo de um ponto turístico"
                else
                    "Você está próximo do ponto turístico ${placeName}"

                val message = "Toque para ver no mapa"

                NotificationUtils.notifyNearby(
                    context = context,
                    notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                    title = title,
                    message = message,
                    targetLat = loc.latitude,
                    targetLng = loc.longitude
                )
                Log.d("GeofenceReceiver", "Notificação enviada: $title (lat=${loc.latitude}, lng=${loc.longitude})")
            } else {
                Log.w("GeofenceReceiver", "triggeringLocation ou geofences vazio")
            }
        }
    }

    private fun parseRequestId(requestId: String): Pair<String?, String?> {
        // Formato esperado: "<placeId>::<placeName>"
        val parts = requestId.split("::", limit = 2)
        val id = parts.getOrNull(0)
        val name = parts.getOrNull(1)
        return id to name
    }
}
