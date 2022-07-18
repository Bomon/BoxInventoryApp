package com.pixlbee.heros.fragments

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.MenuItemCompat
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.ItemTouchHelper
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
                        val boxModel: BoxModel = BoxModel(System.currentTimeMillis(), "", "", "", "", "", "", "", "", ContextCompat.getColor(requireContext(), R.color.default_box_color), "", ArrayList<ContentItem>())
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
                    val saved_order_by_btn_id: Int = Utils.getButtonForSortSetting(sharedPreferences.getString("settings_box_order_by", "order_by_id"))
                    val saved_order_asc_desc_btn_id: Int = Utils.getButtonForSortSettingAscDesc(sharedPreferences.getString("settings_box_order_asc_desc", "order_asc"))

                    radioGroup.findViewById<RadioButton>(saved_order_by_btn_id).isChecked = true
                    radioGroupAscDesc.findViewById<RadioButton>(saved_order_asc_desc_btn_id).isChecked = true

                    // Build dialog
                    builder.setPositiveButton(R.string.btn_save) { dialog, which ->
                        val editor = sharedPreferences.edit()
                        editor.putString("settings_box_order_asc_desc", Utils.getSortSettingAscDescForButton( radioGroupAscDesc.checkedRadioButtonId ))
                        editor.putString("settings_box_order_by", Utils.getSortSettingForButton( radioGroup.checkedRadioButtonId ))
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

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)

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

        exitTransition = MaterialFadeThrough()

        viewGroup = container
        val view: View = inflater.inflate(R.layout.fragment_home, container, false)
        val recyclerview = view.findViewById<View>(R.id.RV_home) as RecyclerView

        adapter = BoxAdapter(boxList)
        adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        adapter.setOnBoxClickListener(object: BoxAdapter.OnBoxClickListener{
            override fun onBoxClicked(box: BoxModel, view: View) {
                exitTransition = Hold()
                val extras = FragmentNavigatorExtras(
                    view to box.id
                )
                val navController: NavController = Navigation.findNavController(view)
                navController.navigate(HomeFragmentDirections.actionNavigationHomeToBoxFragment(box), extras)
            }
        })

        recyclerview.layoutManager = LinearLayoutManager(activity)
        recyclerview.adapter = adapter

        // Item decorators for inserting dividers into RecyclerView
        //val dividerItemDecoration: ItemDecoration = BoxDividerItemDecorator(
        //    ContextCompat.getDrawable(
        //        context!!, R.drawable.rv_divider
        //    )!!
        //)
        //recyclerview.addItemDecoration(dividerItemDecoration)

        // Swipe functionality
        ItemTouchHelper(object :
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
                    val oldBoxModel: BoxModel = boxList[viewHolder.adapterPosition]
                    val position = viewHolder.adapterPosition
                    //boxList.removeAt(viewHolder.adapterPosition)

                    var color: Int = Utils.getNextColor(context!!, oldBoxModel.color)
                    if (direction == ItemTouchHelper.RIGHT) {
                        color = Utils.getPreviousColor(context!!, oldBoxModel.color)
                    }
                    oldBoxModel.color = color

                    adapter.updateColorInFirebase(position)
                }
            }
        }).attachToRecyclerView(recyclerview)

        initFirebase()

        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }
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


    private fun replaceUmlauteForSorting(text: String): String{
        var text = text.lowercase(Locale.getDefault()).replace("ä","ae")
        text = text.replace("ö","oe")
        text = text.replace("ü","ue")
        return text
    }


    private fun filterAndSort(models: List<BoxModel>): List<BoxModel> {
        val query = searchQueryText.lowercase(Locale.getDefault())
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
        val saved_order_by_btn_id = Utils.getButtonForSortSetting(sharedPreferences.getString("settings_box_order_by", "order_by_id"))
        val saved_order_asc_desc_btn_id = Utils.getButtonForSortSettingAscDesc(sharedPreferences.getString("settings_box_order_asc_desc", "order_asc"))

        // Sort list according to settings
        when (saved_order_by_btn_id) {
            R.id.radioButtonOrderLatest -> {
                if (saved_order_asc_desc_btn_id == R.id.radioButtonOrderAscending)
                    filteredModelList.reverse()
                true
            }
            R.id.radioButtonOrderId -> {
                filteredModelList.sortWith(
                    compareBy(String.CASE_INSENSITIVE_ORDER) { replaceUmlauteForSorting(it.id) }
                )
                if (saved_order_asc_desc_btn_id == R.id.radioButtonOrderDescending)
                    filteredModelList.reverse()
                true
            }
            R.id.radioButtonOrderName -> {
                filteredModelList.sortWith(
                    compareBy(String.CASE_INSENSITIVE_ORDER) { replaceUmlauteForSorting(it.name) }
                )
                if (saved_order_asc_desc_btn_id == R.id.radioButtonOrderDescending)
                    filteredModelList.reverse()
                true
            }
            R.id.radioButtonOrderLocation -> {
                filteredModelList.sortWith(
                    compareBy(String.CASE_INSENSITIVE_ORDER) { replaceUmlauteForSorting(it.location) }
                )
                if (saved_order_asc_desc_btn_id == R.id.radioButtonOrderDescending)
                    filteredModelList.reverse()
                true
            }
            R.id.radioButtonOrderStatus -> {
                filteredModelList.sortWith(
                    compareBy(String.CASE_INSENSITIVE_ORDER) { replaceUmlauteForSorting(it.status) }
                )
                if (saved_order_asc_desc_btn_id == R.id.radioButtonOrderDescending)
                    filteredModelList.reverse()
                true
            }
            R.id.radioButtonOrderColor -> {
                filteredModelList.sortBy { it.color }
                if (saved_order_asc_desc_btn_id == R.id.radioButtonOrderDescending)
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
        FirebaseDatabase.getInstance().reference.removeEventListener(firebase_listener)
    }

    
}