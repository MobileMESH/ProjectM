package fi.mobilemesh.projectm.network

import android.content.Context
import fi.mobilemesh.projectm.database.entities.Message
import fi.mobilemesh.projectm.database.entities.MessageData
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
    // Networks we have joined (TODO: Save somewhere, currently runtime only)
    private val currentNetworks: MutableMap<String, MutableList<Device>> = mutableMapOf()

    /**
     * Returns the first network/chat group id available for us. Testing only
     * @return id of the first network we have available
     */
    fun getTestGroupId(): String {
        return "TEST_GROUP"
    }

    /**
     * Used to initialize a network/chat group between two devices
     * @param other the other [Device] to create this group with
     * @param networkId unique id for the network. Should be null (as is by default) when
     * initiating the creation, and should be set if receiving creation request
     */
    fun createNetwork(other: Device, networkId: String?=null) {
        if (networkId == null) {
            //val newNetworkId = UUID.randomUUID().toString()
            val newNetworkId = getTestGroupId() // TODO: Test purposes
            currentNetworks[newNetworkId] = mutableListOf(other)
            CoroutineScope(Dispatchers.IO).launch {
                val own = broadcastManager.getThisDevice()
                broadcastManager.sendData(other.getAddress(), Pair(own, newNetworkId))
            }
        }

        else {
            currentNetworks[networkId] = mutableListOf(other)
        }
    }

    /**
     * Sends a group-wide message to the network/chat group specified in the networkId
     * @param message actual [Message] to send to the group
     * @param alreadySent set of devices the message was already sent to, avoiding repeat
     */
    fun sendGroupMessage(message: Message, alreadySent: Set<Device>?=null) {
        val network = currentNetworks[message.chatGroupId] ?: return
        val availableDevices = broadcastManager.getThisDevice().getAvailableDevices()

        // Valid devices are both in range (available) and within the selected network
        var validDevices = availableDevices.filter { a: Device ->
            network.any { n: Device ->
                a.getName() == n.getName() } } as MutableList

        // Don't send this to those it has already been sent to
        if (alreadySent != null) {
            validDevices = validDevices.filterNot { v: Device ->
                alreadySent.any { a: Device ->
                    v.getName() == a.getName() } } as MutableList<Device>
        }
        // Our device can also be considered to already have received the message
        val thisDevice = broadcastManager.getThisDevice()
        validDevices.add(thisDevice)

        val messageData = MessageData(message, validDevices.toSet())

        CoroutineScope(Dispatchers.IO).launch {
            validDevices.forEach {
                if (it.getName() != thisDevice.getName()) {
                    broadcastManager.sendData(it.getAddress(), messageData)
                }
            }
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