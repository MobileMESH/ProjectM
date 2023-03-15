package fi.mobilemesh.projectm.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import fi.mobilemesh.projectm.database.entities.ChatGroup
import fi.mobilemesh.projectm.database.entities.DateConverter
import fi.mobilemesh.projectm.database.entities.Message

/**
 * Database object for messages and chat groups. Credit to Phillip Lackner
 */
@Database(
    entities = [
        Message::class,
        ChatGroup::class
    ],
    version = 1
)
@TypeConverters(DateConverter::class)
abstract class MessageDatabase : RoomDatabase() {
    //Data Access Object for tables/entities
    abstract val dao: MessageQueries

    companion object {
        @Volatile
        private var INSTANCE: MessageDatabase? = null

        fun getInstance(context: Context): MessageDatabase {
            synchronized(this) {
                return INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MessageDatabase::class.java,
                    "MessageDatabase"
                ).build().also {
                    INSTANCE = it
                }
            }
        }
    }
}