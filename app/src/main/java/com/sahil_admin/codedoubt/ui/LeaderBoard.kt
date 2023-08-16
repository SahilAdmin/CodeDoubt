package com.sahil_admin.codedoubt.ui

import FirebaseAuthenticator.auth
import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.SearchView
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.sahil_admin.codedoubt.R
import com.sahil_admin.codedoubt.Utility.makeToast
import com.sahil_admin.codedoubt.adapters.LeaderBoardAdapter
import com.sahil_admin.codedoubt.databinding.ActivityLeaderboardBinding
import com.sahil_admin.codedoubt.objects.AuthUser
import com.sahil_admin.codedoubt.objects.Doubt
import kotlinx.coroutines.launch
import okhttp3.internal.notify
import java.util.Collections
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LeaderBoard : AppCompatActivity(), LeaderBoardAdapter.LeaderBoardRVListener {

    private val viewModel by viewModels<LeaderboardViewModel>()
    private val userCollectionRef = Firebase.firestore.collection("Users")
    private val binding by lazy { ActivityLeaderboardBinding.inflate(layoutInflater) }
    private val leaders = mutableListOf<AuthUser>()
    private val upvotes = mutableListOf<String?>()
    private var search: String? = null
    private var rvAdapter: LeaderBoardAdapter? = null

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

                    is LeaderboardViewModel.UpvoteEvent.Error -> makeToast("Error: ${it.error}")
                }
            }
        }
    }

    private fun subscribeToRV () {

        rvAdapter = LeaderBoardAdapter(leaders, upvotes, this)

        binding.leaderBoardRV.layoutManager = LinearLayoutManager(this)
        binding.leaderBoardRV.adapter = rvAdapter

        userCollectionRef.addSnapshotListener { _, firebaseFirestoreException ->
            firebaseFirestoreException?.let {
                if (auth.currentUser == null) { // User logged out before query completion
                    makeToast(it.message ?: "Unknown Message")
                }

                return@addSnapshotListener
            }


            lifecycleScope.launch {
                val querySnapshot = suspendCoroutine { continuation ->
                    userCollectionRef
                        .orderBy("upvotes", Query.Direction.DESCENDING).get()
                        .addOnCompleteListener {
                            continuation.resume(it.result)
                        }
                }

                querySnapshot?.let {
                    leaders.clear()
                    for (document in it.documents) {
                        val user = document.toObject<AuthUser>()

                        user?.let {
                            if (it.email!! == auth.currentUser!!.email && it.upvoted_list != null) {
                                upvotes.clear()
                                for (str in it.upvoted_list!!) upvotes.add(str)
                            }

                            if(search != null && !search!!.isEmpty()) {
                                if(it.name!!.uppercase().contains(search!!.uppercase())) {
                                    leaders.add(it)
                                } else {
                                    // do nothing
                                }
                            } else leaders.add(it)
                        }
                    }

                    rvAdapter?.let {
                        it.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    private fun searchAccordingToString () {
        lifecycleScope.launch {
            val querySnapshot = suspendCoroutine { continuation ->
                userCollectionRef
                    .orderBy("upvotes", Query.Direction.DESCENDING).get()
                    .addOnCompleteListener {
                        continuation.resume(it.result)
                    }
            }

            querySnapshot?.let {
                leaders.clear()
                for (document in it.documents) {
                    val user = document.toObject<AuthUser>()

                    user?.let {
                        if (it.email!! == auth.currentUser!!.email && it.upvoted_list != null) {
                            upvotes.clear()
                            for (str in it.upvoted_list!!) upvotes.add(str)
                        }

                        if(search != null && !search!!.isEmpty()) {
                            if(it.name!!.uppercase().contains(search!!.uppercase())) {
                                leaders.add(it)
                            } else {
                                var str = it.name!!.toString()
                                str += "sahil"
                                val e = 101
                                // do nothing
                            }
                        } else leaders.add(it)
                    }
                }

                rvAdapter?.let {
                    it.notifyDataSetChanged()
                }
            }
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