package fi.mobilemesh.projectm.database

import androidx.room.*
import fi.mobilemesh.projectm.database.entities.ChatGroup
import fi.mobilemesh.projectm.database.entities.Message

@Dao
interface MessageQueries {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatGroup(chatGroup: ChatGroup)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Transaction
    @Query("SELECT * FROM chatGroup WHERE chatGroupId = :chatGroupId")
    suspend fun getChatGroupMessages(chatGroupId: Int): List<ChatGroupMessages>
}