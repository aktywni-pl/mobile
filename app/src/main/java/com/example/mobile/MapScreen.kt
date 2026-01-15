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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.mobile.network.CreateActivityRequest
import com.example.mobile.network.RetrofitInstance
import com.example.mobile.network.TrackPoint
import com.example.mobile.network.TrackRequest
import com.example.mobile.network.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
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
    var isPaused by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    var trackData = remember { mutableStateListOf<TrackPoint>() }

    var currentDistanceKm by remember { mutableStateOf(0.0) }
    var currentDurationStr by remember { mutableStateOf("00:00") }
    var currentPaceStr by remember { mutableStateOf("--:--") }
    var totalSeconds by remember { mutableStateOf(0L) }

    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    var mapView by remember { mutableStateOf<MapView?>(null) }

    var currentPolyline by remember { mutableStateOf<Polyline?>(null) }
    var startNewSegment by remember { mutableStateOf(true) }
    var lastValidLocation by remember { mutableStateOf<Location?>(null) }

    LaunchedEffect(isRecording, isPaused) {
        if (isRecording && !isPaused) {
            val startTime = System.currentTimeMillis() - (totalSeconds * 1000)
            while (isRecording && !isPaused) {
                val now = System.currentTimeMillis()
                totalSeconds = (now - startTime) / 1000

                val mm = totalSeconds / 60
                val ss = totalSeconds % 60
                currentDurationStr = String.format("%02d:%02d", mm, ss)

                if (currentDistanceKm > 0.05) {
                    val paceSeconds = totalSeconds / currentDistanceKm
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
                if (isRecording && !isPaused) {
                    if (location.accuracy > 50) return

                    val lat = location.latitude
                    val lon = location.longitude
                    val geoPoint = GeoPoint(lat, lon)

                    val nowStr = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        java.time.Instant.now().toString()
                    } else { java.util.Date().toString() }

                    trackData.add(TrackPoint(lat, lon, timestamp = nowStr))

                    if (startNewSegment) {
                        val newLine = Polyline().apply {
                            outlinePaint.color = android.graphics.Color.RED
                            outlinePaint.strokeWidth = 15f
                        }
                        newLine.addPoint(geoPoint)
                        mapView?.overlays?.add(newLine)
                        currentPolyline = newLine

                        startNewSegment = false
                        lastValidLocation = location
                        mapView?.invalidate()
                    } else {
                        currentPolyline?.addPoint(geoPoint)

                        lastValidLocation?.let { last ->
                            val dist = last.distanceTo(location)
                            currentDistanceKm += (dist / 1000.0)
                        }
                        lastValidLocation = location
                        mapView?.invalidate()
                    }

                    mapView?.controller?.animateTo(geoPoint)
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
        trackData.clear()
        currentDistanceKm = 0.0
        currentDurationStr = "00:00"
        currentPaceStr = "--:--"
        totalSeconds = 0L
        lastValidLocation = null

        mapView?.overlays?.removeIf { it is Polyline }
        mapView?.invalidate()

        startNewSegment = true
        currentPolyline = null

        isRecording = true
        isPaused = false

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 2f, locationListener)
        Toast.makeText(context, "Start!", Toast.LENGTH_SHORT).show()
    }

    fun stopAndPauseAction() {
        isPaused = true
        try {
            locationManager.removeUpdates(locationListener)
        } catch (e: Exception) { e.printStackTrace() }
        showSaveDialog = true
    }

    fun resumeRecording() {
        if (!hasPermission) return
        isPaused = false
        showSaveDialog = false
        startNewSegment = true
        lastValidLocation = null
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 2f, locationListener)
    }

    fun discardRecording() {
        isRecording = false
        isPaused = false
        showSaveDialog = false
        totalSeconds = 0L
        trackData.clear()
        currentDistanceKm = 0.0
        currentDurationStr = "00:00"
        lastValidLocation = null

        mapView?.overlays?.removeIf { it is Polyline }
        mapView?.invalidate()
        Toast.makeText(context, "Trening odrzucony", Toast.LENGTH_SHORT).show()
    }

    fun finishAndUpload(name: String, type: String, note: String) {
        val distanceInKm = currentDistanceKm
        val durationMinutes = totalSeconds / 60.0

        if (trackData.size < 2) {
            Toast.makeText(context, "Za mało danych GPS.", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(context, "Wysyłanie...", Toast.LENGTH_SHORT).show()
        showSaveDialog = false

        scope.launch {
            try {
                val token = UserSession.token ?: return@launch
                val startIso = trackData.firstOrNull()?.timestamp ?: java.time.Instant.now().toString()

                val finalName = if (note.isNotBlank()) "$name - $note" else name

                val createReq = CreateActivityRequest(
                    user_id = UserSession.userId ?: 0,
                    name = finalName,
                    type = type,
                    started_at = startIso,
                    distance_km = distanceInKm,
                    duration_min = durationMinutes
                )

                val createdActivity = RetrofitInstance.api.createActivity("Bearer $token", createReq)
                val activityId = createdActivity.id

                val requestBody = TrackRequest(points = trackData)
                RetrofitInstance.api.uploadTrack("Bearer $token", activityId, requestBody)

                withContext(Dispatchers.Main) {
                    val mm = totalSeconds / 60
                    val ss = totalSeconds % 60
                    val timeReadable = String.format("%02d:%02d", mm, ss)
                    val distStr = String.format("%.2f", distanceInKm)

                    Toast.makeText(context, "Zapisano! $distStr km | Czas: $timeReadable", Toast.LENGTH_LONG).show()
                    discardRecording()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Błąd: ${e.message}", Toast.LENGTH_LONG).show()
                    isPaused = true
                    showSaveDialog = true
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
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    controller.setZoom(18.0)
                    val startPoint = GeoPoint(51.2070, 16.1553)
                    controller.setCenter(startPoint)

                    val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                    locationOverlay.enableMyLocation()
                    locationOverlay.enableFollowLocation()
                    overlays.add(locationOverlay)

                    keepScreenOn = true

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
                FloatingActionButton(
                    onClick = { stopAndPauseAction() },
                    containerColor = Color.Red,
                    modifier = Modifier.size(80.dp).align(Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Default.Stop, null, modifier = Modifier.size(40.dp), tint = Color.White)
                }
            }
        }
    }

    if (showSaveDialog) {
        SaveActivityDialog(
            distanceKm = currentDistanceKm,
            totalSeconds = totalSeconds,
            onResume = { resumeRecording() },
            onDiscard = { discardRecording() },
            onSave = { name, type, note ->
                finishAndUpload(name, type, note)
            }
        )
    }
}

@Composable
fun SaveActivityDialog(
    distanceKm: Double,
    totalSeconds: Long,
    onResume: () -> Unit,
    onDiscard: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("run") }

    val mm = totalSeconds / 60
    val ss = totalSeconds % 60
    val timeFormatted = String.format("%02d:%02d", mm, ss)

    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false),
        title = { Text("Trening wstrzymany") },
        text = {
            Column {
                Text("Dystans: ${String.format("%.2f", distanceKm)} km")
                Text("Czas: $timeFormatted", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Rodzaj aktywności:", fontSize = 12.sp, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TypeButton(Icons.AutoMirrored.Filled.DirectionsRun, "run", selectedType) { selectedType = "run" }
                    TypeButton(Icons.AutoMirrored.Filled.DirectionsWalk, "walk", selectedType) { selectedType = "walk" }
                    TypeButton(Icons.AutoMirrored.Filled.DirectionsBike, "bike", selectedType) { selectedType = "bike" }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nazwa (np. Ranny Bieg)") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notatka") },
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        val finalName = if (name.isBlank()) "Trening" else name
                        onSave(finalName, selectedType, note)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Zapisz")
                }
            }
        },
        dismissButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDiscard) {
                    Text("Usuń", color = Color.Red)
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onResume) {
                    Text("Wróć do treningu")
                }
            }
        }
    )
}

@Composable
fun TypeButton(icon: ImageVector, type: String, selected: String, onClick: () -> Unit) {
    val isSelected = type == selected
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.3f)
    val iconColor = if (isSelected) Color.White else Color.Black

    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable { onClick() }
            .border(2.dp, if(isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = iconColor)
    }
}