package fi.mobilemesh.projectm.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import fi.mobilemesh.projectm.network.Device
import java.util.*

// TODO: Replace sender(string) with sender(device object). Or maybe don't?
/**
 * Class/table representing a single Message.
 * @param messageId unique message id within the chat group, usually in rising order (id 0
 * should be the first message)
 * @param chatGroupId which chat group this message is part of, foreign key related to ChatGroups
 * @param sender name of the message's sender
 * @param timestamp Date on which this message was created/sent
 * @param body actual content of the message
 * @param isOwnMessage if this message was sent by the current user, used to position the message
 * properly. False by default
 */
@Entity
data class Message(
    @PrimaryKey(autoGenerate = false)
    val messageId: Int,

    val chatGroupId: String,

    val sender: String,

    @TypeConverters(DateConverter::class)
    val timestamp: Date,

    val body: String,

    var isOwnMessage: Boolean = true
) : java.io.Serializable

/**
 * Converts a Date to Long and vice versa so it can be stored and read from the Room database
 */
class DateConverter {
    @TypeConverter
    fun toDate(timestamp: Long): Date {
        return Date(timestamp)
    }

    @TypeConverter
    fun toTimeStamp(date: Date): Long {
        return date.time
    }
}

/**
 * Data class for messages being sent through Wi-Fi Direct. Contains both the message
 * and the devices the message has already been sent to
 *  @param message [Message] data to be sent
 *  @param alreadySent [MutableSet] of [Device]s the message has already been sent to,
 *  and where it is not needed to be sent again
 */
data class MessageData(
    val message: Message,

    val alreadySent: MutableSet<Device>
) : java.io.Serializable