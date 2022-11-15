package com.sahil_admin.codedoubt

import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat.startIntentSenderForResult
import androidx.core.content.ContextCompat.startActivity
import androidx.core.widget.addTextChangedListener
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.sahil_admin.codedoubt.Utility.REQUEST_CODE_GOOGLE_ONE_TAP
import com.sahil_admin.codedoubt.Utility.isValidEmail
import com.sahil_admin.codedoubt.Utility.isValidPassword
import com.sahil_admin.codedoubt.Utility.makeToast
import com.sahil_admin.codedoubt.databinding.ActivitySignupBinding
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SignupActivity : AppCompatActivity() {

    private val TAG = "LOG_Signup_Activity"
    private val binding by lazy { ActivitySignupBinding.inflate(layoutInflater) }
    private val userCollectionRef = Firebase.firestore.collection("Users")
    private val auth = Firebase.auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(binding.root)

        subscribeViews()
    }

    fun subscribeViews() {
        super.onStart()

        binding.buttonLogin.setOnClickListener { startActivity(Intent(this, LoginActivity::class.java)) }
        binding.buttonRegister.setOnClickListener { registerClicked() }
        binding.imageViewGoogle.setOnClickListener { googleSignIn() }
        binding.etEnterEmail.addTextChangedListener { binding.tvWrongEmail.visibility = View.GONE }
        binding.etEnterPassword.addTextChangedListener { binding.tvWrongPassword.visibility = View.GONE }
        binding.etReEnterPassword.addTextChangedListener { binding.tvWrongRePassword.visibility = View.GONE }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_GOOGLE_ONE_TAP -> {
                try {
                    googleAuthForFirebase(data)

                } catch (e: Exception) {
                    // closed dialog without signing in
                }
            }
        }
    }

    private fun registerClicked() {
        val email = binding.tvYourEmail.text.toString()
        val password = binding.tvYourPassword.text.toString()
        val rePassword = binding.tvReEnterPassword.text.toString()

        if(!validateEmailPassword(email, password, rePassword)) return

        // Validated everything
        emailPasswordSignIn(email, password)
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
                            profileSetUp()

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

    private fun emailPasswordSignIn (email: String, password: String) {
        val auth = Firebase.auth

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    profileSetUp()

                } else {
                    // If sign in fails, display a message to the user.
                    makeToast("User already exists, please sign in")
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
                    .setFilterByAuthorizedAccounts(false)
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

    private fun profileSetUp () = CoroutineScope(Dispatchers.IO).launch {
        val auth = Firebase.auth
        val user = auth.currentUser!!

        val querySnapshot = suspendCoroutine { continuation ->
            userCollectionRef.whereEqualTo("email", user.email).get().addOnCompleteListener {
                continuation.resume(it.result)
            }
        }

        if(querySnapshot.documents.isEmpty()) {
            suspendCoroutine { continuation ->
                userCollectionRef.add(
                    User(user.email.toString(), user.displayName.toString(), 0, 0)

                ).addOnCompleteListener { continuation.resume(Unit) }
            }
        }

        withContext(Dispatchers.Main) {
            startActivity(Intent(this@SignupActivity, DashboardActivity::class.java))
            finish()
        }
    }

    private fun validateEmailPassword (email: String, password: String, re_password: String): Boolean {
        if(!isValidEmail(email)) {binding.tvWrongEmail.visibility = View.VISIBLE; return false}
        if(!isValidPassword(password)) {binding.tvWrongPassword.visibility = View.VISIBLE; return false}
        if(password != re_password) {binding.tvWrongRePassword.visibility = View.VISIBLE; return false}

        return true
    }
}