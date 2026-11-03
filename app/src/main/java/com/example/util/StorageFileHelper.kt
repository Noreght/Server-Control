package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class SelectedFileInfo(
  val name: String,
  val sizeBytes: Long,
  val formattedSize: String,
  val contentText: String,
  val bytes: ByteArray
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as SelectedFileInfo
    return name == other.name && sizeBytes == other.sizeBytes && contentText == other.contentText
  }

  override fun hashCode(): Int {
    var result = name.hashCode()
    result = 31 * result + sizeBytes.hashCode()
    result = 31 * result + contentText.hashCode()
    return result
  }
}

object StorageFileHelper {

  suspend fun readSelectedFile(context: Context, uri: Uri): Result<SelectedFileInfo> = withContext(Dispatchers.IO) {
    try {
      var displayName = "selected_file"
      var fileSize: Long = -1L

      // Query metadata (file name and size) using OpenableColumns via ContentResolver
      try {
        context.contentResolver.query(
          uri,
          arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
          null,
          null,
          null
        )?.use { cursor ->
          if (cursor.moveToFirst()) {
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIdx != -1 && !cursor.isNull(nameIdx)) {
              displayName = cursor.getString(nameIdx)
            }
            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIdx != -1 && !cursor.isNull(sizeIdx)) {
              fileSize = cursor.getLong(sizeIdx)
            }
          }
        }
      } catch (_: Exception) {
        // Some custom providers may restrict metadata columns; fallback continues
      }

      // Read byte stream through openInputStream() without requiring dangerous storage permissions
      val bytes = context.contentResolver.openInputStream(uri)?.use { inputStream ->
        inputStream.readBytes()
      } ?: return@withContext Result.failure(IllegalStateException("Could not open input stream for file URI"))

      val actualSize = if (fileSize >= 0) fileSize else bytes.size.toLong()
      val formattedSize = formatFileSize(actualSize)
      val contentText = bytes.toString(Charsets.UTF_8).trim()

      Result.success(
        SelectedFileInfo(
          name = displayName,
          sizeBytes = actualSize,
          formattedSize = formattedSize,
          contentText = contentText,
          bytes = bytes
        )
      )
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun formatFileSize(bytes: Long): String {
    return when {
      bytes < 0 -> "0 B"
      bytes < 1024 -> "$bytes B"
      bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
      bytes < 1024 * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
      else -> String.format(Locale.US, "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
  }
}
