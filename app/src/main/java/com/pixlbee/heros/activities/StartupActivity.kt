package com.pixlbee.heros.activities

import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.elevation.SurfaceColors
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase


class StartupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        FirebaseDatabase.getInstance().getReference("boxes").keepSynced(true)
        FirebaseDatabase.getInstance().getReference("items").keepSynced(true)
        FirebaseDatabase.getInstance().getReference("write_users").keepSynced(true)
        FirebaseDatabase.getInstance().getReference("read_users").keepSynced(true)

        Log.e("Error", FirebaseAuth.getInstance().currentUser?.getUid().toString())

        getWindow().setNavigationBarColor(SurfaceColors.SURFACE_2.getColor(this));
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }
}