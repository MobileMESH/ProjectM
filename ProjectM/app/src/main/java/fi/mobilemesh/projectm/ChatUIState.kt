package fi.mobilemesh.projectm

sealed class ChatUIState {
    object Chat : ChatUIState()
    object Details : ChatUIState()
    object Disconnected : ChatUIState()
}