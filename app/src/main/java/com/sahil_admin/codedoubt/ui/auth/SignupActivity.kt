package com.sahil_admin.codedoubt.ui.auth

import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.widget.addTextChangedListener
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.sahil_admin.codedoubt.R
import com.sahil_admin.codedoubt.objects.AuthUser
import com.sahil_admin.codedoubt.Utility.REQUEST_CODE_GOOGLE_ONE_TAP
import com.sahil_admin.codedoubt.Utility.isValidEmail
import com.sahil_admin.codedoubt.Utility.isValidPassword
import com.sahil_admin.codedoubt.Utility.makeToast
import com.sahil_admin.codedoubt.databinding.ActivitySignupBinding
import com.sahil_admin.codedoubt.ui.DashboardActivity
import kotlinx.coroutines.*
import java.util.*
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

    // signin dialog builder
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
                val uniqueId = UUID.randomUUID()
                userCollectionRef.add(
                    AuthUser(uniqueId.toString(), user.email.toString(), user.displayName.toString(), 0, 0)

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

/*
VIEW_MODEL ->

implementation 'androidx.activity:activity-ktx:1.5.1' // for by viewModels (for activities)
implementation 'androidx.fragment:fragment-ktx:1.5.4' // for by activityViewModels (for fragments)

To share data we can use LiveData, StateFlow, SharedFlow, Flows

LiveData is lifecycle aware, i.e. it will only observe when the activity it is observing for is in the
foreground, we have to provide context (View) for livedata

A flow on the other hand is like a coroutine which can return multiple values (of the same type)
Flows can be of 2 types hot flow and a cold flow

Hot flow / hot stream is a flow which emits data irrespective of whether there are collectors listening to it or not
Whereas a Cold flow is a flow which emits data only when there is a collector listening to it

SharedFlow and StateFlow are hot flows.

// Syntax to make a flow
val countDownFlow = flow<Int> {
    val startingValue = 10;
    var currentValue = startingValue
    emit(currentValue)

    while(currentValue > 0) {
        delay(1000L)
        currentValue--
        emit(currentValue)
    }
}

private fun initiateCountdown() = viewModelScope.launch {
    countDownFlow.collect {
        println("The value is $it")
    }
} // This function will complete as soon as the countdown completes or if the ViewModel is destroyed
  // since it is in the viewModelScope


SharedFlow and StateFlow should only called from lifeCycleScope.launchWhenStarted or from the
repeatOnLifeCycle(LifeCycle.State.STARTED) block since a flow is not lifecycle aware and if we call
it from the lifeCycleScope.launch block it will continue observing even when the activity is in the
background.

The main difference between sharedFlow and a stateFlow is that a sharedFlow is used to emit a single
event whereas a stateFlow is used to observe like a LiveData.
StateFlow requires a default value which is emitted as soon the observer is connected whereas with
SharedFlow we do not require a default value

Under the hood StateFlow is a SharedFlow which never completes.

private val _stateFlow = MutableStateFlow(1)
val stateFlow = _stateFlow.asStateFlow()

private val _sharedFlow = MutableSharedFlow<Int>(replay = 10)
val sharedFlow = _sharedFlow.asSharedFlow()

Here the replay value in the shared flow is the number of emissions we want to save so that
new observer can observe these emits. Since, if there are no observers of sharedFlow during the time
of emit then nothing will happen.

fun incrementFlow () {
    viewModelScope.launch {
        delay(2000L)
        _stateFlow.value ++
    }
}

fun incrementLiveData () {
    viewModelScope.launch {
        delay(2000L)
        _liveData.value = _liveData.value!! + 1
    }
}

private fun <T> AppCompatActivity.collectStateFlow (flow: Flow<T>, collect: suspend (T) -> Unit) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect(collect)
        }
    }
} // extension function to shorten the code

collectStateFlow(viewModel.stateFlow) {
        Snackbar.make(
            binding.root,
            "Hi",
            Snackbar.LENGTH_LONG
        ).show()
        Log.d(TAG, "onCreate: $it")
    }
*/