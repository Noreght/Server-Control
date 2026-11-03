package com.example.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AuthMethod
import com.example.data.ServerEntity
import com.example.data.ServerRepository
import com.example.model.ContainerState
import com.example.model.DockerContainer
import com.example.model.HostSystemInfo
import com.example.security.SecurityManager
import com.example.ssh.SshManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ContainersUiState {
  data object Idle : ContainersUiState
  data object Loading : ContainersUiState
  data class Success(val containers: List<DockerContainer>) : ContainersUiState
  data class Error(val message: String) : ContainersUiState
}

data class LogsDialogState(
  val isOpen: Boolean = false,
  val containerId: String = "",
  val containerName: String = "",
  val logs: String = "",
  val isLoading: Boolean = false,
  val error: String? = null,
  val tailLines: Int = 200
)

data class ServerFormState(
  val id: Long = 0,
  val name: String = "",
  val host: String = "",
  val port: String = "22",
  val username: String = "root",
  val authType: AuthMethod = AuthMethod.PASSWORD,
  val password: String = "",
  val privateKeyContent: String = "",
  val privateKeyFileName: String = "",
  val passphrase: String = "",
  val isTesting: Boolean = false,
  val testResult: Pair<Boolean, String>? = null // isSuccess to message
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: ServerRepository
  val securityManager = SecurityManager(application)

  private val _isPinSet = MutableStateFlow(securityManager.isPinSet())
  val isPinSet: StateFlow<Boolean> = _isPinSet.asStateFlow()

  // App requires PIN or Biometric unlock on launch
  private val _isAppUnlocked = MutableStateFlow(false)
  val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

  private val _isBiometricEnabled = MutableStateFlow(securityManager.isBiometricEnabled())
  val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

  val isBiometricSupported: Boolean = securityManager.isBiometricSupported(application)

  val allServers: StateFlow<List<ServerEntity>>
  val activeServer: StateFlow<ServerEntity?>

  private val _isInitialServersLoaded = MutableStateFlow(false)
  val isInitialServersLoaded: StateFlow<Boolean> = _isInitialServersLoaded.asStateFlow()

  private val _containersState = MutableStateFlow<ContainersUiState>(ContainersUiState.Idle)
  val containersState: StateFlow<ContainersUiState> = _containersState.asStateFlow()

  private val _hostInfo = MutableStateFlow<HostSystemInfo?>(null)
  val hostInfo: StateFlow<HostSystemInfo?> = _hostInfo.asStateFlow()

  private val _isRefreshing = MutableStateFlow(false)
  val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

  private val _actionInProgressContainerId = MutableStateFlow<String?>(null)
  val actionInProgressContainerId: StateFlow<String?> = _actionInProgressContainerId.asStateFlow()

  private val _snackbarMessage = MutableStateFlow<String?>(null)
  val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

  private val _logsState = MutableStateFlow(LogsDialogState())
  val logsState: StateFlow<LogsDialogState> = _logsState.asStateFlow()

  private val _showServerDialog = MutableStateFlow(false)
  val showServerDialog: StateFlow<Boolean> = _showServerDialog.asStateFlow()

  private val _serverForm = MutableStateFlow(ServerFormState())
  val serverForm: StateFlow<ServerFormState> = _serverForm.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _stateFilter = MutableStateFlow<ContainerState?>(null)
  val stateFilter: StateFlow<ContainerState?> = _stateFilter.asStateFlow()

  private val _selectedServerMenuId = MutableStateFlow<Long?>(null)
  val selectedServerMenuId: StateFlow<Long?> = _selectedServerMenuId.asStateFlow()

  private var fetchJob: Job? = null

  init {
    val database = AppDatabase.getDatabase(application)
    repository = ServerRepository(database.serverDao())

    allServers = repository.allServers.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

    activeServer = repository.activeServer.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = null
    )

    // Check initial servers from database and only prompt on the first launch if no servers exist
    viewModelScope.launch {
      val initialList = repository.getAllServersList()
      val prefs = application.getSharedPreferences("docker_remote_prefs", android.content.Context.MODE_PRIVATE)
      val hasPrompted = prefs.getBoolean("has_prompted_first_launch", false)

      if (!hasPrompted) {
        prefs.edit().putBoolean("has_prompted_first_launch", true).apply()
        if (initialList.isEmpty()) {
          openAddServerDialog()
        }
      }

      // If servers exist but none is marked active, activate the first one automatically
      if (initialList.isNotEmpty() && initialList.none { it.isActive }) {
        repository.setActiveServer(initialList.first().id)
      }

      _isInitialServersLoaded.value = true
    }

    // Listen to active server changes and auto-fetch containers
    viewModelScope.launch {
      activeServer.collectLatest { server ->
        if (server != null && _isAppUnlocked.value) {
          refreshContainers(showFullLoading = true)
        } else if (server == null) {
          _containersState.value = ContainersUiState.Idle
          _hostInfo.value = null
          SshManager.disconnect()
        }
      }
    }
  }

  fun decryptServer(server: ServerEntity): ServerEntity {
    return server.copy(
      password = server.password?.let { securityManager.decrypt(it) },
      passphrase = server.passphrase?.let { securityManager.decrypt(it) },
      privateKeyContent = server.privateKeyContent?.let { securityManager.decrypt(it) }
    )
  }

  fun verifyPin(pin: String): Boolean {
    val ok = securityManager.verifyPin(pin)
    if (ok) {
      _isAppUnlocked.value = true
      activeServer.value?.let { refreshContainers(showFullLoading = false) }
    }
    return ok
  }

  fun unlockWithBiometric() {
    _isAppUnlocked.value = true
    activeServer.value?.let { refreshContainers(showFullLoading = false) }
  }

  fun setInitialPin(pin: String) {
    securityManager.setPin(pin)
    _isPinSet.value = true
    _isAppUnlocked.value = true
    _snackbarMessage.value = "PIN code set successfully"
    activeServer.value?.let { refreshContainers(showFullLoading = false) }
  }

  fun changePin(oldPin: String, newPin: String): Boolean {
    val ok = securityManager.verifyPin(oldPin)
    if (ok) {
      securityManager.setPin(newPin)
      _snackbarMessage.value = "PIN code updated successfully"
      return true
    }
    return false
  }

  fun toggleBiometric(enabled: Boolean) {
    securityManager.setBiometricEnabled(enabled)
    _isBiometricEnabled.value = enabled
  }

  fun lockApp() {
    _isAppUnlocked.value = false
    SshManager.disconnect()
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun setStateFilter(filter: ContainerState?) {
    _stateFilter.value = filter
  }

  fun clearSnackbarMessage() {
    _snackbarMessage.value = null
  }

  fun openAddServerDialog(prefillHost: String = "") {
    _serverForm.value = ServerFormState(
      name = if (prefillHost.isNotBlank()) "Server $prefillHost" else "My Server",
      host = prefillHost,
      port = "22",
      username = "root",
      authType = AuthMethod.PASSWORD
    )
    _showServerDialog.value = true
  }

  fun openEditServerDialog(server: ServerEntity) {
    val decrypted = decryptServer(server)
    _serverForm.value = ServerFormState(
      id = decrypted.id,
      name = decrypted.name,
      host = decrypted.host,
      port = decrypted.port.toString(),
      username = decrypted.username,
      authType = if (decrypted.authType == AuthMethod.PRIVATE_KEY.name) AuthMethod.PRIVATE_KEY else AuthMethod.PASSWORD,
      password = decrypted.password.orEmpty(),
      privateKeyContent = decrypted.privateKeyContent.orEmpty(),
      privateKeyFileName = decrypted.privateKeyFileName.orEmpty(),
      passphrase = decrypted.passphrase.orEmpty()
    )
    _showServerDialog.value = true
  }

  fun closeServerDialog() {
    _showServerDialog.value = false
  }

  fun updateServerForm(transform: (ServerFormState) -> ServerFormState) {
    _serverForm.value = transform(_serverForm.value)
  }

  fun onKeyFilePicked(uri: Uri) {
    viewModelScope.launch {
      val context = getApplication<Application>()
      val result = com.example.util.StorageFileHelper.readSelectedFile(context, uri)
      result.onSuccess { fileInfo ->
        if (fileInfo.contentText.isNotBlank()) {
          _serverForm.value = _serverForm.value.copy(
            privateKeyContent = fileInfo.contentText,
            privateKeyFileName = "${fileInfo.name} (${fileInfo.formattedSize})",
            testResult = null
          )
          _snackbarMessage.value = "Loaded: ${fileInfo.name} (${fileInfo.formattedSize})"
        } else {
          _snackbarMessage.value = "Selected file is empty"
        }
      }.onFailure { e ->
        _snackbarMessage.value = "Error reading file: ${e.message}"
      }
    }
  }

  fun testServerConnection() {
    val form = _serverForm.value
    if (form.host.isBlank()) {
      _serverForm.value = form.copy(testResult = Pair(false, "Please enter server IP or Hostname"))
      return
    }
    val portInt = form.port.toIntOrNull() ?: 22
    val entity = ServerEntity(
      id = form.id,
      name = form.name.ifBlank { form.host },
      host = form.host.trim(),
      port = portInt,
      username = form.username.trim().ifBlank { "root" },
      authType = form.authType.name,
      password = form.password.takeIf { form.authType == AuthMethod.PASSWORD },
      privateKeyContent = form.privateKeyContent.takeIf { form.authType == AuthMethod.PRIVATE_KEY },
      privateKeyFileName = form.privateKeyFileName.takeIf { form.authType == AuthMethod.PRIVATE_KEY },
      passphrase = form.passphrase.takeIf { form.authType == AuthMethod.PRIVATE_KEY && it.isNotBlank() }
    )

    _serverForm.value = form.copy(isTesting = true, testResult = null)

    viewModelScope.launch {
      val res = SshManager.testConnection(entity)
      res.fold(
        onSuccess = { msg ->
          _serverForm.value = _serverForm.value.copy(
            isTesting = false,
            testResult = Pair(true, msg)
          )
        },
        onFailure = { err ->
          _serverForm.value = _serverForm.value.copy(
            isTesting = false,
            testResult = Pair(false, err.message ?: "Connection failed")
          )
        }
      )
    }
  }

  fun saveServer() {
    val form = _serverForm.value
    if (form.host.isBlank()) {
      _snackbarMessage.value = "Server IP / Host is required"
      return
    }
    val portInt = form.port.toIntOrNull() ?: 22
    val entity = ServerEntity(
      id = form.id,
      name = form.name.ifBlank { form.host },
      host = form.host.trim(),
      port = portInt,
      username = form.username.trim().ifBlank { "root" },
      authType = form.authType.name,
      password = form.password.takeIf { form.authType == AuthMethod.PASSWORD && it.isNotBlank() }?.let { securityManager.encrypt(it) },
      privateKeyContent = form.privateKeyContent.takeIf { form.authType == AuthMethod.PRIVATE_KEY && it.isNotBlank() }?.let { securityManager.encrypt(it) },
      privateKeyFileName = form.privateKeyFileName.takeIf { form.authType == AuthMethod.PRIVATE_KEY },
      passphrase = form.passphrase.takeIf { form.authType == AuthMethod.PRIVATE_KEY && it.isNotBlank() }?.let { securityManager.encrypt(it) },
      isActive = true // Activate upon save to immediately connect!
    )

    viewModelScope.launch {
      if (entity.id == 0L) {
        val newId = repository.insertServer(entity)
        repository.setActiveServer(newId)
        _snackbarMessage.value = "Server '${entity.name}' added"
      } else {
        repository.updateServer(entity)
        repository.setActiveServer(entity.id)
        _snackbarMessage.value = "Server '${entity.name}' updated"
      }
      _showServerDialog.value = false
    }
  }

  fun selectActiveServer(server: ServerEntity) {
    viewModelScope.launch {
      SshManager.disconnect()
      repository.setActiveServer(server.id)
      _snackbarMessage.value = "Connected to ${server.name}"
    }
  }

  fun openServerMenu(server: ServerEntity) {
    selectActiveServer(server)
    _selectedServerMenuId.value = server.id
  }

  fun exitServerMenu() {
    _selectedServerMenuId.value = null
  }

  fun deleteServer(server: ServerEntity) {
    viewModelScope.launch {
      if (_selectedServerMenuId.value == server.id) {
        _selectedServerMenuId.value = null
      }
      if (server.isActive) {
        SshManager.disconnect()
      }
      repository.deleteServer(server)
      _snackbarMessage.value = "Server '${server.name}' deleted"
    }
  }

  fun refreshContainers(showFullLoading: Boolean = false) {
    if (!_isAppUnlocked.value) return
    val currentServer = activeServer.value?.let { decryptServer(it) } ?: return
    fetchJob?.cancel()
    fetchJob = viewModelScope.launch {
      if (showFullLoading) {
        _containersState.value = ContainersUiState.Loading
      } else {
        _isRefreshing.value = true
      }

      val result = SshManager.fetchContainers(currentServer)
      result.fold(
        onSuccess = { list ->
          _containersState.value = ContainersUiState.Success(list)
          _isRefreshing.value = false
        },
        onFailure = { err ->
          _containersState.value = ContainersUiState.Error(
            err.message ?: "Failed to connect or fetch containers from ${currentServer.host}"
          )
          _isRefreshing.value = false
        }
      )

      // Also refresh system stats in background
      launch {
        val sysRes = SshManager.fetchHostSystemInfo(currentServer)
        if (sysRes.isSuccess) {
          _hostInfo.value = sysRes.getOrNull()
        }
      }
    }
  }

  fun startContainer(container: DockerContainer) {
    val server = activeServer.value?.let { decryptServer(it) } ?: return
    viewModelScope.launch {
      _actionInProgressContainerId.value = container.id
      val res = SshManager.startContainer(server, container.id)
      _actionInProgressContainerId.value = null
      res.fold(
        onSuccess = {
          _snackbarMessage.value = "Container '${container.cleanName}' started"
          refreshContainers(showFullLoading = false)
        },
        onFailure = { err ->
          _snackbarMessage.value = "Start failed: ${err.message}"
        }
      )
    }
  }

  fun stopContainer(container: DockerContainer) {
    val server = activeServer.value?.let { decryptServer(it) } ?: return
    viewModelScope.launch {
      _actionInProgressContainerId.value = container.id
      val res = SshManager.stopContainer(server, container.id)
      _actionInProgressContainerId.value = null
      res.fold(
        onSuccess = {
          _snackbarMessage.value = "Container '${container.cleanName}' stopped"
          refreshContainers(showFullLoading = false)
        },
        onFailure = { err ->
          _snackbarMessage.value = "Stop failed: ${err.message}"
        }
      )
    }
  }

  fun restartContainer(container: DockerContainer) {
    val server = activeServer.value?.let { decryptServer(it) } ?: return
    viewModelScope.launch {
      _actionInProgressContainerId.value = container.id
      val res = SshManager.restartContainer(server, container.id)
      _actionInProgressContainerId.value = null
      res.fold(
        onSuccess = {
          _snackbarMessage.value = "Container '${container.cleanName}' restarted"
          refreshContainers(showFullLoading = false)
        },
        onFailure = { err ->
          _snackbarMessage.value = "Restart failed: ${err.message}"
        }
      )
    }
  }

  fun deleteContainer(container: DockerContainer) {
    val server = activeServer.value?.let { decryptServer(it) } ?: return
    viewModelScope.launch {
      _actionInProgressContainerId.value = container.id
      val res = SshManager.removeContainer(server, container.id)
      _actionInProgressContainerId.value = null
      res.fold(
        onSuccess = {
          _snackbarMessage.value = "Container '${container.cleanName}' deleted"
          refreshContainers(showFullLoading = false)
        },
        onFailure = { err ->
          _snackbarMessage.value = "Delete failed: ${err.message}"
        }
      )
    }
  }

  fun showContainerLogs(container: DockerContainer, tail: Int = 200) {
    val server = activeServer.value?.let { decryptServer(it) } ?: return
    _logsState.value = LogsDialogState(
      isOpen = true,
      containerId = container.id,
      containerName = container.cleanName,
      logs = "Fetching logs...",
      isLoading = true,
      tailLines = tail
    )

    fetchLogsInternal(server, container.id, tail)
  }

  fun refreshCurrentLogs() {
    val current = _logsState.value
    val server = activeServer.value?.let { decryptServer(it) } ?: return
    if (!current.isOpen || current.containerId.isBlank()) return
    _logsState.value = current.copy(isLoading = true, error = null)
    fetchLogsInternal(server, current.containerId, current.tailLines)
  }

  fun changeLogsTail(lines: Int) {
    val current = _logsState.value
    val server = activeServer.value?.let { decryptServer(it) } ?: return
    if (!current.isOpen) return
    _logsState.value = current.copy(isLoading = true, tailLines = lines, error = null)
    fetchLogsInternal(server, current.containerId, lines)
  }

  private fun fetchLogsInternal(server: ServerEntity, containerId: String, tail: Int) {
    viewModelScope.launch {
      val res = SshManager.getContainerLogs(server, containerId, tail)
      res.fold(
        onSuccess = { logs ->
          _logsState.value = _logsState.value.copy(
            logs = logs.ifBlank { "[No logs output for container]" },
            isLoading = false,
            error = null
          )
        },
        onFailure = { err ->
          _logsState.value = _logsState.value.copy(
            logs = "",
            isLoading = false,
            error = err.message ?: "Failed to fetch logs"
          )
        }
      )
    }
  }

  fun closeLogsDialog() {
    _logsState.value = LogsDialogState(isOpen = false)
  }
}
