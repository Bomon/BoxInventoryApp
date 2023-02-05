package com.pixlbee.heros.fragments

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.view.*
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.MenuItemCompat
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.pixlbee.heros.R
import com.pixlbee.heros.adapters.BoxAdapter
import com.pixlbee.heros.models.BoxItemModel
import com.pixlbee.heros.models.BoxModel
import com.pixlbee.heros.models.ContentItem
import com.pixlbee.heros.utility.Utils
import java.util.*


class BoxesFragment : Fragment(), SearchView.OnQueryTextListener {

    private var viewGroup: ViewGroup? = null
    var mBoxList: ArrayList<BoxModel> = ArrayList<BoxModel>()
    lateinit var mAdapter: BoxAdapter
    private lateinit var mFirebaseListener: ValueEventListener

    private lateinit var exitToast: Toast
    private var searchQueryText: String = ""

    val vehicleIdNameLookup: HashMap<String, String> = HashMap()

    private lateinit var searchView: SearchView
    private lateinit var searchBtn: MenuItem

    private lateinit var animationType: String


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_boxes, menu)
        searchBtn = menu.findItem(R.id.boxes_btn_search)
        searchView = MenuItemCompat.getActionView(searchBtn) as SearchView
        searchView.setOnQueryTextListener(this)
        searchBtn.setOnActionExpandListener(object :
            MenuItem.OnActionExpandListener {
            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                // Do something when collapsed
                searchQueryText = ""
                setFilterAndSort(mBoxList)
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
        setFilterAndSort(mBoxList)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.boxes_btn_search -> {
                true
            }
            R.id.boxes_btn_add -> {
                if (view != null) {
                    if(Utils.checkHasWritePermission(context)){
                        val bundle = Bundle()
                        val boxModel = BoxModel(
                            (UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE).toString(),
                            "",
                            "",
                            "Box",
                            "",
                            "",
                            "-1",
                            "",
                            "",
                            "",
                            ContextCompat.getColor(requireContext(), R.color.default_box_color),
                            "",
                            ArrayList<ContentItem>(),
                            ArrayList<String>()
                        )
                        bundle.putSerializable("boxModel", boxModel)
                        bundle.putSerializable("items", ArrayList<BoxItemModel>().toTypedArray())
                        bundle.putSerializable("isNewBox", true)
                        val navController: NavController = Navigation.findNavController(view!!)
                        navController.navigate(R.id.action_navigation_boxes_to_boxEditFragment, bundle)
                    }
                }
                true
            }
            R.id.boxes_btn_sort -> {
                if (context != null){
                    val builder = MaterialAlertDialogBuilder(context!!)
                    builder.setTitle(context!!.resources.getString(R.string.dialog_sort_title))

                    val viewInflated: View = LayoutInflater.from(context)
                        .inflate(R.layout.dialog_sort_boxes, viewGroup, false)

                    builder.setView(viewInflated)

                    // init radio groups
                    val radioGroup = viewInflated.findViewById<View>(R.id.radioButtonGroup) as RadioGroup
                    val radioGroupAscDesc = viewInflated.findViewById<View>(R.id.radioButtonGroupAscDesc) as RadioGroup

                    // Set button checked that is stored in settings
                    val sharedPreferences = context!!.getSharedPreferences("AppPreferences", MODE_PRIVATE)
                    val savedOrderByBtnId: Int = Utils.getButtonForSortSetting(sharedPreferences.getString("settings_box_order_by", "order_by_id"))
                    val savedOrderAscDescBtnId: Int = Utils.getButtonForSortSettingAscDesc(sharedPreferences.getString("settings_box_order_asc_desc", "order_ascending"))

                    radioGroup.findViewById<RadioButton>(savedOrderByBtnId).isChecked = true
                    radioGroupAscDesc.findViewById<RadioButton>(savedOrderAscDescBtnId).isChecked = true

                    // Build dialog
                    builder.setPositiveButton(R.string.btn_save) { dialog, which ->
                        val editor = sharedPreferences.edit()
                        editor.putString("settings_box_order_asc_desc", Utils.getSortSettingAscDescForButton( radioGroupAscDesc.checkedRadioButtonId ))
                        editor.putString("settings_box_order_by", Utils.getSortSettingForButton( radioGroup.checkedRadioButtonId ))
                        editor.commit()

                        setFilterAndSort(mBoxList)
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

        val sharedPreferences = context!!.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        animationType = sharedPreferences.getString("animation_type", "simple").toString()
        if (animationType == "elegant") {
            enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
            returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
        } else if (animationType == "simple"){
            enterTransition = MaterialFadeThrough()
            returnTransition = MaterialFadeThrough()
        }

    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (animationType in listOf("simple", "elegant")){
            exitTransition = MaterialFadeThrough()
        }

        viewGroup = container
        val view: View = inflater.inflate(R.layout.fragment_boxes, container, false)
        val recyclerview = view.findViewById<View>(R.id.RV_boxes) as RecyclerView

        mAdapter = BoxAdapter(mBoxList, locationClickable = true, tagClickable = true)
        mAdapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        mAdapter.setOnBoxClickListener(object: BoxAdapter.OnBoxClickListener{
            override fun onBoxClicked(box: BoxModel, view: View) {
                if (animationType == "elegant"){
                    exitTransition = MaterialFadeThrough()
                }
                val extras = FragmentNavigatorExtras(
                    view to box.id
                )
                val navController: NavController = Navigation.findNavController(view)
                navController.navigate(BoxesFragmentDirections.actionNavigationBoxesToBoxFragment(box), extras)
            }

            override fun onBoxTagClicked(tag: String) {
                if (animationType == "elegant"){
                    exitTransition = Hold()
                }
                searchBtn.expandActionView()
                searchView.setQuery(tag, true)
            }
        })

        recyclerview.layoutManager = LinearLayoutManager(activity)
        recyclerview.adapter = mAdapter

        // Possible optimizations:
        //recyclerview.setHasFixedSize(true)
        //recyclerview.setItemViewCacheSize(100);

        // Item decorators for inserting dividers into RecyclerView
        /*val dividerItemDecoration: ItemDecoration = BoxDividerItemDecorator(
            ContextCompat.getDrawable(
                context!!, R.drawable.rv_divider
            )!!
        )
        recyclerview.addItemDecoration(dividerItemDecoration)*/

        // Swipe functionality
        /*ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT + ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (Utils.checkHasWritePermission(context, false)) {
                    super.onChildDraw(
                        c,
                        recyclerView,
                        viewHolder,
                        dX / 5,
                        dY,
                        actionState,
                        isCurrentlyActive
                    )
                } else {
                    super.onChildDraw(
                        c,
                        recyclerView,
                        viewHolder,
                        0f,
                        dY,
                        actionState,
                        isCurrentlyActive
                    )
                }
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (Utils.checkHasWritePermission(context, false)) {
                    val oldBoxModel: BoxModel = mBoxList[viewHolder.adapterPosition]
                    val position = viewHolder.adapterPosition
                    //boxList.removeAt(viewHolder.adapterPosition)

                    var color: Int = Utils.getNextColor(context!!, oldBoxModel.color)
                    if (direction == ItemTouchHelper.RIGHT) {
                        color = Utils.getPreviousColor(context!!, oldBoxModel.color)
                    }
                    oldBoxModel.color = color

                    mAdapter.updateColorInFirebase(position)
                }
            }
        }).attachToRecyclerView(recyclerview)*/

        initFirebase()

        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        if (animationType == "elegant") {
            postponeEnterTransition()
            view.doOnPreDraw { startPostponedEnterTransition() }
        }
    }


    private fun initFirebase() {
        mFirebaseListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot){
                vehicleIdNameLookup.clear()
                val vehicles = dataSnapshot.child(Utils.getCurrentlySelectedOrg(context!!)).child("vehicles")
                for (vehicle: DataSnapshot in vehicles.children) {
                    var vehicleId = vehicle.child("id").value.toString()
                    var vehicleName = vehicle.child("name").value.toString()
                    vehicleIdNameLookup[vehicleId] = vehicleName
                }

                mBoxList.clear()
                val boxes = dataSnapshot.child(Utils.getCurrentlySelectedOrg(context!!)).child("boxes")
                for (box: DataSnapshot in boxes.children){
                    val boxModel = Utils.readBoxModelFromDataSnapshot(context, box)
                    mBoxList.add(boxModel)
                }
                setFilterAndSort(mBoxList)
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        }
        FirebaseDatabase.getInstance().reference.addValueEventListener(mFirebaseListener)
    }


    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }


    private fun setFilterAndSort(models: List<BoxModel>) {
        val query = searchQueryText.lowercase(Locale.getDefault())
        val filteredModelList: MutableList<BoxModel> = ArrayList()
        // filter list according to query
        for (model in models) {
            if (model.id.lowercase().contains(query)) {
                filteredModelList.add(model)
            } else if (model.name.lowercase().contains(query)) {
                filteredModelList.add(model)
            } else if (model.status.lowercase().contains(query)) {
                filteredModelList.add(model)
            } else {
                for (contentItem in model.content) {
                    if (contentItem.invnum.lowercase().contains(query)) {
                        filteredModelList.add(model)
                        break
                    }
                }
            }
        }
        val sharedPreferences = context!!.getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val savedOrderByBtnId = Utils.getButtonForSortSetting(sharedPreferences.getString("settings_box_order_by", "order_by_id"))
        val savedOrderAscDescBtnId = Utils.getButtonForSortSettingAscDesc(sharedPreferences.getString("settings_box_order_asc_desc", "order_ascending"))

        // Sort list according to settings
        when (savedOrderByBtnId) {
            R.id.radioButtonOrderLatest -> {
                if (savedOrderAscDescBtnId == R.id.radioButtonOrderAscending)
                    filteredModelList.reverse()
                true
            }
            R.id.radioButtonOrderId -> {
                filteredModelList.sortWith(
                    compareBy(String.CASE_INSENSITIVE_ORDER) { Utils.replaceUmlauteForSorting(it.id) }
                )
                if (savedOrderAscDescBtnId == R.id.radioButtonOrderDescending)
                    filteredModelList.reverse()
                true
            }
            R.id.radioButtonOrderName -> {
                filteredModelList.sortWith(
                    compareBy(String.CASE_INSENSITIVE_ORDER) { Utils.replaceUmlauteForSorting(it.name) }
                )
                if (savedOrderAscDescBtnId == R.id.radioButtonOrderDescending)
                    filteredModelList.reverse()
                true
            }
            R.id.radioButtonOrderVehicle -> {
                filteredModelList.sortWith(
                    compareBy(String.CASE_INSENSITIVE_ORDER) { Utils.replaceUmlauteForSorting(
                        vehicleIdNameLookup[it.in_vehicle].toString()
                    ) }
                )
                if (savedOrderAscDescBtnId == R.id.radioButtonOrderDescending)
                    filteredModelList.reverse()
                true
            }
            R.id.radioButtonCompleteness -> {
                filteredModelList.sortWith(
                    compareBy { it.content.sumOf { ic -> ic.amount_taken.toInt() * -1 } }
                )
                if (savedOrderAscDescBtnId == R.id.radioButtonOrderDescending)
                    filteredModelList.reverse()
                true
            }
            R.id.radioButtonOrderStatus -> {
                filteredModelList.sortWith(
                    compareBy(String.CASE_INSENSITIVE_ORDER) { Utils.replaceUmlauteForSorting(it.status) }
                )
                if (savedOrderAscDescBtnId == R.id.radioButtonOrderDescending)
                    filteredModelList.reverse()
                true
            }
            R.id.radioButtonOrderColor -> {
                filteredModelList.sortBy { it.color }
                if (savedOrderAscDescBtnId == R.id.radioButtonOrderDescending)
                    filteredModelList.reverse()
                true
            }
        }

        mAdapter.setFilter(filteredModelList)

        // serach for vehicle name / callname afterwards, as this is async it may take longer and will update search results
        asyncSearchVehicleName(models, filteredModelList)
    }


    private fun asyncSearchVehicleName(models: List<BoxModel>, filteredModels: MutableList<BoxModel>) {
        var query = searchQueryText.lowercase(Locale.getDefault())
        query = query.lowercase(Locale.getDefault())

        val vehiclesRef =
            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!))
                .child("vehicles")
        vehiclesRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val vehicles: DataSnapshot? = task.result
                if (vehicles != null) {
                    var resultsUpdated = false
                    var foundVehicleIds = ArrayList<String>()
                    // search all vehicles for queried name or callname
                    for (vehicle: DataSnapshot in vehicles.children) {
                        val name = vehicle.child("name").value.toString()
                        val callname = vehicle.child("callname").value.toString()

                        if (query in name || query in callname) {
                            val vehicleId = vehicle.child("id").value.toString()
                            foundVehicleIds.add(vehicleId)
                        }
                    }

                    // Add found vehicles
                    for (mBoxModel in models) {
                        if (foundVehicleIds.contains(mBoxModel.in_vehicle)) {
                            if (mBoxModel !in filteredModels) {
                                resultsUpdated = true
                                filteredModels.add(mBoxModel)
                            }
                        }
                    }

                    // only update search results if there were changes
                    if (resultsUpdated) {
                        filteredModels.sortWith(
                            compareBy(String.CASE_INSENSITIVE_ORDER) {
                                var name = it.name.lowercase(Locale.getDefault()).replace("ä", "ae")
                                name = name.replace("ö", "oe")
                                name = name.replace("ü", "ue")
                                name
                            }
                        )
                        mAdapter.setFilter(filteredModels)
                    }
                }
            }
        }
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