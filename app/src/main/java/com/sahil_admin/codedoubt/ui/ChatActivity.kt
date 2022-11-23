package com.sahil_admin.codedoubt.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import com.getstream.sdk.chat.viewmodel.MessageInputViewModel
import com.getstream.sdk.chat.viewmodel.messages.MessageListViewModel
import com.sahil_admin.codedoubt.R
import com.sahil_admin.codedoubt.Utility.CHANNEL_ID
import com.sahil_admin.codedoubt.databinding.ActivityChatBinding
import io.getstream.chat.android.ui.message.input.viewmodel.bindView
import io.getstream.chat.android.ui.message.list.header.viewmodel.MessageListHeaderViewModel
import io.getstream.chat.android.ui.message.list.header.viewmodel.bindView
import io.getstream.chat.android.ui.message.list.viewmodel.bindView
import io.getstream.chat.android.ui.message.list.viewmodel.factory.MessageListViewModelFactory

class ChatActivity : AppCompatActivity() {

    private val binding by lazy {ActivityChatBinding.inflate(layoutInflater)}
    private var onThread = false
    val factory by lazy { MessageListViewModelFactory(intent.getStringExtra(CHANNEL_ID)!!) }
    val messageListHeaderViewModel by viewModels<MessageListHeaderViewModel> { factory }
    val messageListViewModel by viewModels<MessageListViewModel> { factory }
    val messageInputViewModel by viewModels<MessageInputViewModel> { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

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
    }

    override fun onBackPressed() {
        if(onThread) messageListViewModel.onEvent(MessageListViewModel.Event.BackButtonPressed)
        else super.onBackPressed()
    }
}

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