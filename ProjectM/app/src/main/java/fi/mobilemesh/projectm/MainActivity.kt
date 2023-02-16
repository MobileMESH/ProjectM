package fi.mobilemesh.projectm

import android.content.Context
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.Channel
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import network.BroadcastManager

class MainActivity : AppCompatActivity() {
    private lateinit var wifiManager: WifiP2pManager
    private lateinit var channel: Channel
    private lateinit var broadcastManager: BroadcastManager
    private val intentFilter = IntentFilter()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wifiManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = wifiManager.initialize(this, mainLooper, null)
        broadcastManager = BroadcastManager(wifiManager, channel)
        addIntentFilters()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(broadcastManager, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(broadcastManager)
    }

    private fun addIntentFilters() {
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }
}