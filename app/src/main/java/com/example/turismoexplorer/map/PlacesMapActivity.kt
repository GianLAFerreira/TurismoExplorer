// Kotlin
package com.example.turismoexplorer.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.example.turismoexplorer.R
import com.example.turismoexplorer.data.places.NetworkModule
import com.example.turismoexplorer.data.places.PlacesRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import com.example.turismoexplorer.geo.GeofencingHelper


class PlacesMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null
    private val geofencingHelper by lazy { GeofencingHelper(this) }

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private val repository by lazy {
        PlacesRepository(
            api = NetworkModule.googlePlacesApi,
            apiKey = NetworkModule.apiKey
        )
    }

    private val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = result.values.any { it }
            if (granted) {
                enableMyLocation()
                fetchLocationAndSearch()
            } else {
                Toast.makeText(
                    this,
                    "Permissão de localização negada. Você pode buscar manualmente.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_places_map)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.topAppBar)
        toolbar?.setNavigationOnClickListener { finish() }

        // Se abriu por notificação/push com lat/lng, posiciona o mapa quando pronto
        intent?.let { handleDeepLinkIntent(it) }


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


        // Solicita permissão ao abrir
        requestPermissionLauncher.launch(locationPermissions)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMapToolbarEnabled = true
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(-14.2350, -51.9253), 4f))
        enableMyLocation()
        onMapReadyCallback?.invoke()
        onMapReadyCallback = null
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun enableMyLocation() {
        val map = googleMap ?: return
        if (!hasLocationPermission()) return
        try {
            map.isMyLocationEnabled = true
        } catch (_: SecurityException) {
            // Permissão pode ter sido revogada durante a execução
        }
    }

    private fun fetchLocationAndSearch() {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val cityInput = findViewById<EditText>(R.id.cityInput)

        if (!hasLocationPermission()) {
            Toast.makeText(
                this,
                "Permissão de localização negada. Faça a busca manual.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    lifecycleScope.launch {
                        try {
                            val latLng = if (location != null) {
                                LatLng(location.latitude, location.longitude)
                            } else {
                                // Fallback: centro do Brasil
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

                            // Preenche o campo e busca automaticamente
                            cityInput.setText(city)
                            buscarEExibir(city, progressBar)
                        } finally {
                            progressBar.visibility = View.GONE
                        }
                    }
                }
                .addOnFailureListener {
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this,
                        "Não foi possível obter a localização. Faça a busca manual.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } catch (_: SecurityException) {
            progressBar.visibility = View.GONE
            Toast.makeText(
                this,
                "Sem permissão de localização. Faça a busca manual.",
                Toast.LENGTH_SHORT
            ).show()
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

        // Registra geofences para os locais carregados
        geofencingHelper.registerGeofences(places)

        if (marcouAlgum) {
            map.setOnMapLoadedCallback {
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100))
                progressBar.visibility = View.GONE
            }
        } else {
            Toast.makeText(this, "Sem resultados para “$city”.", Toast.LENGTH_SHORT).show()
        }
    }
    private var onMapReadyCallback: (() -> Unit)? = null

    private fun handleDeepLinkIntent(intent: Intent) {
        val lat = intent.getDoubleExtra("target_lat", Double.NaN)
        val lng = intent.getDoubleExtra("target_lng", Double.NaN)
        val name = intent.getStringExtra("target_name") ?: "Local"

        if (!lat.isNaN() && !lng.isNaN()) {
            val action: () -> Unit = {
                val map = googleMap
                if (map != null) {
                    val target = LatLng(lat, lng)
                    map.clear()
                    map.addMarker(
                        MarkerOptions()
                            .position(target)
                            .title(name)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    )
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 16f))
                }
            }

            val map = googleMap
            if (map != null) {
                action()
            } else {
                onMapReadyCallback = action
            }
        }
    }
}