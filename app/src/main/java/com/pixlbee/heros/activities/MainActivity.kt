package com.pixlbee.heros.activities

import android.os.Bundle
import android.view.Menu
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.elevation.SurfaceColors
import com.pixlbee.heros.R
import com.pixlbee.heros.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var listView: ListView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //supportActionBar?.hide()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        NavigationUI.setupActionBarWithNavController(this, navController);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_qrscanner, R.id.navigation_items
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        getWindow().setNavigationBarColor(SurfaceColors.SURFACE_2.getColor(this));

        //Always color the nav button correctly
        navView.setOnItemSelectedListener { item ->
            // In order to get the expected behavior, you have to call default Navigation method manually
            NavigationUI.onNavDestinationSelected(item, navController)

            return@setOnItemSelectedListener true
        }

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