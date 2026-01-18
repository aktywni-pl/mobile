package com.example.mobile

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile.network.LoginRequest
import com.example.mobile.network.RetrofitInstance
import com.example.mobile.network.UserSession
import com.example.mobile.utils.SessionManager
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onNavigateToRegister: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Aktywni", fontSize = 32.sp, style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Hasło") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        isLoading = true
                        errorMessage = ""


                        scope.launch {
                            try {
                                val response = RetrofitInstance.api.login(LoginRequest(email, password))
                                val sessionManager = SessionManager(context)

                                sessionManager.saveAuthToken(response.token)
                                sessionManager.saveUserId(response.id)
                                sessionManager.saveEmail(email)

                                UserSession.token = response.token
                                UserSession.userId = response.id
                                UserSession.email = email

                                Toast.makeText(context, "Zalogowano pomyślnie!", Toast.LENGTH_SHORT).show()
                                onLoginSuccess()
                            } catch (e: Exception) {
                                errorMessage = "Błąd logowania: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    } else {
                        errorMessage = "Wprowadź dane!"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Zaloguj się")
            }
        }

        TextButton(onClick = onNavigateToRegister) {
            Text("Nie masz konta? Zarejestruj się")
        }
    }
}