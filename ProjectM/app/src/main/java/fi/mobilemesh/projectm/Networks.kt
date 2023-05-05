package fi.mobilemesh.projectm

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.core.view.isEmpty
import fi.mobilemesh.projectm.database.MessageDatabase
import fi.mobilemesh.projectm.database.MessageQueries
import fi.mobilemesh.projectm.database.entities.ChatGroup
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
    private lateinit var createNetworkButton: Button
    private lateinit var joinButton: Button
    private lateinit var guidanceText1: TextView
    private lateinit var guidanceText2: TextView
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
        guidanceText1 = view.findViewById(R.id.guidanceText1)
        guidanceText2 = view.findViewById(R.id.guidanceText2)

        //INSTANCE = WeakReference(this)

        createNetworkButton = view.findViewById(R.id.createNetworkButton)
        joinButton = view.findViewById(R.id.joinButton)
        selectList = view.findViewById(R.id.selectList)

        mapButtons()

        return view
    }

    /**
     * Called after onCreateView() has finished, so view is not null
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CoroutineScope(Dispatchers.Main).launch {
            refreshJoinedNetworks(dao.getChatGroups())
        }

        lifecycleScope.launch { observeJoinedNetworks() }
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

    private fun observeJoinedNetworks() {
        dao.getLiveChatGroups().observe(viewLifecycleOwner) { list ->
            refreshJoinedNetworks(list)
        }
    }

    private fun refreshJoinedNetworks(networks: Collection<ChatGroup>) {
        if (view == null) return

        selectList.removeAllViews()

        networks.forEach {
            val btn = Button(view?.context)
            btn.text = it.groupName
            btn.setOnClickListener { _ ->
                MeshManager.activeNetworkId = it.chatGroupId
                btn.backgroundTintList = ColorStateList.valueOf(Color.GRAY)
            }

            if (!networkList.isEmpty()) {
                guidanceText1.text = ""
            }

            selectList.addView(btn)
            if(!selectList.isEmpty()) {
                guidanceText2.text = "Press network to enter chat"
            }
        }
    }
}