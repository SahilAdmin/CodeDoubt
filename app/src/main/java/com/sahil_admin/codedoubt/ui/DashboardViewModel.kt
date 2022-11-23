package com.sahil_admin.codedoubt.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.sahil_admin.codedoubt.objects.AuthUser
import com.sahil_admin.codedoubt.objects.Doubt
import dagger.hilt.android.lifecycle.HiltViewModel
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.models.User
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@HiltViewModel
class DashboardViewModel @Inject constructor(private val client: ChatClient): ViewModel() {

    private val userCollectionRef = Firebase.firestore.collection("Users")
    private val doubtsCollectionRef = Firebase.firestore.collection("Doubts")
    private val auth = Firebase.auth
    private var user: User? = null

    private val _channelCreateEvent = MutableSharedFlow<ChannelCreateEvent>()
    val channelCreateEvent = _channelCreateEvent.asSharedFlow()

    private val _logInEvent = MutableSharedFlow<LogInEvent>()
    val loginEvent = _logInEvent.asSharedFlow()

    fun connectUser () {

        val currentUser = auth.currentUser!!

        viewModelScope.launch {
            val querySnapshot = suspendCoroutine { continuation ->
                userCollectionRef.whereEqualTo("email", currentUser.email).get().addOnCompleteListener {
                    continuation.resume(it.result)
                }
            }

            val user = querySnapshot.documents[0].toObject<AuthUser>()!!

            val result = client.connectUser(
                user = User(
                    id = user.userId!!,
                    name = user.name!!
                ),
                token = client.devToken(user.userId),
                5000L
            ).await()

            if (result.isError) {
                _logInEvent.emit(LogInEvent.Error(result.error().message ?: "Unknown Error"))
                return@launch
            }

            _logInEvent.emit(LogInEvent.Success)
        }
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
                channelId = channelId
            ).create(
                emptyList(

                ),
                mapOf(
                    "name" to trimmedChannelName
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

            suspendCoroutine { continuation ->
                doubtsCollectionRef.add(doubt).addOnCompleteListener {
                    continuation.resume(Unit)
                }
            }

            _channelCreateEvent.emit(ChannelCreateEvent.Success)
        }
    }

    sealed class LogInEvent {
        object Success: LogInEvent()
        class Error (val e: String): LogInEvent()
    }

    sealed class ChannelCreateEvent {
        object Success: ChannelCreateEvent()
        class Error (val e: String): ChannelCreateEvent()
    }
}