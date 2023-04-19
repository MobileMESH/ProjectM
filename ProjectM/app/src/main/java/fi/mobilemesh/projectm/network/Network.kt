package fi.mobilemesh.projectm.network

data class Network(
    val id: String,
    val devices: Collection<Device>
) : java.io.Serializable