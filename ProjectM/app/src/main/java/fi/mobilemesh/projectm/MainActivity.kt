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
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
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

    // Networks
    lateinit var deviceCard: CardView
    lateinit var deviceList: TableRow
    //lateinit var statusField: TextView

    // Chat
    lateinit var receivingField: TextView
    lateinit var sendingField: EditText
    lateinit var sendButton: FloatingActionButton
    lateinit var networkDetails: TextView
    lateinit var navigationBar: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.networks)

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
            if (ContextCompat.checkSelfPermission(this, permission) != PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                REQUEST_CODE
            )
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
        //statusField = findViewById(R.id.statusField)
        deviceList = findViewById(R.id.deviceList)

        sendingField = findViewById(R.id.sendingField)
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

        // Builds network join alert when you click the card
        val builder = AlertDialog.Builder(this)
        deviceCard.setOnClickListener {
            builder.setMessage("Joining network")
            //builder.setNegativeButton("Cancel") { dialogInterface, it ->
                // stop connection
            //}
                .show()
        }

        //Top bar menu listeners
        //chatMenu.setOnClickListener {
        //    val goToChat = Intent(this, Chat::class.java)
        //    startActivity(goToChat)
        //}
    }

    // Not sure if this is how it's done but something like this was shown in the
    // material design guide for the nav bar
    private fun listenNavigation() {
        navigationBar.setOnItemSelectedListener{ item ->
            when(item.itemId) {
                R.id.settingsMenu -> {
                    // Change screen to settings
                    true
                }
                R.id.chatMenu -> {
                    // Change screen to chat
                    true
                }
                R.id.networksMenu -> {
                    // Change screen to networks
                    setContentView(R.layout.networks)
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
