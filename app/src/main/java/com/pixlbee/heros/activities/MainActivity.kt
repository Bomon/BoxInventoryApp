package com.pixlbee.heros.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.pixlbee.heros.R
import com.pixlbee.heros.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var mDrawer: DrawerLayout
    private lateinit var binding: ActivityMainBinding
    private lateinit var listView: ListView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //init firebase
        initFirebaseListener(this)
        initDefaultSettings()

        // Color the status bar
        window.statusBarColor = resources.getColor(R.color.status_bar_color)
        window.navigationBarColor = SurfaceColors.SURFACE_2.getColor(this)

        // init view
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        // Init App bar configuration (defines home fragments where drawer is visible, otherwise back button)
        mDrawer = binding.drawerLayout
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.navigation_home, R.id.navigation_qrscanner, R.id.navigation_items),
            mDrawer
        )

        // add logic to drawer button
        binding.toolbar.setupWithNavController(navController, appBarConfiguration)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.toolbar.setBackgroundColor(SurfaceColors.SURFACE_2.getColor(this))

        val sharedPreferences = this.getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val currentUser = sharedPreferences.getString("current_user", "USER")
        binding.navViewDrawer.getHeaderView(0).findViewById<TextView>(R.id.drawer_header_username).text = currentUser

        //Always color the bottom nav button correctly
        val navView: BottomNavigationView = binding.bottomNavView
        navView.setOnItemSelectedListener { item ->
            // In order to get the expected behavior, you have to call default Navigation method manually
            NavigationUI.onNavDestinationSelected(item, navController)
            return@setOnItemSelectedListener true
        }

        val nvDrawer: NavigationView = findViewById(R.id.nav_view_drawer)
        setupDrawerContent(nvDrawer);

    }


    private fun initDefaultSettings() {
        val sharedPreferences: SharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("pdf_title", resources.getString(R.string.pdf_title))
        editor.putString("pdf_subtitle", resources.getString(R.string.pdf_subtitle))
        editor.putString("pdf_address", resources.getString(R.string.pdf_address))
        editor.commit()
    }


    private fun setupDrawerContent(navigationView: NavigationView) {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            selectDrawerItem(menuItem)
            true
        }
    }


    fun selectDrawerItem(menuItem: MenuItem) {
        // Create a new fragment and specify the fragment to show based on nav item clicked
        when (menuItem.itemId) {
            R.id.nav_logout -> {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            R.id.nav_settings -> {}
        }
        menuItem.setChecked(false)
        mDrawer.closeDrawers()
    }


    private fun initFirebaseListener(context: Context) {
        val firebaseListener: ValueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val sharedPreferences: SharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
                val editor = sharedPreferences.edit()

                val userId = FirebaseAuth.getInstance().currentUser?.uid.toString()
                val write_users = dataSnapshot.child("write_users").value.toString()
                val read_users = dataSnapshot.child("write_users").value.toString()

                if (write_users.contains(userId)) {
                    editor.putBoolean("write_permission", true)
                } else {
                    editor.putBoolean("write_permission", false)
                }

                if (read_users.contains(userId)) {
                    editor.putBoolean("read_permission", true)
                } else {
                    editor.putBoolean("read_permission", false)
                }

                editor.commit()
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        }
        FirebaseDatabase.getInstance().reference.addValueEventListener(firebaseListener)
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        return true
    }


    override fun onSupportNavigateUp(): Boolean {
        val navigationController: NavController =
            Navigation.findNavController(this, R.id.nav_host_fragment_activity_main)
        return navigationController.navigateUp()
    }


    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStackImmediate()
        } else {
            super.onBackPressed()
        }
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.clear()
    }


}