package com.example

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppTab
import com.example.ui.MainTab
import com.example.ui.MainViewModel
import com.example.ui.ServerTab
import com.example.ui.components.LogsSheet
import com.example.ui.components.ServerAuthPromptDialog
import com.example.ui.components.ServerDialog
import com.example.ui.screens.ContainersScreen
import com.example.ui.screens.LockScreen
import com.example.ui.screens.ServersScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {

  private val viewModel: MainViewModel by viewModels()

  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      MyApplicationTheme {
        val isAppUnlocked by viewModel.isAppUnlocked.collectAsStateWithLifecycle()
        val isPinSet by viewModel.isPinSet.collectAsStateWithLifecycle()
        val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
        val isBiometricSupported = viewModel.isBiometricSupported

        val allServers by viewModel.allServers.collectAsStateWithLifecycle()
        val activeServer by viewModel.activeServer.collectAsStateWithLifecycle()
        val isInitialServersLoaded by viewModel.isInitialServersLoaded.collectAsStateWithLifecycle()
        val containersState by viewModel.containersState.collectAsStateWithLifecycle()
        val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
        val actionInProgressContainerId by viewModel.actionInProgressContainerId.collectAsStateWithLifecycle()
        val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
        val logsState by viewModel.logsState.collectAsStateWithLifecycle()
        val showServerDialog by viewModel.showServerDialog.collectAsStateWithLifecycle()
        val serverForm by viewModel.serverForm.collectAsStateWithLifecycle()
        val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
        val stateFilter by viewModel.stateFilter.collectAsStateWithLifecycle()
        val selectedServerMenuId by viewModel.selectedServerMenuId.collectAsStateWithLifecycle()
        val authPromptServer by viewModel.authPromptServer.collectAsStateWithLifecycle()

        var currentMainTab by rememberSaveable { mutableStateOf(MainTab.SERVERS) }
        var currentServerTab by rememberSaveable { mutableStateOf(ServerTab.CONTAINERS) }
        val snackbarHostState = remember { SnackbarHostState() }

        // System back navigation handling: exits server menu back to main menu
        BackHandler(enabled = isAppUnlocked && selectedServerMenuId != null) {
          viewModel.exitServerMenu()
        }

        // Biometric trigger function
        val triggerBiometricAuth: () -> Unit = {
          if (isBiometricSupported && isBiometricEnabled) {
            viewModel.securityManager.authenticateBiometric(
              activity = this@MainActivity,
              onSuccess = { viewModel.unlockWithBiometric() },
              onError = { /* Let user use PIN */ }
            )
          }
        }

        // Auto-prompt biometric authentication if enabled when app starts in locked state
        LaunchedEffect(isAppUnlocked, isPinSet) {
          if (!isAppUnlocked && isPinSet && isBiometricSupported && isBiometricEnabled) {
            triggerBiometricAuth()
          }
        }

        // Show snackbar notifications
        LaunchedEffect(snackbarMessage) {
          snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbarMessage()
          }
        }

        if (!isAppUnlocked) {
          LockScreen(
            isPinSet = isPinSet,
            isBiometricAvailable = isBiometricSupported && isBiometricEnabled,
            onVerifyPin = { viewModel.verifyPin(it) },
            onSetInitialPin = { viewModel.setInitialPin(it) },
            onTriggerBiometric = triggerBiometricAuth
          )
        } else {
          val currentSelectedServer = if (selectedServerMenuId != null) {
            allServers.find { it.id == selectedServerMenuId } ?: activeServer
          } else {
            null
          }

          Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
              TopAppBar(
                title = {
                  if (currentSelectedServer != null) {
                    Text(
                      text = currentSelectedServer.name,
                      fontWeight = FontWeight.Bold,
                      style = MaterialTheme.typography.titleLarge
                    )
                  } else {
                    Text(
                      text = when (currentMainTab) {
                        MainTab.SERVERS -> "Серверы"
                        MainTab.SETTINGS -> "Настройки"
                      },
                      fontWeight = FontWeight.Bold
                    )
                  }
                },
                navigationIcon = {
                  if (currentSelectedServer != null) {
                    IconButton(
                      onClick = { viewModel.exitServerMenu() },
                      modifier = Modifier.testTag("top_back_button")
                    ) {
                      Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад в главное меню"
                      )
                    }
                  }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                  containerColor = MaterialTheme.colorScheme.surface,
                  titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                  if (currentSelectedServer != null) {
                    IconButton(
                      onClick = { viewModel.refreshContainers(showFullLoading = true) },
                      modifier = Modifier.testTag("top_refresh_containers_button")
                    ) {
                      Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Обновить контейнеры"
                      )
                    }
                  } else if (currentMainTab == MainTab.SERVERS) {
                    IconButton(
                      onClick = { viewModel.openAddServerDialog() },
                      modifier = Modifier.testTag("top_add_server_button")
                    ) {
                      Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Добавить сервер"
                      )
                    }
                  }
                }
              )
            },
            bottomBar = {
              NavigationBar {
                if (currentSelectedServer != null) {
                  // Server menu tabs: currently Docker containers only
                  NavigationBarItem(
                    selected = currentServerTab == ServerTab.CONTAINERS,
                    onClick = { currentServerTab = ServerTab.CONTAINERS },
                    icon = { Icon(Icons.Default.Storage, contentDescription = "Контейнеры") },
                    label = { Text("Контейнеры") },
                    modifier = Modifier.testTag("tab_server_containers")
                  )
                } else {
                  // Main menu tabs: Серверы and Настройки
                  NavigationBarItem(
                    selected = currentMainTab == MainTab.SERVERS,
                    onClick = { currentMainTab = MainTab.SERVERS },
                    icon = { Icon(Icons.Default.Dns, contentDescription = "Серверы") },
                    label = { Text("Серверы") },
                    modifier = Modifier.testTag("tab_servers")
                  )
                  NavigationBarItem(
                    selected = currentMainTab == MainTab.SETTINGS,
                    onClick = { currentMainTab = MainTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Настройки") },
                    label = { Text("Настройки") },
                    modifier = Modifier.testTag("tab_settings")
                  )
                }
              }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
          ) { innerPadding ->
            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
            ) {
              if (currentSelectedServer != null) {
                when (currentServerTab) {
                  ServerTab.CONTAINERS -> {
                    ContainersScreen(
                      isInitialServersLoaded = isInitialServersLoaded,
                      activeServer = currentSelectedServer,
                      containersState = containersState,
                      isRefreshing = isRefreshing,
                      actionInProgressContainerId = actionInProgressContainerId,
                      searchQuery = searchQuery,
                      stateFilter = stateFilter,
                      onSearchChange = { viewModel.setSearchQuery(it) },
                      onStateFilterChange = { viewModel.setStateFilter(it) },
                      onRefresh = { viewModel.refreshContainers(showFullLoading = false) },
                      onStartContainer = { viewModel.startContainer(it) },
                      onStopContainer = { viewModel.stopContainer(it) },
                      onRestartContainer = { viewModel.restartContainer(it) },
                      onDeleteContainer = { viewModel.deleteContainer(it) },
                      onViewLogs = { viewModel.showContainerLogs(it) },
                      onConnectServerClick = { viewModel.openAddServerDialog() }
                    )
                  }
                }
              } else {
                when (currentMainTab) {
                  MainTab.SERVERS -> {
                    ServersScreen(
                      isInitialServersLoaded = isInitialServersLoaded,
                      servers = allServers,
                      activeServer = activeServer,
                      onAddServer = { viewModel.openAddServerDialog() },
                      onSelectActiveServer = { server ->
                        viewModel.handleServerSelection(server)
                      },
                      onEditServer = { viewModel.openEditServerDialog(it) },
                      onDeleteServer = { viewModel.deleteServer(it) }
                    )
                  }

                  MainTab.SETTINGS -> {
                    SettingsScreen(
                      isBiometricSupported = isBiometricSupported,
                      isBiometricEnabled = isBiometricEnabled,
                      onToggleBiometric = { viewModel.toggleBiometric(it) },
                      onChangePin = { oldPin, newPin -> viewModel.changePin(oldPin, newPin) },
                      onLockApp = { viewModel.lockApp() },
                      onExportServers = { viewModel.exportServersToUri(it) },
                      onImportServers = { viewModel.importServersFromUri(it) },
                      serverCount = allServers.size
                    )
                  }
                }
              }

              // Add/Edit Server Dialog
              if (showServerDialog) {
                ServerDialog(
                  formState = serverForm,
                  onFormChange = { viewModel.updateServerForm(it) },
                  onPickKeyFile = { viewModel.onKeyFilePicked(it) },
                  onTestConnection = { viewModel.testServerConnection() },
                  onSave = { viewModel.saveServer() },
                  onDismiss = { viewModel.closeServerDialog() }
                )
              }

              // Auth Prompt Dialog for imported servers requiring password or passphrase
              if (authPromptServer != null) {
                ServerAuthPromptDialog(
                  server = authPromptServer!!,
                  onConfirm = { secret ->
                    viewModel.submitServerAuth(authPromptServer!!, secret)
                  },
                  onDismiss = {
                    viewModel.cancelAuthPrompt()
                  }
                )
              }

              // Logs Modal Bottom Sheet
              if (logsState.isOpen) {
                LogsSheet(
                  state = logsState,
                  onRefresh = { viewModel.refreshCurrentLogs() },
                  onChangeTail = { viewModel.changeLogsTail(it) },
                  onDismiss = { viewModel.closeLogsDialog() }
                )
              }
            }
          }
        }
      }
    }
  }
}
