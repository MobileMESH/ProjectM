package fi.mobilemesh.projectm.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.WIFI_P2P_SERVICE
import android.content.Intent
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.*
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
import java.io.EOFException
import java.net.SocketException
import java.util.concurrent.CountDownLatch

private const val PORT = 8888
private const val TIMEOUT = 5000

class BroadcastManager(
    private val wifiManager: WifiP2pManager,
    private val channel: Channel
): BroadcastReceiver() {
    /**
     * Used to get the BroadcastManager from any fragment/class
     */
    companion object {
        @Volatile
        private var INSTANCE: BroadcastManager? = null
        private lateinit var dao: MessageQueries
        private lateinit var meshManager: MeshManager

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
                        meshManager = MeshManager.getInstance(context)
                    }
            }
        }
    }
    @Volatile
    private var isConnecting = false

    private var thisDevice: WifiP2pDevice? = null

    private var serverSocket = ServerSocket(PORT)
    private var connectionLatch = CountDownLatch(1)
    private var targetAddress: InetAddress? = null

    /**
     * Listener object for when nearby devices get updated
     */
    private val peerListListener = PeerListListener { peers ->
        Networks.refreshDeviceList(peers.deviceList)
    }

    /**
     * Listener for when connection status to another device changes
     */
    // TODO: Move to its own class? This fires as soon as any, even incomplete information is available
    // TODO: Show the user information about status
    private val connectionInfoListener = ConnectionInfoListener { conn ->
        if (!conn.groupFormed || isConnecting) {
            targetAddress = null
            wifiManager.discoverPeers(channel, null)
            return@ConnectionInfoListener
        }

        isConnecting = true

        CoroutineScope(Dispatchers.IO).launch {
            if (!conn.isGroupOwner) {
                targetAddress = conn.groupOwnerAddress
                sendHandshake()
            } else {
                receiveHandshake()
            }
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

                wifiManager.discoverPeers(channel, null)
            }

              WIFI_P2P_PEERS_CHANGED_ACTION -> {
                wifiManager.requestPeers(channel, peerListListener)
            }

            WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                wifiManager.requestConnectionInfo(channel, connectionInfoListener)
            }

            WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                thisDevice = intent.getParcelableExtra(EXTRA_WIFI_P2P_DEVICE)
            }
        }
    }

    /**
     * Connects this device to given address through Wi-Fi Direct framework
     * @param address address of the target device
     */
    private fun connectToDevice(address: String) {
        val config = WifiP2pConfig()
        config.deviceAddress = address

        wifiManager.connect(channel, config, null)
    }

    /**
     * Used by the "server" when first connecting through [connectToDevice]. Used to get
     * the clients IP address
     */
    private suspend fun receiveHandshake() {
        withContext(Dispatchers.IO) {
            val client = serverSocket.accept()
            targetAddress = client.inetAddress
            client.close()

            println("HS/RECEIVE")
            receiveData()
        }
    }

    /**
     * Used by the client to initiate connection to the "server" device when first
     * connecting through [connectToDevice]. Used to send this devices IP address to "server"
     */
    private suspend fun sendHandshake() {
        withContext(Dispatchers.IO) {
            val socket = Socket()
            socket.connect(InetSocketAddress(targetAddress, PORT), TIMEOUT)
            socket.close()

            println("HS/SEND")
            receiveData()
        }
    }

    /**
     * Continually run by both client and "server" to listen for incoming traffic. Reads incoming
     * data and fires itself again to set up listening
     */
    private suspend fun receiveData() {
        connectionLatch.countDown()
        println("RECEIVE/START")
        withContext(Dispatchers.IO) {
            val client = try {
                serverSocket.accept()
            }
            catch (e: SocketException) {
                println("RECEIVE/ABORT")
                return@withContext
            }
            // Client has connected at this point
            // (Buffered) input stream from client
            val istream = try {
                ObjectInputStream(BufferedInputStream(client.getInputStream()))
            }
            catch (e: EOFException) {
                println(e)
                return@withContext
            }
            catch(e: SocketException) {
                println(e)
                return@withContext
            }

            when (val incoming = istream.readObject()) {
                is Message -> dao.insertMessage(incoming)
                is String -> meshManager.createNetwork(incoming)
            }

            istream.close()
            client.close()

            println("RECEIVE/END")
            resetConnection()
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

        resetConnection()
    }

    /**
     * Used to send information about the creation of a group to the device the group was
     * created with
     */
    fun sendNetworkCreationInfo(other: WifiP2pDevice) {
        connectToDevice(other.deviceAddress)
        connectionLatch.await()

        val socket = Socket()
        socket.connect(InetSocketAddress(targetAddress, PORT), TIMEOUT)
        val ostream = ObjectOutputStream(BufferedOutputStream(socket.getOutputStream()))

        ostream.writeObject(thisDevice?.deviceName)

        ostream.close()
        socket.close()

        println("SEND/END")
        resetConnection()
    }

    /**
     * Resets the connection after all data has been transferred
     */
    private fun resetConnection() {
        connectionLatch = CountDownLatch(1)
        targetAddress = null
        isConnecting = false

        serverSocket.close()
        serverSocket = ServerSocket(PORT)

        wifiManager.removeGroup(channel, null)
        println("DISCONNECTED")
    }

    /**
     * Checks if this device is connected to another device, so messages can be sent
     * @return true if [targetAddress] is set, false otherwise
     */
    fun isConnected(): Boolean {
        return targetAddress != null
    }
}