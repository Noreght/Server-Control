package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun LockScreen(
  isPinSet: Boolean,
  isBiometricAvailable: Boolean,
  onVerifyPin: (String) -> Boolean,
  onSetInitialPin: (String) -> Unit,
  onTriggerBiometric: () -> Unit,
  modifier: Modifier = Modifier
) {
  var enteredPin by remember { mutableStateOf("") }
  var confirmPinStage by remember { mutableStateOf(false) }
  var firstPinAttempt by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val shakeOffset = remember { Animatable(0f) }
  val coroutineScope = rememberCoroutineScope()

  fun triggerShake(message: String) {
    errorMessage = message
    enteredPin = ""
    coroutineScope.launch {
      shakeOffset.animateTo(
        targetValue = 0f,
        animationSpec = keyframes {
          durationMillis = 400
          0f at 0
          -20f at 50
          20f at 100
          -15f at 150
          15f at 200
          -10f at 250
          10f at 300
          0f at 400
        }
      )
    }
  }

  fun onDigitClick(digit: String) {
    if (enteredPin.length < 4) {
      val next = enteredPin + digit
      enteredPin = next
      errorMessage = null

      if (next.length == 4) {
        if (!isPinSet) {
          // Creating initial PIN flow
          if (!confirmPinStage) {
            firstPinAttempt = next
            confirmPinStage = true
            enteredPin = ""
          } else {
            if (next == firstPinAttempt) {
              onSetInitialPin(next)
            } else {
              triggerShake("PIN codes do not match. Try again.")
              confirmPinStage = false
              firstPinAttempt = ""
            }
          }
        } else {
          // Verify existing PIN
          val valid = onVerifyPin(next)
          if (!valid) {
            triggerShake("Incorrect PIN code")
          }
        }
      }
    }
  }

  fun onBackspaceClick() {
    if (enteredPin.isNotEmpty()) {
      enteredPin = enteredPin.dropLast(1)
      errorMessage = null
    }
  }

  // Trigger biometric check on initial display if configured
  LaunchedEffect(isPinSet, isBiometricAvailable) {
    if (isPinSet && isBiometricAvailable) {
      onTriggerBiometric()
    }
  }

  Surface(
    modifier = modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 28.dp, vertical = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Top header with app logo and status
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 24.dp)
      ) {
        Box(
          modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = stringResource(R.string.app_name),
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        val titleText = when {
          !isPinSet && !confirmPinStage -> "Create a 4-digit PIN code"
          !isPinSet && confirmPinStage -> "Confirm your 4-digit PIN"
          else -> "Enter PIN code to unlock"
        }

        Text(
          text = titleText,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // 4 PIN Dots
        Row(
          modifier = Modifier
            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
            .padding(vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(20.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          for (i in 0 until 4) {
            val isFilled = i < enteredPin.length
            Box(
              modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(
                  if (isFilled) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.surfaceVariant
                )
                .border(
                  width = 2.dp,
                  color = if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                  shape = CircleShape
                )
            )
          }
        }

        // Error message if any
        AnimatedVisibility(
          visible = errorMessage != null,
          enter = fadeIn(),
          exit = fadeOut()
        ) {
          Text(
            text = errorMessage ?: "",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 12.dp)
          )
        }
      }

      // Numeric Keypad
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        val rows = listOf(
          listOf("1", "2", "3"),
          listOf("4", "5", "6"),
          listOf("7", "8", "9")
        )

        for (row in rows) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
          ) {
            for (digit in row) {
              KeypadButton(
                text = digit,
                onClick = { onDigitClick(digit) }
              )
            }
          }
        }

        // Bottom row: Biometric (if available) / Empty, "0", Backspace
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (isPinSet && isBiometricAvailable) {
            Box(
              modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .clickable(onClick = onTriggerBiometric)
                .testTag("biometric_unlock_button"),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = "Fingerprint unlock",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
              )
            }
          } else {
            Spacer(modifier = Modifier.size(72.dp))
          }

          KeypadButton(
            text = "0",
            onClick = { onDigitClick("0") }
          )

          Box(
            modifier = Modifier
              .size(72.dp)
              .clip(CircleShape)
              .clickable(onClick = { onBackspaceClick() })
              .testTag("keypad_backspace_button"),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Backspace,
              contentDescription = "Backspace",
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(28.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun KeypadButton(
  text: String,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .size(72.dp)
      .clip(CircleShape)
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
      .clickable(onClick = onClick)
      .testTag("keypad_button_$text"),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.headlineMedium.copy(
        fontSize = 28.sp,
        fontWeight = FontWeight.Medium
      ),
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}
