package fi.mobilemesh.projectm

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment

class OnboardingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
    }

    // not sure if these go here?
    companion object {
        const val REQUEST_CODE = 223312
        const val NOTIFICATION_REQUEST_CODE = 374520
    }
}