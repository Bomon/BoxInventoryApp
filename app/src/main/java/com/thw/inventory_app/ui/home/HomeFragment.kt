package com.thw.inventory_app.ui.home

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import co.lujun.androidtagview.TagContainerLayout
import co.lujun.androidtagview.TagView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.thw.inventory_app.*
import com.thw.inventory_app.R
import com.thw.inventory_app.ui.box.BoxEditFragment
import com.thw.inventory_app.ui.box.BoxFragment
import com.thw.inventory_app.ui.box.BoxItemModel


class HomeFragment : Fragment(), SearchView.OnQueryTextListener {

    var boxList: ArrayList<BoxModel> = ArrayList<BoxModel>()
    lateinit var adapter: BoxAdapter
    lateinit var recyclerview: RecyclerView
    lateinit var filter_dialog: Dialog
    lateinit var firebase_listener: ValueEventListener

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buildDialog()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_home, menu)
        val item = menu.findItem(R.id.home_btn_search)
        val searchView: SearchView = MenuItemCompat.getActionView(item) as SearchView
        searchView.setOnQueryTextListener(this)
        item.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
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
                //filter_dialog.show()
                if (view != null) {
                    val boxModel: BoxModel = BoxModel("", "", "", "", "", "", ArrayList<ContentItem>())
                    val editFragment: Fragment = BoxEditFragment.newInstance(boxModel, ArrayList<BoxItemModel>(), true)
                    val transaction: FragmentTransaction =
                        (context as FragmentActivity).supportFragmentManager.beginTransaction()
                    transaction.replace(R.id.nav_host_fragment_activity_main, editFragment)
                    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    transaction.addToBackStack("edit")
                    transaction.commit()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun buildDialog() {
        filter_dialog = Dialog(requireActivity())
        filter_dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        filter_dialog.setCancelable(false)
        filter_dialog.setContentView(R.layout.dialog_filter)

        val tags: ArrayList<String> = ArrayList()
        tags.add("Tag1")
        tags.add("Tag2")
        tags.add("Tag3")
        tags.add("Tag4")
        val mTagContainerLayout = filter_dialog.findViewById(R.id.filter_tags) as TagContainerLayout
        mTagContainerLayout.setTags(tags)

        //val body = dialog.findViewById(R.id.body) as TextView
        //body.text = title
        val yesBtn = filter_dialog.findViewById(R.id.btnfollow) as Button
        //val noBtn = dialog.findViewById(R.id.noBtn) as TextView
        yesBtn.setOnClickListener {
            filter_dialog.dismiss()
        }
        //noBtn.setOnClickListener { dialog.dismiss() }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_home, container, false)
        val recyclerview = view.findViewById<View>(R.id.RV_home) as RecyclerView
        adapter = BoxAdapter(boxList, false, R.layout.card_box)
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
                Log.e("Error", "Data Change")
                boxList.clear()
                val boxes = dataSnapshot.child("boxes")
                for (box: DataSnapshot in boxes.children){
                    val image = box.child("image").value.toString()
                    val location_image = box.child("location_image").value.toString()
                    val location = box.child("location").value.toString()
                    val id = box.child("id").value.toString()
                    val name = box.child("name").value.toString()
                    val qrcode = box.child("qrcode").value.toString()
                    val content = box.child("content")

                    val contentList = ArrayList<ContentItem>()
                    for (c: DataSnapshot in content.children){
                        val itemAmount = c.child("amount").value.toString()
                        val itemId = c.child("id").value.toString()
                        val itemInvNum = c.child("invnum").value.toString()
                        contentList.add(ContentItem(itemAmount, itemId, itemInvNum))
                    }
                    boxList.add(BoxModel(id, name, qrcode, location, image, location_image, contentList))
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