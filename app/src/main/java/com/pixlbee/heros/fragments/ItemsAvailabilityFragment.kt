package com.pixlbee.heros.fragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.ArrayMap
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.pixlbee.heros.R
import com.pixlbee.heros.adapters.ItemAvailabilityAdapter
import com.pixlbee.heros.models.ItemAvailabilityModel
import com.pixlbee.heros.models.ItemModel
import com.pixlbee.heros.utility.Utils
import java.util.*


open class ItemsAvailabilityFragment() : Fragment(), SearchView.OnQueryTextListener {

    var mItemList: ArrayList<ItemAvailabilityModel> = ArrayList<ItemAvailabilityModel>()
    lateinit var mAdapter: ItemAvailabilityAdapter
    private lateinit var mFirebaseListener: ValueEventListener
    private var searchQueryText: String = ""
    private lateinit var searchView: SearchView
    private lateinit var searchBtn: MenuItem

    private lateinit var animationType: String

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // don't inflate menu, but use the one from parent
        if (menu.findItem(R.id.items_btn_search) != null) {
            searchBtn = menu.findItem(R.id.items_btn_search)
            searchView = MenuItemCompat.getActionView(searchBtn) as SearchView
            searchView.setOnQueryTextListener(this)
            searchBtn.setOnActionExpandListener(object :
                MenuItem.OnActionExpandListener {
                override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                    // Do something when collapsed
                    searchQueryText = ""
                    setFilterAndSort(mItemList)
                    return true // Return true to collapse action view
                }

                override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                    // Do something when expanded
                    return true // Return true to expand action view
                }
            })
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = context!!.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        animationType = sharedPreferences.getString("animation_type", "simple").toString()

        if (animationType == "elegant") {
            val transformEnter = MaterialContainerTransform(requireContext(), true)
            transformEnter.scrimColor = Color.TRANSPARENT
            sharedElementEnterTransition = transformEnter

            val transformReturn = MaterialContainerTransform(requireContext(), false)
            transformReturn.scrimColor = Color.TRANSPARENT
            sharedElementReturnTransition = transformReturn

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

        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_items_availability, container, false)
        val recyclerview = view.findViewById<View>(R.id.RV_items_availability) as RecyclerView

        mAdapter = ItemAvailabilityAdapter()
        mAdapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

        mAdapter.setOnItemClickListener(object: ItemAvailabilityAdapter.OnItemClickListener{
            override fun onItemClicked(item: ItemAvailabilityModel, view: View) {
                if (animationType == "elegant") {
                    exitTransition = Hold()
                }
                val extras = FragmentNavigatorExtras(
                    view to item.item.id
                )
                val navController: NavController = Navigation.findNavController(view)
                navController.navigate(ItemsOverviewFragmentDirections.actionNavigationItemsToItemFragment(item.item), extras)
            }

            override fun onItemTagClicked(tag: String) {
                searchBtn.expandActionView()
                searchView.setQuery(tag, true)
            }
        })

        recyclerview.layoutManager = LinearLayoutManager(activity)
        recyclerview.adapter = mAdapter

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


    private fun initFirebase(){
        mFirebaseListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot){
                mItemList.clear()

                var itemMap = ArrayMap<String, ItemAvailabilityModel>()
                val boxes = dataSnapshot.child(Utils.getCurrentlySelectedOrg(context!!)).child("boxes")
                for (box: DataSnapshot in boxes.children){
                    val boxId = box.child("id").value.toString()
                    for (boxContent: DataSnapshot in box.child("content").children) {
                        val contentItemId = boxContent.child("id").value.toString()
                        var itemAmountTaken = boxContent.child("amount_taken").value.toString()
                        if (itemAmountTaken == "null"){
                            itemAmountTaken = "0"
                        }
                        if (itemAmountTaken != "0"){
                            // Create entry if not exist
                            if (!itemMap.containsKey(contentItemId)) {
                                var initialItemModel = ItemModel(contentItemId, "", "", "", "")
                                var initialTakenMap = ArrayMap<String, Int>()
                                initialTakenMap[boxId] = itemAmountTaken.toInt()
                                itemMap[contentItemId] = ItemAvailabilityModel(initialItemModel, initialTakenMap)
                            // Add to entry
                            } else {
                                itemMap[contentItemId]?.taken?.put(boxId, itemAmountTaken.toInt())
                            }
                        }
                    }
                }

                val items = dataSnapshot.child(Utils.getCurrentlySelectedOrg(context!!)).child("items")
                for (item: DataSnapshot in items.children){
                    val id = item.child("id").value.toString()
                    if (itemMap.keys.contains(id)) {
                        itemMap[id]?.item!!.name = item.child("name").value.toString()
                        itemMap[id]?.item!!.description = item.child("description").value.toString()
                        itemMap[id]?.item!!.tags = item.child("tags").value.toString()
                        itemMap[id]?.item!!.image = item.child("image").value.toString()
                    }
                }

                mItemList = ArrayList(itemMap.values)
                setFilterAndSort(mItemList)
                mAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        }
        FirebaseDatabase.getInstance().reference.addValueEventListener(mFirebaseListener)
    }

    override fun onQueryTextChange(newText: String): Boolean {
        searchQueryText = newText
        setFilterAndSort(mItemList)
        //activity?.runOnUiThread {
        //    adapter.notifyDataSetChanged()
        //}

        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    private fun setFilterAndSort(models: List<ItemAvailabilityModel>) {
        var query = searchQueryText.lowercase(Locale.getDefault())
        query = query.lowercase(Locale.getDefault())
        val filteredModelList: MutableList<ItemAvailabilityModel> = ArrayList()
        for (item_model in models) {
            if (item_model.item.description.lowercase().contains(query)) {
                filteredModelList.add(item_model)
            } else if (item_model.item.id.lowercase().contains(query)) {
                filteredModelList.add(item_model)
            } else if (item_model.item.name.lowercase().contains(query)) {
                filteredModelList.add(item_model)
            } else if (item_model.item.tags.lowercase().contains(query)) {
                filteredModelList.add(item_model)
            }
        }

        filteredModelList.sortWith(
            compareBy(String.CASE_INSENSITIVE_ORDER) {
                var name = it.item.name.lowercase(Locale.getDefault()).replace("ä","ae")
                name = name.replace("ö","oe")
                name = name.replace("ü","ue")
                name
            }
        )

        mAdapter.setFilter(filteredModelList)

        // search for inv nums can take longer, so we do this afterwards
        if (query.length > 2) {
            asyncSearchInvNum(models, filteredModelList)
        }

    }


    private fun asyncSearchInvNum(models: List<ItemAvailabilityModel>, filteredModels: MutableList<ItemAvailabilityModel>) {
        var query = searchQueryText.lowercase(Locale.getDefault())
        query = query.lowercase(Locale.getDefault())

        val boxesRef = FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("boxes")
        boxesRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val boxes: DataSnapshot? = task.result
                if (boxes != null) {
                    var resultsUpdated = false
                    var foundItemIds = ArrayList<String>()
                    // search all boxes for queried invnum
                    for (box: DataSnapshot in boxes.children) {
                        for (item: DataSnapshot in box.child("content").children) {
                            val contentInvNumber = item.child("invnum").value.toString()
                            if (query in contentInvNumber){
                                val itemId = item.child("id").value.toString()
                                foundItemIds.add(itemId)
                            }
                        }
                    }
                    // Add found items
                    for (mItemModel in models) {
                        if (foundItemIds.contains(mItemModel.item.id)){
                            if (mItemModel !in filteredModels) {
                                resultsUpdated = true
                                filteredModels.add(mItemModel)
                            }
                        }
                    }

                    // only update search results if there were changes
                    if (resultsUpdated){
                        filteredModels.sortWith(
                            compareBy(String.CASE_INSENSITIVE_ORDER) {
                                var name = it.item.name.lowercase(Locale.getDefault()).replace("ä","ae")
                                name = name.replace("ö","oe")
                                name = name.replace("ü","ue")
                                name
                            }
                        )

                        mAdapter.setFilter(filteredModels)
                    }
                }
            }
        }

    }


    override fun onDestroyView() {
        super.onDestroyView()
        FirebaseDatabase.getInstance().reference.removeEventListener(mFirebaseListener)
    }

}