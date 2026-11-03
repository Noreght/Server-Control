package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
  @Query("SELECT * FROM servers ORDER BY createdAt DESC")
  fun getAllServers(): Flow<List<ServerEntity>>

  @Query("SELECT * FROM servers ORDER BY createdAt DESC")
  suspend fun getAllServersList(): List<ServerEntity>

  @Query("SELECT * FROM servers WHERE isActive = 1 LIMIT 1")
  fun getActiveServer(): Flow<ServerEntity?>

  @Query("SELECT * FROM servers WHERE id = :id LIMIT 1")
  suspend fun getServerById(id: Long): ServerEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertServer(server: ServerEntity): Long

  @Update
  suspend fun updateServer(server: ServerEntity)

  @Delete
  suspend fun deleteServer(server: ServerEntity)

  @Query("UPDATE servers SET isActive = 0")
  suspend fun deactivateAllServers()

  @Query("UPDATE servers SET isActive = 1 WHERE id = :id")
  suspend fun activateServer(id: Long)

  @Transaction
  suspend fun setActiveServer(id: Long) {
    deactivateAllServers()
    activateServer(id)
  }
}
