package fi.mobilemesh.projectm.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.util.*

@Entity
data class Message(
    @PrimaryKey(autoGenerate = false)
    val messageId: Int,

    val chatGroupId: Int,

    val sender: String,

    @TypeConverters(DateConverter::class)
    val timestamp: Date,

    val body: String) : java.io.Serializable

public class DateConverter {
    @TypeConverter
    fun toDate(timestamp: Long): Date {
        return Date(timestamp)
    }

    @TypeConverter
    fun toTimeStamp(date: Date): Long {
        return date.time
    }
}