package fi.mobilemesh.projectm.objects

import java.util.*

data class Message(val sender: String,
                   val timestamp: Date,
                   val body: String) : java.io.Serializable
