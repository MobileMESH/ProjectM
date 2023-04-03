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
import fi.mobilemesh.projectm.utils.showConfirmationAlert

class ChatNetworkDetails : Fragment() {

    //TODO: implement feature to add and edit network desc
    //private val isDescriptionPresent = false

    lateinit var menuButton: Button
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

        menuButton.setOnClickListener {
            // TODO: create menu to edit network name and desc
        }

        leaveNetworkButton.setOnClickListener {
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
        menuButton = view.findViewById(R.id.menuButton)
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
            // set sharing location to true for some devices to test UI
            // TODO: delete setSharesLocation from Device class when this is updated
            if (i < 4) {
                device.setSharesLocation(true)
            }
            devices.add(device)
        }


        connectedDevicesList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ConnectedDevicesAdapter(devices)
        }
    }
}