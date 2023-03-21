package fi.mobilemesh.projectm.network

import android.net.wifi.p2p.WifiP2pDevice

class MeshManager(private val broadcastManager: BroadcastManager){
    private val connectedDevices: MutableMap<String, Device> = mutableMapOf()
    private val deviceGroups: MutableMap<String,MutableList<Device>> = mutableMapOf()
    private val devicesToJoin: MutableMap<String, Device> = mutableMapOf()


    fun handleDiscoveredDevice(device: WifiP2pDevice){
        // Check if the device is already connected or wants to join
        if (connectedDevices.containsKey(device.deviceAddress) || devicesToJoin.containsKey(device.deviceAddress)) {
            return

    }

    fun connectToDevice(deviceAddress: String) {
        val device = devicesToJoin[deviceAddress] ?: return

        // Use the broadcastManager to connect to the device
        broadcastManager.connectToDevice(deviceAddress)

        // Move the device from devicesToJoin to connectedDevices
        devicesToJoin.remove(deviceAddress)
        connectedDevices[deviceAddress] = device

        // Share the list of devices that want to join with the connected device
        shareDevicesToJoin(deviceAddress)
    }

    fun shareDevicesToJoin(targetDeviceAddress: String) {

    }

    fun createGroup(groupName: String) {
        if (!deviceGroups.containsKey(groupName)) {
            deviceGroups[groupName] = mutableListOf()
        }
    }

    fun joinGroup(groupName: String, deviceAddress: String) {
        val device = connectedDevices[deviceAddress] ?: return
        val group = deviceGroups[groupName] ?: mutableListOf()

        if (!group.contains(device)) {
            group.add(device)
            deviceGroups[groupName] = group
        }
    }

    fun leaveGroup(groupName: String, deviceAddress: String) {
        val group = deviceGroups[groupName] ?: return
        val device = connectedDevices[deviceAddress] ?: return

        if (group.contains(device)) {
            group.remove(device)
        }
    }
}
}