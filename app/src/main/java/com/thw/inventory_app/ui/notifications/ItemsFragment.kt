package com.thw.inventory_app.ui.notifications

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
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialElevationScale
import com.google.firebase.database.*
import com.thw.inventory_app.*
import com.thw.inventory_app.R
import com.thw.inventory_app.ui.box.BoxEditFragment
import com.thw.inventory_app.ui.box.BoxFragment
import com.thw.inventory_app.ui.box.BoxItemModel
import com.thw.inventory_app.ui.box.ItemsAddFragment
import com.thw.inventory_app.ui.item.ItemEditFragment
import com.thw.inventory_app.ui.item.ItemFragment

class ItemsFragment : Fragment(), SearchView.OnQueryTextListener {

    var itemList: ArrayList<ItemModel> = ArrayList<ItemModel>()
    lateinit var adapter: ItemAdapter
    lateinit var rv: RecyclerView
    lateinit var firebase_listener: ValueEventListener


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
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
        adapter = ItemAdapter(itemList)
        adapter.setOnItemClickListener(object: ItemAdapter.OnItemClickListener{
            override fun setOnCLickListener(position: Int, view: View) {
                handleRecyclerViewClick(position, view)
            }
        })
        recyclerview.layoutManager = LinearLayoutManager(activity)
        recyclerview.adapter = adapter

        initFirebase()

        val items_add_button: FloatingActionButton = view.findViewById(R.id.items_add_button)
        items_add_button.setOnClickListener(object: View.OnClickListener {
            override fun onClick(v: View?) {
                if (view != null) {
                    val itemModel: ItemModel = ItemModel("", "", "", "", "")
                    val editFragment: Fragment = ItemEditFragment.newInstance(itemModel, true)
                    Utils.pushFragment(editFragment, requireContext(), "editFragment")
                    //val transaction: FragmentTransaction =
                    //    (context as FragmentActivity).supportFragmentManager.beginTransaction()
                    //transaction.replace(R.id.nav_host_fragment_activity_main, editFragment)
                    //transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    //transaction.addToBackStack("add")
                    //transaction.commit()
                }
            }
        })

        return view
    }

    fun handleRecyclerViewClick(position: Int, view: View) {

        //exitTransition = MaterialElevationScale(false).apply {
        //    duration = 5000
        //}
        //reenterTransition = MaterialElevationScale(true).apply {
        //    duration = 5000
        //}

        var transitionName: String = view.transitionName.toString()
        val newFragment: ItemFragment = ItemFragment.newInstance(itemList[position], position)
        //newFragment.sharedElementEnterTransition = getTransition()
        //newFragment.sharedElementReturnTransition = getTransition()

        newFragment.setSharedElementEnterTransition(MaterialContainerTransform())

        childFragmentManager
            .beginTransaction()
            //.setCustomAnimations(R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out)
            .setReorderingAllowed(true)
            .addSharedElement(view, transitionName)
            .replace(R.id.fragment_items, newFragment)
            .addToBackStack("")
            .commit()
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