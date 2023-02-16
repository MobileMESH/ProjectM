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

class BroadcastManager(wifiManager: WifiP2pManager, channel: Channel, activity: MainActivity): BroadcastReceiver() {
    private val wifiManager = wifiManager
    private val channel = channel
    private val activity = activity

    private val peerList = mutableListOf<WifiP2pDevice>()
    private val peerListListener = PeerListListener { peers ->
        val refreshedPeers = peers.deviceList
        if (refreshedPeers != peerList) {
            peerList.clear()
            peerList.addAll(refreshedPeers)
            refreshedPeers.forEach { createButton(it.deviceAddress) }
        }
    }

    // Temporary placement!!
    private fun createButton(address: String) {
        val btn = Button(activity)
        btn.text = address

        btn.setOnClickListener {
            connectToDevice(address)
        }

        activity.deviceList.addView(btn)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        if (action == WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION) {
            val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
            if (state != WIFI_P2P_STATE_ENABLED) {
                // Wi-Fi Direct is disabled, can't proceed
                println("WiFi is disabled!")
                return
            }
            discoverPeers()
            // Continue...
        }
        else if (action == WIFI_P2P_PEERS_CHANGED_ACTION) {
            wifiManager.requestPeers(channel, peerListListener)
            println("Requested peers")
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
                activity.receivingField.text = "Successfully connected to $address"
                println("Successfully connected")
            }

            override fun onFailure(reason: Int) {
                activity.receivingField.text = "Failed to connect! - code $reason"
                println("Failed to connect - $reason")
            }
        })
    }
}