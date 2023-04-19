package fi.mobilemesh.projectm.network

import android.content.Context
import fi.mobilemesh.projectm.database.MessageDatabase
import fi.mobilemesh.projectm.database.MessageQueries
import fi.mobilemesh.projectm.database.entities.ChatGroup
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
                    .also {
                        INSTANCE = it
                        it.broadcastManager = BroadcastManager.getInstance(context)
                        dao = MessageDatabase.getInstance(context).dao
                    }
            }
        }
    }

    private lateinit var broadcastManager: BroadcastManager
    // Networks we have joined (TODO: Save somewhere, currently runtime only)
    private val currentNetworks: MutableMap<String, MutableSet<Device>> = mutableMapOf()

    /**
     * Returns the first network/chat group id available for us. Testing only
     * @return id of the first network we have available
     */
    /*fun getTestGroupId(): String {
        return "TEST_GROUP"
    }*/

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
            is Network -> data.id
            else -> null
        }

        val network = currentNetworks[networkId] ?: return
        val availableDevices = broadcastManager.getNearbyDevices()

        // Valid devices are both in range (available) and within the selected network,
        // but not in the devices the message has been sent to

        val validDevices = network.filterNot { it in alreadySent }

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
    fun createNetwork(other: Device) {
        val newNetworkId = UUID.randomUUID().toString()
        //val newNetworkId = getTestGroupId() // TODO: Test purposes

        CoroutineScope(Dispatchers.IO).launch {
            dao.insertChatGroup(ChatGroup(newNetworkId))
        }

        if (currentNetworks[newNetworkId] == null) {
            currentNetworks[newNetworkId] = mutableSetOf()
        }
        val own = broadcastManager.getThisDevice()
        currentNetworks[newNetworkId]?.add(own)

        println("CREATE $currentNetworks")
        currentNetworks[newNetworkId]?.forEach {
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
        if (id == null) return
        val currentNetwork = currentNetworks[id] ?: return
        // If the device is already in the network, no need to send information about it again
        if (!currentNetwork.add(other)) return
        val network = Network(id, currentNetwork)

        relayForward(network)
    }

    /**
     * Joins this device to the given network
     */
    fun joinNetwork(network: Network) {
        val id = network.id
        val devices = network.devices
        if (currentNetworks[id] == null) {
            currentNetworks[id] = mutableSetOf()
        }
        currentNetworks[id]?.addAll(devices)
        println("JOIN $currentNetworks")
        currentNetworks[id]?.forEach {println("J ${it.getName()}")}

        CoroutineScope(Dispatchers.IO).launch {
            dao.insertChatGroup(ChatGroup(id))
        }

        activeNetworkId = id
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