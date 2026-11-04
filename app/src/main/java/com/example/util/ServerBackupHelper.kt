package com.example.util

import com.example.data.AuthMethod
import com.example.data.ServerEntity
import org.json.JSONArray
import org.json.JSONObject

object ServerBackupHelper {

  /**
   * Serializes the list of servers into a clean JSON structure.
   * Includes everything present in edit server (name, host, port, username, authType,
   * and if key: the private key string and optional key file name).
   * EXCLUDES passwords and passphrases for security.
   */
  fun exportServersToJson(
    servers: List<ServerEntity>,
    decryptServer: (ServerEntity) -> ServerEntity
  ): String {
    val root = JSONObject()
    root.put("version", 1)
    root.put("exportedAt", System.currentTimeMillis())

    val jsonArray = JSONArray()
    for (server in servers) {
      val decrypted = decryptServer(server)
      val obj = JSONObject()
      obj.put("name", decrypted.name)
      obj.put("host", decrypted.host)
      obj.put("port", decrypted.port)
      obj.put("username", decrypted.username)
      obj.put("authType", decrypted.authType)

      // Include private key content and file name if authentication type is PRIVATE_KEY
      if (decrypted.authType == AuthMethod.PRIVATE_KEY.name) {
        val key = decrypted.privateKeyContent?.trim().orEmpty()
        if (key.isNotEmpty()) {
          obj.put("privateKeyContent", key)
        }
        val fileName = decrypted.privateKeyFileName?.trim().orEmpty()
        if (fileName.isNotEmpty()) {
          obj.put("privateKeyFileName", fileName)
        }
      }
      // Strictly omit password and passphrase!
      jsonArray.put(obj)
    }

    root.put("servers", jsonArray)
    return root.toString(2)
  }

  /**
   * Parses JSON string into a list of ServerEntity objects.
   * Supports both object format {"servers": [...]} and direct array format [...].
   * Sets needsAuthPrompt = true so the app asks for password or passphrase on first connection.
   */
  fun parseServersFromJson(jsonString: String): Result<List<ServerEntity>> {
    return try {
      val trimmed = jsonString.trim()
      if (trimmed.isEmpty()) {
        return Result.failure(IllegalArgumentException("Backup file is empty"))
      }

      val jsonArray = if (trimmed.startsWith("[")) {
        JSONArray(trimmed)
      } else {
        val root = JSONObject(trimmed)
        if (root.has("servers")) {
          root.getJSONArray("servers")
        } else {
          return Result.failure(IllegalArgumentException("Invalid backup format: 'servers' array not found"))
        }
      }

      val resultList = mutableListOf<ServerEntity>()
      val now = System.currentTimeMillis()

      for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.optJSONObject(i) ?: continue
        val host = obj.optString("host", "").trim()
        if (host.isEmpty()) continue

        val name = obj.optString("name", "").trim().ifEmpty { host }
        val port = obj.optInt("port", 22).let { if (it in 1..65535) it else 22 }
        val username = obj.optString("username", "root").trim().ifEmpty { "root" }
        val authTypeRaw = obj.optString("authType", AuthMethod.PASSWORD.name).trim()
        val authType = if (authTypeRaw.equals(AuthMethod.PRIVATE_KEY.name, ignoreCase = true)) {
          AuthMethod.PRIVATE_KEY.name
        } else {
          AuthMethod.PASSWORD.name
        }

        val privateKeyContent = if (authType == AuthMethod.PRIVATE_KEY.name) {
          obj.optString("privateKeyContent", "").trim().takeIf { it.isNotEmpty() }
        } else {
          null
        }

        val privateKeyFileName = if (authType == AuthMethod.PRIVATE_KEY.name) {
          obj.optString("privateKeyFileName", "imported_key").trim().ifEmpty { "imported_key" }
        } else {
          null
        }

        resultList.add(
          ServerEntity(
            id = 0,
            name = name,
            host = host,
            port = port,
            username = username,
            authType = authType,
            password = null,
            privateKeyContent = privateKeyContent,
            privateKeyFileName = privateKeyFileName,
            passphrase = null,
            isActive = false,
            needsAuthPrompt = true,
            createdAt = now + i
          )
        )
      }

      if (resultList.isEmpty()) {
        Result.failure(IllegalArgumentException("No valid servers found in backup file"))
      } else {
        Result.success(resultList)
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
