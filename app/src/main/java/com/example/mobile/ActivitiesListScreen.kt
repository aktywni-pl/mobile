package com.example.mobile

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobile.network.Activity
import com.example.mobile.network.RetrofitInstance
import com.example.mobile.network.UserSession
import androidx.compose.foundation.clickable

@Composable
fun ActivitiesListScreen(onActivityClick: (Int) -> Unit) {
    val context = LocalContext.current
    var activities by remember { mutableStateOf<List<Activity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    suspend fun loadActivities() {
        isLoading = true
        errorMessage = ""
        try {
            val token = UserSession.token
            if (token != null) {

                val response = RetrofitInstance.api.getActivities("Bearer $token")
                activities = response.reversed()
            } else {
                errorMessage = "Brak tokena. Zaloguj się ponownie."
            }
        } catch (e: Exception) {
            errorMessage = "Błąd pobierania: ${e.localizedMessage}"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadActivities()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Twoje Treningi", style = MaterialTheme.typography.headlineMedium)

            IconButton(onClick = {
                Toast.makeText(context, "Przełącz zakładki aby odświeżyć", Toast.LENGTH_SHORT).show()
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "Odśwież")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        } else if (activities.isEmpty()) {
            Text("Brak zapisanych aktywności.", style = MaterialTheme.typography.bodyLarge)
        } else {
            LazyColumn {
                items(activities) { activity ->
                    ActivityItem(activity, onClick = { onActivityClick(activity.id) })
                }
            }
        }
    }
}

@Composable
fun ActivityItem(activity: Activity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = activity.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = activity.started_at.take(10),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = activity.type.uppercase(), style = MaterialTheme.typography.bodyMedium)
                }

                Text(
                    text = String.format("%.2f km", activity.distance_km),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )

                val totalSeconds = (activity.duration_min * 60).toLong()
                val mm = totalSeconds / 60
                val ss = totalSeconds % 60
                val timeFormatted = String.format("%d:%02d min", mm, ss)

                Text(text = timeFormatted, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}