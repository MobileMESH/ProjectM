package fi.mobilemesh.projectm

import android.net.wifi.p2p.WifiP2pDevice
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fi.mobilemesh.projectm.network.Device
import fi.mobilemesh.projectm.network.MeshManager
import fi.mobilemesh.projectm.utils.showConfirmationAlert

class ChatNetworkDetails : Fragment() {

    //TODO: implement feature to add and edit network desc
    //private val isDescriptionPresent = false

    lateinit var networkDetails: TextView
    lateinit var networkDescription: TextView
    lateinit var connectedDevicesHeader: TextView
    lateinit var connectedDevicesList: RecyclerView
    lateinit var leaveNetworkButton: Button
    lateinit var openChatButton: Button

    /**
     * All buttons within this fragment can have their click listeners set here. Should be
     * called only after assigning all UI elements
     */
    private fun mapButtons() {
        openChatButton.setOnClickListener {
            // switch to Chat
            (parentFragment as ContainerFragmentChat).switchFragment(Chat::class.java)
        }
        leaveNetworkButton.setOnClickListener {
            // TODO: This only sets the current network as "unselected", needs to disconnect
            MeshManager.activeNetworkId = null

            showConfirmationAlert(
                "Leave Network",
                "Are you sure you want to leave the network?",
                "Yes",
                "No",
                requireContext(),
                {
                    // TODO: Disconnect device from network
                    (parentFragment as ContainerFragmentChat).switchFragment(ChatDisconnected::class.java)
                },
                {
                    // Do nothing
                }
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_network_details, container, false)

        findUiElements(view)
        mapButtons()
        setupConnectedDevicesList()

        return view
    }

    private fun findUiElements(view: View) {
        networkDetails = view.findViewById(R.id.networkDetails)
        connectedDevicesList = view.findViewById(R.id.connectedDevicesList)
        connectedDevicesHeader = view.findViewById(R.id.connectedDevicesHeader)
        networkDescription = view.findViewById(R.id.networkDescription)
        openChatButton = view.findViewById(R.id.openChatButton)
        leaveNetworkButton = view.findViewById(R.id.leaveNetworkButton)
    }

    /**
     * Sets up the RecyclerView for displaying a list of connected devices.
     * For now only populates the list with test devices for demo purposes.
     */
    private fun setupConnectedDevicesList() {

        // TODO: Replace this with actual connected devices

        val devices = mutableListOf<Device>()

        for (i in 0..6) {
            val device = Device(WifiP2pDevice().apply {
                // Own device first
                deviceName = if (i == 0) {
                    "Own test device"
                } else {
                    "Test device $i"
                }
            })
            devices.add(device)
        }


        connectedDevicesList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ConnectedDevicesAdapter(devices)
        }
    }
}