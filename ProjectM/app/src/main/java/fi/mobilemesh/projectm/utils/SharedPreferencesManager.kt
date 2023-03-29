package fi.mobilemesh.projectm.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import fi.mobilemesh.projectm.network.Device
import java.lang.reflect.Type

const val USER_DATA_PATH = "userData"

class SharedPreferencesManager(context: Context, device: Device) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(USER_DATA_PATH, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val deviceToModify = device

    fun saveDevice() {
        sharedPreferences.edit {
            putString(deviceToModify.getAddress(), gson.toJson(deviceToModify))
        }
    }

    fun getDevice(deviceAddress: String): Device? {
        val deviceJson = sharedPreferences.getString(deviceAddress, null)
        return if (deviceJson != null) {
            gson.fromJson(deviceJson, Device::class.java)
        } else {
            null
        }
    }

    fun deleteDevice(deviceAddress: String) {
        sharedPreferences.edit {
            remove(deviceAddress)
        }
    }

    fun saveAvailableDevices(deviceAddress: String, availableDevices: List<Device>) {
        val listType: Type = object : TypeToken<List<Device>>() {}.type
        sharedPreferences.edit {
            putString("$deviceAddress-availableDevices", gson.toJson(availableDevices, listType))
        }
    }

    fun getAvailableDevices(deviceAddress: String): List<Device> {
        val availableDevicesJson =
            sharedPreferences.getString("$deviceAddress-availableDevices", null)
        return if (availableDevicesJson != null) {
            val listType: Type = object : TypeToken<List<Device>>() {}.type
            gson.fromJson(availableDevicesJson, listType)
        } else {
            emptyList()
        }
    }

    fun deleteAvailableDevices(deviceAddress: String) {
        sharedPreferences.edit {
            remove("$deviceAddress-availableDevices")
        }
    }
}