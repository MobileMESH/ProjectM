package fi.mobilemesh.projectm.utils

import android.content.Context
import android.content.SharedPreferences

const val USER_DATA_PATH = "userData"
const val UUID_PATH = "uuid"
const val USERNAME_PATH = "username"
const val LOCATION_STATUS_PATH = "locationStatus"

/**
 * Class for handling user preferences and attributes, such as location, username and UUID.
 * Should be used for information that needs to be kept even between restarts. The preferences
 * can be cleared manually by clearing app information or by uninstalling
 */
class SharedPreferencesManager(
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        private var INSTANCE: SharedPreferencesManager? = null

        fun getInstance(context: Context): SharedPreferencesManager {
            synchronized(this) {
                val sharedPrefs = context.getSharedPreferences(USER_DATA_PATH, Context.MODE_PRIVATE)
                return INSTANCE ?: SharedPreferencesManager(sharedPrefs)
                    .also {
                        INSTANCE = it
                    }
            }
        }
    }

    /**
     * Saves the user's new UUID if one has not been saved already
     * @param uuid the unique id of this user to save
     * @return true if the new id was saved, false if one had already been saved
     */
    fun saveUUID(uuid: String): Boolean {
        if (getUUID() != null){
            return false
        }
        sharedPreferences.edit()
            .putString(UUID_PATH, uuid)
            .apply()
        return true
    }

    /**
     * Gets the unique id of this user
     * @return the unique id if set, null otherwise
     */
    fun getUUID(): String? {
        return sharedPreferences.getString(UUID_PATH, null)
    }

    /**
     * Saves the user's preferred username that is visible to other users
     * @param username preferred username
     */
    fun saveUsername(username: String) {
        sharedPreferences.edit()
            .putString(USERNAME_PATH, username)
            .apply()
    }

    /**
     * Gets the user's preferred username that is visible to others
     * @return the username if set, null otherwise
     */
    fun getUsername(): String? {
        return sharedPreferences.getString(USERNAME_PATH, null)
    }

    /**
     * Updates the enabled/disabled status of the user's location sharing
     * @param state new preference, true if location can be shared, false if not
     */
    fun setLocationEnabled(state: Boolean) {
        sharedPreferences.edit()
            .putBoolean(LOCATION_STATUS_PATH, state)
            .apply()
    }

    /**
     * Gets the user's preference, whether or not their location can be shared with others
     * @return true/false status of the settings, false if not set
     */
    fun getLocationEnabled(): Boolean {
        return sharedPreferences.getBoolean(LOCATION_STATUS_PATH, false)
    }

    /*fun saveDevice() {
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
    }*/
}