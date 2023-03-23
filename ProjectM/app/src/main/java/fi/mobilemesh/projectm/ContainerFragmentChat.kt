package fi.mobilemesh.projectm

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import fi.mobilemesh.projectm.network.BroadcastManager

class ContainerFragmentChat : Fragment() {

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
        return inflater.inflate(R.layout.fragment_chat_container, container, false)
    }

    override fun onResume() {
        super.onResume()
        switchFragment(Chat::class.java)
    }

    fun switchFragment(target: Class<*>) {
        val f: Fragment = target.newInstance() as Fragment
        val fm = childFragmentManager
        val transaction = fm.beginTransaction()
        transaction.replace(R.id.ChatContainerFragment, f)
        transaction.addToBackStack(null)
        transaction.commit()
    }
}