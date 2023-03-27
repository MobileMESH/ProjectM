package fi.mobilemesh.projectm.network

import android.content.Context
import android.net.wifi.p2p.WifiP2pDevice

open class Device (device: WifiP2pDevice) : WifiP2pDevice() {
    private val macAdd = device.deviceAddress
    private val demoAddess = "AB:CD"
    private val name = device.deviceName
    val availableDevices = listOf<Device>()

    fun returnName(): String{
        return name
    }
    fun returnAddress(): String{
        return macAdd
    }
    fun returnDemoAddress(): String{
        return demoAddess
    }
}


class MyPreferences(context: Context, device: WifiP2pDevice): Device(device) {

    private val sharedPreferences = context.getSharedPreferences("myPrefs", Context.MODE_PRIVATE)

    init {
        setDeviceAddress(returnAddress())
        setDeviceName(returnName())
    }



    fun getDeviceName(): String? {
        return sharedPreferences.getString("deviceName", returnName())
    }
    fun getDeviceAddress(): String? {
        return sharedPreferences.getString("deviceAddress", returnAddress())
    }

    private fun setDeviceName(value: String) {
        sharedPreferences.edit().putString("deviceName", getDeviceName()).apply()
    }

    private fun setDeviceAddress(value: String) {
        sharedPreferences.edit().putString("deviceAddress", getDeviceName()).apply()
    }
    private fun setDemoDeviceAddress(value: String) {
        sharedPreferences.edit().putString("demoDeviceAddress", returnDemoAddress()).apply()
    }
    fun getDemoDeviceAddress(): String? {
        return sharedPreferences.getString("demoDeviceAddress", returnDemoAddress())
    }
}

