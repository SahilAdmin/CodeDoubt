package com.sahil_admin.codedoubt.ui

import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.sahil_admin.codedoubt.R
import com.sahil_admin.codedoubt.Utility.makeToast
import com.sahil_admin.codedoubt.adapters.DoubtsAdapter
import com.sahil_admin.codedoubt.databinding.ActivityDashboardBinding
import com.sahil_admin.codedoubt.objects.Doubt
import com.sahil_admin.codedoubt.ui.auth.MainActivity
import com.sahil_admin.codedoubt.ui.auth.SignupActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlin.coroutines.suspendCoroutine

@AndroidEntryPoint
class DashboardActivity : AppCompatActivity() {

    private val TAG = "LOG_Dashboard"
    private val auth = Firebase.auth
    private val doubtsCollectionRef = Firebase.firestore.collection("Doubts")
    private val binding by lazy { ActivityDashboardBinding.inflate(layoutInflater) }
    private val toggle by lazy {ActionBarDrawerToggle(this, binding.drawerLayout,
        R.string.open,
        R.string.close
    ) }
    private val viewModel by viewModels<DashboardViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(binding.root)

        viewModel.connectUser()

        getUserDetails()
        subscribeToViewModel()
        subscribeToViews()
        subscribeToDoubts()
    }

    private fun subscribeToViewModel () {
        lifecycleScope.launchWhenStarted {
            viewModel.loginEvent.collect {
                when (it) {
                    is DashboardViewModel.LogInEvent.Success -> Log.d(TAG, "onCreate: Successfully LoggedIn")
                    is DashboardViewModel.LogInEvent.Error -> makeToast("Stream LogIn: ${it.e}");
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.logoutEvent.collect {
                when (it) {
                    is DashboardViewModel.LogOutEvent.Success -> {
                        withContext(Dispatchers.Main) {
                            startActivity(Intent(this@DashboardActivity, MainActivity::class.java))
                        }
                    }

                    is DashboardViewModel.LogOutEvent.Error -> makeToast("Stream LogOut: ${it.e}")
                }
            }
        }
    }

    private fun subscribeToViews () {
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState() // show that it's ready to be used

        binding.imageViewMenu.setOnClickListener {
            binding.drawerLayout.open()
        }

        binding.imageViewAskPlus.setOnClickListener {
            CreateDoubtDialog().show(supportFragmentManager, "Create Doubt")
        }

        val navigationView = binding.dashboardNavigationView

        navigationView.setNavigationItemSelectedListener {
            menuItemListener(it)
        }
    }

    private fun getUserDetails() {
        val user = auth.currentUser!!

        binding.textViewProfilename.text = user.displayName
        binding.textViewProfileEmail.text = user.email

        if(user.photoUrl != null) {
            Glide.with(this)
                .load(user.photoUrl)
                .fitCenter()
                .circleCrop()
                .into(binding.imageViewProfilePic)
        }
    }

    private fun subscribeToDoubts() {
        val doubtsList = mutableListOf<Doubt>()
        val doubtsAdapter = DoubtsAdapter(doubtsList)
        binding.rvDoubts.adapter = doubtsAdapter
        binding.rvDoubts.layoutManager = LinearLayoutManager(this)

        doubtsCollectionRef.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            firebaseFirestoreException?.let {
                if (auth.currentUser != null) { // checking auth because if the we logout this will give permission error
                    makeToast(it.message ?: "Unknown Message")
                }
                return@addSnapshotListener
            }

            querySnapshot?.let {
                doubtsList.clear()
                for(document in it.documents) {
                    val doubt = document.toObject<Doubt>()
                    doubt?.let {
                        doubtsList.add(it)
                    }
                }

                doubtsAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun menuItemListener (menuItem: MenuItem) : Boolean {
        when(menuItem.itemId) {
            R.id.menu_logout -> signOut()
        }

        return true
    }

    private fun signOut () = CoroutineScope(Dispatchers.IO).launch {
        auth.signOut()
        viewModel.disconnectUser()
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