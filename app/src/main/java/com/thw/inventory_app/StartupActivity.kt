package com.thw.inventory_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.elevation.SurfaceColors
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase


class StartupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getWindow().setNavigationBarColor(SurfaceColors.SURFACE_2.getColor(this));

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            startActivity(Intent(this, MainActivity::class.java))
            Log.e("Error", "is logged in")
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            Log.e("Error", "is NOT loggeed in")
        }
        finish()
    }
}