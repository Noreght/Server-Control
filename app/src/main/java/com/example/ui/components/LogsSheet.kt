package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.LogsDialogState
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsSheet(
  state: LogsDialogState,
  onRefresh: () -> Unit,
  onChangeTail: (Int) -> Unit,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val context = LocalContext.current

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    modifier = Modifier.testTag("logs_bottom_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
      // Top bar with container title and close button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Terminal,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "Logs: ${state.containerName}",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "ID: ${state.containerId.take(12)}",
              style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              val clip = ClipData.newPlainText("Docker Logs", state.logs)
              clipboard.setPrimaryClip(clip)
              Toast.makeText(context, "Logs copied to clipboard", Toast.LENGTH_SHORT).show()
            },
            enabled = state.logs.isNotBlank(),
            modifier = Modifier.testTag("copy_logs_button")
          ) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy logs")
          }

          IconButton(
            onClick = onRefresh,
            enabled = !state.isLoading,
            modifier = Modifier.testTag("refresh_logs_button")
          ) {
            if (state.isLoading) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
              Icon(Icons.Default.Refresh, contentDescription = "Refresh logs")
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_logs_button")
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Tail filter chips
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Tail lines:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        listOf(50, 100, 200, 500).forEach { lines ->
          FilterChip(
            selected = state.tailLines == lines,
            onClick = { onChangeTail(lines) },
            label = { Text("$lines", fontSize = 12.sp) },
            modifier = Modifier.height(30.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Terminal Box with monospace font and high contrast
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(420.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(TerminalBg)
          .padding(12.dp)
      ) {
        val verticalScrollState = rememberScrollState()
        val horizontalScrollState = rememberScrollState()

        if (state.isLoading && state.logs.isBlank()) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              CircularProgressIndicator(color = TerminalGreen, modifier = Modifier.size(32.dp))
              Spacer(modifier = Modifier.height(8.dp))
              Text("Streaming logs from container...", color = TerminalGreen, style = MaterialTheme.typography.bodySmall)
            }
          }
        } else if (!state.error.isNullOrBlank()) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.padding(16.dp)
            ) {
              Text(
                text = "Error fetching logs:",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = state.error,
                color = Color.White,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
              )
              Spacer(modifier = Modifier.height(12.dp))
              OutlinedButton(
                onClick = onRefresh,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TerminalGreen)
              ) {
                Text("Retry")
              }
            }
          }
        } else {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .verticalScroll(verticalScrollState)
              .horizontalScroll(horizontalScrollState)
          ) {
            Text(
              text = state.logs,
              style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 16.sp
              ),
              color = TerminalGreen
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
