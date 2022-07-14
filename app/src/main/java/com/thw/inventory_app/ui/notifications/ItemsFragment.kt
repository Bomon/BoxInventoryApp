package com.thw.inventory_app.ui.notifications

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.*
import com.thw.inventory_app.*
import com.thw.inventory_app.R

class ItemsFragment : Fragment(), SearchView.OnQueryTextListener {

    var itemList: ArrayList<ItemModel> = ArrayList<ItemModel>()
    lateinit var adapter: ItemAdapter
    lateinit var rv: RecyclerView
    lateinit var firebase_listener: ValueEventListener


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_items, menu)
        val itemBtn = menu.findItem(R.id.items_btn_search)
        val searchView: SearchView = MenuItemCompat.getActionView(itemBtn) as SearchView
        searchView.setOnQueryTextListener(this)
        itemBtn.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                // Do something when collapsed
                adapter.setFilter(itemList)
                return true // Return true to collapse action view
            }

            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                // Do something when expanded
                return true // Return true to expand action view
            }
        })
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //(activity as AppCompatActivity).getSupportActionBar()?.show()
        //activity?.getActionBar()?.show()

        //setHasOptionsMenu(true);
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_items, container, false)
        val recyclerview = view.findViewById<View>(R.id.RV_items) as RecyclerView
        adapter = ItemAdapter(itemList)
        adapter.setOnItemClickListener(object: ItemAdapter.OnItemClickListener{
            override fun onItemClicked(item: ItemModel, view: View) {
                val navController: NavController = Navigation.findNavController(view)
                val bundle = Bundle()
                bundle.putSerializable("itemModel", item)
                navController.navigate(R.id.action_navigation_items_to_itemFragment, bundle)
            }
        })
        recyclerview.layoutManager = LinearLayoutManager(activity)
        recyclerview.adapter = adapter

        initFirebase()

        val items_add_button: FloatingActionButton = view.findViewById(R.id.items_add_button)
        items_add_button.setOnClickListener(object: View.OnClickListener {
            override fun onClick(v: View?) {
                if (view != null) {
                    val bundle = Bundle()
                    val itemModel: ItemModel = ItemModel("", "", "", "", "")
                    bundle.putSerializable("itemModel", itemModel)
                    bundle.putSerializable("isNewItem", true)
                    val navController: NavController = Navigation.findNavController(view!!)
                    navController.navigate(R.id.action_navigation_items_to_itemEditFragment, bundle)
                }
            }
        })

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
    }

    fun initFirebase(){
        firebase_listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot){
                Log.e("Error", "Data Change")
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
                adapter.setFilter(itemList)
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        }
        FirebaseDatabase.getInstance().reference.addValueEventListener(firebase_listener)
    }

    override fun onQueryTextChange(newText: String): Boolean {
        val filteredModelList: List<ItemModel> = filter(itemList, newText)
        adapter.setFilter(filteredModelList)
        activity?.runOnUiThread {
            adapter.notifyDataSetChanged()
        }

        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    private fun filter(models: List<ItemModel>, query: String): List<ItemModel> {
        var query = query
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
        return filteredModelList
    }

    override fun onDestroyView() {
        Log.w("home","destroy")
        super.onDestroyView()
        FirebaseDatabase.getInstance().reference.removeEventListener(firebase_listener)
    }

}