package com.example.mobile

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
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

                // 0 = Lista, 1 = Mapa
                var selectedTab by remember { mutableIntStateOf(0) }

                if (!isLoggedIn) {
                    LoginScreen(
                        onLoginSuccess = {
                            isLoggedIn = true
                            selectedTab = 0
                        },
                        onNavigateToRegister = {
                            Toast.makeText(context, "Rejestracja wyłączona", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    Scaffold(
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = { Text("Mini Strava") },
                                actions = {
                                    IconButton(onClick = {
                                        UserSession.token = null
                                        isLoggedIn = false
                                        Toast.makeText(context, "Wylogowano", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Wyloguj")
                                    }
                                }
                            )
                        },
                        bottomBar = {
                            NavigationBar {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Home, "Start") },
                                    label = { Text("Treningi") },
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Place, "Mapa") },
                                    label = { Text("Nagraj") },
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                            if (selectedTab == 0) {
                                ActivitiesListScreen()
                            } else {
                                MapScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}