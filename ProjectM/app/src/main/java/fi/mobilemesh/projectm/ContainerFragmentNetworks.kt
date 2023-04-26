package fi.mobilemesh.projectm

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import fi.mobilemesh.projectm.network.BroadcastManager

class ContainerFragmentNetworks : Fragment() {

    private lateinit var broadcastManager: BroadcastManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        broadcastManager = BroadcastManager.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_networks_container, container, false)
        switchFragment(Networks::class.java)
        return view
    }

    fun switchFragment(target: Class<*>) {
        val f: Fragment = target.newInstance() as Fragment
        val fm = childFragmentManager
        val transaction = fm.beginTransaction()
        transaction.replace(R.id.NetworksContainerFragment, f)
        transaction.addToBackStack(null)
        transaction.commit()
    }
}