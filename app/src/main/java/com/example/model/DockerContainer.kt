package com.example.model

enum class ContainerState(val displayName: String) {
  RUNNING("Running"),
  EXITED("Exited"),
  PAUSED("Paused"),
  RESTARTING("Restarting"),
  CREATED("Created"),
  DEAD("Dead"),
  UNKNOWN("Unknown");

  companion object {
    fun fromString(value: String): ContainerState {
      return when (value.trim().lowercase()) {
        "running" -> RUNNING
        "exited" -> EXITED
        "paused" -> PAUSED
        "restarting" -> RESTARTING
        "created" -> CREATED
        "dead" -> DEAD
        else -> UNKNOWN
      }
    }
  }
}

data class DockerContainer(
  val id: String,
  val name: String,
  val image: String,
  val status: String,
  val state: ContainerState,
  val ports: String,
  val createdAt: String
) {
  val shortId: String
    get() = if (id.length > 12) id.substring(0, 12) else id

  val cleanName: String
    get() = name.removePrefix("/")

  val isRunning: Boolean
    get() = state == ContainerState.RUNNING
}

data class HostSystemInfo(
  val osInfo: String = "",
  val uptime: String = "",
  val dockerVersion: String = "",
  val totalContainers: String = "",
  val runningContainers: String = "",
  val memoryUsage: String = ""
)
