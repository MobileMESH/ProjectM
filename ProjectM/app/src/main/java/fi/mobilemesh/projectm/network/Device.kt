package fi.mobilemesh.projectm.network

import android.content.Context
import android.net.wifi.p2p.WifiP2pDevice
import android.location.Location
import android.location.LocationManager

class Device (device: WifiP2pDevice) : java.io.Serializable{

    private val macAdd = device.deviceAddress
    private val name = device.deviceName
    private lateinit var location: Location
    private var has_notifications = false
    private var share_location = false
    private var availableDevices = mutableListOf<Device>()

    public fun getName(): String{
        return name
    }

    public fun getAddress(): String{
        return macAdd
    }

    public fun getLocation(): Location{
        return location
    }

    public fun getHasNotification(): Boolean{
        return has_notifications
    }

    public fun getHasSharedLocation(): Boolean {
        return share_location
    }

    public fun getAvailableDevices(): List<Device> {
        return availableDevices
    }
}

