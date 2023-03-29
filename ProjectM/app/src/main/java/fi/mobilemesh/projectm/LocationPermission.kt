package fi.mobilemesh.projectm

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [LocationPermission.newInstance] factory method to
 * create an instance of this fragment.
 */
class LocationPermission : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    lateinit var continueButton: Button

    private var permissionsToRequest = mutableListOf<String>()

    private val permissions = arrayOf(
        "android.permission.ACCESS_WIFI_STATE",
        "android.permission.CHANGE_WIFI_STATE",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.NEARBY_WIFI_DEVICES",
        "android.permission.CHANGE_NETWORK_STATE",
        "android.permission.ACCESS_NETWORK_STATE"
    )

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
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_location_permission, container, false)

        continueButton = view.findViewById(R.id.continueButton)
        continueButton.setOnClickListener {
            requestPermissions()
            nextFragment()
        }

        return view
    }

    private fun nextFragment() {
        val fragment = LocationSharing()
        val transaction = parentFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainerView2, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun requestPermissions() {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(requireActivity(), permission) != PackageManager.PERMISSION_GRANTED)
            {
                permissionsToRequest.add(permission)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                permissionsToRequest.toTypedArray(),
                OnboardingActivity.REQUEST_CODE
            )
        } else {
            //nextFragment()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == OnboardingActivity.REQUEST_CODE) {
            var allGranted = true
            for (grantResult in grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
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

}