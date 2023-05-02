package fi.mobilemesh.projectm.network

import android.net.wifi.p2p.WifiP2pDevice
import android.location.Location
import java.util.UUID

class Device (device: WifiP2pDevice) : java.io.Serializable {

    private val macAddress = device.deviceAddress
    private val name = device.deviceName
    private var location: Location? = null
    private var notificationsEnabled = false
    private var sharesLocation = false
    private var availableDevices = mutableListOf<Device>()

    fun getName(): String {
        return name
    }

    fun getAddress(): String {
        return macAddress
    }

    fun getLocation(): Location? {
        return location
    }

    fun getHasNotification(): Boolean {
        return notificationsEnabled
    }

    fun getHasSharedLocation(): Boolean {
        return sharesLocation
    }

    fun setAvailableDevices(devices: Collection<Device>?) {
        availableDevices.clear()
        availableDevices.addAll(devices ?: listOf())
    }

    fun getAvailableDevices(): Collection<Device> {
        return availableDevices
    }

    // TODO: Replace equality check (name) with UUID
    override fun equals(other: Any?): Boolean {
        if (other !is Device) return super.equals(other)
        return this.getName() == other.getName()
    }

    // TODO: Generate hash from UUID
    override fun hashCode(): Int {
        return (name?.hashCode() ?: 0)
    }
}

