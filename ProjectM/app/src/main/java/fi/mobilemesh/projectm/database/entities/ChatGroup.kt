package fi.mobilemesh.projectm.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import fi.mobilemesh.projectm.network.Device

// TODO: Maybe replace chatGroupId as a string so it can be unique?
/**
 * Class/table representing a single chat group
 * @param chatGroupId unique identifier for this chat group
 */
@Entity
data class ChatGroup(
    @PrimaryKey(autoGenerate = false)
    val chatGroupId: String,

    val groupName: String,

    @TypeConverters(DeviceSetConverter::class)
    val deviceSet: DeviceSet
) : java.io.Serializable

data class DeviceSet(
    val devices: MutableSet<Device>
) : java.io.Serializable

class DeviceSetConverter {
    @TypeConverter
    fun fromDevicesToJSON(devices: DeviceSet): String {
        return Gson().toJson(devices)
    }
    @TypeConverter
    fun fromJSONToDevices(json: String): DeviceSet {
        return Gson().fromJson(json, DeviceSet::class.java)
    }
}