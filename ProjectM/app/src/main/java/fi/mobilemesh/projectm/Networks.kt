package fi.mobilemesh.projectm

import android.graphics.Color
import android.net.wifi.p2p.WifiP2pDevice
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableRow
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import fi.mobilemesh.projectm.network.BroadcastManager

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
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

    private lateinit var broadcastManager: BroadcastManager
    private lateinit var nodeList: CardView

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
            .also { it.setNetworks(this) }
        nodeList = view.findViewById(R.id.nodecard)

        return view
    }

    /**
     * Clears all nearby devices from the nodeCard displaying them
     */
    fun clearDevices() {
        nodeList.removeAllViews()
    }

    /**
     * Creates a card for given device so it can be connected to Usually called from
     * BroadcastManager when a new nearby device is detected
     * @param device device for which to create the interactable card
     */
    fun createCardViewLayout(device: WifiP2pDevice) {
        // Creating CardView
        println(view)
        val cardView = view?.let {
            CardView(it.context).apply {
                id = R.id.nodecard
                layoutParams = TableRow.LayoutParams(dpToPx(380), dpToPx(60))
                setCardBackgroundColor(Color.parseColor("#434343"))
                radius = dpToPx(10f)
                setContentPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
                isClickable = true

                setOnClickListener{
                    broadcastManager.connectToDevice(device.deviceAddress)
                }
            }
        }

        // Creating TextView
        val textView = TextView(context).apply {
            layoutParams = TableRow.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            text = device.deviceName
            setTextAlignment(View.TEXT_ALIGNMENT_CENTER)
            setTextColor(ContextCompat.getColor(context, R.color.white))
            textSize = 16f
        }

        // Adding TextView to CardView
        cardView?.addView(textView)

        // Creating TableRow for CardView
        val tableRow = TableRow(context)
        tableRow.addView(cardView)

        // Adding TableRow to your TableLayout or any other ViewGroup
        nodeList.addView(tableRow)
    }

    // Helper function to convert dp to px
    private fun dpToPx(dp: Int): Int {
        val scale = view?.context?.resources?.displayMetrics?.density
        return (dp * scale!! + 0.5f).toInt()
    }

    private fun dpToPx(dp: Float): Float {
        val scale = view?.context?.resources?.displayMetrics?.density
        return dp * scale!! + 0.5f
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
    }
}