package fi.mobilemesh.projectm.database

import androidx.lifecycle.LiveData
import androidx.room.*
import fi.mobilemesh.projectm.Chat
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
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatGroup(chatGroup: ChatGroup)

    /**
     * Deletes chat group (network) with given id and all associated messages
     */
    @Query("""
        DELETE FROM chatgroup WHERE chatGroupId = :networkId
        """)
    suspend fun deleteChatGroup(networkId: String)

    @Transaction
    @Query("""
        SELECT * FROM chatgroup
        """)
    suspend fun getChatGroups(): List<ChatGroup>

    @Query("""
        SELECT * FROM chatgroup
        """)
    fun getLiveChatGroups(): LiveData<List<ChatGroup>>

    /**
     * Inserts a single message into the database, replacing existing one in case of conflicts
     * @param message Message object to insert
     */
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    // TODO: Maybe replace chatGroupId as a string so it can be unique?
    /**
     * Returns all messages within a chat group as a list. Thread safe
     * @param chatGroupId id of the chat group of which to get messages from
     * @return list of messages within chat group table
     */
    @Transaction
    @Query("""
        SELECT m.chatGroupId, sender, timestamp, body, isOwnMessage 
        FROM chatgroup AS c JOIN message AS m ON c.chatGroupId = m.chatGroupId 
        WHERE m.chatGroupId = :chatGroupId
        """)
    suspend fun getChatGroupMessages(chatGroupId: String): List<Message>

    @Query("""
        SELECT m.chatGroupId, sender, timestamp, body, isOwnMessage 
        FROM chatgroup AS c JOIN message AS m ON c.chatGroupId = m.chatGroupId 
        WHERE m.chatGroupId = :chatGroupId
        """)
    fun getLiveChatGroupMessages(chatGroupId: String): LiveData<List<Message>>

    @Query("""
        DELETE FROM message WHERE chatGroupId = :networkId
        """)
    suspend fun deleteChatGroupMessages(networkId: String)
}