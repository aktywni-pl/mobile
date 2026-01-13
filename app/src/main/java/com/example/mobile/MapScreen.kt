package com.example.mobile

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.mobile.network.CreateActivityRequest
import com.example.mobile.network.RetrofitInstance
import com.example.mobile.network.TrackPoint
import com.example.mobile.network.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.time.LocalDateTime

@SuppressLint("MissingPermission")
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isRecording by remember { mutableStateOf(false) }
    var routePoints = remember { mutableStateListOf<GeoPoint>() }
    var trackData = remember { mutableStateListOf<TrackPoint>() }

    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    var mapView by remember { mutableStateOf<MapView?>(null) }

    val locationListener = remember {
        object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (isRecording) {
                    val lat = location.latitude
                    val lon = location.longitude

                    if (lat == 0.0 && lon == 0.0) return

                    val point = org.osmdroid.util.GeoPoint(lat, lon)
                    routePoints.add(point)

                    val now = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        java.time.Instant.now().toString()
                    } else {
                        java.util.Date().toString()
                    }


                    trackData.add(
                        com.example.mobile.network.TrackPoint(
                            lat = lat,
                            lon = lon,
                            timestamp = now
                        )
                    )

                    mapView?.overlays?.filterIsInstance<org.osmdroid.views.overlay.Polyline>()?.firstOrNull()?.let { line ->
                        line.addPoint(point)
                    } ?: run {
                        val line = org.osmdroid.views.overlay.Polyline()

                        line.outlinePaint.color = android.graphics.Color.RED
                        line.outlinePaint.strokeWidth = 15f
                        line.addPoint(point)
                        mapView?.overlays?.add(line)
                    }
                    mapView?.invalidate()
                }
            }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }
    }

    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    fun startRecording() {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }
        routePoints.clear()
        trackData.clear()

        mapView?.overlays?.removeIf { it is Polyline }
        mapView?.invalidate()

        isRecording = true
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 5f, locationListener)
        Toast.makeText(context, "Start!", Toast.LENGTH_SHORT).show()
    }

    fun stopAndSave() {
        isRecording = false
        try {
            locationManager.removeUpdates(locationListener)
        } catch (e: Exception) { e.printStackTrace() }

        val pointCount = trackData.size
        var localDistanceMeters = 0.0

        if (pointCount > 1) {
            for (i in 0 until pointCount - 1) {
                val p1 = trackData[i]
                val p2 = trackData[i+1]
                val results = FloatArray(1)
                android.location.Location.distanceBetween(
                    p1.lat, p1.lon,
                    p2.lat, p2.lon,
                    results
                )
                localDistanceMeters += results[0]
            }
        }
        val distanceInKm = localDistanceMeters / 1000.0

        var durationMinutes = 0.0
        if (pointCount >= 2) {
            val startTimeStr = trackData.first().timestamp
            val endTimeStr = trackData.last().timestamp

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                try {
                    val start = java.time.Instant.parse(startTimeStr)
                    val end = java.time.Instant.parse(endTimeStr)
                    val durationSeconds = java.time.Duration.between(start, end).seconds
                    durationMinutes = durationSeconds / 60.0
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        if (pointCount < 2) {
            Toast.makeText(context, "Za mało danych GPS.", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(context, "Wysyłanie...", Toast.LENGTH_SHORT).show()

        scope.launch {
            try {
                val token = UserSession.token ?: return@launch
                val startTime = trackData.firstOrNull()?.timestamp ?: java.time.Instant.now().toString()

                val createReq = com.example.mobile.network.CreateActivityRequest(
                    user_id = UserSession.userId ?: 1,
                    name = "Trening Mobile",
                    type = "run",
                    started_at = startTime,
                    distance_km = distanceInKm,
                    duration_min = durationMinutes
                )

                val createdActivity = RetrofitInstance.api.createActivity("Bearer $token", createReq)
                val activityId = createdActivity.id

                val requestBody = com.example.mobile.network.TrackRequest(points = trackData)
                RetrofitInstance.api.uploadTrack("Bearer $token", activityId, requestBody)

                withContext(Dispatchers.Main) {
                    val distStr = String.format("%.2f", distanceInKm)
                    val timeStr = String.format("%.2f", durationMinutes)
                    Toast.makeText(
                        context,
                        "Sukces! $distStr km | $timeStr min",
                        Toast.LENGTH_LONG
                    ).show()
                    trackData.clear()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Błąd wysyłania: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setMultiTouchControls(true)
                    controller.setZoom(18.0)
                    val startPoint = GeoPoint(51.2070, 16.1553)
                    controller.setCenter(startPoint)

                    val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                    locationOverlay.enableMyLocation()
                    locationOverlay.enableFollowLocation()
                    overlays.add(locationOverlay)

                    mapView = this
                }
            }
        )

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)
        ) {
            if (!isRecording) {
                Button(
                    onClick = { startRecording() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Text("START")
                }
            } else {
                Button(
                    onClick = { stopAndSave() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("STOP (ZAPISZ ${trackData.size} pkt)")
                }
            }
        }
    }
}