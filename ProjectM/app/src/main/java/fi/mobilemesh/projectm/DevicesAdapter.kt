package fi.mobilemesh.projectm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

// Define the adapter class and pass in the list of devices as a constructor argument
class DevicesAdapter(private val devices: MutableList<DeviceList>) : RecyclerView.Adapter<DevicesAdapter.ViewHolder>() {

    private val FIRST_ROW = 0
    private val OTHER_ROWS = 1

    // create a new ViewHolder object to represent a new row in the RecyclerView
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.device_list_row, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = devices.size

    // called when a new row needs to be displayed on screen
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = devices[position]
        if (position == FIRST_ROW) {
            holder.deviceName.text = device.deviceName
            holder.locationButton.visibility = View.GONE
            holder.locationUnavailable.visibility = View.GONE
        } else {
            if (device.deviceLocation) {
                holder.deviceName.text = device.deviceName
                holder.locationButton.visibility = View.VISIBLE
                holder.locationUnavailable.visibility = View.GONE
            }
            else {
                holder.deviceName.text = device.deviceName
                holder.locationButton.visibility = View.GONE
                holder.locationUnavailable.visibility = View.VISIBLE
            }
        }
    }

    // this represents a single row in the RecyclerView
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // get references to the TextViews in the layout
        val deviceName: TextView = itemView.findViewById(R.id.deviceName)
        val locationUnavailable: TextView = itemView.findViewById(R.id.locationOff)
        val locationButton: Button = itemView.findViewById(R.id.locationButton)
    }
}
