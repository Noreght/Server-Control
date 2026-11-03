package com.example.ssh

import com.example.data.AuthMethod
import com.example.data.ServerEntity
import com.example.model.ContainerState
import com.example.model.DockerContainer
import com.example.model.HostSystemInfo
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

data class CommandResult(
  val exitCode: Int,
  val stdout: String,
  val stderr: String
) {
  val isSuccess: Boolean
    get() = exitCode == 0
}

object SshManager {

  private var activeSession: Session? = null
  private var activeServerId: Long? = null

  private fun createSession(server: ServerEntity): Session {
    val jsch = JSch()

    if (server.authType == AuthMethod.PRIVATE_KEY.name) {
      val keyContent = server.privateKeyContent?.trim()
      if (!keyContent.isNullOrEmpty()) {
        val keyBytes = keyContent.toByteArray(StandardCharsets.UTF_8)
        val passphraseBytes = if (!server.passphrase.isNullOrEmpty()) {
          server.passphrase.toByteArray(StandardCharsets.UTF_8)
        } else {
          null
        }
        jsch.addIdentity("server_key_${server.id}", keyBytes, null, passphraseBytes)
      }
    }

    val session = jsch.getSession(server.username.trim(), server.host.trim(), server.port)

    if (server.authType == AuthMethod.PASSWORD.name && !server.password.isNullOrEmpty()) {
      session.setPassword(server.password)
    }

    val config = java.util.Properties()
    config["StrictHostKeyChecking"] = "no"
    config["PreferredAuthentications"] = if (server.authType == AuthMethod.PRIVATE_KEY.name) {
      "publickey,password,keyboard-interactive"
    } else {
      "password,keyboard-interactive,publickey"
    }
    // Modern cipher and key-exchange preferences
    config["server_host_key"] = "ssh-ed25519,ecdsa-sha2-nistp256,ecdsa-sha2-nistp384,ecdsa-sha2-nistp521,rsa-sha2-512,rsa-sha2-256,ssh-rsa"
    session.setConfig(config)
    session.timeout = 15000
    return session
  }

  suspend fun testConnection(server: ServerEntity): Result<String> = withContext(Dispatchers.IO) {
    var testSession: Session? = null
    try {
      testSession = createSession(server)
      testSession.connect(12000)

      val channel = testSession.openChannel("exec") as ChannelExec
      channel.setCommand("docker --version || echo 'NO_DOCKER'")
      val outputStream = ByteArrayOutputStream()
      channel.outputStream = outputStream
      channel.connect(8000)

      val startTime = System.currentTimeMillis()
      while (!channel.isClosed && System.currentTimeMillis() - startTime < 8000) {
        Thread.sleep(50)
      }
      channel.disconnect()

      val output = outputStream.toString(StandardCharsets.UTF_8.name()).trim()
      if (output.contains("NO_DOCKER")) {
        Result.success("SSH connected successfully, but Docker was not found in PATH on the host.")
      } else {
        Result.success("Connection successful: $output")
      }
    } catch (e: Exception) {
      val message = when {
        e.message?.contains("Auth fail", ignoreCase = true) == true ->
          "Authentication failed. Please verify username, password/key or passphrase."
        e.message?.contains("timeout", ignoreCase = true) == true ->
          "Connection timed out. Check IP ${server.host} and port ${server.port}."
        e.message?.contains("Connection refused", ignoreCase = true) == true ->
          "Connection refused on ${server.host}:${server.port}. Ensure SSH server is running."
        else -> e.message ?: "Unknown connection error"
      }
      Result.failure(Exception(message, e))
    } finally {
      testSession?.disconnect()
    }
  }

  private fun getOrCreateSession(server: ServerEntity): Session {
    val existing = activeSession
    if (existing != null && existing.isConnected && activeServerId == server.id) {
      return existing
    }

    try {
      activeSession?.disconnect()
    } catch (_: Exception) {}

    val newSession = createSession(server)
    newSession.connect(15000)
    activeSession = newSession
    activeServerId = server.id
    return newSession
  }

  fun disconnect() {
    try {
      activeSession?.disconnect()
    } catch (_: Exception) {}
    activeSession = null
    activeServerId = null
  }

  suspend fun executeCommand(server: ServerEntity, command: String): Result<CommandResult> = withContext(Dispatchers.IO) {
    try {
      val session = try {
        getOrCreateSession(server)
      } catch (e: Exception) {
        // Retry once with clean session
        disconnect()
        getOrCreateSession(server)
      }

      val channel = session.openChannel("exec") as ChannelExec
      channel.setCommand(command)

      val stdoutStream = ByteArrayOutputStream()
      val stderrStream = ByteArrayOutputStream()
      channel.outputStream = stdoutStream
      channel.setErrStream(stderrStream)

      channel.connect(15000)

      val startTime = System.currentTimeMillis()
      while (!channel.isClosed && System.currentTimeMillis() - startTime < 30000) {
        Thread.sleep(50)
      }

      val exitCode = channel.exitStatus
      channel.disconnect()

      val stdout = stdoutStream.toString(StandardCharsets.UTF_8.name())
      val stderr = stderrStream.toString(StandardCharsets.UTF_8.name())

      Result.success(CommandResult(exitCode, stdout, stderr))
    } catch (e: Exception) {
      disconnect()
      Result.failure(e)
    }
  }

  suspend fun fetchContainers(server: ServerEntity): Result<List<DockerContainer>> = withContext(Dispatchers.IO) {
    val cmd = "docker ps -a --no-trunc --format '{{.ID}};;;{{.Names}};;;{{.Image}};;;{{.Status}};;;{{.State}};;;{{.Ports}};;;{{.CreatedAt}}'"
    val result = executeCommand(server, cmd)
    result.fold(
      onSuccess = { commandResult ->
        if (commandResult.exitCode != 0) {
          val errorMsg = commandResult.stderr.ifBlank { "Docker exited with code ${commandResult.exitCode}" }
          return@withContext Result.failure(Exception(errorMsg))
        }

        val containers = mutableListOf<DockerContainer>()
        val lines = commandResult.stdout.lines()
        for (line in lines) {
          if (line.isBlank()) continue
          val parts = line.split(";;;")
          if (parts.size >= 5) {
            val id = parts[0].trim()
            val names = parts.getOrNull(1)?.trim().orEmpty()
            val image = parts.getOrNull(2)?.trim().orEmpty()
            val status = parts.getOrNull(3)?.trim().orEmpty()
            val stateStr = parts.getOrNull(4)?.trim().orEmpty()
            val ports = parts.getOrNull(5)?.trim().orEmpty()
            val createdAt = parts.getOrNull(6)?.trim().orEmpty()

            containers.add(
              DockerContainer(
                id = id,
                name = names,
                image = image,
                status = status,
                state = ContainerState.fromString(stateStr),
                ports = ports,
                createdAt = createdAt
              )
            )
          }
        }
        Result.success(containers)
      },
      onFailure = { Result.failure(it) }
    )
  }

  suspend fun startContainer(server: ServerEntity, containerId: String): Result<String> {
    val cmd = "docker start $containerId"
    val res = executeCommand(server, cmd)
    return res.mapCatching {
      if (!it.isSuccess) throw Exception(it.stderr.ifBlank { "Failed to start container $containerId" })
      it.stdout.trim()
    }
  }

  suspend fun stopContainer(server: ServerEntity, containerId: String): Result<String> {
    val cmd = "docker stop $containerId"
    val res = executeCommand(server, cmd)
    return res.mapCatching {
      if (!it.isSuccess) throw Exception(it.stderr.ifBlank { "Failed to stop container $containerId" })
      it.stdout.trim()
    }
  }

  suspend fun restartContainer(server: ServerEntity, containerId: String): Result<String> {
    val cmd = "docker restart $containerId"
    val res = executeCommand(server, cmd)
    return res.mapCatching {
      if (!it.isSuccess) throw Exception(it.stderr.ifBlank { "Failed to restart container $containerId" })
      it.stdout.trim()
    }
  }

  suspend fun removeContainer(server: ServerEntity, containerId: String): Result<String> {
    val cmd = "docker rm -f $containerId"
    val res = executeCommand(server, cmd)
    return res.mapCatching {
      if (!it.isSuccess) throw Exception(it.stderr.ifBlank { "Failed to remove container $containerId" })
      it.stdout.trim()
    }
  }

  suspend fun getContainerLogs(server: ServerEntity, containerId: String, tail: Int = 200): Result<String> {
    val cmd = "docker logs --tail $tail --timestamps $containerId"
    val res = executeCommand(server, cmd)
    return res.mapCatching {
      // docker logs outputs to both stdout and stderr (common in docker)
      val fullLogs = (it.stdout + "\n" + it.stderr).trim()
      if (fullLogs.isBlank() && !it.isSuccess) {
        throw Exception(it.stderr.ifBlank { "Could not fetch logs for $containerId" })
      }
      fullLogs
    }
  }

  suspend fun fetchHostSystemInfo(server: ServerEntity): Result<HostSystemInfo> = withContext(Dispatchers.IO) {
    val cmd = "echo '---UNAME---'; uname -srmo 2>/dev/null || uname -a; " +
        "echo '---UPTIME---'; uptime 2>/dev/null; " +
        "echo '---DOCKER---'; docker --version 2>/dev/null; " +
        "echo '---STATS---'; docker info --format '{{.Containers}};{{.ContainersRunning}};{{.ContainersPaused}};{{.ContainersStopped}}' 2>/dev/null; " +
        "echo '---MEM---'; free -h 2>/dev/null | grep Mem || echo ''"

    val res = executeCommand(server, cmd)
    res.mapCatching { r ->
      val text = r.stdout
      var uname = ""
      var uptime = ""
      var dockerVer = ""
      var containersTotal = ""
      var containersRunning = ""
      var mem = ""

      val sections = text.split("---")
      for (section in sections) {
        val trimmed = section.trim()
        when {
          trimmed.startsWith("UNAME") -> uname = trimmed.removePrefix("UNAME").trim().lines().firstOrNull().orEmpty()
          trimmed.startsWith("UPTIME") -> uptime = trimmed.removePrefix("UPTIME").trim().lines().firstOrNull().orEmpty()
          trimmed.startsWith("DOCKER") -> dockerVer = trimmed.removePrefix("DOCKER").trim().lines().firstOrNull().orEmpty()
          trimmed.startsWith("STATS") -> {
            val statsLine = trimmed.removePrefix("STATS").trim().lines().firstOrNull().orEmpty()
            val sp = statsLine.split(";")
            if (sp.size >= 2) {
              containersTotal = sp[0]
              containersRunning = sp[1]
            }
          }
          trimmed.startsWith("MEM") -> mem = trimmed.removePrefix("MEM").trim().lines().firstOrNull().orEmpty()
        }
      }

      HostSystemInfo(
        osInfo = uname,
        uptime = uptime,
        dockerVersion = dockerVer,
        totalContainers = containersTotal,
        runningContainers = containersRunning,
        memoryUsage = mem
      )
    }
  }
}
