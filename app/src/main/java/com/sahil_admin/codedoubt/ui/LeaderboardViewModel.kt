package com.sahil_admin.codedoubt.ui

import FirebaseAuthenticator.auth
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.sahil_admin.codedoubt.objects.AuthUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LeaderboardViewModel : ViewModel() {

    private val _upvoteEvent = MutableSharedFlow<UpvoteEvent>()
    val upvoteEvent = _upvoteEvent.asSharedFlow()

    private val userCollectionRef = Firebase.firestore.collection("Users")

    fun upvoteUser (email: String) {
        val currentUser = auth.currentUser

        viewModelScope.launch(Dispatchers.IO) {
            if(currentUser!!.email == email) {
                _upvoteEvent.emit(UpvoteEvent.Error("Can't Upvote yourself"))
                return@launch
            }

            val querySnapshot = suspendCoroutine { continuation ->
                userCollectionRef.whereEqualTo("email", currentUser?.email).get().addOnCompleteListener {
                    continuation.resume(it.result)
                }
            }

            val user = querySnapshot.documents[0].toObject(AuthUser::class.java)

            if(user?.upvoted_list == null) {
                user?.upvoted_list = mutableListOf<String?>()
            }

            if((user?.upvoted_list?.contains(email)) == false) {
                user.upvoted_list!!.add(email)

                val querySnapshot = suspendCoroutine { continuation ->
                    userCollectionRef.whereEqualTo("email", email).get().addOnCompleteListener {
                        continuation.resume(it.result)
                    }
                }

                val user2 = querySnapshot.documents[0].toObject(AuthUser::class.java)
                if(user2?.upvotes == null) {
                    user2?.upvotes = 0
                }

                if(user2?.upvotes != null) {
                    user2?.upvotes = user2?.upvotes!! + 1
                }

                suspendCoroutine <Unit> {continuation ->
                    userCollectionRef.document(querySnapshot.documents[0].id).set(
                        user2!!
                    ).addOnCompleteListener {
                        continuation.resume(Unit)
                    }
                }

            } else {
                if (user != null) {
                    user.upvoted_list!!.remove(email)
                }

                val querySnapshot = suspendCoroutine { continuation ->
                    userCollectionRef.whereEqualTo("email", email).get().addOnCompleteListener {
                        continuation.resume(it.result)
                    }
                }

                val user2 = querySnapshot.documents[0].toObject(AuthUser::class.java)
                if(user2?.upvotes == null) {
                    user2?.upvotes = 0
                }

                if(user2?.upvotes != null) {
                    user2?.upvotes = user2?.upvotes!! - 1
                }

                suspendCoroutine <Unit> {continuation ->
                    userCollectionRef.document(querySnapshot.documents[0].id).set(
                        user2!!
                    ).addOnCompleteListener {
                        continuation.resume(Unit)
                    }
                }
            }

            suspendCoroutine { continuation ->
                userCollectionRef.document(querySnapshot.documents[0].id).set(
                    user!!
                ).addOnCompleteListener {
                    continuation.resume(Unit)
                }
            }

            _upvoteEvent.emit(UpvoteEvent.Success)
        }
    }

    sealed class UpvoteEvent {
        object Success: UpvoteEvent()

        class Error(val error: String): UpvoteEvent()
    }

    fun func() {


    }
}