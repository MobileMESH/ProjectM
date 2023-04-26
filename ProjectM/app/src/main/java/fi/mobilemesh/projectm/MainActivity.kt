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
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import fi.mobilemesh.projectm.network.BroadcastManager
import fi.mobilemesh.projectm.database.MessageDatabase
import fi.mobilemesh.projectm.database.entities.ChatGroup
import fi.mobilemesh.projectm.network.MeshManager
import fi.mobilemesh.projectm.utils.SharedPreferencesManager
import fi.mobilemesh.projectm.utils.MakeNotification
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
    private lateinit var meshManager: MeshManager
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
        // Show the onboarding activity if it hasn't been completed
        if (!isOnboardingCompleted()) {
            showOnboardingActivity()
        }

        setContentView(R.layout.activity_main)

        // moved this to Onboarding
        //requestPermissions()

        //UI
        findUiElements()
        //mapButtons()
        listenNavigation()

        // Initializing handlers and such
        MessageDatabase.getInstance(applicationContext)
        broadcastManager = BroadcastManager.getInstance(applicationContext)
        meshManager = MeshManager.getInstance(applicationContext)
        SharedPreferencesManager.getInstance(applicationContext)
        addIntentFilters()
        handleNotificationClick()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(broadcastManager, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(broadcastManager)
    }

    private fun handleNotificationClick() {
        // Necessary for notification handling. Switches to correct fragment when chat
        // nofitication is clicked.
        val chatId = intent.getIntExtra("chat_id", -1)
        if (chatId != -1) {
            switchFragment(ContainerFragmentChat::class.java)
            navigationBar.selectedItemId = R.id.chat
        }
    }

    private fun isOnboardingCompleted(): Boolean {
        val prefs = getSharedPreferences("my_app", Context.MODE_PRIVATE)
        return prefs.getBoolean("is_onboarding_completed", false)
    }

    private fun showOnboardingActivity() {
        if (!isOnboardingCompleted()) {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }
    }

    private fun findUiElements() {
        navigationBar = findViewById(R.id.navigationBar)
        //networkDetails = findViewById(R.id.networkDetails)
    }

    private fun listenNavigation() {

        navigationBar.setOnItemSelectedListener{ item ->
            when(item.itemId) {
                R.id.settings -> {
                    switchFragment(DeviceSettings::class.java)
                    true
                }
                R.id.chat -> {
                    switchFragment(ContainerFragmentChat::class.java)
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
