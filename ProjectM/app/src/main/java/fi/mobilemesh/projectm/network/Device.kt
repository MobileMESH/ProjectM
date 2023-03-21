package fi.mobilemesh.projectm.network

import android.net.wifi.p2p.WifiP2pDevice

class Device (device: WifiP2pDevice) : WifiP2pDevice() {
    val macAdd = device.deviceAddress
    val name = device.deviceName
    val availableDevices = listOf<Device>()
}