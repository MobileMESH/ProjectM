package fi.mobilemesh.projectm.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.WIFI_P2P_SERVICE
import android.content.Intent
import android.net.wifi.p2p.*
import android.net.wifi.p2p.WifiP2pManager.*
import android.os.Build
import androidx.lifecycle.MutableLiveData
import fi.mobilemesh.projectm.database.MessageDatabase
import fi.mobilemesh.projectm.database.MessageQueries
import fi.mobilemesh.projectm.database.entities.ChatGroup
import fi.mobilemesh.projectm.database.entities.Message
import kotlinx.coroutines.*
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.lang.ref.WeakReference
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlinx.coroutines.*
import java.io.EOFException
import java.net.ConnectException
import java.net.SocketException
import java.util.LinkedList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


private const val PORT = 8888
private const val TIMEOUT = 10000

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
        private lateinit var weakContext: WeakReference<Context>

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
                weakContext = WeakReference(context)
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
    private val nearbyDevices: MutableLiveData<List<Device>> = MutableLiveData(listOf())
    private val requestQueue = LinkedList<Data>()

    @Volatile
    private var isConnectionFree = true
    private var isSender = false

    private var serverSocket = ServerSocket(PORT)
    private var connectionLatch = CountDownLatch(2)
    private var peerLatch = CountDownLatch(1)
    private var targetAddress: InetAddress? = null

    /**
     * Used to initialize information about the current device, so information can be sent
     * properly
     * @param intent if called from an onReceive method, [Intent] is provided to get
     * information about this device. Otherwise, if called with an API level >= 29,
     * no Intent is needed
     */
    private fun initThisDevice(intent: Intent?) {
        if (thisDevice != null) return

        if (Build.VERSION.SDK_INT >= 29) {
            wifiManager.requestDeviceInfo(channel) { dev ->
                if (dev != null) thisDevice = Device(dev)
                else initThisDevice(intent)
            }
        }

        // Old SDK way of initializing needs an Intent, so return if not available
        if (intent == null) return

        // getParcelableExtra deprecated from API >= 33, which is already
        // handled when api >= 29 above
        @Suppress("DEPRECATION")
        val device: WifiP2pDevice? = intent.getParcelableExtra(EXTRA_WIFI_P2P_DEVICE)
        if (device != null) thisDevice = Device(device)
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
    private val peerListListener = PeerListListener { p ->
        val peers = p.deviceList
        val newDevices: MutableList<Device> = mutableListOf()
        peers.forEach { newDevices.add(Device(it)) }

        nearbyDevices.value = newDevices

        thisDevice?.setAvailableDevices(getNearbyDevices())

        val next = requestQueue.firstOrNull()
        if (next == null || peers.any { it.deviceName == next.target }) {
            peerLatch.countDown()
        }
    }

    /**
     * Listener for when connection status to another device changes
     */
    private val connectionInfoListener = ConnectionInfoListener { connInfo ->
        if (!isConnected(connInfo)) return@ConnectionInfoListener

        connectionLatch.countDown()

        CoroutineScope(Dispatchers.IO).launch {
            if (!connInfo.isGroupOwner) {
                targetAddress = connInfo.groupOwnerAddress
                sendHandshake()
            } else {
                receiveHandshake()
            }
        }
    }

    /**
     * Checks if the user is currently connected to another device. If the user has just
     * disconnected, the next data packet will be sent via [queueNextRequest]
     * @param connInfo connection info from the listener
     * @return true if currently connected to another device, false in all other cases
     */
    private fun isConnected(connInfo: WifiP2pInfo): Boolean {
        // Group doesn't exist yet OR disbanded: no way to distinguish from here
        if (!connInfo.groupFormed) {
            if (connectionLatch.count == 0L) {
                // If all data has been sent/received, we probably have disconnected
                connectionLatch = CountDownLatch(2)
                wifiManager.discoverPeers(channel, null)

                CoroutineScope(Dispatchers.IO).launch { queueNextRequest() }
            }
            return false
        }

        // In the middle of connecting
        else if (connectionLatch.count != 2L) {
            return false
        }

        isConnectionFree = false
        return true
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
            try {
                socket.connect(InetSocketAddress(targetAddress, PORT), TIMEOUT)
            }
            catch (e: ConnectException) {
                println("ECONNREFUSED: ${e.stackTrace}")
                timeoutConnection()
            }
            finally {
                socket.close()
            }

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
        if (isSender) return

        println("RECEIVE/START")

        withContext(Dispatchers.IO) {
            val client = try {
                serverSocket.accept()
            }
            // This is used as the primary method to stop listening after disconnecting,
            // by closing the socket
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
                println(e.stackTrace)
                resetConnection()
                return@withContext
            }
            catch(e: SocketException) {
                println("SocketException: $e")
                println(e.stackTrace)
                resetConnection()
                return@withContext
            }

            val incoming = istream.readObject()

            if (incoming !is Data) return@withContext

            println("IN ${incoming.data}")

            when (val data = incoming.data) {
                is Message -> {
                    data.isOwnMessage = false
                    dao.insertMessage(data)
                }

                is ChatGroup -> {
                    meshManager.joinNetwork(data)
                }

                is Pair<*, *> -> {
                    val networkId = data.first as String
                    val deviceId = data.second as String
                    meshManager.removeFromNetwork(networkId, deviceId)
                }
            }

            meshManager.relayForward(incoming.data, incoming.alreadySent)

            istream.close()
            client.close()

            println("RECEIVE/END")
            resetConnection()
        }
    }

    /**
     * Creates a notification from a message if notifications are allowed.
     * @param message a [Message] instance from which to create the notification
     */
    /*private fun createMessageNotification(message: Message) {
            val notificationHelper = weakContext.get()?.let { MakeNotification(it) }
            val intent = Intent(weakContext.get(), MainActivity::class.java)
            // TODO: replace value 0 with actual chat group number, should work that way
            intent.putExtra("chat_id", 0)
            notificationHelper?.showNotification(
                message.sender,
                message.body, intent
            )
    }*/
    /**
     * Used to send any type of data to another device. Connects to given address automatically
     * before sending data
     * @param address MAC address of the target device to send data to
     * @param data any type of data to send to the target
     */
    private fun sendData(address: String, data: Any) {
        println("SEND $data")
        connectToDevice(address)
        // Latch is used to wait for connection to be established
        // Returns false when the timeout has passed
        val success = connectionLatch.await(TIMEOUT.toLong(), TimeUnit.MILLISECONDS)

        if (!success) {
            timeoutConnection()
        }
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
     * Times out a connection attempt
     */
    private fun timeoutConnection() {
        println("TIMED OUT $isSender")
        connectionLatch = CountDownLatch(0)

        val first = requestQueue.removeFirstOrNull()
        if (first != null) {
            println("QUEUE DELAY ${first.data}")
            requestQueue.addLast(first)
        }

        wifiManager.cancelConnect(channel, null)
        resetConnection()
        isConnectionFree = true
        return
    }

    /**
     * Resets the connection after all data has been transferred, effectively disconnecting
     * from the connection freeing both devices for further connections
     */
    private fun resetConnection() {
        targetAddress = null
        isSender = false

        wifiManager.removeGroup(channel, null)
        println("DISCONNECTED")
    }

    /**
     * Adds a data sending request to the queue. The queue works on a FIFO basis, and will
     * send the next request as soon as the previous is complete (after disconnect)
     * @param data [Data] to send, including target device's address
     */
    fun addRequestToQueue(data: Data) {
        println("QUEUE ADD $data")
        requestQueue.addLast(data)
        if (isConnectionFree) {
            isConnectionFree = false
            CoroutineScope(Dispatchers.IO).launch { queueNextRequest() }
        }
    }

    // TODO: Better nearby device detection
    /**
     * Sends the next Data packet to the target device, after the previous one has been handled
     * and only after the target device is nearby.
     */
    private suspend fun queueNextRequest() {
        withContext(Dispatchers.IO) {
            val next = requestQueue.firstOrNull()
            if (next == null) {
                println("QUEUE FREE")
                isConnectionFree = true
                return@withContext
            }
            println("QUEUE MOV $next")
            var target = getNearbyDevices().firstOrNull { it.getName() == next.target }
            if (target == null) {
                println("Waiting")
                peerLatch = CountDownLatch(1)
            }
            val success = peerLatch.await(TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
            if (!success) {
                timeoutConnection()
            }

            target = getNearbyDevices().first { it.getName() == next.target }

            requestQueue.removeFirst()
            println("TARGET ${target.getAddress()}")
            isSender = true
            sendData(target.getAddress(), next)
        }
    }

}