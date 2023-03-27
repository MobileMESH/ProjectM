package fi.mobilemesh.projectm.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.WIFI_P2P_SERVICE
import android.content.Intent
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.*
import android.util.Log
import android.widget.Button
import fi.mobilemesh.projectm.database.MessageDatabase
import fi.mobilemesh.projectm.database.MessageQueries
import fi.mobilemesh.projectm.database.entities.Message
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import fi.mobilemesh.projectm.Networks
import kotlinx.coroutines.*


private const val PORT = 8888
private const val TIMEOUT = 5000

class BroadcastManager(
    private val wifiManager: WifiP2pManager,
    private val channel: Channel
): BroadcastReceiver() {
    /**
     * Used to get the BroadcastManager from any fragment/class
     */

    val devices = mutableListOf<Device>()
    companion object {
        @Volatile
        private var INSTANCE: BroadcastManager? = null
        private lateinit var dao: MessageQueries

        /**
         * Gets the common/static BroadcastManager from any fragment/activity
         * @param context [Context] of the fragment/activity from where this is being requested
         * @return existing [BroadcastManager] if one already exists. Creates a new one, returns
         * that and sets that as the static BroadcastManager if none exist
         */
        fun getInstance(context: Context): BroadcastManager {
            synchronized(this) {
                val wifiManager = context.getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
                val channel = wifiManager.initialize(context, context.mainLooper, null)

                return INSTANCE ?: BroadcastManager(wifiManager, channel)
                    .also {
                        INSTANCE = it
                        dao = MessageDatabase.getInstance(context).dao
                    }
            }
        }
    }

    private val serverSocket = ServerSocket(PORT)
    private var targetAddress: InetAddress? = null

    /**
     * Listener object for when nearby devices get updated
     */
//    private val peerListListener = PeerListListener { peers ->
//
//        Networks.refreshDeviceList(peers.deviceList)
//        val deviceList = peers.deviceList
//        val devices = deviceList.map { Device(it) }
//        devices.forEach { device ->
//
//            println("Device name: ${device.returnName()}, address: ${device.returnAddress()}")
//        }
//
//
//    }

    private val peerListListener = PeerListListener { peers ->
        Networks.refreshDeviceList(peers.deviceList)
        val deviceList = peers.deviceList

        for (device in deviceList) {
            val neDevice = Device(device)
            devices.add(neDevice)

        }
        devices.forEach { device ->
            println("Device name: ${device.returnName()}, address: ${device.returnAddress()}")
        }
    }

    /**
     * Listener for when connection status to another device changes
     */
    // TODO: Move to its own class? This fires as soon as any, even incomplete information is available
    // TODO: Show the user information about status
    private val connectionInfoListener = ConnectionInfoListener { conn ->
        // TODO: Get device name instead
        Networks.changeTargetAddress(conn.groupOwnerAddress)

        if (!conn.groupFormed) {
            targetAddress = null
            return@ConnectionInfoListener
        }

        if (!conn.isGroupOwner) {
            targetAddress = conn.groupOwnerAddress
            sendHandshake()
        } else {
           receiveHandshake()
        }
    }


    /**
     * Used to detect status changes related to Wi-Fi Direct, such as nearby devices changing
     * and connection changing
     * @param context context of fragment/activity where the event could fire
     * @param intent intent of the fragment/activity, maybe??
     */

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WIFI_P2P_STATE_CHANGED_ACTION -> {
                val state = intent.getIntExtra(EXTRA_WIFI_STATE, -1)
                if (state != WIFI_P2P_STATE_ENABLED) {
                    return
                }
                discoverPeers()
            }

              WIFI_P2P_PEERS_CHANGED_ACTION -> {
                wifiManager.requestPeers(channel, peerListListener)
            }

            WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                wifiManager.requestConnectionInfo(channel, connectionInfoListener)
            }
        }
    }

    /**
     * Used to refresh list of nearby devices. Has triggers for success and failure, currently
     * not used
     */
    private fun discoverPeers() {
        wifiManager.discoverPeers(channel, object : ActionListener {
            override fun onSuccess() {
                //TODO: Does not seem to need anything?
            }

            override fun onFailure(reason: Int) {
                //TODO: Display error
            }
        })
    }

    /**
     * Connects this device to given address through Wi-Fi Direct framework
     * @param address address of the target device
     */
    fun connectToDevice(address: String) {
        val config = WifiP2pConfig()
        config.deviceAddress = address

        wifiManager.connect(channel, config, object : ActionListener {
            override fun onSuccess() {
            // activity.statusField.text = "Started connection to $address"
                println("Successfully started connection")
            }

            override fun onFailure(reason: Int) {
                //activity.statusField.text = "Failed to connect! - code $reason"
                println("Failed to connect - $reason")
            }
        })
    }

    /**
     * Used by the "server" when first connecting through [connectToDevice]. Used to get
     * the clients IP address
     */
    private fun receiveHandshake() {
        CoroutineScope(Dispatchers.IO).launch {
            val client = serverSocket.accept()
            targetAddress = client.inetAddress
            client.close()

            receiveText()
        }
    }

    /**
     * Used by the client to initiate connection to the "server" device when first
     * connecting through [connectToDevice]. Used to send this devices IP address to "server"
     */
    private fun sendHandshake() {
        CoroutineScope(Dispatchers.IO).launch {
            val socket = Socket()
            socket.connect(InetSocketAddress(targetAddress, PORT), TIMEOUT)
            socket.close()

            receiveText()
        }
    }

    /**
     * Continually run by both client and "server" to listen for incoming traffic. Reads incoming
     * data and fires itself again to set up listening
     */
    private fun receiveText() {
        CoroutineScope(Dispatchers.IO).launch {
            val client = serverSocket.accept()
            // Client has connected
            // (Buffered) input stream from client
            val istream = ObjectInputStream(BufferedInputStream(client.getInputStream()))

            val message: Message = istream.readObject() as Message
            println(message.isOwnMessage)

            istream.close()
            client.close()

            // Insert message to database via Data Access Object
            dao.insertMessage(message)

            receiveText()
        }
    }

    /**
     * Transfers text to the other device with a [Message].
     * @param message [Message] to transfer to the other device
     */
    fun transferText(message: Message) {
        // Should be checked externally but left for redundancy
        if (!isConnected()) {
            return
        }

        // Empty message should be checked externally but left for redundancy
        if (message.body == "") {
            return
        }

        val socket = Socket()
        socket.connect(InetSocketAddress(targetAddress, PORT), TIMEOUT)
        val ostream = ObjectOutputStream(BufferedOutputStream(socket.getOutputStream()))

        ostream.writeObject(message)

        ostream.close()
        socket.close()
    }

    /**
     * Checks if this device is connected to another device, so messages can be sent
     * @return true if [targetAddress] is set, false otherwise
     */
    fun isConnected(): Boolean {
        return targetAddress != null
    }
}