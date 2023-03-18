package fi.mobilemesh.projectm

import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.wifi.p2p.WifiP2pManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import fi.mobilemesh.projectm.network.BroadcastManager
import fi.mobilemesh.projectm.network.Device
import fi.mobilemesh.projectm.network.MyPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Text

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
    lateinit var deviceList: LinearLayout
    lateinit var statusField: TextView
    lateinit var receivingField: TextView
    lateinit var sendingField: EditText
    lateinit var sendButton: Button

    private lateinit var myPrefs: MyPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermissions()

        //UI
        findUiElements()
        mapButtons()

        // Wifi
        wifiManager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        channel = wifiManager.initialize(this, mainLooper, null)
        broadcastManager = BroadcastManager(wifiManager, channel, this)
        addIntentFilters()

        myPrefs = MyPreferences(this)

        saveMyValue("Jahid Sagor")


        getMyValue()?.let { Log.d(" Data: ", it) }

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
        deviceList = findViewById(R.id.deviceList)
        statusField = findViewById(R.id.statusField)
        sendingField = findViewById(R.id.sendingField)
        receivingField = findViewById(R.id.receivingField)
        sendButton = findViewById(R.id.sendTextButton)
    }

    private fun mapButtons() {
        sendButton.setOnClickListener {
            val text = sendingField.text.toString()
            broadcastManager.sendText(text)
            sendingField.text.clear()
        }
    }

    private fun addIntentFilters() {
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }


    fun saveMyValue(value: String) {
        myPrefs.setMyValue(value)
    }

    fun getMyValue(): String? {
        return myPrefs.getMyValue()
    }

}