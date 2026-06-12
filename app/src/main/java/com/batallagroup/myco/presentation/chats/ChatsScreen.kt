package com.batallagroup.myco.presentation.chats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.batallagroup.myco.core.utils.toFormattedTime
import com.batallagroup.myco.presentation.theme.*

@Composable
fun ChatsScreen(
    viewModel: ChatsViewModel = hiltViewModel(),
    onOpenChat: (String) -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val nearbyNodes by viewModel.nearbyNodes.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = MycoBg,
        bottomBar = {
            MycoBottomBar(
                selectedTab = selectedTab,
                onSelectTab = { tab ->
                    selectedTab = tab
                    when (tab) {
                        1 -> onNavigateToContacts()
                        2 -> onNavigateToNetwork()
                        3 -> onNavigateToSettings()
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToContacts,
                containerColor = MycoGreen,
                contentColor = MycoBg,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo chat")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Myco",
                    color = MycoGreen,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )

                // Indicador de nodos cercanos
                if (nearbyNodes.isNotEmpty()) {
                    Surface(
                        color = MycoGreenDark.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MycoGreen)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "${nearbyNodes.size} nodo${if (nearbyNodes.size != 1) "s" else ""} cerca",
                                color = MycoGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MycoSurface3, thickness = 0.5.dp)

            if (conversations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🍄", fontSize = 48.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("Sin conversaciones aún", color = MycoSubtle, fontSize = 15.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Agrega un contacto para empezar", color = MycoSurface3, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn {
                    items(conversations) { item ->
                        ConversationRow(
                            item = item,
                            onClick = { onOpenChat(item.contact.userId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(item: ConversationItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MycoGreenDark),
            contentAlignment = Alignment.Center
        ) {
            Text(
                item.contact.userId.take(2).uppercase(),
                color = MycoGreen,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.contact.alias.ifEmpty { item.contact.userId },
                    color = MycoOnSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                item.lastMessage?.let { msg ->
                    Text(
                        text = msg.timestamp.toFormattedTime(),
                        color = MycoSubtle,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(2.dp))

            Text(
                text = item.lastMessage?.content ?: "Sin mensajes",
                color = MycoSubtle,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 82.dp),
        color = MycoSurface3,
        thickness = 0.5.dp
    )
}

@Composable
private fun MycoBottomBar(selectedTab: Int, onSelectTab: (Int) -> Unit) {
    NavigationBar(containerColor = MycoSurface) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onSelectTab(0) },
            icon = { Icon(Icons.Default.ChatBubble, null) },
            label = { Text("Chats") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MycoGreen,
                selectedTextColor = MycoGreen,
                indicatorColor = MycoGreenDark.copy(alpha = 0.3f),
                unselectedIconColor = MycoSubtle,
                unselectedTextColor = MycoSubtle
            )
        )
        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onSelectTab(1) },
            icon = { Icon(Icons.Default.Person, null) },
            label = { Text("Contactos") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MycoGreen,
                selectedTextColor = MycoGreen,
                indicatorColor = MycoGreenDark.copy(alpha = 0.3f),
                unselectedIconColor = MycoSubtle,
                unselectedTextColor = MycoSubtle
            )
        )
        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onSelectTab(2) },
            icon = { Icon(Icons.Default.Hub, null) },
            label = { Text("Red") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MycoGreen,
                selectedTextColor = MycoGreen,
                indicatorColor = MycoGreenDark.copy(alpha = 0.3f),
                unselectedIconColor = MycoSubtle,
                unselectedTextColor = MycoSubtle
            )
        )
        NavigationBarItem(
            selected = selectedTab == 3,
            onClick = { onSelectTab(3) },
            icon = { Icon(Icons.Default.Settings, null) },
            label = { Text("Ajustes") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MycoGreen,
                selectedTextColor = MycoGreen,
                indicatorColor = MycoGreenDark.copy(alpha = 0.3f),
                unselectedIconColor = MycoSubtle,
                unselectedTextColor = MycoSubtle
            )
        )
    }
}
