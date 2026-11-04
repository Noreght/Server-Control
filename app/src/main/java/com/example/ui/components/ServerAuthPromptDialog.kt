package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.AuthMethod
import com.example.data.ServerEntity

@Composable
fun ServerAuthPromptDialog(
  server: ServerEntity,
  onConfirm: (enteredAuth: String) -> Unit,
  onDismiss: () -> Unit
) {
  val isKeyAuth = server.authType == AuthMethod.PRIVATE_KEY.name
  var inputVal by remember { mutableStateOf("") }
  var isVisible by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier.testTag("server_auth_prompt_dialog"),
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = if (isKeyAuth) Icons.Default.Key else Icons.Default.Lock,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = if (isKeyAuth) "Passphrase для SSH-ключа" else "Пароль для сервера",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "${server.name} (${server.username}@${server.host}:${server.port})",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface
        )

        Text(
          text = if (isKeyAuth) {
            "Конфигурация сервера была импортирована. Введите кодовую фразу (passphrase) для приватного ключа или оставьте поле пустым, если ключ не защищён паролем:"
          } else {
            "Конфигурация сервера была импортирована. Введите пароль пользователя ${server.username} для подключения:"
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
          value = inputVal,
          onValueChange = {
            inputVal = it
            errorMessage = null
          },
          label = { Text(if (isKeyAuth) "Key Passphrase (опционально)" else "Пароль") },
          placeholder = { Text(if (isKeyAuth) "Оставьте пустым, если нет passphrase" else "Введите пароль") },
          visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
          trailingIcon = {
            IconButton(onClick = { isVisible = !isVisible }) {
              Icon(
                imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = "Показать/скрыть"
              )
            }
          },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("auth_prompt_input")
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
          if (!isKeyAuth && inputVal.isBlank()) {
            errorMessage = "Пожалуйста, введите пароль"
            return@Button
          }
          onConfirm(inputVal)
        },
        modifier = Modifier.testTag("auth_prompt_confirm_button")
      ) {
        Text("Подключиться")
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("auth_prompt_cancel_button")
      ) {
        Text("Отмена")
      }
    }
  )
}
