package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Server Control", appName)
  }

  @Test
  fun `container parsing and status mapping`() {
    val container = com.example.model.DockerContainer(
      id = "c1a2b3c4d5e6f7a8b9",
      name = "/web-nginx",
      image = "nginx:alpine",
      status = "Up 4 hours",
      state = com.example.model.ContainerState.fromString("running"),
      ports = "0.0.0.0:80->80/tcp",
      createdAt = "2026-09-16 10:00:00"
    )

    assertEquals("web-nginx", container.cleanName)
    assertEquals("c1a2b3c4d5e6", container.shortId)
    assertEquals(com.example.model.ContainerState.RUNNING, container.state)
    assertEquals(true, container.isRunning)
  }

  @Test
  fun `StorageFileHelper formats file sizes correctly`() {
    assertEquals("0 B", com.example.util.StorageFileHelper.formatFileSize(0))
    assertEquals("512 B", com.example.util.StorageFileHelper.formatFileSize(512))
    assertEquals("1.5 KB", com.example.util.StorageFileHelper.formatFileSize(1536))
    assertEquals("2.0 MB", com.example.util.StorageFileHelper.formatFileSize(2 * 1024 * 1024))
  }

  @Test
  fun `readSelectedFile reads byte stream from file uri`() = kotlinx.coroutines.test.runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val testFile = java.io.File(context.cacheDir, "test_key.pem")
    val fileContent = "-----BEGIN RSA PRIVATE KEY-----\nMIIEowIBAAKCAQEA0...\n-----END RSA PRIVATE KEY-----"
    testFile.writeText(fileContent)

    val uri = android.net.Uri.fromFile(testFile)
    val result = com.example.util.StorageFileHelper.readSelectedFile(context, uri)
    assertEquals(true, result.isSuccess)
    val info = result.getOrThrow()
    assertEquals(fileContent, info.contentText)
    assertEquals(fileContent.toByteArray().size.toLong(), info.sizeBytes)
  }
}
