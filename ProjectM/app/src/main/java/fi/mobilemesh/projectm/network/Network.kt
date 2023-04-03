package fi.mobilemesh.projectm.network

data class Network(
    val id: String,
    val others: Collection<Device>
) : java.io.Serializable