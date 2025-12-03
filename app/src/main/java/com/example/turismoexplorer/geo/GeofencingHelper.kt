package com.example.turismoexplorer.geo

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.turismoexplorer.domain.Place
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofencingHelper(private val context: Context) {

    private val client: GeofencingClient = LocationServices.getGeofencingClient(context)

    private fun geofencePendingIntent(): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }


    fun clearAll(onDone: (() -> Unit)? = null) {
        if (!hasPermission()) {
            Log.w("GeofencingHelper", "Sem permissão para remover geofences")
            onDone?.invoke()
            return
        }
        try {
            client.removeGeofences(geofencePendingIntent())
                .addOnCompleteListener {
                    Log.d("GeofencingHelper", "Geofences removidas (se existiam)")
                    onDone?.invoke()
                }
        } catch (se: SecurityException) {
            Log.w("GeofencingHelper", "SecurityException ao remover geofences", se)
            onDone?.invoke()
        }
    }

    private fun hasPermission(): Boolean {
        val fine = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun registerGeofences(
        places: List<Place>,
        radiusMeters: Float = 400f,            // raio um pouco maior para facilitar testes
        loiteringDelayMs: Int = 10_000         // 10s para DWELL
    ) {
        if (!hasPermission()) {
            Log.w("GeofencingHelper", "Sem permissão de localização para registrar geofences")
            return
        }

        val geofences = places.mapNotNull { p ->
            val lat = p.lat ?: return@mapNotNull null
            val lng = p.lng ?: return@mapNotNull null
            val requestId = "${p.id}::${p.name}" // carrega ID e Nome no requestId
            Geofence.Builder()
                .setRequestId(requestId)
                .setCircularRegion(lat, lng, radiusMeters)
                .setTransitionTypes(
                    Geofence.GEOFENCE_TRANSITION_ENTER or
                            Geofence.GEOFENCE_TRANSITION_DWELL or
                            Geofence.GEOFENCE_TRANSITION_EXIT
                )
                .setLoiteringDelay(loiteringDelayMs)
                .setExpirationDuration(12 * 60 * 60 * 1000L)
                .build()
        }

        if (geofences.isEmpty()) {
            Log.w("GeofencingHelper", "Nenhuma geofence válida para registrar")
            return
        }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()

        // Limpa anteriores e registra de novo (evita ruídos)
        clearAll {
            try {
                client.addGeofences(request, geofencePendingIntent())
                    .addOnSuccessListener {
                        Log.d("GeofencingHelper", "Geofences registradas: ${geofences.size}")
                        Toast.makeText(context, "Geofences registradas (${geofences.size})", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e("GeofencingHelper", "Falha ao registrar geofences: ${e.message}", e)
                        Toast.makeText(context, "Falha ao registrar geofences", Toast.LENGTH_SHORT).show()
                    }
            } catch (se: SecurityException) {
                Log.w("GeofencingHelper", "SecurityException ao registrar geofences (sem permissão?)", se)
            }
        }
    }
}