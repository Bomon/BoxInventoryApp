package com.pixlbee.heros.fragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.pixlbee.heros.R
import com.pixlbee.heros.adapters.VehicleAdapter
import com.pixlbee.heros.models.ItemModel
import com.pixlbee.heros.models.VehicleModel
import com.pixlbee.heros.utility.Utils
import java.text.SimpleDateFormat
import java.util.*

class VehiclesFragment : Fragment(), SearchView.OnQueryTextListener {

    private var returnVehicleInsteadOfShowDetails: Boolean = false
    private var viewGroup: ViewGroup? = null
    var mVehiclesList: ArrayList<VehicleModel> = ArrayList<VehicleModel>()
    lateinit var mAdapter: VehicleAdapter
    private lateinit var mFirebaseListener: ValueEventListener

    private var doubleBackToExitPressedOnce: Boolean = false
    private lateinit var exitToast: Toast
    private var searchQueryText: String = ""
    private var isFirstCreate: Boolean = true


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_vehicles, menu)
        val searchBtn = menu.findItem(R.id.vehicles_btn_search)

        val searchView: SearchView = MenuItemCompat.getActionView(searchBtn) as SearchView
        searchView.setOnQueryTextListener(this)
        searchBtn.setOnActionExpandListener(object :
            MenuItem.OnActionExpandListener {
            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                // Do something when collapsed
                searchQueryText = ""
                mAdapter.setFilter(filterAndSort(mVehiclesList))
                return true // Return true to collapse action view
            }

            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                // Do something when expanded
                return true // Return true to expand action view
            }
        })
    }


    override fun onQueryTextChange(newText: String): Boolean {
        searchQueryText = newText
        mAdapter.setFilter(filterAndSort(mVehiclesList))
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.vehicles_btn_search -> {
                true
            }
            R.id.vehicles_btn_add -> {
                if (view != null) {
                    if(Utils.checkHasWritePermission(context)){
                        val bundle = Bundle()
                        val vehicleModel = VehicleModel(
                            "",
                            "",
                            "",
                            "",
                            "",
                            ""
                        )
                        bundle.putSerializable("vehicleModel", vehicleModel)
                        bundle.putSerializable("isNewVehicle", true)
                        val navController: NavController = Navigation.findNavController(view!!)
                        navController.navigate(R.id.action_navigation_vehicles_to_vehicleEditFragment, bundle)
                    }
                }
                true
            }
            R.id.vehicles_btn_sort -> {
                if (context != null){
                    val builder = MaterialAlertDialogBuilder(context!!)
                    builder.setTitle(context!!.resources.getString(R.string.dialog_sort_title))

                    val viewInflated: View = LayoutInflater.from(context)
                        .inflate(R.layout.dialog_sort_vehicles, viewGroup, false)

                    builder.setView(viewInflated)

                    // init radio groups
                    val radioGroup = viewInflated.findViewById<View>(R.id.radioButtonVehicleGroup) as RadioGroup
                    val radioGroupAscDesc = viewInflated.findViewById<View>(R.id.radioButtonGroupVehiclesAscDesc) as RadioGroup

                    // Set button checked that is stored in settings
                    val sharedPreferences = context!!.getSharedPreferences("AppPreferences",
                        Context.MODE_PRIVATE
                    )
                    val savedOrderByBtnId: Int = Utils.getButtonForSortSetting(sharedPreferences.getString("settings_vehicles_order_by", "vehicles_order_by_name"))
                    val savedOrderAscDescBtnId: Int = Utils.getButtonForSortSettingAscDesc(sharedPreferences.getString("settings_vehicles_order_asc_desc", "vehicles_order_ascending"))

                    radioGroup.findViewById<RadioButton>(savedOrderByBtnId).isChecked = true
                    radioGroupAscDesc.findViewById<RadioButton>(savedOrderAscDescBtnId).isChecked = true

                    // Build dialog
                    builder.setPositiveButton(R.string.btn_save) { dialog, which ->
                        val editor = sharedPreferences.edit()
                        editor.putString("settings_vehicles_order_asc_desc", Utils.getSortSettingAscDescForButton( radioGroupAscDesc.checkedRadioButtonId ))
                        editor.putString("settings_vehicles_order_by", Utils.getSortSettingForButton( radioGroup.checkedRadioButtonId ))
                        editor.commit()

                        mAdapter.setFilter(filterAndSort(mVehiclesList))
                        dialog.dismiss()
                    }

                    builder.setNegativeButton(R.string.btn_cancel) { dialog, which ->
                        dialog.dismiss()
                    }

                    builder.show()
                }

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)

        returnVehicleInsteadOfShowDetails = arguments?.getBoolean("return_vehicle_instead_of_show_details") as Boolean
        if (returnVehicleInsteadOfShowDetails) {
            (activity as AppCompatActivity?)!!.supportActionBar!!.setDisplayHomeAsUpEnabled(false)
            (activity as AppCompatActivity?)!!.supportActionBar!!.setHomeButtonEnabled(false)
        } else {
            // Show double press BACk to exit

            requireActivity().onBackPressedDispatcher.addCallback(this) {
                if (doubleBackToExitPressedOnce) {
                    exitToast.cancel()
                    activity?.finish()
                }
                if (::exitToast.isInitialized){
                    exitToast.cancel()
                }
                doubleBackToExitPressedOnce = true
                Handler(Looper.getMainLooper()).postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
                exitToast = Toast.makeText(context, getText(R.string.press_back_again_to_exit), Toast.LENGTH_LONG)
                exitToast.show()
            }

        }

        val transformEnter = MaterialContainerTransform(requireContext(), true)
        transformEnter.scrimColor = Color.TRANSPARENT
        sharedElementEnterTransition = transformEnter

        val transformReturn = MaterialContainerTransform(requireContext(), false)
        transformReturn.scrimColor = Color.TRANSPARENT
        sharedElementReturnTransition = transformReturn

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)

        (activity as AppCompatActivity).supportActionBar?.title = resources.getString(R.string.title_nav_vehicles)

    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        exitTransition = MaterialFadeThrough()

        viewGroup = container
        val view: View = inflater.inflate(R.layout.fragment_vehicles, container, false)
        val recyclerview = view.findViewById<View>(R.id.RV_vehicles) as RecyclerView

        mAdapter = VehicleAdapter(mVehiclesList)
        mAdapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY


        if (returnVehicleInsteadOfShowDetails) {
            mAdapter.setOnVehicleClickListener(object : VehicleAdapter.OnVehicleClickListener {
                override fun onVehicleClicked(vehicle: VehicleModel, view: View) {
                    val navController: NavController = Navigation.findNavController(view)
                    // push the selected item back to BoxEditFragment
                    navController.previousBackStackEntry?.savedStateHandle?.set("vehicleModel", vehicle)
                    navController.popBackStack()
                }
            })
        } else {
            mAdapter.setOnVehicleClickListener(object: VehicleAdapter.OnVehicleClickListener{
                override fun onVehicleClicked(vehicle: VehicleModel, view: View) {
                    exitTransition = Hold()
                    val extras = FragmentNavigatorExtras(
                        view to vehicle.id.toString()
                    )
                    val navController: NavController = Navigation.findNavController(view)
                    navController.navigate(VehiclesFragmentDirections.actionNavigationVehiclesToVehicleDetailFragment(vehicle), extras)
                }
            })

        }


        recyclerview.layoutManager = LinearLayoutManager(activity)
        recyclerview.adapter = mAdapter

        initFirebase()

        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }

        // Trigger reload on first creation. Reason: Initial vehiclees after login may be not visible
        if (isFirstCreate){
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val task = FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("last_Login").setValue(timeStamp)
            isFirstCreate = false
        }

        if (returnVehicleInsteadOfShowDetails) {
            // receive new item from ItemEditFragment
            val navController = findNavController()
            navController.currentBackStackEntry?.savedStateHandle?.getLiveData<ItemModel>("item")?.observe(
                viewLifecycleOwner) { result ->
                // Push this back to the BoxEditFragment
                navController.previousBackStackEntry?.savedStateHandle?.set("item_id", result.id)
                navController.navigateUp()
            }
        }
    }


    private fun initFirebase() {
        mFirebaseListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot){
                mVehiclesList.clear()
                val vehicles = dataSnapshot.child(Utils.getCurrentlySelectedOrg(context!!)).child("vehicles")
                for (vehicle: DataSnapshot in vehicles.children){
                    val vehicleModel = Utils.readVehicleModelFromDataSnapshot(vehicle)
                    mVehiclesList.add(vehicleModel)
                }
                mAdapter.setFilter(filterAndSort(mVehiclesList))
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        }
        FirebaseDatabase.getInstance().reference.addValueEventListener(mFirebaseListener)
    }


    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }


    private fun filterAndSort(models: List<VehicleModel>): List<VehicleModel> {
        val query = searchQueryText.lowercase(Locale.getDefault())
        val filteredModelList: MutableList<VehicleModel> = ArrayList()
        // filter list according to query
        for (model in models) {
            if (model.name.lowercase().contains(query)) {
                filteredModelList.add(model)
            } else if (model.callname.lowercase().contains(query)) {
                filteredModelList.add(model)
            } else if (model.parking_spot.lowercase().contains(query)) {
                filteredModelList.add(model)
            }
        }
        val sharedPreferences = context!!.getSharedPreferences("AppPreferences",
            Context.MODE_PRIVATE
        )
        val savedOrderByBtnId = Utils.getButtonForSortSetting(sharedPreferences.getString("settings_vehicles_order_by", "vehicles_order_by_name"))
        val savedOrderAscDescBtnId = Utils.getButtonForSortSettingAscDesc(sharedPreferences.getString("settings_vehicles_order_asc_desc", "vehicles_order_ascending"))

        // Sort list according to settings
        when (savedOrderByBtnId) {
            R.id.radioButtonVehiclesOrderName -> {
                filteredModelList.sortWith(
                    compareBy(String.CASE_INSENSITIVE_ORDER) { Utils.replaceUmlauteForSorting(it.name) }
                )
                if (savedOrderAscDescBtnId == R.id.radioButtonVehiclesOrderDescending)
                    filteredModelList.reverse()
                true
            }
            R.id.radioButtonVehiclesOrderCallname -> {
                filteredModelList.sortWith(
                    compareBy(String.CASE_INSENSITIVE_ORDER) { Utils.replaceUmlauteForSorting(it.callname) }
                )
                if (savedOrderAscDescBtnId == R.id.radioButtonVehiclesOrderDescending)
                    filteredModelList.reverse()
                true
            }
            R.id.radioButtonVehiclesOrderParkingSpot -> {
                filteredModelList.sortWith(
                    compareBy(String.CASE_INSENSITIVE_ORDER) { Utils.replaceUmlauteForSorting(it.parking_spot) }
                )
                if (savedOrderAscDescBtnId == R.id.radioButtonVehiclesOrderDescending)
                    filteredModelList.reverse()
                true
            }
        }

        return filteredModelList
    }


    override fun onPause() {
        if (::exitToast.isInitialized){
            exitToast.cancel()
        }
        super.onPause()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        FirebaseDatabase.getInstance().reference.removeEventListener(mFirebaseListener)
    }
}