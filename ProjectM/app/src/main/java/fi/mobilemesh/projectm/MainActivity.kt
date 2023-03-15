package fi.mobilemesh.projectm

import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.wifi.p2p.WifiP2pManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import fi.mobilemesh.projectm.network.BroadcastManager

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE = 223312
    }


    private val permissions = arrayOf(
        "android.permission.ACCESS_WIFI_STATE",
        "android.permission.CHANGE_WIFI_STATE",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.NEARBY_WIFI_DEVICES",
        "android.permission.CHANGE_NETWORK_STATE",
        "android.permission.ACCESS_NETWORK_STATE"
    )

    private lateinit var wifiManager: WifiP2pManager
    private lateinit var channel: Channel
    private lateinit var broadcastManager: BroadcastManager
    private val intentFilter = IntentFilter()

    // UI
    //
    // The deviceList will be found on network view but I'm not sure if we need statusField?
    // The message of having no connection could be shown in the receivingField instead!
    //lateinit var deviceList: LinearLayout
    //lateinit var statusField: TextView
    lateinit var receivingField: LinearLayout
    lateinit var sendingField: EditText
    lateinit var sendButton: FloatingActionButton
    lateinit var networkDetails: TextView
    lateinit var navigationBar: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chat_view)

        requestPermissions()

        //UI
        findUiElements()
        //mapButtons()

        // Wifi
        wifiManager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        channel = wifiManager.initialize(this, mainLooper, null)
        broadcastManager = BroadcastManager(wifiManager, channel, this)
        addIntentFilters()

    }
    private fun requestPermissions() {

        val permissionsToRequest = mutableListOf<String>()
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PERMISSION_GRANTED)
            {
                permissionsToRequest.add(permission)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray() , REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            var allGranted = true
            for (grantResult in grantResults) {
                if (grantResult != PERMISSION_GRANTED) {
                    allGranted = false
                    break
                }
            }
            if (allGranted) {
                return
            } else {
                //finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(broadcastManager, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(broadcastManager)
    }

    private fun findUiElements() {
        // deviceList = findViewById(R.id.deviceList)
        // statusField = findViewById(R.id.statusField)
        receivingField = findViewById(R.id.receivingField)
        sendingField = findViewById(R.id.sendingField)
        sendButton = findViewById(R.id.sendTextButton)
        navigationBar = findViewById(R.id.navigationBar)
        networkDetails = findViewById(R.id.networkDetails)
    }

    private fun mapButtons() {
        sendButton.setOnClickListener {
            val text = sendingField.text.toString().trim()
            broadcastManager.sendText(text)
            sendingField.text.clear()
        }
    }

    // Not sure if this is how it's done but something like this was shown in the
    // material design guide for the nav bar
    private fun listenNavigation() {
        navigationBar.setOnItemSelectedListener{ item ->
            when(item.itemId) {
                R.id.item_1 -> {
                    // Change screen to settings
                    true
                }
                R.id.item_2 -> {
                    // Change screen to chat
                    true
                }
                R.id.item_3 -> {
                    // Change screen to networks
                    true
                }
                else -> false
            }
        }
    }

    private fun addIntentFilters() {
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }
}
