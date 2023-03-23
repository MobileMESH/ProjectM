package fi.mobilemesh.projectm

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.*
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import fi.mobilemesh.projectm.database.MessageDatabase
import fi.mobilemesh.projectm.database.MessageQueries
import fi.mobilemesh.projectm.database.entities.Message
import fi.mobilemesh.projectm.network.BroadcastManager
import fi.mobilemesh.projectm.utils.showNeutralAlert
import fi.mobilemesh.projectm.utils.showConfirmationAlert
import kotlinx.coroutines.*
import java.util.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [Chat.newInstance] factory method to
 * create an instance of this fragment.
 */
class Chat : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private var currentState: ChatUIState = ChatUIState.Chat
    private lateinit var dao: MessageQueries
    private lateinit var broadcastManager: BroadcastManager

    private lateinit var chatLayout: View
    private lateinit var detailsLayout: View
    private lateinit var disconnectedLayout: View
    private lateinit var fragmentChat: FrameLayout

    // chat
    private lateinit var sendButton: FloatingActionButton
    private lateinit var sendingField: EditText
    private lateinit var receivingField: LinearLayout
    private lateinit var openDetailsButton: Button

    // details
    private lateinit var networkDetails: TextView
    private lateinit var networkDescription: TextView
    private lateinit var connectedDevicesHeader: TextView
    private lateinit var connectedDevicesList: RecyclerView
    private lateinit var leaveNetworkButton: Button
    private lateinit var openChatButton: Button

    // disconnected
    private lateinit var disconnectedMsg: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        println("Chat")
    }

    /**
     * Updates the visibility of different UI elements between chat, network details,
     * and disconnected chat based on the current state of the chat section.
     */
    private fun updateUI() {
        when (currentState) {
            is ChatUIState.Chat -> {
                chatLayout.visibility = View.VISIBLE
                detailsLayout.visibility = View.GONE
                disconnectedLayout.visibility = View.GONE
            }
            is ChatUIState.Details -> {
                chatLayout.visibility = View.GONE
                detailsLayout.visibility = View.VISIBLE
                disconnectedLayout.visibility = View.GONE
            }
            is ChatUIState.Disconnected -> {
                chatLayout.visibility = View.GONE
                detailsLayout.visibility = View.GONE
                disconnectedLayout.visibility = View.VISIBLE
            }
        }
    }

    /**
     * All buttons within this fragment can have their click listeners set here. Should be
     * called only after assigning all UI elements
     */
    private fun mapButtons() {
        sendButton.setOnClickListener {
            val text = sendingField.text.toString()
            sendingField.text.clear()
            CoroutineScope(Dispatchers.IO).launch { sendMessage(text) }
        }

        openDetailsButton.setOnClickListener {
            currentState = ChatUIState.Details
            updateUI()
        }

        openChatButton.setOnClickListener {
            currentState = ChatUIState.Chat
            updateUI()
        }

        leaveNetworkButton.setOnClickListener {
            showConfirmationAlert(
                "Leave Network",
                "Are you sure you want to leave the network?",
                "Yes",
                "No",
                requireContext(),
                {
                    // TODO: Disconnect device from network
                    setLayout()
                },
                {
                    // Do nothing
                }
            )
        }

    }

    // This function is used to do all magic
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_chat, container, false)
        fragmentChat = view.findViewById(R.id.fragment_chat)

        inflateChildLayouts(inflater)
        addLayoutsToFrameLayout()

        findUiElements()

        dao = MessageDatabase.getInstance(view.context).dao
        broadcastManager = BroadcastManager.getInstance(view.context)


        //commented out for testing UI without connection
        //setLayout()

        //these two will be removed
        currentState = ChatUIState.Chat
        updateUI()

        mapButtons()

        setupConnectedDevicesList()

        lifecycleScope.launch { observeLiveMessages() }

        return view
    }

    /**
     * Inflate the child layouts inside the FrameLayout
     */
    private fun inflateChildLayouts(inflater: LayoutInflater) {
        chatLayout = inflater.inflate(R.layout.chat, fragmentChat, false)
        detailsLayout = inflater.inflate(R.layout.network_details, fragmentChat, false)
        disconnectedLayout = inflater.inflate(R.layout.chat_disconnected, fragmentChat, false)
    }

    /**
     * Add the child layouts to the FrameLayout
     */
    private fun addLayoutsToFrameLayout() {
        fragmentChat.addView(chatLayout)
        fragmentChat.addView(detailsLayout)
        fragmentChat.addView(disconnectedLayout)
    }

    private fun findUiElements() {
        // chat
        receivingField = chatLayout.findViewById(R.id.receivingField)
        sendingField = chatLayout.findViewById(R.id.sendingField)
        sendButton = chatLayout.findViewById(R.id.sendTextButton)
        openDetailsButton = chatLayout.findViewById(R.id.openDetailsButton)

        // details
        networkDetails = detailsLayout.findViewById(R.id.networkDetails)
        connectedDevicesList = detailsLayout.findViewById(R.id.connectedDevicesList)
        connectedDevicesHeader = detailsLayout.findViewById(R.id.connectedDevicesHeader)
        networkDescription = detailsLayout.findViewById(R.id.networkDescription)
        openChatButton = detailsLayout.findViewById(R.id.openChatButton)
        leaveNetworkButton = detailsLayout.findViewById(R.id.leaveNetworkButton)

        // disconnected
        disconnectedMsg = disconnectedLayout.findViewById(R.id.disconnectedMsg)
    }

    /**
     * Checks connection.
     * Sets chatLayout as the visible layout by default if connection is found.
     */
    private fun setLayout() {
        if (!broadcastManager.isConnected()) {
            currentState = ChatUIState.Disconnected
            updateUI()
        }
        else {
            currentState = ChatUIState.Chat
            updateUI()
        }
    }

    /**
     * Sets up the RecyclerView for displaying a list of connected devices.
     * For now only populates the list with test devices for demo purposes.
     */
    // TODO: Should be updated when Device object is ready
    private fun setupConnectedDevicesList() {

        val devices = mutableListOf<DeviceList>()

        // Note: first device should be the one the app is currently running on so that
        // they appear on top of the device list
        devices.add(DeviceList("Own device", "Own address" ))

        for (i in 0..20) {
            devices.add(DeviceList("Test device", "Test address"))
        }

        connectedDevicesList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = DevicesAdapter(devices)
        }
    }

    /**
     * Updates the chat every time a new message is added to the database (a message is receive).
     * WIP
     */
    // TODO: Don't reload all messages...
    private fun observeLiveMessages() {
        dao.getLiveChatGroupMessages(0).observe(viewLifecycleOwner) {
            //createMessage(it.last(), Color.parseColor("#262626"), Color.WHITE)
            receivingField.removeAllViews()
            loadAllMessages()
        }
    }

    /**
     * Used to initiate sending a message and checking it is valid.
     * Uses BroadcastManager.transferText() for actual transfer
     * @param text text as [String] to send
     */
    private suspend fun sendMessage(text: String) {
        if (!canSendMessage(text)) return

        val time = Date(System.currentTimeMillis())
        // TODO: Set chat group id properly. Current is a placeholder
        val id = dao.getNextMessageId(0)
        // TODO: Get sender name from Device object (probably?)
        // TODO: Get chat group id
        // TODO: Get unique message id within chat group (should be in rising order)
        val message = Message(id, 0, "SENDER", time, text)

        broadcastManager.transferText(message)

        message.isOwnMessage = true
        dao.insertMessage(message)

        // TODO: Set color properly (UI team?)
        val messageColor = Color.parseColor("#017f61")
        val textColor = Color.BLACK

        withContext(Dispatchers.Main) { createMessage(message, messageColor, textColor) }
    }

    /**
     * Creates a message on the chat are from given message parameters.
     * Make sure to call this in the main thread
     * @param message a [Message] instance from which to create the visual message in the chat
     * area
     * @param messageColor background color for the message
     * @param textColor color of the messages text
     */
    // TODO: Make the message using proper tools (UI team?)
    private fun createMessage(message: Message, messageColor: Int, textColor: Int) {
        val btn = Button(activity)
        // Left/right side of screen depending on whose message this is
        val alignment = if (message.isOwnMessage) Gravity.END else Gravity.START

        btn.isClickable = false
        btn.text = "[${message.timestamp}] [${message.sender}] ${message.body}"

        btn.maxWidth = (receivingField.width * 0.67).toInt()

        btn.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, // Width
            LinearLayout.LayoutParams.WRAP_CONTENT  // Height
        ).apply {
            gravity = alignment
        }.also {
            it.setMargins(0, 0, 0, 16)
        }

        // Text alignment
        btn.gravity = Gravity.START
        btn.isAllCaps = false
        btn.setBackgroundColor(messageColor)
        btn.setTextColor(textColor)

        receivingField.addView(btn)
    }

    /**
     * Loads all messages (within the default chat group 0) from the database.
     * Used when loading Chat fragment
     */
    // TODO: Implement loading messages of particular chat group (pretty much
    //  only one extra parameter needed)
    private fun loadAllMessages() {
        CoroutineScope(Dispatchers.Main).launch {
            val messages = dao.getChatGroupMessages(0)
            messages.forEach {
                val messageColor = if (it.isOwnMessage) Color.parseColor("#017f61")
                    else Color.parseColor("#262626")
                val textColor = if (it.isOwnMessage) Color.BLACK
                    else Color.WHITE
                createMessage(it, messageColor, textColor)
            }
        }
    }

    /**
     * Checks if a given message can be sent. Checks both connection status through
     * BroadcastManager and that the message isn't empty/all whitespace
     * @param text text content of the message to check
     * @return true if the device is connected to the network and the message is not empty,
     * false if either condition fails
     */
    private suspend fun canSendMessage(text: String): Boolean {
        return CoroutineScope(Dispatchers.Main).async {
            // Can't send message if there is no connection
            // TODO: Remove alert since connection is already checked
            //  when setting a layout?
            if (!broadcastManager.isConnected()) {
                view?.let {
                    showNeutralAlert(
                        "No connection!",
                        "You are not connected to any device.",
                        it.context
                    )
                }
                return@async false
            }
            // Can't send empty message
            if (text.trim() == "") {
                view?.let {
                    showNeutralAlert(
                        "Empty message",
                        "Can not send empty message (this is a placeholder)",
                        it.context
                    )
                }
                return@async false
            }
            return@async true
        }.await()
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
            Chat().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}