package fi.mobilemesh.projectm.network

import android.content.Context
import android.net.wifi.p2p.WifiP2pDevice

open class Device (device: WifiP2pDevice) :java.io.Serializable{
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


class MyPreferences(context: Context, device: Device){

    private val sharedPreferences = context.getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
    private lateinit var devices: Device

    init {
        devices = device
        setDeviceAddress(device.returnAddress())
        setDeviceName(device.returnName())
    }



    fun getDeviceName(): String? {
        return sharedPreferences.getString("deviceName", devices.returnName())
    }
    fun getDeviceAddress(): String? {
        return sharedPreferences.getString("deviceAddress", devices.returnAddress())
    }

    private fun setDeviceName(value: String) {
        sharedPreferences.edit().putString("deviceName", devices.returnName()).apply()
    }

    private fun setDeviceAddress(value: String) {
        sharedPreferences.edit().putString("deviceAddress", devices.returnAddress()).apply()
    }
    private fun setDemoDeviceAddress(value: String) {
        sharedPreferences.edit().putString("demoDeviceAddress", devices.returnDemoAddress()).apply()
    }
    fun getDemoDeviceAddress(): String? {
        return sharedPreferences.getString("demoDeviceAddress", devices.returnDemoAddress())
    }
}

