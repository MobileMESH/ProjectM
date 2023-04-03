package fi.mobilemesh.projectm.network

import android.content.Context
import fi.mobilemesh.projectm.database.entities.Message
import fi.mobilemesh.projectm.database.entities.MessageData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    private val currentNetworks: MutableMap<String, MutableSet<Device>> = mutableMapOf()

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
    fun createNetwork(other: Device) {
        //val newNetworkId = UUID.randomUUID().toString()
        val newNetworkId = getTestGroupId() // TODO: Test purposes

        if (currentNetworks[newNetworkId] == null) {
            currentNetworks[newNetworkId] = mutableSetOf()
        }
        val own = broadcastManager.getThisDevice()
        currentNetworks[newNetworkId]?.add(other)
        currentNetworks[newNetworkId]?.add(own)

        println("CREATE $currentNetworks")
        currentNetworks[newNetworkId]?.forEach {
            println("C ${it.getName()}")
        }

        addToNetwork(other, newNetworkId)

        /*CoroutineScope(Dispatchers.IO).launch {
            broadcastManager.sendData(other.getAddress(), Pair(own, newNetworkId))
        }

        else {
            if (currentNetworks[networkId] == null) {
                currentNetworks[networkId] = mutableSetOf()
            }
            currentNetworks[networkId]?.add(other)
            println("TEST 2 $currentNetworks")
        }*/
    }

    fun addToNetwork(other: Device, id: String=getTestGroupId()) {
        val network = Network(id, currentNetworks[id]!!)
        CoroutineScope(Dispatchers.IO).launch {
            broadcastManager.sendData(other.getAddress(), network)
        }
    }

    fun joinNetwork(network: Network) {
        val id = network.id
        val others = network.others
        if (currentNetworks[id] == null) {
            currentNetworks[id] = mutableSetOf()
        }
        currentNetworks[id]?.addAll(others)

        println("JOIN ${currentNetworks}")
        currentNetworks[id]?.forEach {
            println("J ${it.getName()}")
        }
    }

    /**
     * Sends a group-wide message to the network/chat group specified in the networkId
     * @param message actual [Message] to send to the group
     * @param alreadySent set of devices the message was already sent to, avoiding repeat
     */
    fun sendGroupMessage(message: Message,
                         alreadySent: MutableSet<Device>
                         = mutableSetOf(broadcastManager.getThisDevice())) {

        val network = currentNetworks[message.chatGroupId] ?: return
        val availableDevices = broadcastManager.getNearbyDevices()

        // Valid devices are both in range (available) and within the selected network,
        // but not in the devices the message has been sent to

        val validDevices = availableDevices
            .filter { it in network }
            .filterNot { it in alreadySent }

        alreadySent.addAll(validDevices)

        val messageData = MessageData(message, alreadySent)

        CoroutineScope(Dispatchers.IO).launch {
            validDevices.forEach {
                broadcastManager.sendData(it.getAddress(), messageData)
                delay(100)
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