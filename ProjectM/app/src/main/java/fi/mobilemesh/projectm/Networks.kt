package fi.mobilemesh.projectm

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import fi.mobilemesh.projectm.database.MessageDatabase
import fi.mobilemesh.projectm.database.MessageQueries
import fi.mobilemesh.projectm.network.BroadcastManager
import fi.mobilemesh.projectm.network.Device
import fi.mobilemesh.projectm.network.MeshManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
class Networks : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    // Wi-Fi Direct
    private lateinit var broadcastManager: BroadcastManager
    private lateinit var meshManager: MeshManager
    private var selectedDevice: Device? = null

    // Database
    private lateinit var dao: MessageQueries

    // UI
    private lateinit var availableView: TextView
    private lateinit var networkList: LinearLayout
    private lateinit var nodeList: LinearLayout
    private lateinit var createNetworkButton: Button
    private lateinit var selectNetworkButton: Button
    private lateinit var joinButton: Button
    private lateinit var selectButton: Button
    private lateinit var selectList: LinearLayout


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
        meshManager = MeshManager.getInstance(view.context)
        dao = MessageDatabase.getInstance(view.context).dao

        availableView = view.findViewById(R.id.availableView)
        networkList = view.findViewById(R.id.networkList)

        //INSTANCE = WeakReference(this)

        createNetworkButton = view.findViewById(R.id.createNetworkButton)
        joinButton = view.findViewById(R.id.joinButton)

        selectButton = view.findViewById(R.id.selectButton)
        selectList = view.findViewById(R.id.selectList)

        mapButtons()

        // lifecycleScope.launch { observeNearbyDevices() }

        return view
    }

    /**
     * Called after onCreateView() has finished, so view is not null
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CoroutineScope(Dispatchers.Main).launch {
            dao.getChatGroups().forEach {
                val btn = Button(view.context)
                btn.text = it.groupName
                btn.setOnClickListener { _ ->
                    MeshManager.activeNetworkId = it.chatGroupId
                }
                networkList.addView(btn)
            }
        }
        //refreshDeviceCards()

        //if (deviceList.isNotEmpty()) {
       //     refreshDeviceCards()
        //}
        //else {
       //     val txt = TextView(view?.context)
        //    txt.gravity = Gravity.CENTER_HORIZONTAL
         //   txt.text = "No devices available currently. You can try creating a new network."
        //    txt.setPadding(10,0,10,0)
          //  txt.setTextColor(Color.parseColor("#aec2bd"))
           // nodeList.addView(txt)
      //  }
        //refreshDeviceCards()
    }

    private fun mapButtons() {
        createNetworkButton.setOnClickListener {
            // switch to Create
            (parentFragment as ContainerFragmentNetworks).switchFragment(CreateNetwork::class.java)
        }
        // TODO: add join button
        /*addButton.setOnClickListener {
            if (selectedDevice != null) {
                meshManager.addToNetwork(selectedDevice!!, MeshManager.activeNetworkId)
            }*/
    }

    /*
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
            nodeList.removeAllViews()
            broadcastManager.getNearbyDevices().forEach { createCardViewLayout(it) }
        }
    }

    /**
     * Creates a card for given device so it can be connected to Usually called from
     * BroadcastManager when a new nearby device is detected
     * @param device device for which to create the interactable card
     */
    private fun createCardViewLayout(device: Device) {
        val btn = Button(view?.context)

        // Styles for buttons
        btn.text = device.getName()
        btn.setTextColor(Color.WHITE)
        btn.setBackgroundColor(Color.DKGRAY)

        // This drawable doesn't work either, but there could be a way
        // btn.setBackgroundResource(R.drawable.network_card)

        btn.setOnClickListener {
            nodeList.forEach { it.setBackgroundColor(Color.WHITE) }
            btn.setBackgroundColor(Color.GRAY)
            selectedDevice = device
        }
        nodeList.addView(btn)
    }*/

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
    }

}