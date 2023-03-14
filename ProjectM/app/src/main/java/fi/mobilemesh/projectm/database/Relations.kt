package fi.mobilemesh.projectm.database

import androidx.room.Embedded
import androidx.room.Relation
import fi.mobilemesh.projectm.database.entities.ChatGroup
import fi.mobilemesh.projectm.database.entities.Message

// 1-to-N relationship between chat groups (1) and (N) messages
data class ChatGroupMessages(
    @Embedded val chatGroup: ChatGroup,
    @Relation(
        parentColumn = "chatGroupId",
        entityColumn = "chatGroupId"
    )
    val messages: List<Message>
)
