package fi.mobilemesh.projectm.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.WIFI_P2P_SERVICE
import android.content.Intent
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.*
import android.os.Build
import androidx.lifecycle.MutableLiveData
import fi.mobilemesh.projectm.database.MessageDatabase
import fi.mobilemesh.projectm.database.MessageQueries
import fi.mobilemesh.projectm.database.entities.MessageData
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
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
                        it.initThisDevice(null)
                    }
            }
        }
    }

    private var thisDevice: Device? = null
    private var nearbyDevices: MutableLiveData<List<Device>> = MutableLiveData(listOf())

    @Volatile
    private var isConnecting = false

    private var serverSocket = ServerSocket(PORT)
    private var connectionLatch = CountDownLatch(1)
    private var targetAddress: InetAddress? = null

    /**
     * Used to initialize information about the current device, so information can be sent
     * properly
     * @param intent if called from an onReceive method, [Intent] is provided to get
     * information about this device. Otherwise, if called with an API level >= 29,
     * no Intent is needed
     */
    private fun initThisDevice(intent: Intent?) {
        if (Build.VERSION.SDK_INT >= 29) {
            wifiManager.requestDeviceInfo(channel) { dev ->
                thisDevice = Device(dev ?: WifiP2pDevice())
            }
        }

        // Old SDK way of initializing needs an Intent, so return if not available
        if (intent == null) return

        // getParcelableExtra deprecated from API >= 33, which is already
        // handled when api >= 29 above
        @Suppress("DEPRECATION")
        if (thisDevice == null) {
            val device: WifiP2pDevice? = intent.getParcelableExtra(EXTRA_WIFI_P2P_DEVICE)
            if (device != null) thisDevice = Device(device)
        }
    }

    /**
     * Returns the device we are currently running this application on
     * @return [Device] representing the current Android device
     */
    fun getThisDevice(): Device {
        return thisDevice ?: Device(WifiP2pDevice())
    }

    /**
     * Returns a [MutableLiveData], which includes a list of all detected devices.
     * Note that devices that aren't available anymore are included in this as well, as
     * there is currently no way to remove them from available peers
     * @return live data which contains a list of 'available' peers
     */
    fun getLiveNearbyDevices(): MutableLiveData<List<Device>> {
        return nearbyDevices
    }

    /**
     * Returns a static [Collection] of all detected devices, for when that information
     * is not needed live but rather momentarily
     * @return collection of detected devices at the moment of calling this function
     */
    fun getNearbyDevices(): Collection<Device> {
        return nearbyDevices.value ?: listOf()
    }

    /**
     * Listens to changes in available devices, updating live lists accordingly
     */
    private val peerListListener = PeerListListener { peers ->
        val newDevices: MutableList<Device> = mutableListOf()
        peers.deviceList.forEach { newDevices.add(Device(it)) }

        nearbyDevices.value = newDevices

        thisDevice?.setAvailableDevices(getNearbyDevices())
    }

    /**
     * Listener for when connection status to another device changes
     */
    private val connectionInfoListener = ConnectionInfoListener { conn ->
        println(conn)
        if (!conn.groupFormed || isConnecting) {
            targetAddress = null
            wifiManager.discoverPeers(channel, null)
            return@ConnectionInfoListener
        }

        isConnecting = true
        if (serverSocket.isClosed) serverSocket = ServerSocket(PORT)

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
            WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                initThisDevice(intent)
            }

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
        }
    }

    /**
     * Connects this device to given address through Wi-Fi Direct framework
     * @param address MAC address of the target device
     */
    private fun connectToDevice(address: String) {
        println("CONNECT")
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
     * Run by both client and "server" to listen for incoming traffic. Reads incoming
     * data and shuts down after reading
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
                println("EOFException $e")
                return@withContext
            }
            catch(e: SocketException) {
                println("SocketException: $e")
                return@withContext
            }

            when (val incoming = istream.readObject()) {
                is MessageData -> {
                    val message = incoming.message
                    val alreadySent = incoming.alreadySent

                    message.isOwnMessage = false
                    dao.insertMessage(message)
                    meshManager.sendGroupMessage(message, alreadySent)
                }

                is Pair<*, *> -> {
                    // TODO: Placeholder until a Network class is created
                    val other = incoming.first as Device
                    val id = incoming.second as String?
                    meshManager.createNetwork(other, id)
                }
            }

            istream.close()
            client.close()

            println("RECEIVE/END")
            resetConnection()
        }
    }

    /**
     * Used to send any type of data to another device. Connects to given address automatically
     * before sending data
     * @param address MAC address of the target device to send data to
     * @param data any type of data to send to the target
     */
    fun sendData(address: String, data: Any) {
        println("SEND FROM ${getThisDevice().getName()}")
        connectToDevice(address)
        // Latch is used to wait for connection to be established
        connectionLatch.await()

        val socket = Socket()
        socket.connect(InetSocketAddress(targetAddress, PORT), TIMEOUT)
        val ostream = ObjectOutputStream(BufferedOutputStream(socket.getOutputStream()))

        ostream.writeObject(data)

        ostream.close()
        socket.close()

        println("SEND/END")
        resetConnection()
    }

    /**
     * Resets the connection after all data has been transferred, effectively disconnecting
     * from the connection freeing both devices for further connections
     */
    private fun resetConnection() {
        connectionLatch = CountDownLatch(1)
        targetAddress = null
        isConnecting = false

        println(serverSocket.close())

        wifiManager.removeGroup(channel, null)
        println("DISCONNECTED")
    }
}