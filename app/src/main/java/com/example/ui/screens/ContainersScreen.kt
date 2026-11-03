package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ServerEntity
import com.example.model.ContainerState
import com.example.model.DockerContainer
import com.example.ui.ContainersUiState
import com.example.ui.components.ContainerCard
import com.example.ui.theme.StatusRunning

@Composable
fun ContainersScreen(
  isInitialServersLoaded: Boolean,
  activeServer: ServerEntity?,
  containersState: ContainersUiState,
  isRefreshing: Boolean,
  actionInProgressContainerId: String?,
  searchQuery: String,
  stateFilter: ContainerState?,
  onSearchChange: (String) -> Unit,
  onStateFilterChange: (ContainerState?) -> Unit,
  onRefresh: () -> Unit,
  onStartContainer: (DockerContainer) -> Unit,
  onStopContainer: (DockerContainer) -> Unit,
  onRestartContainer: (DockerContainer) -> Unit,
  onDeleteContainer: (DockerContainer) -> Unit,
  onViewLogs: (DockerContainer) -> Unit,
  onConnectServerClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .testTag("containers_screen")
  ) {
    // 0. Database initial loading state (prevent empty screen flicker)
    if (!isInitialServersLoaded) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
      }
      return
    }

    // 1. First-time Onboarding / No Server state
    if (activeServer == null) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(24.dp),
        contentAlignment = Alignment.Center
      ) {
        ElevatedCard(
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
          ),
          elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Box(
              modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Dns,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
              )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
              text = "Connect to Linux Server",
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold,
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = "To manage Docker containers, connect to your server over SSH using IP and a Password or Private Key file.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
              onClick = onConnectServerClick,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("onboarding_connect_server_button")
            ) {
              Icon(Icons.Default.Add, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Connect Server Now", fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }
      return
    }

    // Search and Status Filters
    Spacer(modifier = Modifier.height(16.dp))
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        placeholder = { Text("Search container by name, image, or ID...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { onSearchChange("") }) {
              Icon(Icons.Default.Clear, contentDescription = "Clear search")
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("search_containers_input")
      )

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        FilterChip(
          selected = stateFilter == null,
          onClick = { onStateFilterChange(null) },
          label = { Text("All") },
          modifier = Modifier.testTag("filter_all")
        )

        FilterChip(
          selected = stateFilter == ContainerState.RUNNING,
          onClick = {
            onStateFilterChange(if (stateFilter == ContainerState.RUNNING) null else ContainerState.RUNNING)
          },
          label = { Text("Running") },
          modifier = Modifier.testTag("filter_running")
        )

        FilterChip(
          selected = stateFilter == ContainerState.EXITED,
          onClick = {
            onStateFilterChange(if (stateFilter == ContainerState.EXITED) null else ContainerState.EXITED)
          },
          label = { Text("Stopped") },
          modifier = Modifier.testTag("filter_stopped")
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 4. Containers List Content / States
    when (val state = containersState) {
      is ContainersUiState.Loading -> {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
              text = "Connecting via SSH and loading containers...",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      is ContainersUiState.Error -> {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
              .clip(RoundedCornerShape(16.dp))
              .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
              .border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(16.dp))
              .padding(20.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "Connection / Command Error",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = state.message,
              style = MaterialTheme.typography.bodySmall,
              textAlign = TextAlign.Center,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRefresh) {
              Text("Retry")
            }
          }
        }
      }

      is ContainersUiState.Success -> {
        val filteredList = state.containers.filter { container ->
          val matchesSearch = searchQuery.isBlank() ||
              container.name.contains(searchQuery, ignoreCase = true) ||
              container.image.contains(searchQuery, ignoreCase = true) ||
              container.id.contains(searchQuery, ignoreCase = true)

          val matchesState = stateFilter == null || container.state == stateFilter
          matchesSearch && matchesState
        }

        if (filteredList.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(32.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Default.CloudQueue,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.outline
              )
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = if (state.containers.isEmpty()) "No Docker containers found on server" else "No matching containers",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = if (state.containers.isEmpty())
                  "Use 'docker run' on host to create containers, or pull to refresh."
                else
                  "Try changing search text or filter.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
              )
              if (state.containers.isEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onRefresh) {
                  Text("Refresh")
                }
              }
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            items(
              items = filteredList,
              key = { it.id }
            ) { container ->
              ContainerCard(
                container = container,
                isActionInProgress = actionInProgressContainerId == container.id,
                onStart = { onStartContainer(container) },
                onStop = { onStopContainer(container) },
                onRestart = { onRestartContainer(container) },
                onDelete = { onDeleteContainer(container) },
                onViewLogs = { onViewLogs(container) }
              )
            }
          }
        }
      }

      is ContainersUiState.Idle -> {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator()
        }
      }
    }
  }
}
