package com.pixlbee.heros.activities

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.elevation.SurfaceColors
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.pixlbee.heros.R
import com.pixlbee.heros.databinding.ActivitySettingsBinding
import com.pixlbee.heros.fragments.SettingsFragment
import com.pixlbee.heros.utility.Utils

class SettingsActivity : AppCompatActivity() {

    private lateinit var sharedPrefChangeListener: SharedPreferences.OnSharedPreferenceChangeListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appToolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.appToolbar.setBackgroundColor(SurfaceColors.SURFACE_2.getColor(this))

        window.statusBarColor = resources.getColor(R.color.status_bar_color)
        window.navigationBarColor = SurfaceColors.SURFACE_2.getColor(this)

        supportFragmentManager.beginTransaction()
            .replace(R.id.content, SettingsFragment())
            .commit()


        val sharedPreferences: SharedPreferences = getSharedPreferences("AppPreferences",
            MODE_PRIVATE
        )

        sharedPrefChangeListener = SharedPreferences.OnSharedPreferenceChangeListener{sharedPreferences, s ->
            val syncSettings = listOf("pdf_title", "pdf_subtitle", "pdf_address")
            if (s in syncSettings) {
                val settingsValue = sharedPreferences.getString(s, "")
                if (settingsValue != ""){
                    val orgRef = FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(this))
                    orgRef.get().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val org: DataSnapshot? = task.result
                            if (org != null) {
                                FirebaseDatabase.getInstance().reference.child(
                                    Utils.getCurrentlySelectedOrg(this)
                                ).child(s)
                                    .setValue(settingsValue)
                            }
                        }
                    }
                }
            }

        }
        // Sync selected settings from the preferences to firebase
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPrefChangeListener)

    }

    override fun onDestroy() {
        val sharedPreferences: SharedPreferences = getSharedPreferences("AppPreferences",
            MODE_PRIVATE
        )
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPrefChangeListener)

        super.onDestroy()
    }

}