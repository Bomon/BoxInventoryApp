package com.pixlbee.heros.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.app.SharedElementCallback
import androidx.core.view.MenuItemCompat
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.transition.*
import com.google.firebase.database.*
import com.pixlbee.heros.R
import com.pixlbee.heros.adapters.ItemAdapter
import com.pixlbee.heros.models.ItemModel
import com.pixlbee.heros.utility.Utils
import java.util.*
import androidx.core.view.ViewPropertyAnimatorListenerAdapter

import androidx.core.view.ViewCompat





class ItemsFragment : Fragment(), SearchView.OnQueryTextListener {

    private var return_item_instead_of_show_details: Boolean = false
    var itemList: ArrayList<ItemModel> = ArrayList<ItemModel>()
    lateinit var adapter: ItemAdapter
    lateinit var rv: RecyclerView
    lateinit var firebase_listener: ValueEventListener
    private var searchQueryText: String = ""


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_items, menu)
        val itemBtn = menu.findItem(R.id.items_btn_search)
        val searchView: SearchView = MenuItemCompat.getActionView(itemBtn) as SearchView
        searchView.setOnQueryTextListener(this)
        itemBtn.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                // Do something when collapsed
                searchQueryText = ""
                adapter.setFilter(filterAndSort(itemList))
                return true // Return true to collapse action view
            }

            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                // Do something when expanded
                return true // Return true to expand action view
            }
        })
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val transformEnter = MaterialContainerTransform(requireContext(), true)
        transformEnter.scrimColor = Color.TRANSPARENT
        sharedElementEnterTransition = transformEnter

        val transformReturn = MaterialContainerTransform(requireContext(), false)
        transformReturn.scrimColor = Color.TRANSPARENT
        sharedElementReturnTransition = transformReturn

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        exitTransition = MaterialFadeThrough()

        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_items, container, false)
        val recyclerview = view.findViewById<View>(R.id.RV_items) as RecyclerView

        adapter = ItemAdapter(itemList)
        adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

        return_item_instead_of_show_details = arguments?.getBoolean("return_item_instead_of_show_details") as Boolean
        if (return_item_instead_of_show_details){
            adapter.setOnItemClickListener(object: ItemAdapter.OnItemClickListener{
                override fun onItemClicked(item: ItemModel, view: View) {
                    var item_id = item.id
                    //adapter.setFilter(itemList)
                    val navController: NavController = Navigation.findNavController(view!!)
                    // push the selected item back to BoxEditFragment
                    navController.previousBackStackEntry?.savedStateHandle?.set("item_id", item_id)
                    navController.popBackStack()
                }
            })
        } else {
            adapter.setOnItemClickListener(object: ItemAdapter.OnItemClickListener{
                override fun onItemClicked(item: ItemModel, view: View) {
                    exitTransition = Hold()
                    val extras = FragmentNavigatorExtras(
                        view to item.id
                    )
                    val navController: NavController = Navigation.findNavController(view)
                    navController.navigate(ItemsFragmentDirections.actionNavigationItemsToItemFragment(item), extras)
                }
            })
        }


        recyclerview.layoutManager = LinearLayoutManager(activity)
        recyclerview.adapter = adapter

        initFirebase()

        val items_add_button: FloatingActionButton = view.findViewById(R.id.items_add_button)
        items_add_button.setOnClickListener(object: View.OnClickListener {
            override fun onClick(viwe: View?) {
                if (viwe != null) {
                    if(Utils.checkHasWritePermission(context)) {
                        val itemModel: ItemModel = ItemModel("", "", "", "", "")
                        exitTransition = Hold()
                        val extras = FragmentNavigatorExtras(
                            viwe to "transition_add_item"
                        )
                        findNavController().navigate(ItemsFragmentDirections.actionNavigationItemsToItemEditFragment(itemModel, true), extras)
                    }
                }
            }
        })

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }

        if (return_item_instead_of_show_details) {
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


    fun initFirebase(){
        firebase_listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot){
                itemList.clear()
                val boxes = dataSnapshot.child("items")
                for (box: DataSnapshot in boxes.children){
                    val description = box.child("description").value.toString()
                    val id = box.child("id").value.toString()
                    val image = box.child("image").value.toString()
                    val name = box.child("name").value.toString()
                    val tags = box.child("tags").value.toString()
                    itemList.add(ItemModel(id, name, description, tags, image))
                }
                adapter.setFilter(filterAndSort(itemList))
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        }
        FirebaseDatabase.getInstance().reference.addValueEventListener(firebase_listener)
    }

    override fun onQueryTextChange(newText: String): Boolean {
        searchQueryText = newText
        adapter.setFilter(filterAndSort(itemList))
        //activity?.runOnUiThread {
        //    adapter.notifyDataSetChanged()
        //}

        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    private fun filterAndSort(models: List<ItemModel>): List<ItemModel> {
        var query = searchQueryText.toLowerCase()
        query = query.toLowerCase()
        val filteredModelList: MutableList<ItemModel> = ArrayList()
        for (item_model in models) {
            if (item_model.description.lowercase().contains(query)) {
                filteredModelList.add(item_model)
            } else if (item_model.id.lowercase().contains(query)) {
                filteredModelList.add(item_model)
            } else if (item_model.name.lowercase().contains(query)) {
                filteredModelList.add(item_model)
            } else if (item_model.tags.lowercase().contains(query)) {
                filteredModelList.add(item_model)
            }
        }

        filteredModelList.sortWith(
            compareBy(String.CASE_INSENSITIVE_ORDER) {
                var name = it.name.lowercase(Locale.getDefault()).replace("ä","ae")
                name = name.replace("ö","oe")
                name = name.replace("ü","ue")
                name
            }
        )

        return filteredModelList
    }

    override fun onDestroyView() {
        Log.w("home","destroy")
        super.onDestroyView()
        FirebaseDatabase.getInstance().reference.removeEventListener(firebase_listener)
    }

}