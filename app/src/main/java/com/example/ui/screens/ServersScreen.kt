package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AuthMethod
import com.example.data.ServerEntity
import com.example.ui.theme.StatusRunning
import com.example.ui.theme.StatusRunningBg

@Composable
fun ServersScreen(
  isInitialServersLoaded: Boolean,
  servers: List<ServerEntity>,
  activeServer: ServerEntity?,
  onAddServer: () -> Unit,
  onSelectActiveServer: (ServerEntity) -> Unit,
  onEditServer: (ServerEntity) -> Unit,
  onDeleteServer: (ServerEntity) -> Unit,
  modifier: Modifier = Modifier
) {
  var serverToDelete by remember { mutableStateOf<ServerEntity?>(null) }

  if (!isInitialServersLoaded) {
    Box(
      modifier = modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      androidx.compose.material3.CircularProgressIndicator()
    }
    return
  }

  if (serverToDelete != null) {
    val target = serverToDelete!!
    AlertDialog(
      onDismissRequest = { serverToDelete = null },
      title = { Text("Delete Server") },
      text = { Text("Are you sure you want to remove '${target.name}' (${target.host}) from your saved servers?") },
      confirmButton = {
        TextButton(
          onClick = {
            onDeleteServer(target)
            serverToDelete = null
          },
          colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { serverToDelete = null }) {
          Text("Cancel")
        }
      }
    )
  }

  Box(
    modifier = modifier.fillMaxSize()
  ) {
    if (servers.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Default.Dns,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
          )
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "No Linux Servers Added",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Add your first server to connect via SSH and inspect Docker containers.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(20.dp))
          Button(onClick = onAddServer) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Server Now")
          }
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(
          items = servers,
          key = { it.id }
        ) { server ->
          val isActive = activeServer?.id == server.id

          OutlinedCard(
            onClick = { onSelectActiveServer(server) },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("server_card_${server.id}"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.outlinedCardColors(
              containerColor = if (isActive)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
              else
                MaterialTheme.colorScheme.surface
            )
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.Dns,
                    contentDescription = null,
                    tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(24.dp)
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Column {
                    Text(
                      text = server.name,
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    )
                    Text(
                      text = "${server.username}@${server.host}:${server.port}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }

                if (isActive) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                      .clip(RoundedCornerShape(16.dp))
                      .background(StatusRunningBg)
                      .border(1.dp, StatusRunning.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                      .padding(horizontal = 8.dp, vertical = 4.dp)
                  ) {
                    Box(
                      modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(StatusRunning)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "Active",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = StatusRunning
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              // Authentication Badge & file details
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                val isKey = server.authType == AuthMethod.PRIVATE_KEY.name
                Icon(
                  imageVector = if (isKey) Icons.Default.Key else Icons.Default.Lock,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                  tint = MaterialTheme.colorScheme.outline
                )
                Text(
                  text = if (isKey) {
                    server.privateKeyFileName?.takeIf { it.isNotBlank() } ?: "Private Key"
                  } else {
                    "Password Authentication"
                  },
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }

              Spacer(modifier = Modifier.height(14.dp))

              // Actions Row
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
              ) {
                IconButton(
                  onClick = { onEditServer(server) },
                  modifier = Modifier.testTag("edit_server_btn_${server.id}")
                ) {
                  Icon(Icons.Default.Edit, contentDescription = "Edit server")
                }
                IconButton(
                  onClick = { serverToDelete = server },
                  modifier = Modifier.testTag("delete_server_btn_${server.id}")
                ) {
                  Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete server",
                    tint = MaterialTheme.colorScheme.error
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
