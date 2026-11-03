package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.AuthMethod
import com.example.ui.ServerFormState
import com.example.ui.theme.StatusRunning
import com.example.ui.theme.StatusRunningBg

@Composable
fun ServerDialog(
  formState: ServerFormState,
  onFormChange: ((ServerFormState) -> ServerFormState) -> Unit,
  onPickKeyFile: (android.net.Uri) -> Unit,
  onTestConnection: () -> Unit,
  onSave: () -> Unit,
  onDismiss: () -> Unit
) {
  var passwordVisible by remember { mutableStateOf(false) }
  var passphraseVisible by remember { mutableStateOf(false) }
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current

  val keyPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri ->
    if (uri != null) {
      try {
        onPickKeyFile(uri)
      } catch (e: Exception) {
        Toast.makeText(context, "Error loading key: ${e.message}", Toast.LENGTH_SHORT).show()
      }
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier
      .padding(16.dp)
      .fillMaxWidth()
      .testTag("server_dialog"),
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Dns,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
          text = if (formState.id == 0L) "Connect Linux Server" else "Edit Server",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "Specify your Linux server IP address and SSH credentials to manage Docker containers.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Server friendly name
        OutlinedTextField(
          value = formState.name,
          onValueChange = { onFormChange { s -> s.copy(name = it) } },
          label = { Text("Server Name") },
          placeholder = { Text("e.g. Production VPS") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("server_name_input")
        )

        // IP and Port in Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = formState.host,
            onValueChange = { onFormChange { s -> s.copy(host = it) } },
            label = { Text("IP / Hostname *") },
            placeholder = { Text("192.168.1.50") },
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              .testTag("server_host_input")
          )

          OutlinedTextField(
            value = formState.port,
            onValueChange = { onFormChange { s -> s.copy(port = it) } },
            label = { Text("Port") },
            placeholder = { Text("22") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
              .width(90.dp)
              .testTag("server_port_input")
          )
        }

        // Username
        OutlinedTextField(
          value = formState.username,
          onValueChange = { onFormChange { s -> s.copy(username = it) } },
          label = { Text("SSH Username") },
          placeholder = { Text("root") },
          leadingIcon = {
            Icon(Icons.Default.Person, contentDescription = null)
          },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("server_user_input")
        )

        // Auth Method Choice
        Text(
          text = "Authentication Method",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FilterChip(
            selected = formState.authType == AuthMethod.PASSWORD,
            onClick = { onFormChange { s -> s.copy(authType = AuthMethod.PASSWORD) } },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) },
            modifier = Modifier.weight(1f)
          )

          FilterChip(
            selected = formState.authType == AuthMethod.PRIVATE_KEY,
            onClick = { onFormChange { s -> s.copy(authType = AuthMethod.PRIVATE_KEY) } },
            label = { Text("SSH Key File") },
            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp)) },
            modifier = Modifier.weight(1f)
          )
        }

        // Auth Method inputs
        if (formState.authType == AuthMethod.PASSWORD) {
          OutlinedTextField(
            value = formState.password,
            onValueChange = { onFormChange { s -> s.copy(password = it) } },
            label = { Text("Password") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
              IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                  imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = "Toggle password"
                )
              }
            },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("server_password_input")
          )
        } else {
          // SSH Private Key section
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
              .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = "Private Key File (RSA, Ed25519, PEM)",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
              val isWide = maxWidth >= 260.dp
              if (isWide) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  OutlinedButton(
                    onClick = {
                      try {
                        keyPickerLauncher.launch("*/*")
                      } catch (e: Exception) {
                        Toast.makeText(context, "Cannot open file picker: ${e.message}", Toast.LENGTH_LONG).show()
                      }
                    },
                    modifier = Modifier
                      .weight(1f)
                      .testTag("pick_key_file_button"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                  ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Select file", maxLines = 1)
                  }

                  OutlinedButton(
                    onClick = {
                      val clip = clipboardManager.getText()?.text
                      if (!clip.isNullOrBlank()) {
                        onFormChange { s ->
                          s.copy(
                            privateKeyContent = clip.trim(),
                            privateKeyFileName = "pasted_key",
                            testResult = null
                          )
                        }
                        Toast.makeText(context, "Key pasted from clipboard", Toast.LENGTH_SHORT).show()
                      } else {
                        Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                      }
                    },
                    modifier = Modifier
                      .weight(1f)
                      .testTag("paste_key_button"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                  ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Paste", maxLines = 1)
                  }
                }
              } else {
                Column(
                  modifier = Modifier.fillMaxWidth(),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  OutlinedButton(
                    onClick = {
                      try {
                        keyPickerLauncher.launch("*/*")
                      } catch (e: Exception) {
                        Toast.makeText(context, "Cannot open file picker: ${e.message}", Toast.LENGTH_LONG).show()
                      }
                    },
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("pick_key_file_button"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                  ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Select file")
                  }

                  OutlinedButton(
                    onClick = {
                      val clip = clipboardManager.getText()?.text
                      if (!clip.isNullOrBlank()) {
                        onFormChange { s ->
                          s.copy(
                            privateKeyContent = clip.trim(),
                            privateKeyFileName = "pasted_key",
                            testResult = null
                          )
                        }
                        Toast.makeText(context, "Key pasted from clipboard", Toast.LENGTH_SHORT).show()
                      } else {
                        Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                      }
                    },
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("paste_key_button"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                  ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Paste")
                  }
                }
              }
            }

            if (formState.privateKeyContent.isNotBlank()) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .background(MaterialTheme.colorScheme.surface)
                  .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                  Text(
                    text = if (formState.privateKeyFileName.isNotBlank() && formState.privateKeyFileName != "pasted_key")
                      "Key loaded (${formState.privateKeyFileName})"
                    else
                      "Key loaded",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                  )
                  Text(
                    text = formState.privateKeyContent.lines().firstOrNull().orEmpty(),
                    style = MaterialTheme.typography.bodySmall.copy(
                      fontFamily = FontFamily.Monospace,
                      fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }

                IconButton(
                  onClick = {
                    onFormChange { s ->
                      s.copy(
                        privateKeyContent = "",
                        privateKeyFileName = "",
                        testResult = null
                      )
                    }
                  },
                  modifier = Modifier.testTag("clear_key_button")
                ) {
                  Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear key",
                    tint = MaterialTheme.colorScheme.error
                  )
                }
              }
            }

            // Optional Passphrase
            OutlinedTextField(
              value = formState.passphrase,
              onValueChange = { onFormChange { s -> s.copy(passphrase = it) } },
              label = { Text("Key Passphrase (Optional)") },
              placeholder = { Text("Leave empty if no passphrase") },
              visualTransformation = if (passphraseVisible) VisualTransformation.None else PasswordVisualTransformation(),
              trailingIcon = {
                IconButton(onClick = { passphraseVisible = !passphraseVisible }) {
                  Icon(
                    imageVector = if (passphraseVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Toggle passphrase"
                  )
                }
              },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("server_passphrase_input")
            )
          }
        }

        // Test Connection Button & Animated Result Icon in the same row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Start,
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = onTestConnection,
            enabled = !formState.isTesting && formState.host.isNotBlank(),
            modifier = Modifier.testTag("test_connection_button")
          ) {
            if (formState.isTesting) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Testing...")
            } else {
              Text("Test SSH Connection")
            }
          }

          Spacer(modifier = Modifier.width(16.dp))

          AnimatedVisibility(
            visible = formState.testResult != null && !formState.isTesting,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
          ) {
            formState.testResult?.let { (isSuccess, _) ->
              if (isSuccess) {
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(StatusRunningBg)
                    .border(1.5.dp, StatusRunning, CircleShape)
                    .testTag("ssh_test_success_indicator"),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Success",
                    tint = StatusRunning,
                    modifier = Modifier.size(22.dp)
                  )
                }
              } else {
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                    .border(1.5.dp, MaterialTheme.colorScheme.error, CircleShape)
                    .testTag("ssh_test_failure_indicator"),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Failure",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(22.dp)
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onSave,
        enabled = formState.host.isNotBlank(),
        modifier = Modifier.testTag("save_server_button")
      ) {
        Text("Connect & Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
