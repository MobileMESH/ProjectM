package fi.mobilemesh.projectm

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.app.AlertDialog

class NetworkDetailsActivity : AppCompatActivity() {

    // UI
    lateinit var navigationBar: BottomNavigationView
    lateinit var networkDetails: TextView
    lateinit var networkDescription: TextView
    lateinit var connectedDevicesHeader: TextView
    lateinit var connectedDevicesList: RecyclerView
    lateinit var leaveNetworkButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.network_details)

        // UI
        findUiElements()

        // Back to chat
        setupOpenChatBtn()

        // Alert to disconnect from network
        confirmLeaveNetwork()

        // This needs to be updated when Device object is ready.
        setupConnectedDevicesList()
    }

    private fun setupOpenChatBtn() {
        // Set up the "Back to Chat" button to return to MainActivity (as it is the chat for now)
        val button = findViewById<Button>(R.id.openChatButton)
        button.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun confirmLeaveNetwork() {
        leaveNetworkButton.setOnClickListener {
            val builder = AlertDialog.Builder(this@NetworkDetailsActivity)
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
            layoutManager = LinearLayoutManager(this@NetworkDetailsActivity)
            adapter = DevicesAdapter(devices)
        }
    }

    private fun findUiElements() {
        navigationBar = findViewById(R.id.navigationBar)
        networkDetails = findViewById(R.id.networkDetails)
        connectedDevicesList = findViewById(R.id.connectedDevicesList)
        connectedDevicesHeader = findViewById(R.id.connectedDevicesHeader)
        networkDescription = findViewById(R.id.networkDescription)
        leaveNetworkButton = findViewById(R.id.leaveNetworkButton)
    }
}