package com.pixlbee.heros.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialSharedAxis
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.pixlbee.heros.*
import com.pixlbee.heros.adapters.ItemAdapter
import com.pixlbee.heros.adapters.ItemAddAdapter
import com.pixlbee.heros.models.ItemModel
import java.util.*
import kotlin.collections.ArrayList

class ItemsAddFragment : Fragment(), SearchView.OnQueryTextListener {

    var itemList: ArrayList<ItemModel> = ArrayList<ItemModel>()
    lateinit var adapter: ItemAdapter
    lateinit var recyclerview: RecyclerView
    lateinit var firebase_listener: ValueEventListener
    private var searchQueryText: String = ""


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_items, menu)
        val item = menu.findItem(R.id.items_btn_search)
        val searchView: SearchView = MenuItemCompat.getActionView(item) as SearchView
        searchView.setOnQueryTextListener(this)
        item.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                // Do something when collapsed
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
        (activity as AppCompatActivity).supportActionBar?.title = resources.getString(R.string.items_add_fragment_title)
        exitTransition = Hold()

        val transformEnter = MaterialContainerTransform(requireContext(), true)
        transformEnter.scrimColor = Color.TRANSPARENT
        sharedElementEnterTransition = transformEnter

        val transformReturn = MaterialContainerTransform(requireContext(), false)
        transformReturn.scrimColor = Color.TRANSPARENT
        sharedElementReturnTransition = transformReturn

        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view: View = inflater.inflate(R.layout.fragment_items, container, false)
        val recyclerview = view.findViewById<View>(R.id.RV_items) as RecyclerView
        adapter = ItemAdapter(itemList)
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

        recyclerview.layoutManager = LinearLayoutManager(activity)
        recyclerview.adapter = adapter

        initFirebase()

        val items_add_button: FloatingActionButton = view.findViewById(R.id.items_add_button)
        items_add_button.setOnClickListener(object: View.OnClickListener {
            override fun onClick(v: View?) {
                if (view != null) {
                    //val bundle = Bundle()
                    val itemModel: ItemModel = ItemModel("", "", "", "", "")
                    //bundle.putSerializable("itemModel", itemModel)
                    //bundle.putSerializable("isNewItem", true)
                    //bundle.putSerializable("isBoxEditMode", true)
                    //val navController: NavController = Navigation.findNavController(view!!)
                    //navController.navigate(R.id.action_itemsAddFragment_to_itemEditFragment, bundle)

                    exitTransition = Hold()
                    val extras = FragmentNavigatorExtras(
                        view to "transition_add_item"
                    )
                    val navController: NavController = Navigation.findNavController(view)
                    findNavController().navigate(ItemsAddFragmentDirections.actionItemsAddFragmentToItemEditFragment(itemModel, true), extras)
                }
            }
        })

        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        val navController: NavController = Navigation.findNavController(view!!)
        // receive new item from ItemEditFragment
        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<String>("item_id")?.observe(
            viewLifecycleOwner) { result ->
            // Push this back to the BoxEditFragment
            navController.previousBackStackEntry?.savedStateHandle?.set("item_id", result)
            navController.popBackStack()
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
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        }
        FirebaseDatabase.getInstance().reference.addValueEventListener(firebase_listener)
    }


    override fun onQueryTextChange(newText: String): Boolean {
        searchQueryText = newText
        adapter.setFilter(filterAndSort(itemList))

        return true
    }


    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }


    private fun filterAndSort(models: List<ItemModel>): List<ItemModel> {
        var query = searchQueryText.toLowerCase()
        query = query.toLowerCase()
        val filteredModelList: MutableList<ItemModel> = java.util.ArrayList()
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
        super.onDestroyView()
        FirebaseDatabase.getInstance().reference.removeEventListener(firebase_listener)
    }

}