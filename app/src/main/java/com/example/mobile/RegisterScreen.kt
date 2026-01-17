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
import com.example.mobile.network.RegisterRequest
import com.example.mobile.network.RetrofitInstance
import com.example.mobile.network.UserSession
import com.example.mobile.utils.SessionManager
import kotlinx.coroutines.launch
import retrofit2.HttpException

@Composable
fun RegisterScreen(onRegisterSuccess: () -> Unit, onNavigateToLogin: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

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
        Text(text = "Nowe konto", fontSize = 32.sp, style = MaterialTheme.typography.titleLarge)

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

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Powtórz hasło") },
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
                    if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                        if (password == confirmPassword) {
                            isLoading = true
                            errorMessage = ""

                            scope.launch {
                                try {
                                    val request = RegisterRequest(email, password, confirmPassword)
                                    val response = RetrofitInstance.api.register(request)

                                    if (response.token != null) {
                                        val sessionManager = SessionManager(context)
                                        sessionManager.saveAuthToken(response.token)
                                        sessionManager.saveUserId(response.id)
                                        sessionManager.saveEmail(email)

                                        UserSession.token = response.token
                                        UserSession.userId = response.id
                                        UserSession.email = email

                                        Toast.makeText(context, "Zarejestrowano pomyślnie!", Toast.LENGTH_SHORT).show()
                                        onRegisterSuccess()
                                    } else {
                                        Toast.makeText(context, "Konto utworzone! Zaloguj się.", Toast.LENGTH_LONG).show()
                                        onNavigateToLogin()
                                    }

                                } catch (e: HttpException) {
                                    isLoading = false
                                    if (e.code() == 409) {
                                        errorMessage = "Ten email jest już zajęty!"
                                    } else {
                                        errorMessage = "Błąd rejestracji (Kod: ${e.code()})"
                                    }
                                } catch (e: Exception) {
                                    isLoading = false
                                    errorMessage = "Błąd: ${e.message}"
                                }
                            }
                        } else {
                            errorMessage = "Hasła muszą być takie same!"
                        }
                    } else {
                        errorMessage = "Wypełnij wszystkie pola!"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Zarejestruj się")
            }
        }

        TextButton(onClick = onNavigateToLogin) {
            Text("Masz już konto? Zaloguj się")
        }
    }
}