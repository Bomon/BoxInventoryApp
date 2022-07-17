package com.pixlbee.heros.fragments

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.addCallback
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.MenuItemCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.pixlbee.heros.*
import com.pixlbee.heros.R
import com.pixlbee.heros.activities.LoginActivity
import com.pixlbee.heros.adapters.BoxAdapter
import com.pixlbee.heros.models.BoxItemModel
import com.pixlbee.heros.models.BoxModel
import com.pixlbee.heros.models.ContentItem
import com.pixlbee.heros.utility.Utils


class HomeFragment : Fragment(), SearchView.OnQueryTextListener {

    private var viewGroup: ViewGroup? = null
    var boxList: ArrayList<BoxModel> = ArrayList<BoxModel>()
    lateinit var adapter: BoxAdapter
    lateinit var rv: RecyclerView
    lateinit var firebase_listener: ValueEventListener
    private var doubleBackToExitPressedOnce: Boolean = false
    lateinit var exitToast: Toast
    private var searchQueryText: String = ""


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_home, menu)
        val searchBtn = menu.findItem(R.id.home_btn_search)

        val searchView: SearchView = MenuItemCompat.getActionView(searchBtn) as SearchView
        searchView.setOnQueryTextListener(this)
        searchBtn.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                // Do something when collapsed
                searchQueryText = ""
                adapter.setFilter(filterAndSort(boxList))
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
        adapter.setFilter(filterAndSort(boxList))
        //activity?.runOnUiThread {
        //    adapter.notifyDataSetChanged()
        //}
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.home_btn_search -> {
                true
            }
            R.id.home_btn_add -> {
                if (view != null) {
                    if(Utils.checkHasWritePermission(context)){
                        val bundle = Bundle()
                        val boxModel: BoxModel = BoxModel("", "", "", "", "", "", "", "", ContextCompat.getColor(requireContext(), R.color.default_box_color), "", ArrayList<ContentItem>())
                        bundle.putSerializable("boxModel", boxModel)
                        bundle.putSerializable("items", ArrayList<BoxItemModel>().toTypedArray())
                        bundle.putSerializable("isNewBox", true)
                        val navController: NavController = Navigation.findNavController(view!!)
                        navController.navigate(R.id.action_navigation_home_to_boxEditFragment, bundle)
                    }
                }
                true
            }
            R.id.home_btn_sort -> {
                if (context != null){
                    val builder: MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(context!!)
                    builder.setTitle(context!!.resources.getString(R.string.dialog_sort_title))

                    val viewInflated: View = LayoutInflater.from(context)
                        .inflate(R.layout.dialog_sort_boxes, viewGroup, false)

                    builder.setView(viewInflated)

                    // init radio groups
                    val radioGroup = viewInflated.findViewById<View>(R.id.radioButtonGroup) as RadioGroup
                    val radioGroupAscDesc = viewInflated.findViewById<View>(R.id.radioButtonGroupAscDesc) as RadioGroup

                    // Set button checked that is stored in settings
                    val sharedPreferences = context!!.getSharedPreferences("AppPreferences", MODE_PRIVATE)
                    val saved_order_by_btn = sharedPreferences.getInt("settings_box_order_by", R.id.radioButtonOrderId)
                    val saved_order_asc_desc = sharedPreferences.getInt("settings_box_order_asc_desc", R.id.radioButtonOrderAscending)
                    radioGroup.findViewById<RadioButton>(saved_order_by_btn).isChecked = true
                    radioGroupAscDesc.findViewById<RadioButton>(saved_order_asc_desc).isChecked = true

                    // Build dialog
                    builder.setPositiveButton(R.string.btn_save) { dialog, which ->
                        val editor = sharedPreferences.edit()
                        editor.putInt("settings_box_order_asc_desc", radioGroupAscDesc.checkedRadioButtonId)
                        editor.putInt("settings_box_order_by", radioGroup.checkedRadioButtonId)
                        editor.commit()

                        adapter.setFilter(filterAndSort(boxList))
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


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewGroup = container
        val view: View = inflater.inflate(R.layout.fragment_home, container, false)
        val recyclerview = view.findViewById<View>(R.id.RV_home) as RecyclerView
        adapter = BoxAdapter(boxList)
        adapter.setOnBoxClickListener(object: BoxAdapter.OnBoxClickListener{
            override fun onBoxClicked(box: BoxModel, view: View) {
                val navController: NavController = Navigation.findNavController(view)
                val bundle = Bundle()
                bundle.putSerializable("boxModel", box)
                navController.navigate(R.id.action_navigation_home_to_boxFragment, bundle)
            }
        })
        recyclerview.layoutManager = LinearLayoutManager(activity)
        recyclerview.adapter = adapter

        initFirebase()

        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
    }


    fun initFirebase() {
        firebase_listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot){
                boxList.clear()
                val boxes = dataSnapshot.child("boxes")
                for (box: DataSnapshot in boxes.children){
                    val boxModel = Utils.readBoxModelFromDataSnapshot(context, box)
                    boxList.add(boxModel)
                }
                adapter.setFilter(filterAndSort(boxList))
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        }
        FirebaseDatabase.getInstance().reference.addValueEventListener(firebase_listener)
    }


    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }


    private fun filterAndSort(models: List<BoxModel>): List<BoxModel> {
        val query = searchQueryText.toLowerCase()
        val filteredModelList: MutableList<BoxModel> = ArrayList()
        // filter list according to query
        for (model in models) {
            if (model.id.lowercase().contains(query)) {
                filteredModelList.add(model)
            } else if (model.location.lowercase().contains(query)) {
                filteredModelList.add(model)
            } else if (model.name.lowercase().contains(query)) {
                filteredModelList.add(model)
            } else if (model.status.lowercase().contains(query)) {
                filteredModelList.add(model)
            }
        }
        val sharedPreferences = context!!.getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val order_by = sharedPreferences.getInt("settings_box_order_by", R.id.radioButtonOrderId)
        val order_asc_desc = sharedPreferences.getInt("settings_box_order_asc_desc", R.id.radioButtonOrderAscending)
        // Sort list according to settings
        when (order_by) {

            R.id.radioButtonOrderLatest -> {
                if (order_asc_desc == R.id.radioButtonOrderAscending)
                    filteredModelList.reverse()
                true
            }
            R.id.radioButtonOrderId -> {
                if (order_asc_desc == R.id.radioButtonOrderAscending)
                    filteredModelList.sortBy { it.id }
                else
                    filteredModelList.sortByDescending { it.id }
                true
            }
            R.id.radioButtonOrderName -> {
                if (order_asc_desc == R.id.radioButtonOrderAscending)
                    filteredModelList.sortBy { it.name }
                else
                    filteredModelList.sortByDescending { it.name }
                true
            }
            R.id.radioButtonOrderLocation -> {
                if (order_asc_desc == R.id.radioButtonOrderAscending)
                    filteredModelList.sortBy { it.location }
                else
                    filteredModelList.sortByDescending { it.location }
                true
            }
            R.id.radioButtonOrderStatus -> {
                if (order_asc_desc == R.id.radioButtonOrderAscending)
                    filteredModelList.sortBy { it.status }
                else
                    filteredModelList.sortByDescending { it.status }
                true
            }
            R.id.radioButtonOrderColor -> {
                if (order_asc_desc == R.id.radioButtonOrderAscending)
                    filteredModelList.sortBy { it.color }
                else
                    filteredModelList.sortByDescending { it.color }
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
        FirebaseDatabase.getInstance().reference.removeEventListener(firebase_listener)
    }

    
}