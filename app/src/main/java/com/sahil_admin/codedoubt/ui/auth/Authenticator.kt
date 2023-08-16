package com.sahil_admin.codedoubt.ui.auth

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.sahil_admin.codedoubt.R
import com.sahil_admin.codedoubt.Utility
import com.sahil_admin.codedoubt.Utility.makeToast
import com.sahil_admin.codedoubt.objects.AuthUser
import com.sahil_admin.codedoubt.ui.DashboardActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

interface Authenticator {

    fun AppCompatActivity.googleSignIn()

    suspend fun AppCompatActivity.emailPasswordSignIn(email: String, password: String) : Boolean

    suspend fun AppCompatActivity.emailPasswordSignUp(email: String, password: String) : Boolean

    fun AppCompatActivity.profileSetUp(name: String? = null) : Job

    fun AppCompatActivity.profileCheck() : Job

    fun AppCompatActivity.googleAuthForFirebase(data: Intent?)
}