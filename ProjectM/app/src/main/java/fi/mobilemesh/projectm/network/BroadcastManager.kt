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

class BroadcastManager(
    private val wifiManager: WifiP2pManager,
    private val channel: Channel,
    private val activity: MainActivity
): BroadcastReceiver() {

    private var socket: Socket? = null
    private var targetAddress: InetAddress? = null

    private val peerList = mutableListOf<WifiP2pDevice>()
    private val peerListListener = PeerListListener { peers ->
        val refreshedPeers = peers.deviceList
        if (refreshedPeers != peerList) {
            activity.deviceList.removeAllViews()
            peerList.clear()
            peerList.addAll(refreshedPeers)
            refreshedPeers.forEach { createButton(it) }
        }
    }

    private val connectionInfoListener = ConnectionInfoListener { conn ->
        if (conn.groupFormed) {
            activity.statusField.text = "Connection successful"
            receiveText()
            socket = Socket()
            socket!!.bind(null)
            targetAddress = conn.groupOwnerAddress

        } else {
            activity.statusField.text = "Connection failed: device declined connection?"
            socket = null
            targetAddress = null
        }
    }

    // Temporary placement!!
    private fun createButton(device: WifiP2pDevice) {
        val btn = Button(activity)
        btn.text = device.deviceName

        btn.setOnClickListener {
            connectToDevice(device.deviceAddress)
        }

        activity.deviceList.addView(btn)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        // Wifi P2P connectivity switched on/off
        if (action == WIFI_P2P_STATE_CHANGED_ACTION) {
            val state = intent.getIntExtra(EXTRA_WIFI_STATE, -1)
            if (state != WIFI_P2P_STATE_ENABLED) {
                // Wi-Fi Direct is disabled, can't proceed
                println("WiFi is disabled!")
                return
            }
            discoverPeers()
        }
        // Peer (nearby devices) list changed
        else if (action == WIFI_P2P_PEERS_CHANGED_ACTION) {
            wifiManager.requestPeers(channel, peerListListener)
            println("Requested peers")
        }
        // Connection status changed
        else if (action == WIFI_P2P_CONNECTION_CHANGED_ACTION) {
            wifiManager.requestConnectionInfo(channel, connectionInfoListener)
        }
    }

    private fun discoverPeers() {
        wifiManager.discoverPeers(channel, object : ActionListener {
            override fun onSuccess() {
                println("discoverPeers Success")
            }

            override fun onFailure(reason: Int) {
                println("discoverPeers $reason")
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

    private fun receiveText() {
        CoroutineScope(Dispatchers.IO).launch {
            val serverSocket = ServerSocket(8888)
            val client = serverSocket.accept()
            // Client has connected
            val istream = client.getInputStream()

            val sb = java.lang.StringBuilder()
            var c = istream.read()
            while ((c >= 0) && (c != 0x0a)) {
                if (c != 0x0d) {
                    sb.append(c.toChar())
                }
                c = istream.read()
            }

            istream.close()
            val text = sb.toString()

            withContext(Dispatchers.Main) {
                activity.receivingField.text = text
            }
        }
    }

    fun sendText(text: String) {
        if (socket == null) {
            showNeutralAlert("No connection!", "You are not connected to any device.", activity)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            socket!!.connect(InetSocketAddress(targetAddress, 8888), 5000)
            val ostream = socket!!.getOutputStream()
            ostream.write(text.toByteArray())
            ostream.write(0x0a)
            ostream.close()
            socket!!.close()
        }
    }
}