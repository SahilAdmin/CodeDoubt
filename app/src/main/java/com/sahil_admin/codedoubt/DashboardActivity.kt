package com.sahil_admin.codedoubt

import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.content.ContextCompat.startActivity
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.sahil_admin.codedoubt.databinding.ActivityDashboardBinding
import kotlin.math.sign


class DashboardActivity : AppCompatActivity() {

    private val auth = Firebase.auth
    private val binding by lazy { ActivityDashboardBinding.inflate(layoutInflater) }
    private val toggle by lazy {ActionBarDrawerToggle(this, binding.drawerLayout, R.string.open, R.string.close) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(binding.root)

        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState() // show that it's ready to be used

        binding.imageViewMenu.setOnClickListener {
            binding.drawerLayout.open()
        }

        val navigationView = binding.dashboardNavigationView

        navigationView.setNavigationItemSelectedListener {
            menuItemListener(it)
        }
    }

    override fun onStart() {
        super.onStart()

        val user = auth.currentUser!!

        binding.textViewProfilename.text = user.displayName
        binding.textViewProfileEmail.text = user.email

        if(user.photoUrl != null) {
            Glide.with(this)
                .load(user.photoUrl)
                .fitCenter()
                .circleCrop()
                .into(binding.imageViewProfilePic)
        }
    }

    private fun menuItemListener (menuItem: MenuItem) : Boolean {
        when(menuItem.itemId) {
            R.id.menu_logout -> signOut()
        }

        return true
    }

    private fun signOut () {
        auth.signOut()
        startActivity(Intent(this, MainActivity::class.java))
    }
}

