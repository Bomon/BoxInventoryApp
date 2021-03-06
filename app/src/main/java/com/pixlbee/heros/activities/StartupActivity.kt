package com.pixlbee.heros.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.elevation.SurfaceColors
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser


class StartupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.navigationBarColor = SurfaceColors.SURFACE_2.getColor(this)
        val user: FirebaseUser? = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val sharedPreferences: SharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("current_user", user.email.toString().replaceAfter("@", "").replace("@", ""))
            editor.commit()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        finish()
    }
}