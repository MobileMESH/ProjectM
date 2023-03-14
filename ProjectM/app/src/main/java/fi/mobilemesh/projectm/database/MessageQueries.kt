package fi.mobilemesh.projectm.database

import androidx.room.*
import androidx.room.util.query
import fi.mobilemesh.projectm.database.entities.ChatGroup
import fi.mobilemesh.projectm.database.entities.Message

@Dao
interface MessageQueries {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatGroup(chatGroup: ChatGroup)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Transaction
    @Query("SELECT * FROM chatGroup WHERE chatGroupId = :chatGroupId LIMIT 1")
    suspend fun getChatGroupMessages(chatGroupId: Int): ChatGroupWithMessages

    suspend fun getMessages(chatGroupId: Int): List<Message> {
        return getChatGroupMessages(chatGroupId).messages
    }
}