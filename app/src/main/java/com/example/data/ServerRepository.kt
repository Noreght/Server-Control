package com.example.data

import kotlinx.coroutines.flow.Flow

class ServerRepository(private val serverDao: ServerDao) {
  val allServers: Flow<List<ServerEntity>> = serverDao.getAllServers()
  val activeServer: Flow<ServerEntity?> = serverDao.getActiveServer()

  suspend fun getAllServersList(): List<ServerEntity> = serverDao.getAllServersList()

  suspend fun getServerById(id: Long): ServerEntity? = serverDao.getServerById(id)

  suspend fun insertServer(server: ServerEntity): Long {
    val id = serverDao.insertServer(server)
    if (server.isActive) {
      serverDao.setActiveServer(id)
    }
    return id
  }

  suspend fun updateServer(server: ServerEntity) {
    serverDao.updateServer(server)
    if (server.isActive) {
      serverDao.setActiveServer(server.id)
    }
  }

  suspend fun deleteServer(server: ServerEntity) {
    serverDao.deleteServer(server)
  }

  suspend fun setActiveServer(id: Long) {
    serverDao.setActiveServer(id)
  }

  suspend fun deactivateAll() {
    serverDao.deactivateAllServers()
  }
}
