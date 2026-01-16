package com.example.mobile

import android.graphics.Paint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.mobile.network.Activity
import com.example.mobile.network.RetrofitInstance
import com.example.mobile.network.TrackPoint
import com.example.mobile.network.UserSession
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailsScreen(activityId: Int, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var activity by remember { mutableStateOf<Activity?>(null) }
    var trackPoints by remember { mutableStateOf<List<TrackPoint>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    BackHandler { onBack() }

    LaunchedEffect(activityId) {
        scope.launch {
            try {
                val token = UserSession.token
                if (token != null) {
                    activity = RetrofitInstance.api.getActivity("Bearer $token", activityId)
                    try {
                        val trackResponse = RetrofitInstance.api.getActivityTrack("Bearer $token", activityId)
                        trackPoints = trackResponse.points
                    } catch (e: Exception) {
                        println("Brak trasy: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(activity?.name ?: "Szczegóły", style = MaterialTheme.typography.titleMedium)
                        activity?.started_at?.take(10)?.let { date ->
                            Text(text = date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.shadow(4.dp)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            activity?.let { act ->
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {

                    Box(
                        modifier = Modifier
                            .weight(0.65f)
                            .fillMaxWidth()
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                MapView(ctx).apply {
                                    setMultiTouchControls(true)
                                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                                }
                            },
                            update = { map ->
                                map.overlays.clear()

                                if (trackPoints.isNotEmpty()) {
                                    val startGeo = GeoPoint(trackPoints.first().lat, trackPoints.first().lon)

                                    fun createPolyline(): Polyline {
                                        return Polyline().apply {
                                            outlinePaint.color = android.graphics.Color.parseColor("#FF5722")
                                            outlinePaint.strokeWidth = 12f
                                            outlinePaint.strokeJoin = Paint.Join.ROUND
                                            outlinePaint.strokeCap = Paint.Cap.ROUND
                                        }
                                    }

                                    var currentPolyline = createPolyline()
                                    currentPolyline.addPoint(startGeo)

                                    for (i in 0 until trackPoints.size - 1) {
                                        val p1 = trackPoints[i]
                                        val p2 = trackPoints[i+1]

                                        val g1 = GeoPoint(p1.lat, p1.lon)
                                        val g2 = GeoPoint(p2.lat, p2.lon)

                                        if (g1.distanceToAsDouble(g2) > 50) {
                                            map.overlays.add(currentPolyline)
                                            currentPolyline = createPolyline()
                                            currentPolyline.addPoint(g2)
                                        } else {
                                            currentPolyline.addPoint(g2)
                                        }
                                    }
                                    map.overlays.add(currentPolyline)

                                    map.post {
                                        map.controller.setZoom(16.0)
                                        map.controller.setCenter(startGeo)
                                    }
                                }
                                map.invalidate()
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxWidth()
                            .shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.SpaceAround
                        ) {

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "DYSTANS",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.Gray,
                                    letterSpacing = 2.sp
                                )
                                Text(
                                    text = String.format("%.2f km", act.distance_km),
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Divider(color = Color.LightGray.copy(alpha = 0.3f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                val totalSeconds = (act.duration_min * 60).toLong()
                                val hh = totalSeconds / 3600
                                val mm = (totalSeconds % 3600) / 60
                                val ss = totalSeconds % 60

                                val timeStr = if (hh > 0) {
                                    String.format(java.util.Locale.US, "%d:%02d:%02d", hh, mm, ss)
                                } else {
                                    String.format(java.util.Locale.US, "%d:%02d", mm, ss)
                                }

                                DetailStatItem(
                                    icon = Icons.Default.Timer,
                                    label = "CZAS",
                                    value = timeStr
                                )

                                val paceStr = if (act.distance_km > 0.001) {
                                    val paceMinPerKm = act.duration_min / act.distance_km
                                    val paceTotalSeconds = (paceMinPerKm * 60).toLong()
                                    val pMin = paceTotalSeconds / 60
                                    val pSec = paceTotalSeconds % 60

                                    if (pMin < 600) String.format(java.util.Locale.US, "%d:%02d", pMin, pSec) else "--:--"
                                } else {
                                    "--:--"
                                }

                                DetailStatItem(
                                    icon = Icons.Default.Speed,
                                    label = "TEMPO /KM",
                                    value = paceStr
                                )
                            }
                        }
                    }
                }
            } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nie udało się pobrać danych.")
            }
        }
    }
}

@Composable
fun DetailStatItem(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
    }
}