package fi.mobilemesh.projectm

import android.content.res.ColorStateList
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.material.switchmaterial.SwitchMaterial
import fi.mobilemesh.projectm.utils.SharedPreferencesManager
//import fi.mobilemesh.projectm.OnBoardingActivity

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [Settings.newInstance] factory method to
 * create an instance of this fragment.
 */
class Settings : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var deviceName: EditText
    private lateinit var saveNameButton: Button
    private lateinit var locationButton: SwitchMaterial
    private lateinit var notificationsButton: SwitchMaterial
    private lateinit var devicePermissions: CardView

    lateinit var sharedPreferencesManager: SharedPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // UI
        val view =  inflater.inflate(R.layout.fragment_settings, container, false)
        deviceName = view.findViewById(R.id.settingsDeviceName)
        locationButton = view.findViewById(R.id.locationSwitch)
        notificationsButton = view.findViewById(R.id.notificationsSwitch)
        devicePermissions = view.findViewById(R.id.deviceSettings)

        // Inflate the layout for this fragment
        return view
    }

    //Update user's device name
    private fun changeDeviceName () {
        saveNameButton.setOnClickListener{
            val name = deviceName.text.toString()
            sharedPreferencesManager.saveUsername(name)
        }
    }


    // Change SharedPreferences for location and notifications on switch click
    private fun shareLocation () {
        locationButton.setOnCheckedChangeListener { buttonView, isChecked ->
            val isEnabled = sharedPreferencesManager.getLocationEnabled()
            if (!isEnabled) {
                sharedPreferencesManager.setLocationEnabled(true)
                //buttonView.setStyle(SwitchChecked)

            } else {
                sharedPreferencesManager.setLocationEnabled(false)
                locationButton.isChecked = !isChecked
            }

        }
    }

    private fun shareNotifications () {
        notificationsButton.setOnClickListener{
            /* val status = //TODO: Connect to notifications status
            if (status == false) {
                // TODO: Notifs on
            } else {
                // TODO: Notifs off
            }*/
        }
    }

    private fun goToDeviceSettings () {
        devicePermissions.setOnClickListener{
            //TODO: Handle going to the device settings
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment Settings.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            Settings().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}