package fi.mobilemesh.projectm.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.*
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import fi.mobilemesh.projectm.MainActivity
import fi.mobilemesh.projectm.database.MessageDatabase
import fi.mobilemesh.projectm.database.entities.Message
import fi.mobilemesh.projectm.utils.showNeutralAlert
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.*

private const val PORT = 8888
private const val TIMEOUT = 5000

class BroadcastManager(
    private val wifiManager: WifiP2pManager,
    private val channel: Channel,
    private val activity: MainActivity

): BroadcastReceiver() {


    // TODO: (General) Move text field editing to separate class/back to MainActivity.kt

    private val dao = MessageDatabase.getInstance(activity).dao

    private val serverSocket = ServerSocket(PORT)
    private var targetAddress: InetAddress? = null

    private val peerListListener = PeerListListener { peers ->
        val refreshedPeers = peers.deviceList
        activity.deviceList.removeAllViews()
        refreshedPeers.forEach { createDeviceCard(it) }
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

    // TODO: Move this somewhere more sensible
    private fun createDeviceCard(device: WifiP2pDevice) {
        val card = Button(activity)
        card.text = device.deviceName

        card.setOnClickListener {
            connectToDevice(device.deviceAddress)
        }

        activity.deviceList.addView(card)
    init {
        CoroutineScope(Dispatchers.Main).launch {
            val messages = dao.getChatGroupMessages(0)
            messages.forEach {
                val messageColor = if (it.isOwnMessage) Color.parseColor("#017f61")
                    else Color.parseColor("#262626")
                val textColor = if (it.isOwnMessage) Color.BLACK
                    else Color.WHITE
                createMessage(it, messageColor, textColor)
            }
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

    private fun connectToDevice(address: String) {
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

            istream.close()
            client.close()

            withContext(Dispatchers.Main) {
                createMessage(message, Color.parseColor("#262626"), Color.WHITE)
            }

            dao.insertMessage(message)

            receiveText()
        }
    }

    fun sendText(text: String) {
        if (targetAddress == null) {
            showNeutralAlert("No connection!", "You are not connected to any device.", activity)
            return
        }

        // TODO: Should not be able to send empty message
        if (text == "") {
            showNeutralAlert("Empty message",
                "Can not send empty message (this is a placeholder)",
                activity)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val time = Date(System.currentTimeMillis())
            // TODO: Set chat group id properly. Current is a placeholder
            val id = dao.getNextMessageId(0)
            // TODO: Get sender name from Device object (probably?)
            // TODO: Get chat group id
            // TODO: Get unique message id within chat group (should be in rising order)
            val message = Message(id, 0, "SENDER", time, text)

            val socket = Socket()
            socket.connect(InetSocketAddress(targetAddress, PORT), TIMEOUT)
            val ostream = ObjectOutputStream(BufferedOutputStream(socket.getOutputStream()))

            ostream.writeObject(message)

            ostream.close()
            socket.close()

            message.isOwnMessage = true

            withContext(Dispatchers.Main) {
                createMessage(message, Color.parseColor("#017f61"), Color.BLACK)
            }

            dao.insertMessage(message)
        }
    }


    private fun createMessage(message: Message, messageColor: Int, textColor: Int) {
        val btn = Button(activity)
        val alignment = if (message.isOwnMessage) Gravity.END else Gravity.START

        btn.isClickable = false
        btn.text = "[${message.timestamp}] [${message.sender}] ${message.body}"

        btn.maxWidth = (activity.receivingField.width * 0.67).toInt()

        btn.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, // Width
            LinearLayout.LayoutParams.WRAP_CONTENT  // Height
        ).apply {
            gravity = alignment
        }

        // Text alignment
        btn.gravity = Gravity.START
        btn.isAllCaps = false
        btn.setBackgroundColor(messageColor)
        btn.setTextColor(textColor)

        activity.receivingField.addView(btn)
    }

    // TODO: Move this somewhere more sensible
    private fun createDeviceButton(device: WifiP2pDevice) {
        val btn = Button(activity)
        btn.text = device.deviceName

        btn.setOnClickListener {
            connectToDevice(device.deviceAddress)
        }

       activity.deviceList.addView(btn)
    }
}