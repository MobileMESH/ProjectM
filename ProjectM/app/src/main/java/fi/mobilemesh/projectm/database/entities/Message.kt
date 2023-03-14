package fi.mobilemesh.projectm.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class Message(
    @PrimaryKey(autoGenerate = false)
    val messageId: Int,
    val chatGroupId: Int,
    val sender: String,
    val timestamp: Date,
    val body: String) : java.io.Serializable
