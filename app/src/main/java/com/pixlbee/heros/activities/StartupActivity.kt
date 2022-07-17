package com.pixlbee.heros.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.elevation.SurfaceColors
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.pixlbee.heros.R


class StartupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        } catch (e: Exception){
            Log.e("Error", "Persistence cannot be enabled")
        }

        FirebaseDatabase.getInstance().getReference("boxes").keepSynced(true)
        FirebaseDatabase.getInstance().getReference("items").keepSynced(true)
        FirebaseDatabase.getInstance().getReference("write_users").keepSynced(true)
        FirebaseDatabase.getInstance().getReference("read_users").keepSynced(true)

        Log.e("Error", FirebaseAuth.getInstance().currentUser?.getUid().toString())

        window.navigationBarColor = SurfaceColors.SURFACE_2.getColor(this);
        val user: FirebaseUser? = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val sharedPreferences: SharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("current_user", user?.email.toString().replaceAfter("@", "").replace("@", ""))
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