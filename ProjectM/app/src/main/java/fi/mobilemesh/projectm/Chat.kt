package fi.mobilemesh.projectm

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import fi.mobilemesh.projectm.utils.showNeutralAlert
import kotlinx.coroutines.*
import java.util.*
import kotlin.time.Duration.Companion.hours

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

    lateinit var sendButton: FloatingActionButton
    lateinit var sendingField: EditText
    lateinit var receivingField: LinearLayout

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

        dao = MessageDatabase.getInstance(view.context).dao
        broadcastManager = BroadcastManager.getInstance(view.context)

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
    @SuppressLint("SetTextI18n")
    private fun createMessage(message: Message, messageType: Int){
        val txt = TextView(activity)
        // Left/right side of screen depending on whose message this is
        val alignment = if (message.isOwnMessage) Gravity.END else Gravity.START
        txt.setBackgroundResource(messageType)
        //${message.timestamp.time}
        txt.text = "${message.sender} ${message.body}"
        txt.setPadding(20,4,60,10)

        //txt.maxWidth = (receivingField.width * 0.67).toInt()

        txt.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, // Width
            LinearLayout.LayoutParams.WRAP_CONTENT  // Height
        ).apply {
            gravity = alignment
        }.also {
            it.setMargins(0, 10, 0, 16)
        }

        // Text alignment
        txt.gravity = Gravity.START
        txt.setTextColor(Color.WHITE)
        txt.isAllCaps = false

        receivingField.addView(txt)
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
    private suspend fun canSendMessage(text: String): Boolean {
        return CoroutineScope(Dispatchers.Main).async {
            // Can't send message if there is no connection
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