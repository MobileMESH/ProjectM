package fi.mobilemesh.projectm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import fi.mobilemesh.projectm.network.Device

// Define the adapter class and pass in the list of devices as a constructor argument
class ConnectedDevicesAdapter(private val devices: MutableList<Device>) : RecyclerView.Adapter<ConnectedDevicesAdapter.ViewHolder>() {

    private val FIRST_ROW = 0

    // create a new ViewHolder object to represent a new row in the RecyclerView
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.device_list_row, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = devices.size

    // called when a new row needs to be displayed on screen
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // own device first, doesn't show location info
        val device = devices[position]
        if (position == FIRST_ROW) {
            holder.deviceName.text = device.getName()
        } else {
            holder.deviceName.text = device.getName()
        }
    }

    // this represents a single row in the RecyclerView
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceName: TextView = itemView.findViewById(R.id.deviceName)
    }
}