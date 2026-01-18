package com.example.mobile

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
// Poniższe dwa importy naprawiają błąd z "by"
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile.network.Activity
import com.example.mobile.network.RetrofitInstance
import com.example.mobile.network.UserSession
import com.example.mobile.utils.SessionManager
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun SocialScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager(context) }
    val prefs = context.getSharedPreferences("social_prefs", Context.MODE_PRIVATE)

    var followedIds by remember {
        val savedSet = prefs.getStringSet("followed_ids", emptySet()) ?: emptySet()
        mutableStateOf(savedSet)
    }

    fun toggleFollow(userId: Int) {
        val idStr = userId.toString()
        val currentSet = followedIds.toMutableSet()

        if (currentSet.contains(idStr)) {
            currentSet.remove(idStr)
        } else {
            currentSet.add(idStr)
        }

        prefs.edit().putStringSet("followed_ids", currentSet).apply()
        followedIds = currentSet
    }

    fun isFollowing(userId: Int): Boolean {
        return followedIds.contains(userId.toString())
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Feed", "Ranking", "Ludzie")

    var feedActivities by remember { mutableStateOf<List<Activity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val myId = UserSession.userId ?: sessionManager.getUserId()
    val myEmail = UserSession.email ?: sessionManager.getEmail() ?: "Ja"

    LaunchedEffect(Unit) {
        scope.launch {
            val token = UserSession.token ?: sessionManager.fetchAuthToken()
            if (token != null) {
                try {
                    val response = RetrofitInstance.api.getFeed("Bearer $token")
                    feedActivities = response
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isLoading = false
                }
            } else {
                isLoading = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                when (selectedTab) {
                    0 -> FeedTab(
                        activities = feedActivities,
                        myId = myId,
                        myEmail = myEmail,
                        isFollowing = { isFollowing(it) }
                    )
                    1 -> RankingTab(
                        activities = feedActivities,
                        myId = myId,
                        myEmail = myEmail
                    )
                    2 -> PeopleDiscoveryTab(
                        activities = feedActivities,
                        followedIds = followedIds,
                        onToggleFollow = { toggleFollow(it) },
                        myId = myId
                    )
                }
            }
        }
    }
}

@Composable
fun FeedTab(activities: List<Activity>, myId: Int, myEmail: String, isFollowing: (Int) -> Boolean) {
    if (activities.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Brak aktywności w feedzie.")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(activities) { activity ->
                ActivityItemSocial(activity, myId, myEmail, isFollowing(activity.user_id))
            }
        }
    }
}

@Composable
fun ActivityItemSocial(activity: Activity, myId: Int, myEmail: String, isFriend: Boolean) {
    var isLiked by remember { mutableStateOf(false) }
    var likeCount by remember { mutableIntStateOf(if (activity.distance_km > 10) 3 else 0) }
    var showCommentDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val isMe = (activity.user_id == myId)

    val userName = if (isMe) "TY ($myEmail)" else "Użytkownik #${activity.user_id}"
    val userColor = if (isMe) MaterialTheme.colorScheme.primary else if (isFriend) Color(0xFF4CAF50) else Color.Gray

    if (showCommentDialog) {
        CommentDialog(
            onDismiss = { showCommentDialog = false },
            onSend = {
                Toast.makeText(context, "Komentarz dodany!", Toast.LENGTH_SHORT).show()
                showCommentDialog = false
            }
        )
    }

    Card(
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(userColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(userName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                        if (isFriend && !isMe) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("(Znajomy)", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                    Text(
                        try { activity.started_at.take(10) } catch(e:Exception){""},
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.weight(1f))

                if (!isMe) {
                    IconButton(onClick = { Toast.makeText(context, "Zgłoszono użytkownika.", Toast.LENGTH_SHORT).show() }) {
                        Icon(Icons.Outlined.Warning, "Zgłoś", tint = Color.LightGray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "${activity.type.uppercase()} - ${activity.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                Text("${activity.distance_km} km", fontWeight = FontWeight.Bold)
                Text(" • ")
                Text("${activity.duration_min} min")
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = {
                        if (!isMe) {
                            isLiked = !isLiked
                            if (isLiked) likeCount++ else likeCount--
                        } else {
                            Toast.makeText(context, "To Twój trening", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if(isMe) Color.LightGray else if(isLiked) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                ) {
                    Icon(if(isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if(likeCount > 0) "Kudos ($likeCount)" else "Kudos")
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(onClick = { showCommentDialog = true }) {
                    Text("Skomentuj", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun CommentDialog(onDismiss: () -> Unit, onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj komentarz") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Treść komentarza") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onSend(text) }) {
                Text("Wyślij")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

@Composable
fun RankingTab(activities: List<Activity>, myId: Int, myEmail: String) {
    val ranking = remember(activities) {
        activities.groupBy { it.user_id }
            .map { (uid, acts) -> uid to acts.sumOf { it.distance_km } }
            .sortedByDescending { it.second }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        item {
            Text("Ranking Tygodniowy", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
        }

        itemsIndexed(ranking) { index, (uid, km) ->
            val isMe = (uid == myId)
            val name = if (isMe) "TY ($myEmail)" else "Użytkownik #$uid"
            val rankColor = when(index) {
                0 -> Color(0xFFFFD700)
                1 -> Color(0xFFC0C0C0)
                2 -> Color(0xFFCD7F32)
                else -> MaterialTheme.colorScheme.onSurface
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .background(if(isMe) MaterialTheme.colorScheme.primaryContainer.copy(alpha=0.3f) else Color.Transparent, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "#${index + 1}",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = rankColor,
                    modifier = Modifier.width(40.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, fontWeight = if(isMe) FontWeight.Bold else FontWeight.Normal)
                }
                Text(
                    String.format(Locale.US, "%.1f km", km),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Divider(color = Color.LightGray.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun PeopleDiscoveryTab(
    activities: List<Activity>,
    followedIds: Set<String>,
    onToggleFollow: (Int) -> Unit,
    myId: Int
) {
    val uniqueUsers = remember(activities) {
        activities.map { it.user_id }.distinct().filter { it != myId }.sorted()
    }

    var searchQuery by remember { mutableStateOf("") }
    val filteredList = if(searchQuery.isBlank()) uniqueUsers else uniqueUsers.filter { it.toString().contains(searchQuery) }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Znajdź znajomego (wpisz ID)") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, null) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Polecani znajomi:", fontWeight = FontWeight.Bold)

        LazyColumn {
            items(filteredList) { userId ->
                val isFollowing = followedIds.contains(userId.toString())

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("#$userId", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Biegacz #$userId", fontWeight = FontWeight.SemiBold)
                            Text(if(isFollowing) "Twój znajomy" else "Aktywny użytkownik", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }

                    Button(
                        onClick = { onToggleFollow(userId) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if(isFollowing) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
                            contentColor = if(isFollowing) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(if(isFollowing) "Obserwujesz" else "Dodaj")
                    }
                }
                Divider()
            }
        }
    }
}