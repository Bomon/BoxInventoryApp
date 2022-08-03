package com.pixlbee.heros.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.pixlbee.heros.R
import com.pixlbee.heros.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private var mOrganizationItemIds: ArrayList<Int> = ArrayList()

    private lateinit var mNavViewDrawer: NavigationView
    private lateinit var mDrawer: DrawerLayout
    private lateinit var binding: ActivityMainBinding

    private val FLEXIBLE_APP_UPDATE_REQ_CODE = 5550123
    private val appUpdateManager: AppUpdateManager by lazy { AppUpdateManagerFactory.create(this) }
    private val installStateUpdatedListener: InstallStateUpdatedListener by lazy {
        object : InstallStateUpdatedListener {
            override fun onStateUpdate(installState: InstallState) {
                when {
                    installState.installStatus() == InstallStatus.DOWNLOADED -> {
                        val snackbar = Snackbar.make(
                            findViewById<View>(android.R.id.content).rootView,
                            resources.getString(R.string.snackbar_update_ready),
                            Snackbar.LENGTH_INDEFINITE
                        )
                            .setAction(resources.getString(R.string.snackbar_btn_install)) {
                                if (appUpdateManager != null) {
                                    appUpdateManager.completeUpdate()
                                }
                            }
                        snackbar.anchorView = findViewById(R.id.bottom_nav_view)
                        snackbar.show()
                    }
                    installState.installStatus() == InstallStatus.INSTALLED -> appUpdateManager.unregisterListener(this)
                }
            }
        }
    }

    private fun checkUpdate() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() === UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.FLEXIBLE,
                    this,
                    FLEXIBLE_APP_UPDATE_REQ_CODE)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        } catch (e: Exception){
            Log.e("Error", "Persistence cannot be enabled")
        }

        appUpdateManager.registerListener(installStateUpdatedListener)
        checkUpdate()

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
            setOf(R.id.navigation_vehicles, R.id.navigation_boxes, R.id.navigation_items, R.id.navigation_qrscanner),
            mDrawer
        )

        // fix icon highlight of bottom nav bar on back navigation
        binding.bottomNavView.setupWithNavController(navController)
        // if icon is clicked again, it will go back to root view
        binding.bottomNavView.setOnItemReselectedListener { item ->
            // Pop everything up to the reselected item
            val reselectedDestinationId = item.itemId
            navController.popBackStack(reselectedDestinationId, inclusive = false)
        }
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

        mNavViewDrawer = findViewById(R.id.nav_view_drawer)
        setupDrawerContent(mNavViewDrawer)

        binding.navHostLoadingSpinner.visibility = View.VISIBLE
        //init firebase
        initFirebaseListener(this, mNavViewDrawer)
    }


    private fun setupDrawerContent(navigationView: NavigationView) {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            selectDrawerItem(menuItem)
            true
        }
    }


    private fun selectDrawerItem(menuItem: MenuItem) {
        // Create a new fragment and specify the fragment to show based on nav item clicked
        when (menuItem.itemId) {
            R.id.nav_logout -> {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            R.id.nav_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        }
        menuItem.isChecked = false
        mDrawer.closeDrawers()
    }


    private fun initFirebaseListener(context: Context, navViewDrawer: NavigationView) {
        val firebaseListener: ValueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val userId = FirebaseAuth.getInstance().currentUser?.uid.toString()
                val sharedPreferences: SharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
                val editor = sharedPreferences.edit()

                val menu: Menu = navViewDrawer.menu

                // remove previous org items
                for (oldItemId in mOrganizationItemIds){
                    menu.removeItem(oldItemId)
                }

                // find all orgs that the user is allowed to read
                val organizationIds = ArrayList<String>()
                val newItemIds = ArrayList<Int>()
                val newItems = ArrayList<MenuItem>()
                val itemNameIdLookup: HashMap<String, String> = HashMap()
                for (org in dataSnapshot.child("read_permissions").children){
                    val orgId: String = org.key.toString()
                    val orgName = dataSnapshot.child(orgId).child("name").value.toString()
                    val orgReadPermissions = org.value.toString()

                    //FirebaseDatabase.getInstance().reference.keepSynced(false)
                    FirebaseDatabase.getInstance().getReference(orgId).keepSynced(true)
                    FirebaseDatabase.getInstance().getReference("read_permissions").keepSynced(true)
                    FirebaseDatabase.getInstance().getReference("write_permissions").keepSynced(true)

                    // add these to the drawer
                    if (orgReadPermissions.contains(userId)) {
                        itemNameIdLookup[orgName] = orgId
                        // ensure that a new item has a unique id
                        var newItemId = (Int.MIN_VALUE..Int.MAX_VALUE).random()
                        while(newItemId in newItemIds){
                            newItemId = (Int.MIN_VALUE..Int.MAX_VALUE).random()
                        }

                        // create new item
                        val addedMenuItem: MenuItem = menu.add(
                            R.id.drawer_group_organizations,
                            newItemId,
                            Menu.NONE,
                            orgName
                        )
                        addedMenuItem.isCheckable = true
                        addedMenuItem.setOnMenuItemClickListener {
                            val sharedPreferences: SharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
                            val editor = sharedPreferences.edit()
                            val currentlySelectedOrg: String = itemNameIdLookup[it.title.toString()].toString()
                            editor.putString("selected_organization", currentlySelectedOrg)
                            editor.commit()
                            it.isChecked = true
                            mDrawer.closeDrawers()
                            // restart activity to load new org content
                            startActivity(intent)
                            finish()
                            overridePendingTransition(0, 0)
                            true
                        }

                        // add icon to org that indicates write or read permissions
                        val writeUsers = dataSnapshot.child("write_permissions").child(orgId).value.toString()
                        if (writeUsers.contains(userId)) {
                            addedMenuItem.icon = context.getDrawable(R.drawable.ic_baseline_edit_24)
                        } else {
                            addedMenuItem.icon = context.getDrawable(R.drawable.ic_baseline_remove_red_eye_24)
                        }

                        // store info about new item
                        newItems.add(addedMenuItem)
                        organizationIds.add(orgId)
                        newItemIds.add(newItemId)
                    }
                }
                mOrganizationItemIds = newItemIds

                // Get selected org from settings
                var selectedOrg: String = sharedPreferences.getString("selected_organization", "").toString()
                // If the selected org is not inside the orgs the user is allowed to read
                if (selectedOrg !in organizationIds){
                    // If user has read access to at least one org
                    if (organizationIds.size != 0) {
                        // set the first org as current org
                        selectedOrg = organizationIds[0]
                        editor.putString("selected_organization", organizationIds[0])
                        newItems[0].isChecked = true
                        // get write permission status for this org
                        val writeUsers = dataSnapshot.child("write_permissions").child(selectedOrg).value.toString()
                        if (writeUsers.contains(userId)) {
                            mNavViewDrawer.menu.findItem(R.id.nav_settings).isEnabled = true
                            editor.putBoolean("write_permission", true)
                        } else {
                            mNavViewDrawer.menu.findItem(R.id.nav_settings).isEnabled = false
                            editor.putBoolean("write_permission", false)
                        }
                    }
                // else if the selected org is inside the available orgs
                } else {
                    newItems[organizationIds.indexOf(selectedOrg)].isChecked = true
                    // get write permissions
                    val writeUsers = dataSnapshot.child("write_permissions").child(selectedOrg).value.toString()
                    if (writeUsers.contains(userId)) {
                        mNavViewDrawer.menu.findItem(R.id.nav_settings).isEnabled = true
                        editor.putBoolean("write_permission", true)
                    } else {
                        mNavViewDrawer.menu.findItem(R.id.nav_settings).isEnabled = false
                        editor.putBoolean("write_permission", false)
                    }
                }

                // Put PDF settings into SharedSettings
                val pdfAddress = dataSnapshot.child(selectedOrg).child("pdf_address").value.toString()
                val pdfTitle = dataSnapshot.child(selectedOrg).child("pdf_title").value.toString()
                val pdfSubtitle = dataSnapshot.child(selectedOrg).child("pdf_subtitle").value.toString()
                val pdfLogoLeft = dataSnapshot.child(selectedOrg).child("pdf_logo_left").value.toString()
                val pdfLogoRight = dataSnapshot.child(selectedOrg).child("pdf_logo_right").value.toString()
                editor.putString("pdf_title", pdfTitle)
                editor.putString("pdf_subtitle", pdfSubtitle)
                editor.putString("pdf_address", pdfAddress)
                editor.putString("pdf_logo_left", pdfLogoLeft)
                editor.putString("pdf_logo_right", pdfLogoRight)

                editor.commit()


                binding.navHostLoadingSpinner.visibility = View.GONE
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