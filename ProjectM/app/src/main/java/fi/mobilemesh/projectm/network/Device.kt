package fi.mobilemesh.projectm.network

import android.content.Context
import android.net.wifi.p2p.WifiP2pDevice
import android.util.Log
import android.widget.Toast
import java.util.Objects

class Device (device: WifiP2pDevice) : WifiP2pDevice() {
    val macAdd = device.deviceAddress
    val name = device.deviceName
    val availableDevices = listOf<Device>()




}

class MyPreferences(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("myPrefs", Context.MODE_PRIVATE)

    fun getMyValue(): String? {
        return sharedPreferences.getString("SavedData", "Sagor")
    }

    fun setMyValue(value: String) {
        sharedPreferences.edit().putString("SavedData", value).apply()
    }
}
