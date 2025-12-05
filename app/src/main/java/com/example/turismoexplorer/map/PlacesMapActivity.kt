package com.example.turismoexplorer.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.example.turismoexplorer.R
import com.example.turismoexplorer.data.places.NetworkModule
import com.example.turismoexplorer.data.places.PlacesRepository
import com.example.turismoexplorer.geo.GeofencingHelper
import com.google.android.gms.location.*
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class PlacesMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null

    private val repository by lazy {
        PlacesRepository(
            api = NetworkModule.googlePlacesApi,
            apiKey = NetworkModule.apiKey
        )
    }
    private val geofencingHelper by lazy { GeofencingHelper(this) }
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private val requestFinePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            enableMyLocation()
            fetchLocationAndSearch()
        } else {
            showPreciseLocationRequiredDialog()
        }
    }

    private val requestBackgroundPermissionApi29 = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            showBackgroundLocationRequiredDialog()
        }
    }

    private val requestLocationPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                enableMyLocation()
                fetchLocationAndSearch()
            } else {
                Toast.makeText(this, "Permissão de localização negada. Faça a busca manual.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_places_map)

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        toolbar.setNavigationOnClickListener { finish() }

        val mapFragment = SupportMapFragment.newInstance()
        supportFragmentManager.commit {
            replace(R.id.mapContainer, mapFragment)
        }
        mapFragment.getMapAsync(this)

        val cityInput = findViewById<EditText>(R.id.cityInput)
        val loadButton = findViewById<Button>(R.id.loadButton)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        loadButton.setOnClickListener {
            val city = cityInput.text?.toString()?.trim().orEmpty()
            if (city.isEmpty()) {
                Toast.makeText(this, "Informe uma cidade", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            progressBar.visibility = View.VISIBLE
            lifecycleScope.launch {
                try {
                    buscarEExibir(city, progressBar)
                } finally {
                    progressBar.visibility = View.GONE
                }
            }
        }

        // Permissões iniciais (foreground)
        requestLocationPermissions.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMapToolbarEnabled = true
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(-14.2350, -51.9253), 4f))
        enableMyLocation()

        // DEBUG: long press cria uma geofence única no ponto clicado e força fix
        map.setOnMapLongClickListener { latLng ->
            if (!hasFinePermission()) {
                Toast.makeText(this, "Conceda localização precisa para criar geofence", Toast.LENGTH_SHORT).show()
                return@setOnMapLongClickListener
            }
            val testPlace = listOf(
                com.example.turismoexplorer.domain.Place(
                    id = "debug:${latLng.latitude},${latLng.longitude}",
                    name = "Ponto de teste",
                    address = "Long press",
                    rating = null,
                    lat = latLng.latitude,
                    lng = latLng.longitude
                )
            )
            geofencingHelper.registerGeofences(testPlace, radiusMeters = 400f, loiteringDelayMs = 5_000)
            startTemporaryLocationUpdates()   // força “fix”
            forceGetCurrentLocation()        // força “fix” imediato
            Toast.makeText(this, "Geofence de teste criada. Aguarde alguns segundos.", Toast.LENGTH_SHORT).show()
        }
    }

    // ------------ Permissões ------------
    private fun hasFinePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasBackgroundPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun requireFineForGeofencing(onReady: () -> Unit) {
        if (hasFinePermission()) {
            onReady()
            return
        }
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            AlertDialog.Builder(this)
                .setTitle("Ativar localização precisa")
                .setMessage("Para avisar quando você estiver próximo a um ponto turístico, precisamos da localização precisa.")
                .setPositiveButton("Permitir") { _, _ ->
                    requestFinePermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        } else {
            requestFinePermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun requireBackgroundForGeofencing(onReady: () -> Unit) {
        if (hasBackgroundPermission()) {
            onReady()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            showBackgroundLocationRequiredDialog()
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            requestBackgroundPermissionApi29.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            onReady()
        }
    }

    private fun showPreciseLocationRequiredDialog() {
        AlertDialog.Builder(this)
            .setTitle("Localização precisa necessária")
            .setMessage("Para alertas de proximidade, ative a Localização precisa nas permissões do aplicativo.")
            .setPositiveButton("Abrir configurações") { _, _ -> openAppSettings() }
            .setNegativeButton("Agora não", null)
            .show()
    }

    private fun showBackgroundLocationRequiredDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permitir localização em segundo plano")
            .setMessage("Para receber alertas mesmo com o app em segundo plano, permita 'Sempre permitir' em Localização.")
            .setPositiveButton("Abrir configurações") { _, _ -> openAppSettings() }
            .setNegativeButton("Agora não", null)
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
    // ------------ fim permissões ------------

    private fun enableMyLocation() {
        val map = googleMap ?: return
        if (!hasFinePermission()) return
        try {
            map.isMyLocationEnabled = true
        } catch (_: SecurityException) { }
    }

    private fun fetchLocationAndSearch() {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val cityInput = findViewById<EditText>(R.id.cityInput)

        if (!hasFinePermission()) return

        progressBar.visibility = View.VISIBLE
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    lifecycleScope.launch {
                        try {
                            val latLng = if (location != null) {
                                LatLng(location.latitude, location.longitude)
                            } else {
                                LatLng(-14.2350, -51.9253)
                            }

                            val city = withContext(Dispatchers.IO) {
                                try {
                                    val geocoder = Geocoder(this@PlacesMapActivity, Locale("pt", "BR"))
                                    val list = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                                    val addr = list?.firstOrNull()
                                    val locality = addr?.locality ?: addr?.subAdminArea
                                    val country = addr?.countryName ?: "Brazil"
                                    if (!locality.isNullOrBlank()) "$locality, $country" else null
                                } catch (_: Exception) {
                                    null
                                }
                            } ?: "Brazil"

                            cityInput.setText(city)
                            buscarEExibir(city, progressBar)
                        } finally {
                            progressBar.visibility = View.GONE
                        }
                    }
                }
                .addOnFailureListener {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Não foi possível obter a localização. Faça a busca manual.", Toast.LENGTH_SHORT).show()
                }
        } catch (_: SecurityException) {
            progressBar.visibility = View.GONE
            Toast.makeText(this, "Sem permissão de localização. Faça a busca manual.", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun buscarEExibir(city: String, progressBar: ProgressBar) {
        val map = googleMap ?: run {
            Toast.makeText(this, "Mapa ainda não está pronto", Toast.LENGTH_SHORT).show()
            return
        }

        val places = withContext(Dispatchers.IO) {
            repository.popularByCity(city, limit = 30)
        }

        map.clear()
        val bounds = LatLngBounds.Builder()
        var marcouAlgum = false
        places.forEach { p ->
            val lat = p.lat; val lng = p.lng
            if (lat != null && lng != null) {
                val pos = LatLng(lat, lng)
                map.addMarker(
                    MarkerOptions()
                        .position(pos)
                        .title(p.name)
                        .snippet(p.address)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                )
                bounds.include(pos)
                marcouAlgum = true
            }
        }

        // Precisão + background antes de registrar geofences
        requireFineForGeofencing {
            requireBackgroundForGeofencing {
                geofencingHelper.registerGeofences(places)
                // Força “fix” de localização para acionar o processamento de cercas
                startTemporaryLocationUpdates()
                forceGetCurrentLocation()
            }
        }

        if (marcouAlgum) {
            map.setOnMapLoadedCallback {
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100))
                progressBar.visibility = View.GONE
            }
        } else {
            Toast.makeText(this, "Sem resultados para “$city”.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startTemporaryLocationUpdates() {
        if (!hasFinePermission()) return

        val request = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
                .setWaitForAccurateLocation(true)
                .setMinUpdateIntervalMillis(1000L)
                .setMaxUpdates(10)
                .build()
        } else {
            @Suppress("DEPRECATION")
            LocationRequest.create().apply {
                interval = 2000L
                fastestInterval = 1000L
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                numUpdates = 10
            }
        }

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                // Sem ação: apenas acorda o provider
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                callback,
                Looper.getMainLooper()
            )
            window.decorView.postDelayed({
                fusedLocationClient.removeLocationUpdates(callback)
            }, 15_000L)
        } catch (_: SecurityException) { }
    }

    private fun forceGetCurrentLocation() {
        if (!hasFinePermission()) return
        val cts = CancellationTokenSource()
        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cts.token
            ).addOnSuccessListener {
                // Apenas solicitar já ajuda a disparar avaliação de cercas
            }
        } catch (_: SecurityException) { }
    }
}