package fi.mobilemesh.projectm

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.forEach
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import fi.mobilemesh.projectm.network.BroadcastManager
import fi.mobilemesh.projectm.network.Device
import fi.mobilemesh.projectm.network.MeshManager
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
//  the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [Networks.newInstance] factory method to
 * create an instance of this fragment.
 */
class CreateNetwork : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    // Wi-Fi Direct
    private lateinit var broadcastManager: BroadcastManager
    private lateinit var meshManager: MeshManager
    private var selectedDevice: Device? = null

    // UI frames
    private lateinit var chooseDeviceLayout: View
    private lateinit var nameNetworkLayout: View
    private lateinit var frameCreateNetwork: FrameLayout

    // UI for chooseDeviceLayout (fragment_create_network)
    private lateinit var availableView: TextView
    private lateinit var deviceList: LinearLayout
    private lateinit var createButton: Button
    private lateinit var cancelButton: Button

    // UI for nameNetworkLayout (fragment_name_network)
    private lateinit var backButton: Button
    private lateinit var networkName: TextInputEditText
    private lateinit var finishButton: Button

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
        val view = inflater.inflate(R.layout.fragment_networks_container, container, false)
        frameCreateNetwork = view.findViewById(R.id.NetworksContainerFragment)

        inflateChildLayouts(inflater)
        addLayoutsToFrameLayout()

        // set chooseDeviceLayout as default layout
        chooseDeviceLayout.visibility = View.VISIBLE
        nameNetworkLayout.visibility = View.GONE

        broadcastManager = BroadcastManager.getInstance(view.context)
        meshManager = MeshManager.getInstance(view.context)

        findUiElements()
        mapButtons()

        lifecycleScope.launch { observeNearbyDevices() }

        return view
    }

    /**
     * Inflate the child layouts inside the FrameLayout
     */
    private fun inflateChildLayouts(inflater: LayoutInflater) {
        chooseDeviceLayout = inflater.inflate(R.layout.fragment_create_network, frameCreateNetwork, false)
        nameNetworkLayout = inflater.inflate(R.layout.fragment_name_network, frameCreateNetwork, false)
    }

    /**
     * Add the child layouts to the FrameLayout
     */
    private fun addLayoutsToFrameLayout() {
        arrayOf(chooseDeviceLayout, nameNetworkLayout)
            .forEach { frameCreateNetwork.addView(it) }
    }

    /**
     * switch between the two layouts by setting one layout's visibility to GONE,
     * the other to VISIBLE
     */
    private fun switchLayout(fromLayout: View, toLayout: View) {
        fromLayout.visibility = View.GONE
        toLayout.visibility = View.VISIBLE
    }

    private fun findUiElements() {
        // available devices
        availableView = chooseDeviceLayout.findViewById(R.id.availableView)
        deviceList = chooseDeviceLayout.findViewById(R.id.deviceList)
        cancelButton = chooseDeviceLayout.findViewById(R.id.cancelButton)
        createButton = chooseDeviceLayout.findViewById(R.id.createButton)

        // network naming
        backButton = nameNetworkLayout.findViewById(R.id.backButton)
        finishButton = nameNetworkLayout.findViewById(R.id.finishButton)
        networkName = nameNetworkLayout.findViewById(R.id.networkName)
    }


    /**
     * Called after onCreateView() has finished, so view is not null
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        refreshDeviceCards()
    }

    private fun mapButtons() {
        cancelButton.setOnClickListener {

            // switch back to Networks fragment
            (parentFragment as ContainerFragmentNetworks).switchFragment(Networks::class.java)
        }

        // disable initially
        //createButton.isEnabled = false

        createButton.setOnClickListener {
            // TODO: enable when a device has been selected

            if (selectedDevice != null) {
                meshManager.addToNetwork(selectedDevice!!, MeshManager.activeNetworkId)
            }
            // go to nameNetworkLayout
            switchLayout(chooseDeviceLayout, nameNetworkLayout)
        }

        backButton.setOnClickListener {
            // close the keyboard and reset the input field
            val inputMethodManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(networkName.windowToken, 0)
            networkName.text = null
            // go back to chooseDeviceLayout
            switchLayout(nameNetworkLayout, chooseDeviceLayout)
        }

        // TODO: change the color of disabled buttons
        // disable initially
        finishButton.isEnabled = false

        // check text input field
        networkName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                finishButton.isEnabled = !s.isNullOrEmpty()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        finishButton.setOnClickListener {
            // switch back to Networks fragment
            (parentFragment as ContainerFragmentNetworks).switchFragment(Networks::class.java)
        }


    }

    private fun observeNearbyDevices() {
        broadcastManager.getLiveNearbyDevices().observe(viewLifecycleOwner) { list ->
            if (!list.any { it == selectedDevice }) selectedDevice = null
            refreshDeviceCards()
        }
    }

    /**
     * Reloads the device list onto view
     */
    private fun refreshDeviceCards() {
        if (view?.context != null) {
            deviceList.removeAllViews()
            broadcastManager.getNearbyDevices().forEach { createCardViewLayout(it) }
        }
    }

    /**
     * Creates a card for given device so it can be connected to Usually called from
     * BroadcastManager when a new nearby device is detected
     * @param device device for which to create the interactable card
     */
    // TODO: Styles for buttons
    private fun createCardViewLayout(device: Device) {
        val btn = Button(view?.context)
        btn.text = device.getName()
        btn.setOnClickListener {
            deviceList.forEach { it.setBackgroundColor(Color.WHITE) }
            btn.setBackgroundColor(Color.GRAY)
            selectedDevice = device
        }

        deviceList.addView(btn)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment networks.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            CreateNetwork().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}