package fi.mobilemesh.projectm

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import fi.mobilemesh.projectm.connectionManager.ConnectionManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val connectionManager = ConnectionManager(this)
    }
}