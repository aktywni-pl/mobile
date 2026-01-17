package com.example.mobile

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
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
import java.util.Calendar
import java.util.Locale

@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager(context) }

    var activitiesCount by remember { mutableStateOf(sessionManager.getActivitiesCount()) }
    var totalKm by remember { mutableStateOf(sessionManager.getTotalKm()) }
    var totalTimeString by remember { mutableStateOf(sessionManager.getTotalTime()) }

    var firstName by remember { mutableStateOf(sessionManager.getFirstName() ?: "") }
    var lastName by remember { mutableStateOf(sessionManager.getLastName() ?: "") }
    var birthDate by remember { mutableStateOf(sessionManager.getBirthDate() ?: "") }
    var gender by remember { mutableStateOf(sessionManager.getGender() ?: "other") }
    var weight by remember { mutableStateOf(sessionManager.getWeight() ?: "") }
    var height by remember { mutableStateOf(sessionManager.getHeight() ?: "") }

    var activities by remember { mutableStateOf<List<Activity>>(emptyList()) }
    var isEditing by remember { mutableStateOf(false) }
    var isLoadingProfile by remember { mutableStateOf(true) }

    val userEmail = UserSession.email ?: sessionManager.getEmail() ?: "uzytkownik"

    val displayName = remember(firstName, lastName, userEmail) {
        if (firstName.isNotBlank()) "$firstName $lastName".trim() else userEmail.substringBefore("@")
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

                    val count = myActivities.size
                    val km = myActivities.sumOf { it.distance_km }
                    val totalMinutesVal = myActivities.sumOf { it.duration_min }

                    val totalSeconds = (totalMinutesVal * 60).toInt()
                    val hh = totalSeconds / 3600
                    val mm = (totalSeconds % 3600) / 60
                    val ss = totalSeconds % 60

                    val timeStr = if (hh > 0) String.format(Locale.US, "%d:%02d:%02d", hh, mm, ss)
                    else String.format(Locale.US, "%02d:%02d", mm, ss)

                    activitiesCount = count
                    totalKm = km
                    totalTimeString = timeStr
                    sessionManager.saveStatsCache(count, km, timeStr)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                try {
                    val response = RetrofitInstance.api.getProfile("Bearer $token")
                    if (response.profile != null) {
                        val p = response.profile

                        firstName = p.first_name
                        lastName = p.last_name
                        birthDate = p.birth_date?.substringBefore("T") ?: ""
                        gender = p.gender ?: "other"
                        height = p.height_cm?.toString() ?: ""
                        weight = p.weight_kg?.toString() ?: ""

                        sessionManager.saveProfileCache(firstName, lastName, birthDate, gender, height, weight)
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

                val weightDouble = weight.replace(",", ".").toDoubleOrNull()
                val heightInt = height.toIntOrNull()

                val request = UpdateProfileRequest(
                    first_name = firstName,
                    last_name = lastName,
                    birth_date = if (birthDate.isNotBlank()) birthDate else null,
                    gender = gender,
                    height_cm = heightInt,
                    weight_kg = weightDouble,
                    city = "",
                    bio = ""
                )

                val response = RetrofitInstance.api.updateProfile("Bearer $token", request)

                if (response.isSuccessful) {
                    Toast.makeText(context, "Zapisano zmiany!", Toast.LENGTH_SHORT).show()
                    isEditing = false
                    sessionManager.saveProfileCache(firstName, lastName, birthDate, gender, height, weight)
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
                modifier = Modifier.fillMaxWidth().padding(16.dp),
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

        if (isLoadingProfile && firstName.isEmpty()) {
            CircularProgressIndicator()
        }

        if (firstName.isNotEmpty() || lastName.isNotEmpty() || !isLoadingProfile) {
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

            Spacer(modifier = Modifier.height(8.dp))

            val calendar = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                context,
                { _, year, month, day ->
                    birthDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            OutlinedTextField(
                value = birthDate,
                onValueChange = { },
                label = { Text("Data urodzenia") },
                enabled = false,
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = isEditing) { datePickerDialog.show() },
                trailingIcon = { Icon(Icons.Default.CalendarToday, null) },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Płeć", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GenderOption("Mężczyzna", "male", gender, isEditing) { gender = "male" }
                GenderOption("Kobieta", "female", gender, isEditing) { gender = "female" }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Waga (kg)") },
                    enabled = isEditing,
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = height,
                    onValueChange = { height = it },
                    label = { Text("Wzrost (cm)") },
                    enabled = isEditing,
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
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
fun GenderOption(label: String, value: String, selectedValue: String, enabled: Boolean, onSelect: () -> Unit) {
    val isSelected = value == selectedValue
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled) { onSelect() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color.Gray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}