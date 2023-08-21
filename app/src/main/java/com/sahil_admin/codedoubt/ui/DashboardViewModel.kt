package com.sahil_admin.codedoubt.ui

import com.sahil_admin.codedoubt.authenticator.FirebaseAuthenticator.auth
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sahil_admin.codedoubt.getStream.Stream
import com.sahil_admin.codedoubt.objects.Doubt
import com.sahil_admin.codedoubt.repository.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.getstream.chat.android.client.ChatClient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Collections.emptyList
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(val client: ChatClient): ViewModel() {

    private val _channelCreateEvent = MutableSharedFlow<Stream.ChannelCreateEvent>()
    val channelCreateEvent = _channelCreateEvent.asSharedFlow()

    private val _logInEvent = MutableSharedFlow<Stream.LogInEvent>()
    val loginEvent = _logInEvent.asSharedFlow()

    private val _logoutEvent = MutableSharedFlow<Stream.LogOutEvent>()
    val logoutEvent = _logoutEvent.asSharedFlow()

    private val _doubtList = MutableStateFlow(mutableListOf<Doubt>())
    val doubtList = _doubtList.asStateFlow()

    private val stream = Stream(client)

    fun connectUser () {
        viewModelScope.launch {
            _logInEvent.emit(stream.connectUser())
        }
    }

    fun updateUsername (newName: String) {
        viewModelScope.launch {
            FirebaseRepository.updateUser(newName)
            connectUser()
        }
    }

    fun disconnectUser () = viewModelScope.launch {
        _logoutEvent.emit(stream.disconnectUser())
    }

    fun createChannel(question: String) = viewModelScope.launch {
        _channelCreateEvent.emit(stream.createChannel(question))
    }

    fun getDoubts () {
        viewModelScope.launch {
            val doubts = FirebaseRepository.getDoubtList()
            _doubtList.value = doubts
        }
    }
}