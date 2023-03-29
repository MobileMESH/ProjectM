package fi.mobilemesh.projectm

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.wifi.p2p.WifiP2pManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.IntentFilter
import android.widget.Button
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import fi.mobilemesh.projectm.network.BroadcastManager
import fi.mobilemesh.projectm.database.MessageDatabase
import fi.mobilemesh.projectm.database.entities.ChatGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // IF A NEW PROJECT IS CREATED WITH THE BOTTOM NAVIGATION VIEW ALREADY IN IT, THIS IS WHAT
    // THE MAINACTIVITY FILE HAS. I BET THIS COULD BE USED AS IT IS AFTER EVERYTHING CURRENTLY
    // IN THE FILE IS TRANSFERRED TO THEIR OWN FILES!
    //
    // -------------------------------
    //
    //
    //import android.os.Bundle
    //import com.google.android.material.bottomnavigation.BottomNavigationView
    //import androidx.appcompat.app.AppCompatActivity
    //import androidx.navigation.findNavController
    //import androidx.navigation.ui.AppBarConfiguration
    //import androidx.navigation.ui.setupActionBarWithNavController
    //import androidx.navigation.ui.setupWithNavController
    //import com.example.testproject.databinding.ActivityMainBinding
    //
    //class MainActivity : AppCompatActivity() {
    //
    //    private lateinit var binding: ActivityMainBinding
    //
    //    override fun onCreate(savedInstanceState: Bundle?) {
    //        super.onCreate(savedInstanceState)
    //
    //        binding = ActivityMainBinding.inflate(layoutInflater)
    //        setContentView(binding.root)
    //
    //        val navView: BottomNavigationView = binding.navView
    //
    //        val navController = findNavController(R.id.nav_host_fragment_activity_main)
    //        // Passing each menu ID as a set of Ids because each
    //        // menu should be considered as top level destinations.
    //        val appBarConfiguration = AppBarConfiguration(
    //            setOf(
    //                R.id.settings R.id.chat, R.id.networks
    //            )
    //        )
    //        setupActionBarWithNavController(navController, appBarConfiguration)
    //        navView.setupWithNavController(navController)
    //    }
    //}
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

    private lateinit var broadcastManager: BroadcastManager
    private val intentFilter = IntentFilter()

    // UI
    //
    // The deviceList will be found on network view but I'm not sure if we need statusField?
    // The message of having no connection could be shown in the receivingField instead!
    //lateinit var deviceList: LinearLayout
    //lateinit var statusField: TextView
    //lateinit var networkDetails: TextView
    lateinit var navigationBar: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the shared preferences
        val prefs = getSharedPreferences("my_app", Context.MODE_PRIVATE)

        // Check if this is the first time the app is being opened
        val isFirstTime = prefs.getBoolean("is_first_time", true)

        if (isFirstTime) {
            // Show the onboarding activity
            startActivity(Intent(this, OnboardingActivity::class.java))

            // Save the flag indicating that the app has been opened at least once
            prefs.edit().putBoolean("is_first_time", false).apply()
        }
        
        setContentView(R.layout.activity_main)

        // TODO: move this to Onboarding
        //requestPermissions()

        //UI
        findUiElements()
        //mapButtons()
        listenNavigation()
        // Wifi
        broadcastManager = BroadcastManager.getInstance(this)
        addIntentFilters()

        // Message database (Data Access Object)
        val dao = MessageDatabase.getInstance(this).dao
        CoroutineScope(Dispatchers.Main).launch {
            // TODO: Placeholder chat group for tests. Should be replaced when chat groups are
            //  implemented
            dao.insertChatGroup(ChatGroup(0))
        }

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
        navigationBar = findViewById(R.id.navigationBar)
        //networkDetails = findViewById(R.id.networkDetails)
    }

    // Not sure if this is how it's done but something like this was shown in the
    // material design guide for the nav bar
    private fun listenNavigation() {

        navigationBar.setOnItemSelectedListener{ item ->
            when(item.itemId) {
                R.id.settings -> {
                    switchFragment(Settings::class.java)
                    true
                }
                R.id.chat -> {
                    switchFragment(Chat::class.java)
                    true
                }
                R.id.networks-> {
                    switchFragment(Networks::class.java)
                    true
                }
                else -> false
            }
        }
    }
    private fun switchFragment(target: Class<*>) {
        val f: Fragment = target.newInstance() as Fragment
        val fm = supportFragmentManager
        val transaction = fm.beginTransaction()
        transaction.replace(R.id.fragmentContainerView, f)
        transaction.commit()
    }


    private fun addIntentFilters() {
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }
}
