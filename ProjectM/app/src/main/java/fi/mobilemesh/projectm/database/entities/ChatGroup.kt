package fi.mobilemesh.projectm.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ChatGroup(
    @PrimaryKey(autoGenerate = false)
    val chatGroupId: Int
)
