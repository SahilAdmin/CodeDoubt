package com.sahil_admin.codedoubt.ui

import com.sahil_admin.codedoubt.authenticator.FirebaseAuthenticator.auth
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
import com.google.firebase.firestore.ktx.firestore
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
    private val doubtsCollectionRef = Firebase.firestore.collection("Doubts")
    private val binding by lazy { ActivityDashboardBinding.inflate(layoutInflater) }
    private val toggle by lazy {ActionBarDrawerToggle(this, binding.drawerLayout,
        R.string.open,
        R.string.close
    ) }
    private val viewModel by viewModels<DashboardViewModel>()
    private val doubtsList = mutableListOf<Doubt>()
    private val doubtsAdapter = DoubtsAdapter(doubtsList)

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

                    is DashboardViewModel.LogInEvent.Error ->
                        makeToast("Stream LogIn: ${it.e}");
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.logoutEvent.collect {
                when (it) {
                    is DashboardViewModel.LogOutEvent.Success ->
                        startActivity(Intent(this@DashboardActivity, MainActivity::class.java))

                    is DashboardViewModel.LogOutEvent.Error ->
                        makeToast("Stream LogOut: ${it.e}")
                }
            }
        }

        lifecycleScope.launchWhenStarted() {
            viewModel.channelCreateEvent.collect {
                when (it) {
                    is DashboardViewModel.ChannelCreateEvent.Success ->
                        makeToast("Channel Created Successfully")

                    is DashboardViewModel.ChannelCreateEvent.Error ->
                        makeToast("Error: ${it.e}")
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.doubtList.collect {
                doubtsList.clear()
                doubtsList.addAll(it)
                doubtsAdapter.notifyDataSetChanged()
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
            AlertDialog.Builder(this)
                .setMessage(R.string.about_us)
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
        binding.rvDoubts.adapter = doubtsAdapter
        binding.rvDoubts.layoutManager = LinearLayoutManager(this)

        doubtsCollectionRef.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            firebaseFirestoreException?.let {
                if (auth.currentUser != null) { // checking auth because if the we logout this will give permission error
                    makeToast(it.message ?: "Unknown Message")
                }
                return@addSnapshotListener
            }

            viewModel.getDoubts()
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

    override fun onBackPressed() {
        signOut()
    }
}