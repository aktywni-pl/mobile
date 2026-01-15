package com.example.mobile

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.mobile.network.UserSession
import com.example.mobile.ui.theme.MobileTheme
import org.osmdroid.config.Configuration

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName

        setContent {
            MobileTheme {
                val context = LocalContext.current

                var isLoggedIn by remember { mutableStateOf(false) }
                var isRegistering by remember { mutableStateOf(false) }

                var selectedActivityId by remember { mutableStateOf<Int?>(null) }
                var selectedTab by remember { mutableIntStateOf(0) }

                if (!isLoggedIn) {
                    if (isRegistering) {
                        RegisterScreen(
                            onRegisterSuccess = {
                                isLoggedIn = true
                                isRegistering = false
                                selectedTab = 0
                                selectedActivityId = null
                            },
                            onNavigateToLogin = { isRegistering = false }
                        )
                    } else {
                        LoginScreen(
                            onLoginSuccess = {
                                isLoggedIn = true
                                selectedTab = 0
                                selectedActivityId = null
                            },
                            onNavigateToRegister = { isRegistering = true }
                        )
                    }
                } else {
                    if (selectedActivityId != null) {
                        ActivityDetailsScreen(
                            activityId = selectedActivityId!!,
                            onBack = { selectedActivityId = null }
                        )
                    } else {
                        Scaffold(
                            topBar = {
                                CenterAlignedTopAppBar(
                                    title = {
                                        when(selectedTab) {
                                            0 -> Text("Moje Treningi")
                                            1 -> Text("Rejestracja")
                                            2 -> Text("Mój Profil")
                                            else -> Text("Mini Strava")
                                        }
                                    }
                                )
                            },
                            bottomBar = {
                                NavigationBar {
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Default.Home, null) },
                                        label = { Text("Treningi") },
                                        selected = selectedTab == 0,
                                        onClick = { selectedTab = 0 }
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Default.Place, null) },
                                        label = { Text("Nagraj") },
                                        selected = selectedTab == 1,
                                        onClick = { selectedTab = 1 }
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Default.Person, null) },
                                        label = { Text("Profil") },
                                        selected = selectedTab == 2,
                                        onClick = { selectedTab = 2 }
                                    )
                                }
                            }
                        ) { innerPadding ->
                            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                                when (selectedTab) {
                                    0 -> ActivitiesListScreen(
                                        onActivityClick = { clickedId ->
                                            selectedActivityId = clickedId
                                        }
                                    )
                                    1 -> MapScreen()
                                    2 -> ProfileScreen(
                                        onLogout = {
                                            UserSession.token = null
                                            UserSession.userId = null
                                            isLoggedIn = false
                                            Toast.makeText(context, "Wylogowano", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}