package fi.mobilemesh.projectm.network

import android.content.Context
import android.net.wifi.p2p.WifiP2pManager.DeviceInfoListener
import fi.mobilemesh.projectm.database.entities.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * handles the management of discovered devices,
 * device connections, and group operations within a mesh network.
 */
class MeshManager {
    companion object {
        private var INSTANCE: MeshManager? = null

        fun getInstance(context: Context): MeshManager {
            synchronized(this) {
                return INSTANCE ?: MeshManager()
                    .also {
                        INSTANCE = it
                        it.broadcastManager = BroadcastManager.getInstance(context)
                    }
            }
        }
    }

    private lateinit var broadcastManager: BroadcastManager

    private val currentNetworks: MutableMap<String, MutableList<Device>> = mutableMapOf()

    fun getRandomNetworkTest(): String {
        return currentNetworks.keys.first()
    }

    fun createNetwork(other: Device, own: Device, networkId: String?=null) {
        if (networkId == null) {
            val newNetworkId = UUID.randomUUID().toString()
            currentNetworks[newNetworkId] = mutableListOf(other)
            CoroutineScope(Dispatchers.IO).launch {
                broadcastManager.sendData(other.getAddress(), Pair(own, newNetworkId))
            }
        }
        else {
            currentNetworks[networkId] = mutableListOf(other)
        }
    }

    fun sendGroupMessage(networkId: String, message: Message) {
        val network = currentNetworks[networkId] ?: return
        val availableDevices = broadcastManager.getThisDevice().getAvailableDevices()
        val validDevices = availableDevices.filter { a -> network.any { b -> a.getName() == b.getName() } }

        println("NET $networkId")
        for (d in network) {
            println("DEV ${d.getName()}")
        }
        println("VALID $validDevices")

        CoroutineScope(Dispatchers.IO).launch {
            validDevices.forEach { broadcastManager.sendData(it.getAddress(), message) }
        }
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