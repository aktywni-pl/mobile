package com.example.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                title = { Text(activity?.name ?: "Szczegóły") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            activity?.let { act ->
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {

                    Box(modifier = Modifier.weight(1f)) {
                        AndroidView(
                            factory = { ctx ->
                                MapView(ctx).apply {
                                    setMultiTouchControls(true)
                                }
                            },
                            update = { map ->
                                map.overlays.clear()

                                if (trackPoints.isNotEmpty()) {
                                    val geoPoints = trackPoints.map { GeoPoint(it.lat, it.lon) }

                                    val line = Polyline()
                                    line.setPoints(geoPoints)
                                    line.outlinePaint.color = android.graphics.Color.RED
                                    line.outlinePaint.strokeWidth = 15f
                                    map.overlays.add(line)


                                    map.post {
                                        if (geoPoints.isNotEmpty()) {
                                            map.controller.setZoom(16.0)
                                            map.controller.setCenter(geoPoints.first())
                                        }
                                    }
                                }
                                map.invalidate()
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White)
                            .padding(16.dp)
                    ) {
                        Text(text = "Data: ${act.started_at.take(10)}", color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatTile("Dystans", String.format("%.2f km", act.distance_km))

                            val totalSeconds = (act.duration_min * 60).toLong()
                            val mm = totalSeconds / 60
                            val ss = totalSeconds % 60
                            StatTile("Czas", String.format("%d:%02d", mm, ss))
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        val paceStr = if (act.distance_km > 0.05) {
                            val totalSec = act.duration_min * 60
                            val paceSec = totalSec / act.distance_km
                            val pMin = (paceSec / 60).toInt()
                            val pSec = (paceSec % 60).toInt()
                            String.format("%d:%02d /km", pMin, pSec)
                        } else "--:--"

                        Text("Średnie Tempo", color = Color.Gray, fontSize = 12.sp)
                        Text(paceStr, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Nie udało się pobrać danych.")
            }
        }
    }
}

@Composable
fun StatTile(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
        Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}