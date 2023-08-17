package com.sahil_admin.codedoubt.ui

import android.widget.MultiAutoCompleteTextView
import com.sahil_admin.codedoubt.authenticator.FirebaseAuthenticator.auth
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.sahil_admin.codedoubt.databinding.DialogChangeNameBinding
import com.sahil_admin.codedoubt.objects.AuthUser
import com.sahil_admin.codedoubt.repository.FirebaseRepository
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LeaderboardViewModel : ViewModel() {

    private val _upvoteEvent = MutableSharedFlow<UpvoteEvent>()
    val upvoteEvent = _upvoteEvent.asSharedFlow()

    private val _userList = MutableStateFlow(mutableListOf<AuthUser>())
    val userList = _userList.asStateFlow()

    private val _upvoteList = MutableStateFlow(mutableListOf<String?>())
    val upvoteList = _upvoteList.asStateFlow()

    fun upvoteUser (email: String) {
        val currentUser = auth.currentUser

        viewModelScope.launch(Dispatchers.IO) {

            if(currentUser!!.email == email) {
                _upvoteEvent.emit(UpvoteEvent.Error("Up-voting yourself is not allowed"))
                return@launch
            }

            FirebaseRepository.upvote(currentUser.email!!, email)

            _upvoteEvent.emit(UpvoteEvent.Success)
        }
    }

    fun updateLists(search: String?) {
        val currentUser = auth.currentUser!!

        viewModelScope.launch(Dispatchers.IO) {
            val userList = FirebaseRepository.getSortedUserList(search)
            val upvoteList = FirebaseRepository.getUpvotedList(currentUser.email!!)

            _userList.emit(userList)
            _upvoteList.emit(upvoteList)
        }
    }

    sealed class UpvoteEvent {
        object Success: UpvoteEvent()

        class Error(val error: String): UpvoteEvent()
    }
}