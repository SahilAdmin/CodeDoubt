package com.sahil_admin.codedoubt.ui.auth

import FirebaseAuthenticator.emailPasswordSignUp
import FirebaseAuthenticator.googleAuthForFirebase
import FirebaseAuthenticator.googleSignIn
import FirebaseAuthenticator.profileSetUp
import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.widget.addTextChangedListener
import com.google.android.gms.auth.api.identity.Identity
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

class   SignupActivity : AppCompatActivity() {

    private val TAG = "LOG_Signup_Activity"
    private val binding by lazy { ActivitySignupBinding.inflate(layoutInflater) }

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
//        binding.etReEnterPassword.addTextChangedListener { binding.tvWrongRePassword.visibility = View.GONE }
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
        val email = binding.etEnterEmail.text.toString()
        val password = binding.etEnterPassword.text.toString()
        val name = binding.etEnterName.text.toString()
//        val rePassword = binding.etReEnterPassword.text.toString()

        if (!validateEmailPassword(email, password, name)) return

        // Validated everything
        CoroutineScope(Dispatchers.IO).launch {
            if (emailPasswordSignUp(email, password)) {
                profileSetUp(name)
            }
        }
    }

    private fun validateEmailPassword (email: String, password: String, name: String): Boolean {
        if(!isValidEmail(email)) {binding.tvWrongEmail.visibility = View.VISIBLE; return false}
        if(!isValidPassword(password)) {binding.tvWrongPassword.visibility = View.VISIBLE; return false}
        if(name.trim().isEmpty()) {binding.tvWrongPassword.visibility = View.VISIBLE; return false}

        return true
    }
}