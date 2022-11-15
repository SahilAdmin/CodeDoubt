package com.sahil_admin.codedoubt

import android.content.Intent
import android.content.IntentSender
import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.app.ActivityCompat.startIntentSenderForResult
import androidx.core.content.ContextCompat.startActivity
import androidx.core.widget.addTextChangedListener
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.sahil_admin.codedoubt.Utility.REQUEST_CODE_GOOGLE_ONE_TAP
import com.sahil_admin.codedoubt.Utility.isValidEmail
import com.sahil_admin.codedoubt.Utility.isValidPassword
import com.sahil_admin.codedoubt.Utility.makeToast
import com.sahil_admin.codedoubt.databinding.ActivityLoginBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LoginActivity : AppCompatActivity() {

    private val binding by lazy {ActivityLoginBinding.inflate(layoutInflater)}
    private val auth = Firebase.auth
    private val userCollectionRef = Firebase.firestore.collection("Users")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        subscribeViews()
    }

    private fun subscribeViews() {
        super.onStart()

        binding.buttonSignUp.setOnClickListener { startActivity(Intent(this, SignupActivity::class.java)) }
        binding.buttonLogin.setOnClickListener { loginClicked() }
        binding.imageViewGoogle.setOnClickListener { googleSignIn() }
        binding.etEnterEmail.addTextChangedListener { binding.tvWrongEmail.visibility = View.GONE }
        binding.etEnterPassword.addTextChangedListener { binding.tvWrongPassword.visibility = View.GONE }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_GOOGLE_ONE_TAP -> {
                try {
                    googleAuthForFirebase(data)

                } catch (e: ApiException) {
                    // closed the dialog without signing in
                }
            }
        }
    }

    private fun googleAuthForFirebase(data: Intent?) {
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
                            profileCheck()

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

    private fun googleSignIn () {

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
                    REQUEST_CODE_GOOGLE_ONE_TAP,
                    null, 0, 0, 0)

            } catch (e: Exception) {
                makeToast(e.message.toString())

            }

        }.addOnFailureListener {
            makeToast("Sign In failed")
        }
    }

    private fun loginClicked() {
        val email = binding.tvYourEmail.text.toString()
        val password = binding.tvYourPassword.text.toString()

        if(!validateEmailPassword(email, password)) return

        // Validated everything
        emailPasswordSignIn(email, password)
    }

    private fun emailPasswordSignIn (email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    profileCheck()

                } else {
                    // Account doesn't exist
                    makeToast("Account does not exists or password does not match!.")
                }
            }
    }

    private fun validateEmailPassword (email: String, password: String): Boolean {
        if(!isValidEmail(email)) {binding.tvWrongEmail.visibility = View.VISIBLE; return false}
        if(!isValidPassword(password)) {binding.tvWrongPassword.visibility = View.VISIBLE; return false}

        return true
    }

    private fun profileCheck () = CoroutineScope(Dispatchers.IO).launch {
        val user = auth.currentUser!!

        val querySnapshot = suspendCoroutine { continuation ->
            userCollectionRef.whereEqualTo("email", user.email).get().addOnCompleteListener {
                continuation.resume(it.result)
            }
        }

        if(querySnapshot.documents.isEmpty()) {
            withContext(Dispatchers.Main) {
                makeToast("User not found, please signUp first")
            }

        } else {
            withContext(Dispatchers.Main) {
                startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                finish()
            }
        }
    }
}