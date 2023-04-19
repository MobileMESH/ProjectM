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
 * Use the [NotificationPermission.newInstance] factory method to
 * create an instance of this fragment.
 */
class NotificationPermission : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    lateinit var continueButton: Button

    private val permission = "android.permission.POST_NOTIFICATIONS"

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
        val view = inflater.inflate(R.layout.fragment_notification_permission, container, false)

        continueButton = view.findViewById(R.id.continueButton)
        continueButton.setOnClickListener {
            requestPermission()
            nextFragment()
        }

        return view
    }

    private fun nextFragment() {
        val fragment = GettingStarted()
        val transaction = parentFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainerView2, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun requestPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), permission)
            == PackageManager.PERMISSION_GRANTED
        ) {
            // proceed with sending notifications
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(permission),
                OnboardingActivity.NOTIFICATION_REQUEST_CODE
            )
        }
    }
}