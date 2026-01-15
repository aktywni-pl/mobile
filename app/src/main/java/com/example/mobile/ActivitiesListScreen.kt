package com.example.mobile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobile.network.Activity
import com.example.mobile.network.RetrofitInstance
import com.example.mobile.network.UserSession
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitiesListScreen(onActivityClick: (Int) -> Unit) {
    val scope = rememberCoroutineScope()
    var allActivities by remember { mutableStateOf<List<Activity>>(emptyList()) }
    var displayedActivities by remember { mutableStateOf<List<Activity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var selectedType by remember { mutableStateOf<String?>(null) }
    var sortOption by remember { mutableStateOf(SortOption.DATE_DESC) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val token = UserSession.token
                if (token != null) {
                    val response = RetrofitInstance.api.getActivities("Bearer $token")
                    val myId = UserSession.userId
                    allActivities = response.filter { it.user_id == myId }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(allActivities, selectedType, sortOption) {
        var result = allActivities

        if (selectedType != null) {
            result = result.filter { it.type == selectedType }
        }

        result = when (sortOption) {
            SortOption.DATE_DESC -> result.sortedByDescending { it.started_at }
            SortOption.DATE_ASC -> result.sortedBy { it.started_at }
            SortOption.DISTANCE_DESC -> result.sortedByDescending { it.distance_km }
            SortOption.DISTANCE_ASC -> result.sortedBy { it.distance_km }
        }

        displayedActivities = result
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Moje Aktywności", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == null,
                        onClick = { selectedType = null },
                        label = { Text("Wszystkie") }
                    )
                    FilterChip(
                        selected = selectedType == "run",
                        onClick = { selectedType = if(selectedType == "run") null else "run" },
                        label = { Icon(Icons.AutoMirrored.Filled.DirectionsRun, null); Spacer(Modifier.width(4.dp)); }
                    )
                    FilterChip(
                        selected = selectedType == "bike",
                        onClick = { selectedType = if(selectedType == "bike") null else "bike" },
                        label = { Icon(Icons.AutoMirrored.Filled.DirectionsBike, null); Spacer(Modifier.width(4.dp)); }
                    )
                    FilterChip(
                        selected = selectedType == "walk",
                        onClick = { selectedType = if(selectedType == "walk") null else "walk" },
                        label = { Icon(Icons.AutoMirrored.Filled.DirectionsWalk, null); Spacer(Modifier.width(4.dp)); }
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Sort, null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sortuj:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(onClick = { sortOption = SortOption.DATE_DESC }) {
                        Text("Najnowsze", fontWeight = if(sortOption == SortOption.DATE_DESC) FontWeight.Bold else FontWeight.Normal)
                    }
                    TextButton(onClick = { sortOption = SortOption.DISTANCE_DESC }) {
                        Text("Najdłuższe", fontWeight = if(sortOption == SortOption.DISTANCE_DESC) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (displayedActivities.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Brak aktywności.", color = MaterialTheme.colorScheme.secondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayedActivities) { activity ->
                    ActivityItem(activity, onClick = { onActivityClick(activity.id) })
                }
            }
        }
    }
}

enum class SortOption {
    DATE_DESC, DATE_ASC, DISTANCE_DESC, DISTANCE_ASC
}

@Composable
fun ActivityItem(activity: Activity, onClick: () -> Unit) {
    val icon = when (activity.type) {
        "run" -> Icons.AutoMirrored.Filled.DirectionsRun
        "bike" -> Icons.AutoMirrored.Filled.DirectionsBike
        "walk" -> Icons.AutoMirrored.Filled.DirectionsWalk
        else -> Icons.AutoMirrored.Filled.DirectionsRun
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(activity.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = activity.started_at.take(10) + " • " + activity.type.uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("${String.format(Locale.US, "%.2f", activity.distance_km)} km", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                val totalSeconds = (activity.duration_min * 60).toLong()
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val seconds = totalSeconds % 60

                val timeString = if (hours > 0) {
                    String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
                } else {
                    String.format(Locale.US, "%d:%02d", minutes, seconds)
                }

                Text("$timeString min", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}