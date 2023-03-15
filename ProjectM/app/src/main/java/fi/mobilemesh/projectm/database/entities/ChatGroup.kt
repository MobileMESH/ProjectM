package fi.mobilemesh.projectm.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

// TODO: Maybe replace chatGroupId as a string so it can be unique?
/**
 * Class/table representing a single chat group
 * @param chatGroupId unique identifier for this chat group
 */
@Entity
data class ChatGroup(
    @PrimaryKey(autoGenerate = false)
    val chatGroupId: Int
)
