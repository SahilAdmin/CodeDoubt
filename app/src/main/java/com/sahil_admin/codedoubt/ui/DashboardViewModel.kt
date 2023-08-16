package com.sahil_admin.codedoubt.ui

import FirebaseAuthenticator.auth
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.sahil_admin.codedoubt.R
import com.sahil_admin.codedoubt.objects.AuthUser
import com.sahil_admin.codedoubt.objects.Doubt
import dagger.hilt.android.lifecycle.HiltViewModel
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.models.User
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.*
import java.util.Collections.emptyList
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@HiltViewModel
class DashboardViewModel @Inject constructor(private val client: ChatClient): ViewModel() {

    private val userCollectionRef = Firebase.firestore.collection("Users")
    private val doubtsCollectionRef = Firebase.firestore.collection("Doubts")
    private var user: User? = null

    private val _channelCreateEvent = MutableSharedFlow<ChannelCreateEvent>()
    val channelCreateEvent = _channelCreateEvent.asSharedFlow()

    private val _logInEvent = MutableSharedFlow<LogInEvent>()
    val loginEvent = _logInEvent.asSharedFlow()

    private val _logoutEvent = MutableSharedFlow<LogOutEvent>()
    val logoutEvent = _logoutEvent.asSharedFlow()

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
            val querySnapshot = suspendCoroutine { continuation ->
                userCollectionRef.whereEqualTo("email", currentUser.email).get()
                    .addOnCompleteListener {
                        continuation.resume(it.result)
                    }
            }

            val user = querySnapshot.documents[0].toObject<AuthUser>()!!

            user.name = newName

            suspendCoroutine<Unit> { continuation ->
                userCollectionRef.document(querySnapshot.documents[0].id).set(
                    user
                ).addOnCompleteListener {
                    continuation.resume(Unit)
                }
            }

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
                    "image" to "https://img.icons8.com/emoji/512/red-circle-emoji.png"
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

        val deff = viewModelScope.async {

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

/*
Coroutines -
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.5'
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.5'

// For viewModelScope and lifecycleScope -
implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1" // for lifecycle scope
implementation "androidx.lifecycle:lifecycle-runtime-ktx:2.5.1" // for viewModel scope

// contexts
Dispatchers.IO -> for downloading related work, uses thread other than the main thread
Dispatchers.Main -> for main thread (used for work related to design)
    // We can change the thread in between a thread by using withContext(Dispatchers.Main)

Dispatchers.Default -> for complex and long running calculations
Dispatchers.Unconfined -> not confined to one thread (takes the thread resumed by suspend function)

GlobalScope -> works as long as the application is working
lifecycleScope -> the coroutine will work as long as the lifecycle of the current context (activity) is alive
viewModelScope -> coroutine sticks to the lifecycle of the viewModel

Suspend functions -> suspend functions are the functions which can only called from other suspend functions.
    They usually cause a delay in the thread in which they are called.
    delay is also a suspend function -> delay (5000L) will delay a thread by 5 seconds.

runblocking -> Coroutine scope which will block the main UI thread. Dispatchers.Main context does not block
    the UI thread it's just used to get the context properties. But runblocking will stop the UI thread.
    it's used to make the UI thread to wait for suspend functions.
    eg: runblocking {
        delay (5000L)
    } this will delay the UI thread for 5000L

Jobs -> whenever we need to wait for a coroutine to finish we use jobs. Job.join() is used to wait until
    that coroutine is finished.

Cancellation -> we can also use job.cancel to cancel a job. but we have to use it properly as it does not
    work everytime. for example if we are in a for loop coroutine will be too busy to check if it is cancelled

    On way to overcome this is by using isActive property of the coroutine.
    Another way is to wrap the code within withTimeout lambda
        eg: withTimeout (5000L) {
            doNetworkCall()
        } // here if the network call takes more than 5 seconds the coroutine will stop

Async -> When we want to get the result of something asynchronously we use async
    It returns a deffered object of specific type which can be obtained by using await() method
        eg: val deferred = async { getString() }
            val str = deferred.await()

Suspend coroutine -> A suspend coroutine is a builder function that is mainly used to convert callbacks
    into suspend functions

suspend fun awaitLocation(): Location? {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

    return suspendCoroutine { continuation ->
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            ==PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.lastLocation.addOnSuccessListener {
                continuation.resume(it)
            }

        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISIONS_LOCATION, REQUEST_CODE_LOCATION_PERMISSIONS)
        }
    }
}
*/

/*
STREAM CHAT ->
// Stream Dependencies
implementation "io.getstream:stream-chat-android-ui-components:5.8.2"
implementation "io.getstream:stream-chat-android-offline:5.8.2"
implementation 'io.coil-kt:coil:2.1.0'

IN SETTINGS.gradle file
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url "https://jitpack.io" } // needed for stream chat
    }
}

MODULE ->
@Module
@InstallIn(SingletonComponent::class)
object Module {

    @Singleton
    @Provides
    fun provideOfflinePlugin (@ApplicationContext context: Context) = StreamOfflinePluginFactory(
        config = Config(
            backgroundSyncEnabled = true,
            userPresence = true,
            persistenceEnabled = true,
            uploadAttachmentsNetworkType = UploadAttachmentsNetworkType.NOT_ROAMING,
        ),
        appContext = context,
    )

    @Singleton
    @Provides
    fun provideChatClient(@ApplicationContext context: Context, offlinePluginFactory: StreamOfflinePluginFactory) =
        ChatClient.Builder(context.getString(R.string.stream_api_key), context)
        .withPlugin(offlinePluginFactory)
        .logLevel(ChatLogLevel.ALL) // Set to NOTHING in prod
        .build()
}

FOR CONNECTING USER USING DEV TOKEN ->
fun connectUser () {
    viewModelScope.launch {

        val result = client.connectUser(
            user = User(
                id = "sahil2",
                name = "sahil2",
            ),
            token = client.devToken("sahil2"),
            5000L
        ).await()

        if (result.isError) {
            _logInEvent.emit(LogInEvent.Error(result.error().message ?: "Unknown Error"))
            return@launch
        }

        _logInEvent.emit(LogInEvent.Success)
    }
}

fun createChannel (str: String) {
val trimmedChannelName = str.trim()
viewModelScope.launch {
    if(trimmedChannelName.isEmpty()) {
        _channelCreateEvent.emit(ChannelCreateEvent.Error("Channel name can't be empty"))
        return@launch
    }

    val result = client.channel (
        channelType = "messaging",
        channelId = UUID.randomUUID().toString()
    ).create(
        emptyList(

        )
        ,
        mapOf(
            "name" to trimmedChannelName
        )
    ).await()

    if(result.isError) {
        _channelCreateEvent.emit(ChannelCreateEvent.Error(result.error().message ?: "Unknown Error"))
        return@launch
    }

    _channelCreateEvent.emit(ChannelCreateEvent.Success)
}

// CHANNELS ACTIVITY
val factory = ChannelListViewModelFactory (
    filter = Filters.and(
        eq("type", "messaging"),
    ),
    sort = ChannelListViewModel.DEFAULT_SORT,
    limit = 50
)

val channelViewModel: ChannelListViewModel by viewModels {factory}
val channelListHeaderViewModel: ChannelListHeaderViewModel by viewModels ()

channelViewModel.bindView(binding.channelListView, this@MainActivity)
channelListHeaderViewModel.bindView(binding.channelListHeaderView, this)

// CHAT ACTIVITY
private var onThread = false
val factory by lazy {MessageListViewModelFactory(intent.getStringExtra(CHANNEL_ID)!!)}
val messageListHeaderViewModel by viewModels<MessageListHeaderViewModel> { factory }
val messageListViewModel by viewModels<MessageListViewModel> { factory }
val messageInputViewModel by viewModels<MessageInputViewModel> { factory }

messageListHeaderViewModel.bindView(binding.messageListHeaderView, this)
messageListViewModel.bindView(binding.messageListView, this)
messageInputViewModel.bindView(binding.messageInputView, this)

// for private thread
messageListViewModel.mode.observe(this) { mode ->
    when(mode) {
        is MessageListViewModel.Mode.Thread -> {
            messageListHeaderViewModel.setActiveThread(mode.parentMessage)
            messageInputViewModel.setActiveThread(mode.parentMessage)
            onThread = true
        }
        is MessageListViewModel.Mode.Normal -> {
            messageListHeaderViewModel.resetThread()
            messageInputViewModel.resetThread()
            onThread = false
        }
    }
}

binding.messageListView.setMessageEditHandler(messageInputViewModel::postMessageToEdit)

binding.messageListHeaderView.setBackButtonClickListener {
    messageListViewModel.bindView(binding.messageListView, this)
}

binding.messageListHeaderView.setBackButtonClickListener {
    messageListViewModel.onEvent(MessageListViewModel.Event.BackButtonPressed)
}

override fun onBackPressed() {
    if(onThread) messageListViewModel.onEvent(MessageListViewModel.Event.BackButtonPressed)
    else super.onBackPressed()
}

// CHANNELS LAYOUT
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <io.getstream.chat.android.ui.channel.list.ChannelListView
        android:id="@+id/channelListView"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintHorizontal_bias="0.5"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" >

    </io.getstream.chat.android.ui.channel.list.ChannelListView>

</androidx.constraintlayout.widget.ConstraintLayout>

// CHAT LAYOUT
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <io.getstream.chat.android.ui.message.list.header.MessageListHeaderView
        android:id="@+id/messageListHeaderView"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintHorizontal_bias="0.5"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <io.getstream.chat.android.ui.message.list.MessageListView
        android:id="@+id/messageListView"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintBottom_toTopOf="@+id/messageInputView"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintHorizontal_bias="0.5"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@+id/messageListHeaderView" />

    <io.getstream.chat.android.ui.message.input.MessageInputView
        android:id="@+id/messageInputView"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintHorizontal_bias="0.5"
        app:layout_constraintStart_toStartOf="parent" />


</androidx.constraintlayout.widget.ConstraintLayout>
*/