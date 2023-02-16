package network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.net.wifi.p2p.WifiP2pManager.PeerListListener
import android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_ENABLED
import android.util.Log
import androidx.core.os.persistableBundleOf

class BroadcastManager(wifiManager: WifiP2pManager, channel: Channel): BroadcastReceiver() {
    private val wifiManager = wifiManager
    private val channel = channel

    private val peerListListener = PeerListListener {}

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
        else if (action == WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION) {
            wifiManager.requestPeers(channel, peerListListener)
            println("Bojoing")
        }
    }

    //TODO: Make this a public function so a user can refresh the list?
    private fun discoverPeers() {
        wifiManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                println("discoverPeers Success")
            }

            override fun onFailure(reason: Int) {
                println("discoverPeers $reason")
            }
        })
    }
}