package com.sahil_admin.codedoubt.authenticator

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.ktx.Firebase
import com.sahil_admin.codedoubt.R
import com.sahil_admin.codedoubt.Utility
import com.sahil_admin.codedoubt.Utility.makeToast
import com.sahil_admin.codedoubt.objects.AuthUser
import com.sahil_admin.codedoubt.repository.FirebaseRepository
import com.sahil_admin.codedoubt.ui.DashboardActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object FirebaseAuthenticator : Authenticator {

    val auth = Firebase.auth

    override fun AppCompatActivity.googleSignIn() {
        val signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // Your server's client ID, not your Android client ID.
                    .setServerClientId(getString(R.string.default_web_client_id))
                    // Only show accounts previously used to sign in.
                    .setFilterByAuthorizedAccounts(true)
                    .build())
            .build()

        val signInClient = Identity.getSignInClient(this)
        signInClient.beginSignIn(signInRequest).addOnSuccessListener {
            try {
                startIntentSenderForResult(
                    it.pendingIntent.intentSender,
                    Utility.REQUEST_CODE_GOOGLE_ONE_TAP,
                    null, 0, 0, 0)

            } catch (e: Exception) {
                makeToast(e.message.toString())

            }

        }.addOnFailureListener {
            makeToast("Sign In failed")
        }
    }

    override suspend fun AppCompatActivity.emailPasswordSignIn
                (email: String, password: String) = suspendCoroutine { continuation ->

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    continuation.resume(true)

                } else {
                    // Account doesn't exist
                    makeToast("Account does not exists or password does not match!.")
                    continuation.resume(false)
                }
            }
    }

    override suspend fun AppCompatActivity.emailPasswordSignUp
                (email: String, password: String) = suspendCoroutine {continuation ->

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    continuation.resume(true)

                } else {
                    // If sign in fails, display a message to the user.
                    makeToast("User already exists, please sign in")
                    continuation.resume(false)
                }
            }
    }

    override fun AppCompatActivity.profileSetUp (name: String?) = CoroutineScope(Dispatchers.IO).launch {
        val auth = Firebase.auth
        val user = auth.currentUser!!

        if(name != null) {
            suspendCoroutine { continuation ->
                user.updateProfile(userProfileChangeRequest {
                    displayName = name
                }).addOnSuccessListener { continuation.resume(Unit) }
            }
        }

        if(FirebaseRepository.containsUser(user.email!!)) {
            val uniqueId = UUID.randomUUID()
            val authUser = AuthUser(uniqueId.toString(),
                user.email.toString(),
                user.displayName.toString(),
                0,
                mutableListOf())

            FirebaseRepository.addUser(authUser)
        }

        withContext(Dispatchers.Main) {
            startActivity(Intent(this@profileSetUp, DashboardActivity::class.java))
            finish()
        }
    }

    override fun AppCompatActivity.profileCheck () = CoroutineScope(Dispatchers.IO).launch {
        val user = auth.currentUser!!

        if(!FirebaseRepository.containsUser(user.email!!)) {
            withContext(Dispatchers.Main) {
                makeToast("User not found, please signUp first")
            }

        } else {
            withContext(Dispatchers.Main) {
                startActivity(Intent(this@profileCheck, DashboardActivity::class.java))
                finish()
            }
        }
    }

    override fun AppCompatActivity.googleAuthForFirebase(data: Intent?, signUp: Boolean) {
        val signInClient = Identity.getSignInClient(this)
        val credential = signInClient.getSignInCredentialFromIntent(data)
        val idToken = credential.googleIdToken
        when {
            idToken != null -> {
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(firebaseCredential)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Sign in success, update UI with the signed-in user's information
                            if(signUp) profileSetUp()
                            else profileCheck()

                        } else {
                            // If sign in fails, display a message to the user.
                            makeToast("SignIn failed!")
                        }
                    }
            }
            else -> {
                // Shouldn't happen.
                makeToast("API error, please try again")
            }
        }
    }
}