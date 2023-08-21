package com.sahil_admin.codedoubt.getStream

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.sahil_admin.codedoubt.authenticator.FirebaseAuthenticator
import com.sahil_admin.codedoubt.objects.AuthUser
import com.sahil_admin.codedoubt.objects.Doubt
import com.sahil_admin.codedoubt.repository.FirebaseRepository
import com.sahil_admin.codedoubt.ui.DashboardViewModel
import dagger.Component
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.models.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class Stream (
    val client: ChatClient
){

    suspend fun connectUser () : LogInEvent {

        val user = FirebaseRepository.getCurrentUser()

        val result = client.connectUser(
            user = User(
                id = user.userId!!,
                name = user.name!!
            ),
            token = client.devToken(user.userId),
        ).await()

        if(result.isError) {
            return LogInEvent.Error(result.error().message ?: "Unknown Error")
        }

        return LogInEvent.Success(user)
    }

    suspend fun disconnectUser () : LogOutEvent {
        val result = client.disconnect(true).await()

        if(result.isError) {
            return LogOutEvent.Error(result.error().message ?: "Unknown Error")
        }

        return LogOutEvent.Success
    }

    suspend fun createChannel(question: String) : ChannelCreateEvent {
        val trimmedChannelName = question.trim()

        if (trimmedChannelName.isEmpty()) {
            return ChannelCreateEvent.Error("Channel name can't be empty")
        }

        val channelId = UUID.randomUUID().toString()

        val result = client.channel(
            channelType = "messaging",
            channelId = channelId,
        ).create(
            Collections.emptyList(

            ),
            mapOf(
                "name" to trimmedChannelName,
//                    "image" to "https://img.icons8.com/emoji/512/red-circle-emoji.png"
            )
        ).await()

        if (result.isError) {
            return ChannelCreateEvent.Error (
                    result.error().message ?: "Unknown Error"
                )
        }

        val doubt = Doubt("messaging:$channelId",
            question,
            false,
            FirebaseAuthenticator.auth.currentUser!!.displayName)

        FirebaseRepository.addDoubt(doubt)

        return ChannelCreateEvent.Success
    }

    sealed class LogInEvent {
        class Success (val user: AuthUser): LogInEvent()
        class Error (val e: String): LogInEvent()
    }

    sealed class LogOutEvent {
        object Success: LogOutEvent()
        class Error (val e: String): LogOutEvent()
    }

    sealed class ChannelCreateEvent {
        object Success : ChannelCreateEvent()
        class Error(val e: String) : ChannelCreateEvent()
    }
}