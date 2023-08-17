package com.sahil_admin.codedoubt.repository

import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.sahil_admin.codedoubt.objects.AuthUser
import com.sahil_admin.codedoubt.objects.Doubt
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object FirebaseRepository : Repository {

    private val userCollectionRef = Firebase.firestore.collection("Users")
    private val doubtCollectionRef = Firebase.firestore.collection("Doubts")

    private suspend fun getUserSnapshot (email: String) : QuerySnapshot {
        val querySnapshot = suspendCoroutine {continuation ->
            userCollectionRef.whereEqualTo("email", email).get().addOnCompleteListener {
                continuation.resume(it.result)
            }
        }

        return querySnapshot
    }

    private suspend fun updateUser (querySnapshot: QuerySnapshot, user: AuthUser) {
        suspendCoroutine { continuation ->
            userCollectionRef.document(querySnapshot.documents[0].id).set(
                user
            ).addOnCompleteListener {
                continuation.resume(Unit)
            }
        }
    }

    override suspend fun getUser(email: String): AuthUser {
        return getUserSnapshot(email).documents[0].toObject(AuthUser::class.java)!!
    }

    override suspend fun updateUser(email: String, newName: String) {
        val querySnapshot = getUserSnapshot(email)

        val user = querySnapshot.documents[0].toObject(AuthUser::class.java)!!

        user.name = newName

        updateUser(querySnapshot, user)
    }

    override suspend fun addUser (authUser: AuthUser) {
        suspendCoroutine { continuation ->
            userCollectionRef
                .add(authUser)
                .addOnCompleteListener {
                    continuation.resume(Unit)
                }
        }
    }

    override suspend fun containsUser(email: String): Boolean {
        val querySnapshot = getUserSnapshot(email)

        return querySnapshot.documents.isNotEmpty()
    }

    override suspend fun upvote (currentEmail: String, otherEmail: String) {
        val currentQuerySnapshot = getUserSnapshot(currentEmail)
        val otherQuerySnapshot = getUserSnapshot(otherEmail)
        val currentUser = currentQuerySnapshot.documents[0].toObject(AuthUser::class.java)!!
        val otherUser = otherQuerySnapshot.documents[0].toObject(AuthUser::class.java)!!

        val upvoted = currentUser.upvoted_list!!.contains(otherUser.email)

        if(upvoted) {
            currentUser.upvoted_list!!.remove(otherEmail)
            otherUser.upvotes = otherUser.upvotes!! - 1
        } else {
            currentUser.upvoted_list!!.add(otherEmail)
            otherUser.upvotes = otherUser.upvotes!! + 1
        }

        updateUser(currentQuerySnapshot, currentUser)
        updateUser(otherQuerySnapshot, otherUser)
    }

    override suspend fun addDoubt(doubt: Doubt) {
        suspendCoroutine { continuation ->
            doubtCollectionRef.add(doubt).addOnCompleteListener {
                continuation.resume(Unit)
            }
        }
    }

    override suspend fun getSortedUserList(search: String?) : MutableList<AuthUser> {
        val querySnapshot = suspendCoroutine { continuation ->
            userCollectionRef
                .orderBy("upvotes", Query.Direction.DESCENDING).get()
                .addOnCompleteListener {
                    continuation.resume(it.result)
                }
        }

        val ret = mutableListOf<AuthUser>()

        querySnapshot?.let { it ->
            for (document in it.documents) {
                val user = document.toObject<AuthUser>()

                user?.let {

                    if (!search.isNullOrEmpty()) {
                        if (it.name!!.uppercase().contains(search.uppercase())) {
                            ret.add(it)
                        } else {
                            // do nothing
                        }
                    } else ret.add(it)
                }
            }
        }

        return ret
    }

    override suspend fun getUpvotedList(email: String): MutableList<String?> {
        val user = getUser(email)
        return user.upvoted_list!!
    }

    override suspend fun getDoubtList(): MutableList<Doubt> {
        val querySnapshot = suspendCoroutine { continuation ->
            doubtCollectionRef
                .orderBy("question", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener {
                    continuation.resume(it.result)
                }
        }

        val ret = mutableListOf<Doubt>()

        querySnapshot?.let {
            for (document in it.documents) {
                val doubt = document.toObject<Doubt>()!!

                ret.add(doubt)
            }
        }

        return ret
    }
}