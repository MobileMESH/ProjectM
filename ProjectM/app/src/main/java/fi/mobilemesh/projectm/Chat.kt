package fi.mobilemesh.projectm

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout

import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import fi.mobilemesh.projectm.database.MessageDatabase
import fi.mobilemesh.projectm.database.MessageQueries
import fi.mobilemesh.projectm.database.entities.Message
import fi.mobilemesh.projectm.network.BroadcastManager
import fi.mobilemesh.projectm.network.MeshManager
import fi.mobilemesh.projectm.utils.showNeutralAlert
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

    private lateinit var dao: MessageQueries
    private lateinit var broadcastManager: BroadcastManager
    private lateinit var meshManager: MeshManager

    lateinit var sendButton: FloatingActionButton
    lateinit var sendingField: EditText
    lateinit var receivingField: LinearLayout
    lateinit var openDetailsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        println("Chat")
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
            // switch to Details
            (parentFragment as ContainerFragmentChat).switchFragment(ChatNetworkDetails::class.java)
        }
    }

    // This function is used to do all magic
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_chat, container, false)

        receivingField = view.findViewById(R.id.receivingField)
        sendingField = view.findViewById(R.id.sendingField)
        sendButton = view.findViewById(R.id.sendTextButton)
        openDetailsButton = view.findViewById(R.id.openDetailsButton)

        dao = MessageDatabase.getInstance(view.context).dao
        broadcastManager = BroadcastManager.getInstance(view.context)
        meshManager = MeshManager.getInstance(view.context)

        mapButtons()

        lifecycleScope.launch { observeLiveMessages() }

        return view
    }

    /**
     * Updates the chat every time a new message is added to the database (a message is receive).
     * WIP
     */
    // TODO: Don't reload all messages...
    private fun observeLiveMessages() {
        dao.getLiveChatGroupMessages(meshManager.getTestGroupId()).observe(viewLifecycleOwner) {
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
        if (!isMessageValid(text)) return

        val networkId = meshManager.getTestGroupId()
        val messageId = dao.getNextMessageId(networkId)
        // TODO: Get chat group id, this just gets a random one for testing
        val sender = broadcastManager.getThisDevice().getName()
        val time = Date(System.currentTimeMillis())

        val message = Message(messageId, networkId, sender, time, text)

        meshManager.sendGroupMessage(message)
        dao.insertMessage(message)

        val messageType = R.drawable.outgoing_bubble

        withContext(Dispatchers.Main) { createMessage(message, messageType) }
    }

    /**
     * Creates a message on the chat are from given message parameters.
     * Make sure to call this in the main thread
     * @param message a [Message] instance from which to create the visual message in the chat
     * area
     * @param messageType is the drawable for the message
     */
    // TODO: Make the message using proper tools (UI team?)
    private fun createMessage(message: Message, messageType: Int){
        // Creating base for the message
        val base = LinearLayout(activity)
        base.orientation = LinearLayout.VERTICAL
        base.setBackgroundResource(messageType)

        createMessageComponents(message, base)

        // Left/right side of screen depending on whose message this is
        val alignment = if (message.isOwnMessage) Gravity.END else Gravity.START

        // How messages are shown in the parent layout
        base.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, // Width
            LinearLayout.LayoutParams.WRAP_CONTENT  // Height
        ).apply {
            gravity = alignment
        }.also {
            it.setMargins(20, 20, 20, 0)
        }
        // Text alignment
        base.gravity = Gravity.START
        // Add everything to parent
        receivingField.addView(base)
    }

    /**
     * Creates visual components for the contents of the message (sender,
     * message itself and time)
     * @param message a [Message] instance from which to create the visual message in the chat
     * area
     * @param base to put all components in
     */
    private fun createMessageComponents(message: Message, base: LinearLayout) {
        // Creating TextView for the sender
        val sender = TextView(activity)
         if (message.isOwnMessage) {
             sender.text ="You"
             sender.setTextColor(Color.parseColor("#dff0e9"))
         }
         else {
             sender.text="${message.sender}"
             sender.setTextColor(Color.parseColor("#8bc9b1"))
         }
        sender.typeface = Typeface.DEFAULT_BOLD

        // Creating TextView for message contents
        val messageBody = TextView(activity)
        messageBody.text="${message.body}"
        messageBody.setTextColor(Color.WHITE)

        //Create visual timestamp to the message
        val time = TextView(activity)
        val date:Date = message.timestamp
        val cal = Calendar.getInstance()
        cal.time = date
        val hours = cal.get(Calendar.HOUR_OF_DAY)
        val minutes = cal.get(Calendar.MINUTE)

        if (minutes < 10) {
            time.text = "$hours:0$minutes"
        }
        else {
            time.text = "$hours:$minutes"
        }
        time.setTextColor(Color.parseColor("#c9c7c7"))
        time.gravity = Gravity.RIGHT
        time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)

        // Place components to the base
        sender.setPadding(20,7,20,5)
        messageBody.setPadding(20,0,20,5)
        time.setPadding(0,0,20,10)
        base.addView(sender)
        base.addView(messageBody)
        base.addView(time)
    }

    /**
     * Loads all messages (within the default chat group 0) from the database.
     * Used when loading Chat fragment
     */
    // TODO: Implement loading messages of particular chat group (pretty much
    //  only one extra parameter needed)
    private fun loadAllMessages() {
        CoroutineScope(Dispatchers.Main).launch {
            val messages = dao.getChatGroupMessages(meshManager.getTestGroupId())
            messages.forEach {
                val messageType = if (it.isOwnMessage) R.drawable.outgoing_bubble
                    else R.drawable.incoming_bubble
                createMessage(it, messageType)
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
    private fun isMessageValid(text: String): Boolean {
        if (text.trim() == "") {
            view?.let {
                showNeutralAlert(
                    "Empty message",
                    "Can not send empty message (this is a placeholder)",
                    it.context
                )
            }
            return false
        }
        return true
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