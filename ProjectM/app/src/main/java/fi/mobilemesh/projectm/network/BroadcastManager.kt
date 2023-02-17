package fi.mobilemesh.projectm.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Address
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.*
import android.widget.Button
import fi.mobilemesh.projectm.MainActivity

class BroadcastManager(
    private val wifiManager: WifiP2pManager,
    private val channel: Channel,
    private val activity: MainActivity
): BroadcastReceiver() {
    private var connected = false

    private val peerList = mutableListOf<WifiP2pDevice>()
    private val peerListListener = PeerListListener { peers ->
        val refreshedPeers = peers.deviceList
        if (refreshedPeers != peerList) {
            peerList.clear()
            peerList.addAll(refreshedPeers)
            refreshedPeers.forEach { createButton(it) }
        }
    }

    private val connectionInfoListener = ConnectionInfoListener { conn ->
        connected = conn.groupFormed
        if (connected) {
            activity.statusField.text = "Connection successful"
        } else {
            activity.statusField.text = "Connection failed: device declined connection?"
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
}