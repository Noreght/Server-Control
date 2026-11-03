package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AuthMethod {
  PASSWORD,
  PRIVATE_KEY
}

@Entity(tableName = "servers")
data class ServerEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val host: String,
  val port: Int = 22,
  val username: String = "root",
  val authType: String = AuthMethod.PASSWORD.name,
  val password: String? = null,
  val privateKeyContent: String? = null,
  val privateKeyFileName: String? = null,
  val passphrase: String? = null,
  val isActive: Boolean = false,
  val createdAt: Long = System.currentTimeMillis()
)
