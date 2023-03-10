package fi.mobilemesh.projectm.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.*
import android.widget.Button
import fi.mobilemesh.projectm.MainActivity
import fi.mobilemesh.projectm.utils.showNeutralAlert
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

private const val PORT = 8888
private const val TIMEOUT = 5000

class BroadcastManager(
    private val wifiManager: WifiP2pManager,
    private val channel: Channel,
    private val activity: MainActivity
): BroadcastReceiver() {

    //TODO: Move text field editing to separate class/back to MainActivity.kt

    private val serverSocket = ServerSocket(PORT)
    private var targetAddress: InetAddress? = null

    private val peerListListener = PeerListListener { peers ->
        val refreshedPeers = peers.deviceList
        activity.deviceList.removeAllViews()
        refreshedPeers.forEach { createButton(it) }
    }

    // TODO: Move to its own class? This fires as soon as any, even incomplete information is available
    private val connectionInfoListener = ConnectionInfoListener { conn ->
        if (!conn.groupFormed) {
            activity.statusField.text = "Connection failed: device declined connection?"
            targetAddress = null
            return@ConnectionInfoListener
        }

        activity.statusField.text = "Connection successful"
        if (!conn.isGroupOwner) {
            targetAddress = conn.groupOwnerAddress
            sendHandshake()
        } else {
            receiveHandshake()
        }
    }

    // TODO: Move this somewhere more sensible
    private fun createButton(device: WifiP2pDevice) {
        val btn = Button(activity)
        btn.text = device.deviceName

        btn.setOnClickListener {
            connectToDevice(device.deviceAddress)
        }

        activity.deviceList.addView(btn)
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
                activity.statusField.text = "Started connection to $address"
                println("Successfully started connection")
            }

            override fun onFailure(reason: Int) {
                activity.statusField.text = "Failed to connect! - code $reason"
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
            val istream = client.getInputStream()

            val sb = java.lang.StringBuilder()

            // Should be able to be replaced with readAll() in API 33 upwards
            var c = istream.read()
            while ((c >= 0) && (c != 0x0a)) {
                if (c != 0x0d) {
                    sb.append(c.toChar())
                }
                c = istream.read()
            }

            istream.close()
            client.close()
            val text = sb.toString()

            withContext(Dispatchers.Main) {
                activity.receivingField.text = text
            }

            receiveText()
        }
    }

    fun sendText(text: String) {
        if (targetAddress == null) {
            showNeutralAlert("No connection!", "You are not connected to any device.", activity)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val socket = Socket()
            socket.connect(InetSocketAddress(targetAddress, PORT), TIMEOUT)
            val ostream = socket.getOutputStream()
            ostream.write(text.toByteArray())
            ostream.close()
            socket.close()
        }
    }
}