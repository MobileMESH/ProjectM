package fi.mobilemesh.projectm.utils

import android.content.Context
import android.content.SharedPreferences

const val USER_DATA_PATH = "userData"
const val UUID_PATH = "uuid"
const val USERNAME_PATH = "username"
const val LOCATION_STATUS_PATH = "locationStatus"
const val NETWORKS_PATH = "joinedNetworks"

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

    /**
     * Saves the id of a network the user has joined
     * @param id unique id of the network that has been joined
     * @return true if the id had not already been saved, false otherwise
     */
    fun saveJoinedNetwork(id: String): Boolean {
        val joined = getJoinedNetworks() ?: mutableSetOf()
        // Can't modify original set so we have to do it like this
        val set = joined.toMutableSet()
        if (!set.add(id)) {
            return false
        }
        sharedPreferences.edit()
            .putStringSet(NETWORKS_PATH, set)
            .apply()
        return true
    }

    /**
     * Removes the network with given id from joined networks
     * @param id unique id of network to leave
     * @return true if the network was found and successfully left, false if no networks
     * had been saved or given id was not found
     */
    fun leaveJoinedNetwork(id: String): Boolean {
        val joined = getJoinedNetworks() ?: return false
        // Can't modify original set so we have to do it like this
        val set = joined.toMutableSet()
        if (!set.remove(id)) {
            return false
        }
        sharedPreferences.edit()
            .putStringSet(NETWORKS_PATH, set)
            .apply()
        return true
    }

    /**
     * Gets a set of id's of the networks the user has joined. DO NOT MODIFY THE SET DIRECTLY!
     * This is an Android limitation and any edits to the set should be made by copying it to
     * another, using [toSet] or its variants!
     * @return [Set] of id's of joined networks if found, null otherwise
     */
    fun getJoinedNetworks(): Set<String>? {
        return sharedPreferences.getStringSet(NETWORKS_PATH, null)
    }
}