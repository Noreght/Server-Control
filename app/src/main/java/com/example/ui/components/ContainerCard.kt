package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ContainerState
import com.example.model.DockerContainer
import com.example.ui.theme.StatusExited
import com.example.ui.theme.StatusExitedBg
import com.example.ui.theme.StatusPaused
import com.example.ui.theme.StatusPausedBg
import com.example.ui.theme.StatusRunning
import com.example.ui.theme.StatusRunningBg

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContainerCard(
  container: DockerContainer,
  isActionInProgress: Boolean,
  onStart: () -> Unit,
  onStop: () -> Unit,
  onRestart: () -> Unit,
  onDelete: () -> Unit,
  onViewLogs: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showDeleteConfirmDialog by remember { mutableStateOf(false) }

  if (showDeleteConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirmDialog = false },
      title = { Text("Delete Container") },
      text = {
        Text("Are you sure you want to permanently delete '${container.cleanName}' (${container.shortId})? This action cannot be undone.")
      },
      confirmButton = {
        TextButton(
          onClick = {
            showDeleteConfirmDialog = false
            onDelete()
          },
          colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.testTag("confirm_delete_button")
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showDeleteConfirmDialog = false }
        ) {
          Text("Cancel")
        }
      }
    )
  }

  val (statusColor, statusBgColor) = when (container.state) {
    ContainerState.RUNNING -> Pair(StatusRunning, StatusRunningBg)
    ContainerState.PAUSED, ContainerState.RESTARTING -> Pair(StatusPaused, StatusPausedBg)
    else -> Pair(StatusExited, StatusExitedBg)
  }

  OutlinedCard(
    modifier = modifier
      .fillMaxWidth()
      .testTag("container_card_${container.shortId}"),
    shape = RoundedCornerShape(16.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      // Top header: Name, Status Badge, and short ID
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = container.cleanName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "ID: ${container.shortId}",
            style = MaterialTheme.typography.bodySmall.copy(
              fontFamily = FontFamily.Monospace,
              fontSize = 11.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Status Badge
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(statusBgColor)
            .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(statusColor)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = container.state.displayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = statusColor
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Image Name
      Text(
        text = "Image: ${container.image}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      // Status text (e.g. "Up 2 hours" or "Exited (0) 5 mins ago")
      if (container.status.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = container.status,
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
          color = MaterialTheme.colorScheme.outline
        )
      }

      // Ports tags if present
      if (container.ports.isNotBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          val portTokens = container.ports.split(", ")
          portTokens.take(3).forEach { port ->
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = port.trim(),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontFamily = FontFamily.Monospace,
                  fontSize = 10.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
              )
            }
          }
          if (portTokens.size > 3) {
            Text(
              text = "+${portTokens.size - 3} more",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              color = MaterialTheme.colorScheme.outline,
              modifier = Modifier.align(Alignment.CenterVertically)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Action Buttons Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Start or Stop primary action
        if (isActionInProgress) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 6.dp)
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Applying...",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary
            )
          }
        } else {
          FilledTonalButton(
            onClick = {
              if (container.isRunning) onStop() else onStart()
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
              containerColor = if (container.isRunning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
              contentColor = if (container.isRunning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.testTag("container_start_stop_${container.shortId}")
          ) {
            Icon(
              imageVector = if (container.isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
              contentDescription = if (container.isRunning) "Stop" else "Start",
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (container.isRunning) "Stop" else "Start",
              fontWeight = FontWeight.Medium
            )
          }
        }

        // Secondary quick actions: Logs, Restart (1 click), Delete (1 click)
        Row(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Logs Button
          IconButton(
            onClick = onViewLogs,
            modifier = Modifier.testTag("container_logs_${container.shortId}")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Article,
              contentDescription = "View container logs",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          // Restart Button (One click)
          IconButton(
            onClick = onRestart,
            enabled = !isActionInProgress,
            modifier = Modifier.testTag("container_restart_${container.shortId}")
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Restart container in one click",
              tint = MaterialTheme.colorScheme.primary
            )
          }

          // Delete Button (One click with confirmation prompt)
          IconButton(
            onClick = { showDeleteConfirmDialog = true },
            enabled = !isActionInProgress,
            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.testTag("container_delete_${container.shortId}")
          ) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = "Delete container",
              tint = MaterialTheme.colorScheme.error
            )
          }
        }
      }
    }
  }
}
