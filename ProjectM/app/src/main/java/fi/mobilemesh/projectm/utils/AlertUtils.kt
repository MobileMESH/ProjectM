package fi.mobilemesh.projectm.utils

import android.app.AlertDialog
import android.content.Context

fun showNeutralAlert(title: String, body: String, context: Context) {
    AlertDialog.Builder(context)
        .setTitle(title)
        .setMessage(body)
        .setNeutralButton("OK", null)
        .show()
}