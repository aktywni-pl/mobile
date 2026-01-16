package com.example.mobile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobile.network.Activity
import com.example.mobile.network.RetrofitInstance
import com.example.mobile.network.UserSession
import com.example.mobile.utils.SessionManager
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager(context) }

    var activities by remember { mutableStateOf<List<Activity>>(emptyList()) }
    var totalKm by remember { mutableStateOf(0.0) }
    var totalTimeString by remember { mutableStateOf("00:00") }
    var activitiesCount by remember { mutableStateOf(0) }

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("80") }
    var height by remember { mutableStateOf("180") }
    var isEditing by remember { mutableStateOf(false) }

    val userEmail = UserSession.email ?: sessionManager.getEmail() ?: "uzytkownik"
    val nick = userEmail.substringBefore("@").replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val token = UserSession.token ?: sessionManager.fetchAuthToken()
                val currentUserId = if (UserSession.userId != 0) UserSession.userId else sessionManager.getUserId()

                if (token != null && currentUserId != -1) {
                    val all = RetrofitInstance.api.getActivities("Bearer $token")
                    val myActivities = all.filter { it.user_id == currentUserId }

                    activities = myActivities
                    activitiesCount = myActivities.size
                    totalKm = myActivities.sumOf { it.distance_km }

                    val totalMinutesVal = myActivities.sumOf { it.duration_min }
                    val totalSeconds = (totalMinutesVal * 60).toInt()

                    val hours = totalSeconds / 3600
                    val minutes = (totalSeconds % 3600) / 60
                    val seconds = totalSeconds % 60

                    totalTimeString = if (hours > 0) {
                        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
                    } else {
                        String.format(Locale.US, "%02d:%02d", minutes, seconds)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = nick,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(label = "Aktywności", value = activitiesCount.toString())
                StatItem(label = "Dystans (km)", value = String.format(Locale.US, "%.1f", totalKm))
                StatItem(label = "Czas", value = totalTimeString)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Dane profilowe", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = { isEditing = !isEditing }) {
                Icon(Icons.Default.Edit, contentDescription = "Edytuj")
            }
        }

        OutlinedTextField(
            value = if (firstName.isEmpty()) nick else firstName,
            onValueChange = { firstName = it },
            label = { Text("Imię") },
            enabled = isEditing,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Nazwisko") },
            enabled = isEditing,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                label = { Text("Waga (kg)") },
                enabled = isEditing,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = height,
                onValueChange = { height = it },
                label = { Text("Wzrost (cm)") },
                enabled = isEditing,
                modifier = Modifier.weight(1f)
            )
        }

        if (isEditing) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    isEditing = false
                    Toast.makeText(context, "Zapisano dane (lokalnie)", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Zapisz zmiany")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(
            onClick = {
                Toast.makeText(context, "Funkcja zmiany hasła dostępna przez reset e-mail", Toast.LENGTH_LONG).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Zmień hasło")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                sessionManager.clearSession()
                onLogout()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Wyloguj się")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}