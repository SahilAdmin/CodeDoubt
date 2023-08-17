package com.sahil_admin.codedoubt.authenticator

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Job

interface Authenticator {

    fun AppCompatActivity.googleSignIn()

    suspend fun AppCompatActivity.emailPasswordSignIn(email: String, password: String) : Boolean

    suspend fun AppCompatActivity.emailPasswordSignUp(email: String, password: String) : Boolean

    fun AppCompatActivity.profileSetUp(name: String? = null) : Job

    fun AppCompatActivity.profileCheck() : Job

    fun AppCompatActivity.googleAuthForFirebase(data: Intent?, signUp: Boolean)
}