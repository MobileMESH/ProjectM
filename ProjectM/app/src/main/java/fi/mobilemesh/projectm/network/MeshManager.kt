package fi.mobilemesh.projectm.network

import android.net.wifi.p2p.WifiP2pDevice

class MeshManager(private val broadcastManager: BroadcastManager){
    private val discoveredDevices: MutableMap<String, Device> = mutableMapOf()


    fun handleDiscoveredDevice(device: WifiP2pDevice){

    }
    

}