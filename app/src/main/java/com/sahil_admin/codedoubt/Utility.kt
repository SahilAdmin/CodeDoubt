package com.sahil_admin.codedoubt

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

object Utility {
    fun isValidEmail (str: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(str).matches()
    }

    fun isValidPassword (str: String): Boolean {
        return str.length >= 10
    }

    const val REQUEST_CODE_GOOGLE_ONE_TAP = 100

    fun AppCompatActivity.makeToast (str: String) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show()
    }
}
