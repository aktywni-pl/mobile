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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.mobile.network.RetrofitInstance
import com.example.mobile.network.TrackPoint
import com.example.mobile.network.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@SuppressLint("MissingPermission")
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isRecording by remember { mutableStateOf(false) }
    var routePoints = remember { mutableStateListOf<GeoPoint>() }
    var trackData = remember { mutableStateListOf<TrackPoint>() }

    var currentDistanceKm by remember { mutableStateOf(0.0) }
    var currentDurationStr by remember { mutableStateOf("00:00") }
    var currentPaceStr by remember { mutableStateOf("--:--") }

    var runStartTime by remember { mutableStateOf<java.time.Instant?>(null) }

    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    var mapView by remember { mutableStateOf<MapView?>(null) }

    LaunchedEffect(isRecording, runStartTime) {
        if (isRecording && runStartTime != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            while (isRecording) {
                val now = java.time.Instant.now()
                val durationSeconds = java.time.Duration.between(runStartTime, now).seconds

                val mm = durationSeconds / 60
                val ss = durationSeconds % 60
                currentDurationStr = String.format("%02d:%02d", mm, ss)

                if (currentDistanceKm > 0.05) {
                    val paceSeconds = durationSeconds / currentDistanceKm
                    val paceMin = (paceSeconds / 60).toInt()
                    val paceSec = (paceSeconds % 60).toInt()
                    currentPaceStr = String.format("%d:%02d /km", paceMin, paceSec)
                } else {
                    currentPaceStr = "--:--"
                }

                delay(1000L)
            }
        }
    }

    val locationListener = remember {
        object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (isRecording) {
                    val lat = location.latitude
                    val lon = location.longitude

                    if (lat == 0.0 && lon == 0.0) return

                    val geoPoint = GeoPoint(lat, lon)
                    val nowStr = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        java.time.Instant.now().toString()
                    } else { java.util.Date().toString() }

                    routePoints.add(geoPoint)
                    trackData.add(TrackPoint(lat, lon, timestamp = nowStr))

                    var distMeters = 0.0
                    if (trackData.size > 1) {
                        for (i in 0 until trackData.size - 1) {
                            val p1 = trackData[i]
                            val p2 = trackData[i+1]
                            val res = FloatArray(1)
                            android.location.Location.distanceBetween(p1.lat, p1.lon, p2.lat, p2.lon, res)
                            distMeters += res[0]
                        }
                    }
                    currentDistanceKm = distMeters / 1000.0

                    mapView?.overlays?.filterIsInstance<Polyline>()?.firstOrNull()?.let { line ->
                        line.addPoint(geoPoint)
                    } ?: run {
                        val line = Polyline()
                        line.outlinePaint.color = android.graphics.Color.RED
                        line.outlinePaint.strokeWidth = 15f
                        line.addPoint(geoPoint)
                        mapView?.overlays?.add(line)
                    }

                    mapView?.controller?.animateTo(geoPoint)
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
    ) { isGranted -> hasPermission = isGranted }

    fun startRecording() {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }
        routePoints.clear()
        trackData.clear()
        currentDistanceKm = 0.0
        currentDurationStr = "00:00"
        currentPaceStr = "--:--"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            runStartTime = java.time.Instant.now()
        }

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
        val distanceInKm = currentDistanceKm

        var durationSecondsTotal = 0L
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && runStartTime != null) {
            val now = java.time.Instant.now()
            durationSecondsTotal = java.time.Duration.between(runStartTime, now).seconds
        }

        val durationMinutes = durationSecondsTotal / 60.0

        if (pointCount < 2) {
            Toast.makeText(context, "Za mało danych GPS.", Toast.LENGTH_SHORT).show()
            isRecording = false
            runStartTime = null
            return
        }

        Toast.makeText(context, "Wysyłanie...", Toast.LENGTH_SHORT).show()

        scope.launch {
            try {
                val token = UserSession.token ?: return@launch
                val startIso = trackData.firstOrNull()?.timestamp ?: java.time.Instant.now().toString()

                val createReq = com.example.mobile.network.CreateActivityRequest(
                    user_id = UserSession.userId ?: 1,
                    name = "Trening Mobile",
                    type = "run",
                    started_at = startIso,
                    distance_km = distanceInKm,
                    duration_min = durationMinutes
                )

                val createdActivity = RetrofitInstance.api.createActivity("Bearer $token", createReq)
                val activityId = createdActivity.id

                val requestBody = com.example.mobile.network.TrackRequest(points = trackData)
                RetrofitInstance.api.uploadTrack("Bearer $token", activityId, requestBody)

                withContext(Dispatchers.Main) {
                    val mm = durationSecondsTotal / 60
                    val ss = durationSecondsTotal % 60
                    val timeReadable = String.format("%02d:%02d", mm, ss)

                    val distStr = String.format("%.2f", distanceInKm)

                    Toast.makeText(context, "Zapisano! $distStr km | Czas: $timeReadable", Toast.LENGTH_LONG).show()

                    trackData.clear()
                    routePoints.clear()
                    currentDistanceKm = 0.0
                    currentDurationStr = "00:00"
                    currentPaceStr = "--:--"
                    runStartTime = null

                    mapView?.overlays?.removeIf { it is Polyline }
                    mapView?.invalidate()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Błąd: ${e.message}", Toast.LENGTH_LONG).show()
                    isRecording = false
                    runStartTime = null
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

        if (isRecording) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CZAS", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(currentDurationStr, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("DYSTANS", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(String.format("%.2f km", currentDistanceKm), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TEMPO", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(currentPaceStr, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

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
                    Text("STOP (ZAPISZ)")
                }
            }
        }
    }
}