package com.thw.inventory_app.ui.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.thw.inventory_app.*
import com.thw.inventory_app.R
import com.thw.inventory_app.ui.box.BoxItemModel


class HomeFragment : Fragment(), SearchView.OnQueryTextListener {

    var boxList: ArrayList<BoxModel> = ArrayList<BoxModel>()
    lateinit var adapter: BoxAdapter
    lateinit var rv: RecyclerView
    lateinit var firebase_listener: ValueEventListener
    private var doubleBackToExitPressedOnce: Boolean = false
    lateinit var exitToast: Toast


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_home, menu)
        val searchBtn = menu.findItem(R.id.home_btn_search)
        val searchView: SearchView = MenuItemCompat.getActionView(searchBtn) as SearchView
        searchView.setOnQueryTextListener(this)
        searchBtn.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                // Do something when collapsed
                adapter.setFilter(boxList)
                return true // Return true to collapse action view
            }

            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                // Do something when expanded
                return true // Return true to expand action view
            }
        })
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.home_btn_search -> {
                true
            }
            R.id.home_btn_add -> {
                if (view != null) {
                    val bundle = Bundle()
                    val boxModel: BoxModel = BoxModel("", "", "", "", "", "", "", "", R.color.default_box_color, "", ArrayList<ContentItem>())
                    bundle.putSerializable("boxModel", boxModel)
                    bundle.putSerializable("items", ArrayList<BoxItemModel>().toTypedArray())
                    bundle.putSerializable("isNewBox", true)
                    val navController: NavController = Navigation.findNavController(view!!)
                    navController.navigate(R.id.action_navigation_home_to_boxEditFragment, bundle)
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
                    val boxModel = Utils.readBoxModelFromDataSnapshot(box)
                    boxList.add(boxModel)
                }
                adapter.setFilter(boxList)
            }
            override fun onCancelled(databaseError: DatabaseError) {}
        }
        FirebaseDatabase.getInstance().reference.addValueEventListener(firebase_listener)
    }


    override fun onQueryTextChange(newText: String): Boolean {
        val filteredModelList: List<BoxModel> = filter(boxList, newText)
        adapter.setFilter(filteredModelList)
        activity?.runOnUiThread {
            adapter.notifyDataSetChanged()
        }
        return true
    }


    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }


    private fun filter(models: List<BoxModel>, query: String): List<BoxModel> {
        var query = query
        query = query.toLowerCase()
        val filteredModelList: MutableList<BoxModel> = ArrayList()
        for (model in models) {
            if (model.id.lowercase().contains(query)) {
                filteredModelList.add(model)
            } else if (model.location.lowercase().contains(query)) {
                filteredModelList.add(model)
            } else if (model.name.lowercase().contains(query)) {
                filteredModelList.add(model)
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