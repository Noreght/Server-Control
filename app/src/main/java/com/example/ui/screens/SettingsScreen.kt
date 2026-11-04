package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GithubVectorIcon

@Composable
fun SettingsScreen(
  isBiometricSupported: Boolean,
  isBiometricEnabled: Boolean,
  onToggleBiometric: (Boolean) -> Unit,
  onChangePin: (oldPin: String, newPin: String) -> Boolean,
  onLockApp: () -> Unit,
  onExportServers: (Uri) -> Unit,
  onImportServers: (Uri) -> Unit,
  serverCount: Int = 0,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var showChangePinDialog by remember { mutableStateOf(false) }

  val exportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/octet-stream")
  ) { uri ->
    if (uri != null) {
      onExportServers(uri)
    }
  }

  val importLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      onImportServers(uri)
    }
  }

  if (showChangePinDialog) {
    ChangePinDialog(
      onChangePin = { oldPin, newPin ->
        onChangePin(oldPin, newPin)
      },
      onDismiss = { showChangePinDialog = false }
    )
  }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val minScreenHeight = maxHeight
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp)
        .defaultMinSize(minHeight = minScreenHeight)
        .testTag("settings_screen"),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
      Text(
        text = "Security",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
      )

      OutlinedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          // 1. Change PIN code item
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { showChangePinDialog = true }
              .padding(16.dp)
              .testTag("change_pin_item"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.LockReset,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.width(16.dp))
              Column {
                Text(
                  text = "Change PIN Code",
                  style = MaterialTheme.typography.bodyLarge,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = "Update your 4-digit master access PIN",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
              tint = MaterialTheme.colorScheme.outline
            )
          }

          // 2. Fingerprint toggle item
          if (isBiometricSupported) {
            androidx.compose.material3.HorizontalDivider(
              modifier = Modifier.padding(horizontal = 16.dp),
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("biometric_toggle_item"),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.Fingerprint,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                  Text(
                    text = "Unlock with Fingerprint",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                  )
                  Text(
                    text = "Use biometric authentication for faster access",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              Switch(
                checked = isBiometricEnabled,
                onCheckedChange = onToggleBiometric,
                modifier = Modifier.testTag("biometric_switch")
              )
            }
          }

          // 3. Lock App Now item
          androidx.compose.material3.HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
          )

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onLockApp() }
              .padding(16.dp)
              .testTag("lock_app_now_item"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.width(16.dp))
              Column {
                Text(
                  text = "Lock Application",
                  style = MaterialTheme.typography.bodyLarge,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = "Immediately lock access to the app",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
              tint = MaterialTheme.colorScheme.outline
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = "Резервное копирование",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
      )

      OutlinedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          // 1. Export servers item
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                try {
                  exportLauncher.launch("servers.backup")
                } catch (e: Exception) {
                  Toast.makeText(context, "Не удалось открыть проводник: ${e.message}", Toast.LENGTH_LONG).show()
                }
              }
              .padding(16.dp)
              .testTag("export_servers_item"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.FileUpload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.width(16.dp))
              Column {
                Text(
                  text = "Экспорт серверов",
                  style = MaterialTheme.typography.bodyLarge,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = "Сохранить конфигурации в servers.backup (без паролей)",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
              tint = MaterialTheme.colorScheme.outline
            )
          }

          androidx.compose.material3.HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
          )

          // 2. Import servers item
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                try {
                  importLauncher.launch(arrayOf("*/*"))
                } catch (e: Exception) {
                  Toast.makeText(context, "Не удалось открыть проводник: ${e.message}", Toast.LENGTH_LONG).show()
                }
              }
              .padding(16.dp)
              .testTag("import_servers_item"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.FileDownload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.width(16.dp))
              Column {
                Text(
                  text = "Импорт серверов",
                  style = MaterialTheme.typography.bodyLarge,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = "Загрузить конфигурации из файла .backup",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
              tint = MaterialTheme.colorScheme.outline
            )
          }
        }
      }
    }

    // Bottom section: App Version and GitHub link arranged vertically
    Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 28.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // App Version
        Text(
          text = "Version 1.0",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // GitHub link with GitHub icon beside it
        Row(
          modifier = Modifier
            .clickable {
              val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Noreght"))
              context.startActivity(intent)
            }
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("github_link_button"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          GithubVectorIcon(
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "https://github.com/Noreght",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
          )
        }
      }
    }
  }
}

@Composable
private fun ChangePinDialog(
  onChangePin: (oldPin: String, newPin: String) -> Boolean,
  onDismiss: () -> Unit
) {
  var oldPin by remember { mutableStateOf("") }
  var newPin by remember { mutableStateOf("") }
  var confirmPin by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text("Change PIN Code", fontWeight = FontWeight.Bold)
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedTextField(
          value = oldPin,
          onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) oldPin = it },
          label = { Text("Current PIN (4 digits)") },
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("current_pin_input")
        )

        OutlinedTextField(
          value = newPin,
          onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPin = it },
          label = { Text("New PIN (4 digits)") },
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("new_pin_input")
        )

        OutlinedTextField(
          value = confirmPin,
          onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it },
          label = { Text("Confirm New PIN") },
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("confirm_new_pin_input")
        )

        if (errorMessage != null) {
          Text(
            text = errorMessage!!,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (oldPin.length != 4) {
            errorMessage = "Current PIN must be 4 digits"
            return@Button
          }
          if (newPin.length != 4) {
            errorMessage = "New PIN must be 4 digits"
            return@Button
          }
          if (newPin != confirmPin) {
            errorMessage = "New PIN and confirmation do not match"
            return@Button
          }
          val success = onChangePin(oldPin, newPin)
          if (success) {
            onDismiss()
          } else {
            errorMessage = "Current PIN is incorrect"
          }
        },
        modifier = Modifier.testTag("save_new_pin_button")
      ) {
        Text("Change PIN")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
