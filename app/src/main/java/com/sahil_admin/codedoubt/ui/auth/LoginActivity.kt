package com.sahil_admin.codedoubt.ui.auth

import com.sahil_admin.codedoubt.authenticator.FirebaseAuthenticator.emailPasswordSignIn
import com.sahil_admin.codedoubt.authenticator.FirebaseAuthenticator.googleAuthForFirebase
import com.sahil_admin.codedoubt.authenticator.FirebaseAuthenticator.googleSignIn
import com.sahil_admin.codedoubt.authenticator.FirebaseAuthenticator.profileCheck
import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.widget.addTextChangedListener
import com.google.android.gms.common.api.ApiException
import com.sahil_admin.codedoubt.Utility.REQUEST_CODE_GOOGLE_ONE_TAP
import com.sahil_admin.codedoubt.Utility.isValidEmail
import com.sahil_admin.codedoubt.Utility.isValidPassword
import com.sahil_admin.codedoubt.databinding.ActivityLoginBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private val binding by lazy {ActivityLoginBinding.inflate(layoutInflater)}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        subscribeViews()
    }

    private fun subscribeViews() {

        binding.buttonSignUp.setOnClickListener { startActivity(Intent(this, SignupActivity::class.java)) }
        binding.buttonLogin.setOnClickListener {loginClicked() }
        binding.imageViewGoogle.setOnClickListener { googleSignIn() }
        binding.etEnterEmail.addTextChangedListener { binding.tvWrongEmail.visibility = View.GONE }
        binding.etEnterPassword.addTextChangedListener { binding.tvWrongPassword.visibility = View.GONE }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_GOOGLE_ONE_TAP -> {
                try {
                    googleAuthForFirebase(data, false)

                } catch (e: ApiException) {
                    // closed the dialog without signing in
                }
            }
        }
    }

    private fun loginClicked() {
        val email = binding.etEnterEmail.text.toString()
        val password = binding.etEnterPassword.text.toString()

        if(!validateEmailPassword(email, password)) return

        // Validated everything
        CoroutineScope(Dispatchers.IO).launch {
            if (emailPasswordSignIn(email, password)) {
                profileCheck()
            }
        }
    }

    private fun validateEmailPassword (email: String, password: String): Boolean {
        if(!isValidEmail(email)) {binding.tvWrongEmail.visibility = View.VISIBLE; return false}
        if(!isValidPassword(password)) {binding.tvWrongPassword.visibility = View.VISIBLE; return false}

        return true
    }
}