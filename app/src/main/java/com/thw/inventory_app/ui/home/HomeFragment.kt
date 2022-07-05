package com.thw.inventory_app.ui.home

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.transition.*
import android.util.Log
import android.view.*
import androidx.annotation.Nullable
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialSharedAxis
import com.google.android.material.transition.platform.MaterialFadeThrough
import com.google.firebase.database.*
import com.thw.inventory_app.*
import com.thw.inventory_app.R
import com.thw.inventory_app.ui.box.BoxEditFragment
import com.thw.inventory_app.ui.box.BoxFragment
import com.thw.inventory_app.ui.box.BoxItemModel


class HomeFragment : Fragment(), SearchView.OnQueryTextListener {

    var boxList: ArrayList<BoxModel> = ArrayList<BoxModel>()
    lateinit var adapter: BoxAdapter
    lateinit var rv: RecyclerView
    lateinit var firebase_listener: ValueEventListener

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exitTransition = MaterialContainerTransform()
        reenterTransition = MaterialContainerTransform()
        //setExitTransition(MaterialElevationScale(false));
        //setReenterTransition(MaterialElevationScale(true));
        //exitTransition = Hold()
    }

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
                    val boxModel: BoxModel = BoxModel("", "", "", "", "", "", "", "", "", "", ArrayList<ContentItem>())
                    val editFragment: Fragment = BoxEditFragment.newInstance(boxModel, ArrayList<BoxItemModel>(), true)
                    Utils.pushFragment(editFragment, requireContext(), "boxEditFragment")
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_home, container, false)
        val recyclerview = view.findViewById<View>(R.id.RV_home) as RecyclerView

        adapter = BoxAdapter(boxList)
        adapter.setOnBoxClickListener(object: BoxAdapter.OnBoxClickListener{
            override fun setOnCLickListener(position: Int, view: View) {
                handleRecyclerViewClick(position, view)
            }
        })
        recyclerview.layoutManager = LinearLayoutManager(activity)
        recyclerview.adapter = adapter

        initFirebase()

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
        val newFragment: BoxFragment = BoxFragment.newInstance(boxList[position], position)
        //newFragment.sharedElementEnterTransition = getTransition()
        //newFragment.sharedElementReturnTransition = getTransition()

        newFragment.setSharedElementEnterTransition(MaterialContainerTransform().apply {
            drawingViewId = R.id.fragment_home
            scrimColor = Color.TRANSPARENT
        })


        childFragmentManager
            .beginTransaction()
            //.setCustomAnimations(R.anim.slide_in, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out)
            .setReorderingAllowed(true)
            .addSharedElement(view, transitionName)
            .replace(R.id.fragment_home, newFragment)
            .addToBackStack("")
            .commit()
    }

    private fun getTransition(): Transition? {
        val set = TransitionSet()
        set.setOrdering(TransitionSet.ORDERING_TOGETHER)
        set.addTransition(ChangeBounds())
        set.addTransition(ChangeImageTransform())
        set.addTransition(ChangeTransform())
        return set
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        //required to animate correctly when returned from detail
        //postponeEnterTransition()
        //(requireView().parent as ViewGroup).viewTreeObserver
        //    .addOnPreDrawListener {
        //        startPostponedEnterTransition()
        //        true
        //    }
    }

    fun initFirebase() {
        firebase_listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot){
                Log.e("Error", "Data Change")
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


    override fun onDestroyView() {
        Log.w("home","destroy")
        super.onDestroyView()
        FirebaseDatabase.getInstance().reference.removeEventListener(firebase_listener)
    }


}