package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.components.ContainerCard(
          container = com.example.model.DockerContainer(
            id = "abcdef1234567890",
            name = "nginx-proxy",
            image = "nginx:latest",
            status = "Up 3 hours",
            state = com.example.model.ContainerState.RUNNING,
            ports = "0.0.0.0:80->80/tcp, 0.0.0.0:443->443/tcp",
            createdAt = "2026-09-16 08:00:00"
          ),
          isActionInProgress = false,
          onStart = {},
          onStop = {},
          onRestart = {},
          onDelete = {},
          onViewLogs = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
