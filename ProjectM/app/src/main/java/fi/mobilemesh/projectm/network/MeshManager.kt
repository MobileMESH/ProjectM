package fi.mobilemesh.projectm.network

import android.content.Context
import fi.mobilemesh.projectm.database.MessageDatabase
import fi.mobilemesh.projectm.database.MessageQueries
import fi.mobilemesh.projectm.database.entities.ChatGroup
import fi.mobilemesh.projectm.database.entities.DeviceSet
import fi.mobilemesh.projectm.database.entities.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

/**
 * handles the management of discovered devices,
 * device connections, and group operations within a mesh network.
 */
class MeshManager {
    companion object {
        private var INSTANCE: MeshManager? = null
        private lateinit var dao: MessageQueries
        var activeNetworkId: String? = null

        fun getInstance(context: Context): MeshManager {
            synchronized(this) {
                return INSTANCE ?: MeshManager()
                    .also { ins ->
                        INSTANCE = ins
                        ins.broadcastManager = BroadcastManager.getInstance(context)
                        dao = MessageDatabase.getInstance(context).dao

                        // Loads networks/chat groups from the database
                        CoroutineScope(Dispatchers.IO).launch {
                            ins.currentNetworks = dao.getChatGroups()
                                .associateBy({it.chatGroupId}, {it})
                                    as MutableMap<String, ChatGroup>
                        }
                    }
            }
        }
    }

    private lateinit var broadcastManager: BroadcastManager
    private lateinit var currentNetworks: MutableMap<String, ChatGroup>

    /**
     * Relays given data forward in the network, avoiding sending it to devices it has
     * (probably) already reached
     * @param data [Data] to send through the network. Should contain the network id (might
     * change later)
     * @param alreadySent [MutableSet] of devices this data has been sent to, to avoid repeatedly
     * sending it back and forth. If not specified, will create a new set containing this device
     */
    fun relayForward(data: Any,
                     alreadySent: MutableSet<Device>
                     = mutableSetOf(broadcastManager.getThisDevice())) {

        val networkId = when (data) {
            is Message -> data.chatGroupId
            is ChatGroup -> data.chatGroupId
            else -> null
        }

        val network = currentNetworks[networkId] ?: return
        val availableDevices = broadcastManager.getNearbyDevices()

        // Valid devices are both in range (available) and within the selected network,
        // but not in the devices the message has been sent to

        val validDevices = network.deviceSet.devices.filterNot { it in alreadySent }

        alreadySent.addAll(validDevices)

        validDevices.forEach {
            val payload = Data(it.getName(), data, alreadySent)
            broadcastManager.addRequestToQueue(payload)
        }
    }

    /**
     * Used to initialize a network/chat group between two devices
     * @param other the other [Device] to create this group with
     * initiating the creation, and should be set if receiving creation request
     */
    fun createNetwork(other: Device, name: String="Test Group Name") {
        val newNetworkId = UUID.randomUUID().toString()
        //val newNetworkId = getTestGroupId() // TODO: Test purposes

        val network = ChatGroup(newNetworkId, name, DeviceSet(mutableSetOf()))

        CoroutineScope(Dispatchers.IO).launch {
            dao.insertChatGroup(network)
        }

        if (currentNetworks[newNetworkId] == null) {
            currentNetworks[newNetworkId] = network
        }
        val own = broadcastManager.getThisDevice()
        currentNetworks[newNetworkId]?.deviceSet?.devices?.add(own)

        println("CREATE $currentNetworks")
        currentNetworks[newNetworkId]?.deviceSet?.devices?.forEach {
            println("C ${it.getName()}")
        }

        activeNetworkId = newNetworkId
        addToNetwork(other, newNetworkId)
    }

    /**
     * Adds a given device to the network with given id. Networks use sets, so trying to add
     * devices already in the network doesn't do anything
     * @param other [Device] to add to the network
     * @param id unique id of the network to add the device to
     */
    fun addToNetwork(other: Device, id: String?) {
        println("ADD ${other.getName()}")
        if (id == null) return
        val currentNetwork = currentNetworks[id] ?: return
        // If the device is already in the network, no need to send information about it again
        if (!currentNetwork.deviceSet.devices.add(other)) return

        relayForward(currentNetwork)
    }

    /**
     * Joins this device to the given network
     */
    fun joinNetwork(network: ChatGroup) {
        val id = network.chatGroupId
        val devices = network.deviceSet.devices
        if (currentNetworks[id] == null) {
            currentNetworks[id] = network
        }
        currentNetworks[id]?.deviceSet?.devices?.addAll(devices)
        println("JOIN $currentNetworks")
        currentNetworks[id]?.deviceSet?.devices?.forEach {println("J ${it.getName()}")}

        CoroutineScope(Dispatchers.IO).launch {
            dao.insertChatGroup(network)
        }

        activeNetworkId = id
    }

    fun leaveNetwork() {
        if (activeNetworkId == null) return
        val currentNetworkId = activeNetworkId!!
        activeNetworkId = null

        val deviceName = broadcastManager.getThisDevice().getName()

        currentNetworks.remove(currentNetworkId)

        val payload = Pair(currentNetworkId, deviceName)
        relayForward(payload)

        CoroutineScope(Dispatchers.IO).launch {
            dao.deleteChatGroupMessages(currentNetworkId)
            dao.deleteChatGroup(currentNetworkId)
        }
    }

    fun removeFromNetwork(networkId: String, deviceId: String) {
        val currentNetwork = currentNetworks[networkId] ?: return
        val removedDevice = currentNetwork.deviceSet.devices.firstOrNull { it.getName() == deviceId }
        currentNetwork.deviceSet.devices.remove(removedDevice)
    }

    /**
     * Sends a group-wide message to the network/chat group specified in the networkId
     * @param message actual [Message] to send to the group
     */
    fun sendGroupMessage(message: Message) {
        relayForward(message)
    }
}

/**
 * Generalized class for data to be sent through the network.
 * @param target MAC address of device to send to
 * @param data [Message], [Network] or similar data to send
 * @param alreadySent [MutableSet] of devices the data has already been sent to
 */
data class Data(
    val target: String,
    val data: Any,
    val alreadySent: MutableSet<Device>
) : java.io.Serializable