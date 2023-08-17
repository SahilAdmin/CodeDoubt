package com.sahil_admin.codedoubt.ui

import com.sahil_admin.codedoubt.authenticator.FirebaseAuthenticator.auth
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sahil_admin.codedoubt.objects.AuthUser
import com.sahil_admin.codedoubt.objects.Doubt
import com.sahil_admin.codedoubt.repository.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.models.User
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import java.util.Collections.emptyList
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(private val client: ChatClient): ViewModel() {

    private val _channelCreateEvent = MutableSharedFlow<ChannelCreateEvent>()
    val channelCreateEvent = _channelCreateEvent.asSharedFlow()

    private val _logInEvent = MutableSharedFlow<LogInEvent>()
    val loginEvent = _logInEvent.asSharedFlow()

    private val _logoutEvent = MutableSharedFlow<LogOutEvent>()
    val logoutEvent = _logoutEvent.asSharedFlow()

    private val _doubtList = MutableStateFlow(mutableListOf<Doubt>())
    val doubtList = _doubtList.asStateFlow()

    fun connectUser () {

        val currentUser = auth.currentUser!!

        viewModelScope.launch {

            val user = FirebaseRepository.getUser(currentUser.email!!)

            val result = client.connectUser(
                user = User(
                    id = user.userId!!,
                    name = user.name!!,
                ),
                token = client.devToken(user.userId),
            ).await()

            if (result.isError) {
                _logInEvent.emit(LogInEvent.Error(result.error().message ?: "Unknown Error"))
                return@launch
            }

            _logInEvent.emit(LogInEvent.Success(user))
        }
    }

    fun updateUsername (newName: String) {
        val currentUser = auth.currentUser!!

        viewModelScope.launch {

            FirebaseRepository.updateUser(currentUser.email!!, newName)

            connectUser()
        }
    }

    fun disconnectUser () = viewModelScope.launch {
        val result = client.disconnect(true).await()

        if(result.isError) {
            _logoutEvent.emit(LogOutEvent.Error(result.error().message ?: "Unknown Error"))
            return@launch
        }

        _logoutEvent.emit(LogOutEvent.Success)
    }

    fun createChannel(question: String) {
        val trimmedChannelName = question.trim()

        viewModelScope.launch {
            if (trimmedChannelName.isEmpty()) {
                _channelCreateEvent.emit(ChannelCreateEvent.Error("Channel name can't be empty"))
                return@launch
            }

            val channelId = UUID.randomUUID().toString()

            val result = client.channel(
                channelType = "messaging",
                channelId = channelId,
            ).create(
                emptyList(

                ),
                mapOf(
                    "name" to trimmedChannelName,
//                    "image" to "https://img.icons8.com/emoji/512/red-circle-emoji.png"
                )
            ).await()

            if (result.isError) {
                _channelCreateEvent.emit(
                    ChannelCreateEvent.Error(
                        result.error().message ?: "Unknown Error"
                    )
                )
                return@launch
            }

            val doubt = Doubt("messaging:$channelId", question, false, auth.currentUser!!.displayName)

            FirebaseRepository.addDoubt(doubt)

            _channelCreateEvent.emit(ChannelCreateEvent.Success)
        }
    }

    fun getDoubts () {
        viewModelScope.launch {
            val doubts = FirebaseRepository.getDoubtList()
            _doubtList.value = doubts
        }
    }

    sealed class LogInEvent {
        class Success (val user: AuthUser): LogInEvent()
        class Error (val e: String): LogInEvent()
    }

    sealed class ChannelCreateEvent {
        object Success: ChannelCreateEvent()
        class Error (val e: String): ChannelCreateEvent()
    }

    sealed class LogOutEvent {
        object Success: LogOutEvent()
        class Error (val e: String): LogOutEvent()
    }
}