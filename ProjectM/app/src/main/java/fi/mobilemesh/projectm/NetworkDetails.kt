package fi.mobilemesh.projectm

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import android.content.Intent
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import fi.mobilemesh.projectm.database.MessageDatabase
import fi.mobilemesh.projectm.network.BroadcastManager
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [NetworkDetails.newInstance] factory method to
 * create an instance of this fragment.
 */
class NetworkDetails : Fragment() {
    // UI
    lateinit var networkDetails: TextView
    lateinit var networkDescription: TextView
    lateinit var connectedDevicesHeader: TextView
    lateinit var connectedDevicesList: RecyclerView
    lateinit var leaveNetworkButton: Button

    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        println("Network Details")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_network_details, container, false)

        networkDetails = view.findViewById(R.id.networkDetails)
        connectedDevicesList = view.findViewById(R.id.connectedDevicesList)
        connectedDevicesHeader = view.findViewById(R.id.connectedDevicesHeader)
        networkDescription = view.findViewById(R.id.networkDescription)
        leaveNetworkButton = view.findViewById(R.id.leaveNetworkButton)

        // Setting up back to chat button
        val button = view.findViewById<Button>(R.id.openChatButton)
        button.setOnClickListener {
            val intent = Intent(requireContext(), Chat::class.java)
            startActivity(intent)
        }

        // Alert to disconnect from network
        confirmLeaveNetwork()

        // This needs to be updated when Device object is ready.
        setupConnectedDevicesList()

        return view
    }

    private fun confirmLeaveNetwork() {
        leaveNetworkButton.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            builder.setMessage("Are you sure you want to leave the network?")
                .setCancelable(false)
                .setPositiveButton("Yes") { dialog, id ->
                    // TODO: Disconnect device from network
                }
                .setNegativeButton("No") { dialog, id ->
                    // Dismiss the dialog
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show()
        }
    }

    private fun setupConnectedDevicesList() {
        // like this for now just so I can see what the device list the looks like while running

        // Set up the RecyclerView for showing connected devices
        val devices = mutableListOf<DeviceList>()

        // Note: first device should be the one the app is currently running on so that
        // they appear on top of the device list
        devices.add(DeviceList("Own device", "Own address" ))

        for (i in 0..20) {
            devices.add(DeviceList("Test device", "Test address"))
        }

        connectedDevicesList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = DevicesAdapter(devices)
        }
    }

}
