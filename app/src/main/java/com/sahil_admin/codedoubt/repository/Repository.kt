package com.sahil_admin.codedoubt.repository

import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.rpc.context.AttributeContext.Auth
import com.sahil_admin.codedoubt.authenticator.FirebaseAuthenticator
import com.sahil_admin.codedoubt.objects.AuthUser
import com.sahil_admin.codedoubt.objects.Doubt
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

interface Repository {

    suspend fun updateUser (email: String, newName: String)

    suspend fun getUser (email: String) : AuthUser

    suspend fun addDoubt (doubt: Doubt)

    suspend fun containsUser (email: String) : Boolean

    suspend fun addUser (authUser: AuthUser)

    suspend fun upvote (currentEmail: String, otherEmail: String)

    suspend fun getSortedUserList (search: String?) : MutableList<AuthUser>

    suspend fun getUpvotedList (email: String) : MutableList<String?>

    suspend fun getDoubtList () : MutableList<Doubt>
}