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

        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val versionCode = 5L //packageInfo.longVersionCode
        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val viewedChangelogVersion = sharedPreferences.getLong("changelog_version", 0)

        // On update or first install, show changelog view
        if (viewedChangelogVersion < versionCode) {
            val editor = sharedPreferences.edit()
            editor.putLong("changelog_version", versionCode)
            editor.commit()
            displayChangeLog()
        } else { // else goto main or login
            window.navigationBarColor = SurfaceColors.SURFACE_2.getColor(this)
            val user: FirebaseUser? = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                val sharedPreferences: SharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putString("current_user", user.email.toString().replaceAfter("@", "").replace("@", ""))
                editor.commit()
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
            }
        }

        finish()
    }

    private fun displayChangeLog(){
        startActivity(Intent(this, HerosAppIntro::class.java))
    }
}