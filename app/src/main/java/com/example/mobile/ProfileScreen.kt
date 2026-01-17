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
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobile.network.Activity
import com.example.mobile.network.RetrofitInstance
import com.example.mobile.network.UpdateProfileRequest
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

    var isEditing by remember { mutableStateOf(false) }
    var isLoadingProfile by remember { mutableStateOf(true) }

    val userEmail = UserSession.email ?: sessionManager.getEmail() ?: "uzytkownik"

    val displayName = remember(firstName, lastName, userEmail) {
        if (firstName.isNotBlank()) {
            "$firstName $lastName".trim()
        } else {
            userEmail.substringBefore("@")
        }
    }

    LaunchedEffect(Unit) {
        scope.launch {
            val token = UserSession.token ?: sessionManager.fetchAuthToken()
            val currentUserId = if (UserSession.userId != 0) UserSession.userId else sessionManager.getUserId()

            if (token != null) {
                try {
                    val all = RetrofitInstance.api.getActivities("Bearer $token")
                    val myActivities = all.filter { it.user_id == currentUserId }
                    activities = myActivities
                    activitiesCount = myActivities.size
                    totalKm = myActivities.sumOf { it.distance_km }

                    val totalMinutesVal = myActivities.sumOf { it.duration_min }
                    val totalSeconds = (totalMinutesVal * 60).toInt()
                    val hh = totalSeconds / 3600
                    val mm = (totalSeconds % 3600) / 60
                    val ss = totalSeconds % 60

                    totalTimeString = if (hh > 0) String.format(Locale.US, "%d:%02d:%02d", hh, mm, ss)
                    else String.format(Locale.US, "%02d:%02d", mm, ss)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    val response = RetrofitInstance.api.getProfile("Bearer $token")

                    if (response.profile != null) {
                        val p = response.profile
                        firstName = p.first_name
                        lastName = p.last_name
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isLoadingProfile = false
                }
            }
        }
    }

    fun saveProfile() {
        scope.launch {
            try {
                val token = UserSession.token ?: return@launch

                if (firstName.isBlank() || lastName.isBlank()) {
                    Toast.makeText(context, "Imię i nazwisko są wymagane!", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val request = UpdateProfileRequest(
                    first_name = firstName,
                    last_name = lastName,
                    city = "",
                    bio = ""
                )

                val response = RetrofitInstance.api.updateProfile("Bearer $token", request)

                if (response.isSuccessful) {
                    Toast.makeText(context, "Profil zaktualizowany!", Toast.LENGTH_SHORT).show()
                    isEditing = false
                } else {
                    Toast.makeText(context, "Błąd serwera: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Błąd połączenia: ${e.message}", Toast.LENGTH_LONG).show()
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
            text = displayName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(text = userEmail, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

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
                Icon(if (isEditing) Icons.Default.Save else Icons.Default.Edit, contentDescription = "Edytuj")
            }
        }

        if (isLoadingProfile) {
            CircularProgressIndicator()
        } else {
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("Imię") },
                enabled = isEditing,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Nazwisko") },
                enabled = isEditing,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isEditing) {
            Button(
                onClick = { saveProfile() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ZAPISZ ZMIANY")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                sessionManager.clearSession()
                onLogout()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)),
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