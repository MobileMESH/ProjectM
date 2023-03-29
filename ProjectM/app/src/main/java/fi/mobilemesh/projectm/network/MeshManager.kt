package fi.mobilemesh.projectm.network

import android.content.Context
import android.net.wifi.p2p.WifiP2pDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * handles the management of discovered devices,
 * device connections, and group operations within a mesh network.
 */
class MeshManager() {
    companion object {
        private var INSTANCE: MeshManager? = null

        fun getInstance(context: Context): MeshManager {
            synchronized(this) {
                return INSTANCE ?: MeshManager()
                    .also {
                        it.broadcastManager = BroadcastManager.getInstance(context)
                    }
            }
        }
    }

    private lateinit var broadcastManager: BroadcastManager

    private val currentNetworks: MutableMap<String, MutableList<String>> = mutableMapOf()

    // TODO: Replace other and own with Devices. Atm they are WifiP2pDevice/Name
    fun createNetwork(other: WifiP2pDevice, own: String, reSend: Boolean=true) {
        //val tempName = UUID.randomUUID().toString()
        currentNetworks["tempName"] = mutableListOf(other.deviceName)
        if (reSend) CoroutineScope(Dispatchers.IO).launch {
            broadcastManager.sendData(other.deviceAddress, own)
        }
        println(currentNetworks)
    }

    /*private val connectedDevices: MutableMap<String, Device> = mutableMapOf()
    private val deviceGroups: MutableMap<String,MutableList<Device>> = mutableMapOf()
    private val devicesToJoin: MutableMap<String, Device> = mutableMapOf()


    fun handleDiscoveredDevice(device: Device){
        // Check if the device is already connected or wants to join
        if (connectedDevices.containsKey(device.deviceAddress) || devicesToJoin.containsKey(device.deviceAddress)) {
            return
        }
        broadcastManager.connectToDevice(device.deviceAddress)

    }

    fun createGroup(groupName: String) {
        if (!deviceGroups.containsKey(groupName)) {
            deviceGroups[groupName] = mutableListOf()
        }
    }

    fun joinGroup(groupName: String, deviceAddress: String) {
        val device = connectedDevices[deviceAddress] ?: return
        val group = deviceGroups[groupName] ?: mutableListOf()

        if (!group.contains(device)) {
            group.add(device)
            deviceGroups[groupName] = group
        }
    }

    fun leaveGroup(groupName: String, deviceAddress: String) {
        val group = deviceGroups[groupName] ?: return
        val device = connectedDevices[deviceAddress] ?: return

        if (group.contains(device)) {
            group.remove(device)
        }
    }*/
}