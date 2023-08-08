package com.sahil_admin.codedoubt.ui

import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

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

        subscribeToImage()
        subscribeToViewModel()
        subscribeToViews()
        subscribeToDoubts()
    }

    private fun subscribeToViewModel () {
        lifecycleScope.launchWhenStarted {
            viewModel.loginEvent.collect {
                when (it) {
                    is DashboardViewModel.LogInEvent.Success -> {
                        Log.d(TAG, "onCreate: Successfully LoggedIn")

                        binding.textViewProfilename.text = it.user.name!!.toString()
                        binding.textViewNoOfUpvotes.text = it.user.upvotes!!.toString()
                        binding.textViewProfileEmail.text = it.user.email!!.toString()
                    }

                    is DashboardViewModel.LogInEvent.Error -> makeToast("Stream LogIn: ${it.e}");
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.logoutEvent.collect {
                when (it) {
                    is DashboardViewModel.LogOutEvent.Success -> startActivity(Intent(this@DashboardActivity, MainActivity::class.java))

                    is DashboardViewModel.LogOutEvent.Error -> makeToast("Stream LogOut: ${it.e}")
                }
            }
        }

        lifecycleScope.launchWhenStarted() {
            viewModel.channelCreateEvent.collect {
                when (it) {
                    is DashboardViewModel.ChannelCreateEvent.Success -> makeToast("Channel Created Successfully")

                    is DashboardViewModel.ChannelCreateEvent.Error -> makeToast("Error: ${it.e}")
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

        binding.imageView3.setOnClickListener {
            AlertDialog.Builder(this).setMessage(R.string.about_us)
                .create()
                .show()
        }
    }

    private fun subscribeToImage() {
        val user = auth.currentUser!!

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

            R.id.menu_topSolvers -> startActivity(Intent(this, LeaderBoard::class.java))

            R.id.menu_settings -> {
                ChangeNameDialog().show(supportFragmentManager, "Update Username")
            }

            R.id.menu_aboutus -> {
                AlertDialog.Builder(this).setMessage(R.string.about_us)
                    .create()
                    .show()
            }
        }

        return true
    }

    private fun signOut () = CoroutineScope(Dispatchers.IO).launch {
        auth.signOut()
        viewModel.disconnectUser()
    }
}

/*
We can apply scope function on every object

Scope functions -
https://kotlinlang.org/docs/scope-functions.html

let -> it -> lambda result
run -> this -> lambda result
run -> - -> lambda result ->
with -> this -> lambda result
apply -> this -> context object
also -> it -> context object

// LET ->
We use let to do null checks
private int x: Int? = 10

val x = nullable?.let {
    val number = it + 1 // this will be treated as Int (not Int?) since it captures the last value before coming into let block
    number // returns the last line

} ?: 3 // it at the time of checking the value was null then this will be returned

// ALSO ->
Also is similar to let, only difference is that it returns the object it is called upon rather than the last line
We use also when we need to perform two operations in a single line, thus the word 'also'

val x = 10

fun squareAndIncrement = (x * x).also { i++ }
this line will return x * x and 'also' increment i

// APPLY ->
Apply is used to get a this reference so that we can don't need to append it for every function
It's like working in a function inside the object

val intent = Intent().also {
    putExtra ("" , "") // putStringExtra
    putExtra ("" , 1)  // putIntExtra
}

// RUN ->
Run is same as apply but we it doesn't return the object after completion
val intent = Intent().also {
    putExtra ("" , "") // putStringExtra
    putExtra ("" , 1)  // putIntExtra
    this
}

// WITH ->
With is same run the difference is of syntax
with (Intent()) {
    putExtra ("" , "") // putStringExtra
    putExtra ("" , 1)  // putIntExtra
    this
}
*/

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
DAGGER HILT - DEPENDENCY INJECTION

plugins {
    ..

    // dagger hilt
    id 'kotlin-kapt'
    id 'com.google.dagger.hilt.android'
}

// Dagger hilt dependency
implementation "com.google.dagger:hilt-android:2.44"
kapt "com.google.dagger:hilt-compiler:2.44"

// Dagger hilt -> Allow references to generated code
kapt {
    correctErrorTypes true
}

IN ROOT GRADLE FILE ->
plugins {
    ...

    id 'com.google.dagger.hilt.android' version '2.44' apply false // dagger hilt dependency
}

MAKE APPLICATION CLASS-------------------------
@HiltAndroidApp
class MyApp: Application() {
}

specify the application name in the manifest:
    android:name=".MyApp"

MAKE MODULE------------------------------------
@Module // to declare a module (A module provides dependencies)
@InstallIn(SingletonComponent::class) // defines the scope of dependencies
object AppModule {

    @Provides // so that dagger knows that this function provides dependency
    @Singleton // so that only one instance is created throughout the lifetime of application
    fun provideObject(
        @Named("str1") str: String // if returning class has constructor we need to inject those parameters
    ) : MyObject {
        return MyObject(str)
    }

    @Provides @Singleton @Named("str1")
    fun provideString1() = "Sahil"

    @Provides @Singleton @Named("str2")
    fun provideString2() = "Mahajan"
}

COMPONENTS -> DEFINES THE LIFECYCLE OF INJECTED DEPENDENCIES
// Singleton Component -> As long as application lives
// ViewModel Component -> View Model
// Activity Component -> Lives as long as the activity, destroyed in onStop method
// Activity Retained Component -> Lives across configuration changes created in onCreate till onDestroy
// Fragment Component -> Lives till the fragment lifecycle it is injected in

For providing abstraction ->
Here we can use abstract function which is not possible in object Module where we have to provide full
implementation details if we want to provide an interface or a class.
This enables dagger to generate less code

@Module
@InstallIn(SingletonComponent::class)
abstract class AbstractAppModule {
    @Binds
    @Singleton
    abstract fun provideObject (myObject: MyObject) : Object // Object is an interface
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