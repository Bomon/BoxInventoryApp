package com.thw.inventory_app.ui.box

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.thw.inventory_app.ItemModel
import com.thw.inventory_app.R
import com.thw.inventory_app.ui.item.ItemEditFragment

class ItemsAddFragment : Fragment(), SearchView.OnQueryTextListener, ItemAddAdapter.Callbacks {

    var itemList: ArrayList<ItemModel> = ArrayList<ItemModel>()
    lateinit var adapter: ItemAddAdapter
    lateinit var recyclerview: RecyclerView
    lateinit var firebase_listener: ValueEventListener

    private var fragmentCallback: FragmentCallback? = null

    fun setFragmentCallback(callback: FragmentCallback) {
        fragmentCallback = callback
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_items, menu)
        val item = menu.findItem(R.id.items_btn_search)
        val searchView: SearchView = MenuItemCompat.getActionView(item) as SearchView
        searchView.setOnQueryTextListener(this)
        item.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
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

        //setHasOptionsMenu(true);
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_items, container, false)
        val recyclerview = view.findViewById<View>(R.id.RV_items) as RecyclerView
        adapter = ItemAddAdapter(itemList, arguments?.getSerializable("box_id") as String, false, this)
        recyclerview.layoutManager = LinearLayoutManager(activity)
        recyclerview.adapter = adapter

        initFirebase()

        val items_add_button: FloatingActionButton = view.findViewById(R.id.items_add_button)
        items_add_button.setOnClickListener(object: View.OnClickListener {
            override fun onClick(v: View?) {
                if (view != null) {
                    val itemModel: ItemModel = ItemModel("", "", "", "", "")
                    val editFragment: Fragment = ItemEditFragment.newInstance(itemModel, true)
                    val transaction: FragmentTransaction =
                        (context as FragmentActivity).supportFragmentManager.beginTransaction()
                    transaction.replace(R.id.nav_host_fragment_activity_main, editFragment)
                    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    transaction.addToBackStack("edit")
                    transaction.commit()
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
                    val tag = box.child("tag").value.toString()
                    itemList.add(ItemModel(id, name, description, tag, image))
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

    override fun handleItemSelection(data: ItemSelection) {
        if (fragmentCallback != null) {
            fragmentCallback!!.onDataSent(data.item_id)
            adapter.setFilter(itemList)
        }
    }

    override fun onDestroyView() {
        Log.w("home","destroy")
        super.onDestroyView()
        FirebaseDatabase.getInstance().reference.removeEventListener(firebase_listener)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment BoxFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(boxId: String) =
            ItemsAddFragment().apply {
                val args = Bundle()
                args.putSerializable("box_id", boxId)
                val fragment = ItemsAddFragment()
                fragment.arguments = args
                return fragment
            }
    }

}