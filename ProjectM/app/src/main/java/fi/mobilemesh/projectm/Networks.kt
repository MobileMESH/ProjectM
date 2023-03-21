package fi.mobilemesh.projectm

import android.graphics.Color
import android.net.wifi.p2p.WifiP2pDevice
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import fi.mobilemesh.projectm.network.BroadcastManager

// TODO: Rename parameter arguments, choose names that match
//  the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [Networks.newInstance] factory method to
 * create an instance of this fragment.
 */
class Networks : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    // Wi-Fi Direct
    private lateinit var broadcastManager: BroadcastManager

    // UI
    private lateinit var availableView: TextView
    private lateinit var nodeList: LinearLayout

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
        val view = inflater.inflate(R.layout.fragment_networks, container, false)
        broadcastManager = BroadcastManager.getInstance(view.context)

        availableView = view.findViewById(R.id.availableView)
        nodeList = view.findViewById(R.id.nodeList)

        INSTANCE = this

        return view
    }

    /**
     * Called after onCreateView() has finished, so view is not null
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (deviceList.isNotEmpty()) refreshDeviceCards()
    }

    /**
     * Reloads the device list onto view
     */
    private fun refreshDeviceCards() {
        if (view?.context != null) {
            nodeList.removeAllViews()
            deviceList.forEach { createCardViewLayout(it) }
        }
    }

    /**
     * Creates a card for given device so it can be connected to Usually called from
     * BroadcastManager when a new nearby device is detected
     * @param device device for which to create the interactable card
     */
    // TODO: Styles for buttons
    private fun createCardViewLayout(device: WifiP2pDevice) {
        val btn = Button(view?.context)
        btn.text = device.deviceName
        btn.setOnClickListener {
            broadcastManager.connectToDevice(device.deviceAddress)
        }

        nodeList.addView(btn)
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
            Networks().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }

        @Volatile
        private var INSTANCE: Networks? = null
        private val deviceList: MutableCollection<WifiP2pDevice> = mutableListOf()

        fun refreshDeviceList(devices: Collection<WifiP2pDevice>) {
            deviceList.clear()
            deviceList.addAll(devices)
            INSTANCE?.refreshDeviceCards()
        }
    }
}