package fi.mobilemesh.projectm.database

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.util.query
import fi.mobilemesh.projectm.database.entities.ChatGroup
import fi.mobilemesh.projectm.database.entities.Message

/**
 * Data Access Object (DAO) for message/chat group database queries
 */
@Dao
interface MessageQueries {
    /**
     * Inserts chat group into the database, replacing existing one in case of conflicts
     * @param chatGroup ChatGroup object to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatGroup(chatGroup: ChatGroup)

    /**
     * Inserts a single message into the database, replacing existing one in case of conflicts
     * @param message Message object to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    // TODO: Maybe replace chatGroupId as a string so it can be unique?
    /**
     * Returns all messages within a chat group as a list. Thread safe
     * @param chatGroupId id of the chat group of which to get messages from
     * @return list of messages within chat group table
     */
    @Transaction
    @Query("""SELECT messageId, m.chatGroupId, sender, timestamp, body, isOwnMessage 
        FROM chatgroup AS c JOIN message AS m ON c.chatGroupId = m.chatGroupId 
        WHERE m.chatGroupId = :chatGroupId""")
    suspend fun getChatGroupMessages(chatGroupId: Int): List<Message>

    @Query("""SELECT messageId, m.chatGroupId, sender, timestamp, body, isOwnMessage 
        FROM chatgroup AS c JOIN message AS m ON c.chatGroupId = m.chatGroupId 
        WHERE m.chatGroupId = :chatGroupId""")
    fun getLiveChatGroupMessages(chatGroupId: Int): LiveData<List<Message>>

    /**
     * Gets the 'amount' of messages within a chat group, thereby giving the id of the
     * next message. (If size is 2, messages with id's 0 and 1 already exist, and the next one
     * should be 2)
     */
    suspend fun getNextMessageId(chatGroupId: Int): Int {
        return getChatGroupMessages(chatGroupId).size
    }
}