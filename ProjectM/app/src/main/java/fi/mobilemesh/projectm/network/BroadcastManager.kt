package fi.mobilemesh.projectm.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.WIFI_P2P_SERVICE
import android.content.Intent
import android.net.wifi.p2p.WifiP2pConfig
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
    // TODO: Find a workaround for this, it ain't gonna work for long
    //  (might crash when switching back and forth between network fragment)
    private lateinit var networks: Networks

    /**
     * Temporary function for setting networks fragment to create buttons for connecting
     * devices
     * @param networks Networks.kt fragment to set as current network screen
     */
    fun setNetworks(networks: Networks) {
        this.networks = networks
    }

    val peerListListener = WifiP2pManager.PeerListListener { peers ->
        val refreshedPeers = peers.deviceList
        networks.clearDevices()
        refreshedPeers.forEach { networks.createCardViewLayout(it) }
    }

    // TODO: Move to its own class? This fires as soon as any, even incomplete information is available
    private val connectionInfoListener = ConnectionInfoListener { conn ->
        if (!conn.groupFormed) {
            //activity.statusField.text = "Connection failed: device declined connection?"
            targetAddress = null
            return@ConnectionInfoListener
        }

     //   activity.statusField.text = "Connection successful"
        if (!conn.isGroupOwner) {
            targetAddress = conn.groupOwnerAddress
            sendHandshake()
        } else {
           receiveHandshake()
        }
    }

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

    private fun receiveHandshake() {
        CoroutineScope(Dispatchers.IO).launch {
            val client = serverSocket.accept()
            targetAddress = client.inetAddress
            client.close()

            receiveText()
        }
    }

    private fun sendHandshake() {
        CoroutineScope(Dispatchers.IO).launch {
            val socket = Socket()
            socket.connect(InetSocketAddress(targetAddress, PORT), TIMEOUT)
            socket.close()

            receiveText()
        }
    }

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

            dao.insertMessage(message)

            receiveText()
        }
    }

    suspend fun transferText(message: Message) {
        // Should be checked externally but left for redundancy
        if (!isConnected()) {
            return
        }

        // Empty message should be checked externally but left for redundancy
        if (message.body == "") {
            return
        }
        val job = CoroutineScope(Dispatchers.IO).async {
            val socket = Socket()
            socket.connect(InetSocketAddress(targetAddress, PORT), TIMEOUT)
            val ostream = ObjectOutputStream(BufferedOutputStream(socket.getOutputStream()))

            ostream.writeObject(message)

            ostream.close()
            socket.close()
        }
        job.await()
    }

    /**
     * Checks if this device is connected to another device, so messages can be sent
     */
    fun isConnected(): Boolean {
        return targetAddress != null
    }
}