package fi.mobilemesh.projectm.connectionManager

import android.content.Context
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.net.wifi.p2p.WifiP2pManager.ActionListener
import kotlin.math.log

class ConnectionManager(context: Context) {

    private val wifiP2pManager: WifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private val channel: Channel = wifiP2pManager.initialize(context, context.mainLooper, null)

    fun connectToDevice(deviceAddress: String, onSuccess: () -> Unit, onFailure: (reason: Int) -> Unit){
        val config = WifiP2pConfig().apply{
            this.deviceAddress = deviceAddress
        }

        wifiP2pManager.connect(channel, config, object : ActionListener {
            override fun onSuccess() {
                TODO("Not yet implemented")
            }

            override fun onFailure(reason: Int) {
                TODO("Not yet implemented")
            }
        })
    }

}