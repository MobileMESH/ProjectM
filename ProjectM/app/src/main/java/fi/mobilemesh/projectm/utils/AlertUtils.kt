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

fun showConfirmationAlert(
    title: String,
    body: String,
    positiveText: String,
    negativeText: String,
    context: Context,
    onPositiveClick: () -> Unit,
    onNegativeClick: () -> Unit
) {
    AlertDialog.Builder(context)
        .setTitle(title)
        .setMessage(body)
        .setPositiveButton(positiveText) { dialog, _ ->
            onPositiveClick()
            dialog.dismiss()
        }
        .setNegativeButton(negativeText) { dialog, _ ->
            onNegativeClick()
            dialog.dismiss()
        }
        .setCancelable(false)
        .show()
}
