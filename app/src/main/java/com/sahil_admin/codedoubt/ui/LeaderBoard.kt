package com.sahil_admin.codedoubt.ui

import com.sahil_admin.codedoubt.authenticator.FirebaseAuthenticator.auth
import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.SearchView
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.sahil_admin.codedoubt.Utility.makeToast
import com.sahil_admin.codedoubt.adapters.LeaderBoardAdapter
import com.sahil_admin.codedoubt.databinding.ActivityLeaderboardBinding
import com.sahil_admin.codedoubt.objects.AuthUser
import kotlinx.coroutines.launch

class LeaderBoard : AppCompatActivity(), LeaderBoardAdapter.LeaderBoardRVListener {

    private val viewModel by viewModels<LeaderboardViewModel>()
    private val userCollectionRef = Firebase.firestore.collection("Users")
    private val binding by lazy { ActivityLeaderboardBinding.inflate(layoutInflater) }
    private val leaders = mutableListOf<AuthUser>()
    private val upvotes = mutableListOf<String?>()
    private val rvAdapter = LeaderBoardAdapter(leaders, upvotes, this)
    private var search: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(binding.root)

        subscribeToViewModel()
        subscribeToRV()
        subscribeToSearch()
    }

    private fun subscribeToViewModel() {
        lifecycleScope.launchWhenStarted {
            viewModel.upvoteEvent.collect {

                when(it) {
                    is LeaderboardViewModel.UpvoteEvent.Success -> {}

                    is LeaderboardViewModel.UpvoteEvent.Error -> makeToast(it.error)
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.userList.collect {
                leaders.clear()
                leaders.addAll(it)
                rvAdapter!!.notifyDataSetChanged()
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.upvoteList.collect {
                upvotes.clear()
                upvotes.addAll(it)
                rvAdapter!!.notifyDataSetChanged()
            }
        }
    }

    private fun subscribeToRV () {

        binding.leaderBoardRV.layoutManager = LinearLayoutManager(this)
        binding.leaderBoardRV.adapter = rvAdapter

        userCollectionRef.addSnapshotListener { _, firebaseFirestoreException ->
            firebaseFirestoreException?.let {
                if (auth.currentUser == null) { // User logged out before query completion
                    makeToast(it.message ?: "Unknown Message")
                }

                return@addSnapshotListener
            }

            viewModel.updateLists(search)
        }
    }

    private fun searchAccordingToString () {
        lifecycleScope.launch {
            viewModel.updateLists(search)
        }
    }

    private fun subscribeToSearch () {
        binding.searchView.setOnQueryTextListener (object: SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(str: String?): Boolean {
                // do nothing
                return true
            }

            override fun onQueryTextChange(str: String?): Boolean {
                search = str
                searchAccordingToString()
                return true
            }
        })
    }

    override fun onFavoriteClicked(email: String) {
        viewModel.upvoteUser(email)
    }
}