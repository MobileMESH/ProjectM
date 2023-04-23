package fi.mobilemesh.projectm

import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import fi.mobilemesh.projectm.utils.SharedPreferencesManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import com.google.android.material.materialswitch.MaterialSwitch
import fi.mobilemesh.projectm.utils.MakeNotification

//import fi.mobilemesh.projectm.OnBoardingActivity

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [Device_settings.newInstance] factory method to
 * create an instance of this fragment.
 */
class DeviceSettings : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var deviceName: EditText
    private lateinit var saveName: Button
    private lateinit var locationButton: MaterialSwitch
    private lateinit var notificationsButton: MaterialSwitch
    private lateinit var devicePermissions: CardView

    private lateinit var sharedPreferencesManager: SharedPreferencesManager

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        sharedPreferencesManager = SharedPreferencesManager.getInstance(requireContext())
    }

    // Listeners for all UI components, called after assigning UI elements.

    @RequiresApi(Build.VERSION_CODES.N)
    private fun mapButtons() {
        //Update user's device name
        saveName.setOnClickListener {
            val name = deviceName.text.toString()
            sharedPreferencesManager.saveUsername(name)
        }

        // Change SharedPreferences for on switch click
        locationButton.setOnCheckedChangeListener { _, _ ->
            val isEnabled = sharedPreferencesManager.getLocationEnabled()
            if (!isEnabled) {
                sharedPreferencesManager.setLocationEnabled(true)

            } else {
                sharedPreferencesManager.setLocationEnabled(false)
                locationButton.isChecked = false
            }
        }

        // Open settings for the user to alter notifications on switch click
        notificationsButton.setOnClickListener{
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
            startActivity(intent)
        }

        // Going to the device's permission settings
        devicePermissions.setOnClickListener {
            val packageName = requireContext().packageName
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null)
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // UI
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        deviceName = view.findViewById(R.id.settingsDeviceName)
        locationButton = view.findViewById(R.id.locationSwitch)
        notificationsButton = view.findViewById(R.id.notificationsSwitch)
        devicePermissions = view.findViewById(R.id.deviceSettings)
        saveName = view.findViewById(R.id.saveName)
        deviceName.setText(sharedPreferencesManager.getUsername())
        locationButton.isChecked = sharedPreferencesManager.getLocationEnabled()
        notificationsButton.isChecked = MakeNotification(requireContext()).getNotificationManager()
                              .areNotificationsEnabled()
        mapButtons()
        // Inflate the layout for this fragment
        return view
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onResume() {
        super.onResume()
        notificationsButton.isChecked = MakeNotification(requireContext()).getNotificationManager()
            .areNotificationsEnabled()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment chat.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            DeviceSettings().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}