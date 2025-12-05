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
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
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
        val fineGranted = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        Log.d("GeofencingHelper", "Permissões: FINE=$fineGranted COARSE=$coarseGranted")
        return fineGranted
    }

    fun registerGeofences(
        places: List<Place>,
        radiusMeters: Float = 500f,
        loiteringDelayMs: Int = 10_000
    ) {
        if (!hasPermission()) {
            Log.w("GeofencingHelper", "Sem permissão (ACCESS_FINE_LOCATION) para registrar geofences")
            Toast.makeText(context, "Permissão de localização precisa é necessária", Toast.LENGTH_SHORT).show()
            return
        }

        val safePlaces = places.filter { it.lat != null && it.lng != null }.take(95)

        val geofences = safePlaces.map { p ->
            val requestId = "${p.id}::${p.name}"
            Geofence.Builder()
                .setRequestId(requestId)
                .setCircularRegion(p.lat!!, p.lng!!, radiusMeters)
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
            Toast.makeText(context, "Nenhum ponto com coordenadas válidas", Toast.LENGTH_SHORT).show()
            return
        }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()

        clearAll {
            try {
                client.addGeofences(request, geofencePendingIntent())
                    .addOnSuccessListener {
                        Log.d("GeofencingHelper", "Geofences registradas: ${geofences.size}")
                        Toast.makeText(context, "Geofences registradas (${geofences.size})", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        val code = (e as? ApiException)?.statusCode
                        val codeMsg = if (code != null) GeofenceStatusCodes.getStatusCodeString(code) else "UNKNOWN"
                        Log.e("GeofencingHelper", "Falha ao registrar geofences: code=$code ($codeMsg)", e)
                        Toast.makeText(context, "Falha ao registrar geofences ($codeMsg)", Toast.LENGTH_SHORT).show()
                    }
            } catch (se: SecurityException) {
                Log.w("GeofencingHelper", "SecurityException ao registrar geofences (sem permissão?)", se)
            }
        }
    }
}