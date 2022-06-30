package com.thw.inventory_app.ui.box

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.thw.inventory_app.*
import com.thw.inventory_app.ContentItem
import com.thw.inventory_app.R

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [BoxFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BoxFragment : Fragment() {
    // TODO: Rename and change types of parameters

    //private val box_id=itemView.findViewById<TextView>(R.id.box_id)
    //private val box_name=itemView.findViewById<TextView>(R.id.box_name)
    //private val box_location=itemView.findViewById<TextView>(R.id.box_location)
    //private val box_img=itemView.findViewById<ImageView>(R.id.box_img)

    private lateinit var box_model: BoxModel
    lateinit var box_item_adapter: BoxItemAdapter
    lateinit var firebase_listener: ValueEventListener
    var itemList: ArrayList<BoxItemModel> = ArrayList<BoxItemModel>()

    lateinit var box_name_field: TextView
    lateinit var box_text_field: TextView
    lateinit var box_summary_image_field: ImageView
    lateinit var box_location_image_field: ImageView

    lateinit var previous_action_bar_title: String

    //override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    //    inflater.inflate(androidx.core.R.menu.example_menu2, menu)
    //}

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_box, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.box_btn_edit) {
            if (view != null) {
                val editFragment: Fragment = BoxEditFragment.newInstance(box_model, itemList, false)
                val transaction: FragmentTransaction =
                    (context as FragmentActivity).supportFragmentManager.beginTransaction()
                transaction.replace(R.id.nav_host_fragment_activity_main, editFragment)
                //transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                transaction.addToBackStack("edit")
                transaction.commit()
            }


        } else if (item.itemId == R.id.box_btn_delete) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        //arguments?.let {
        //    box_model = it.getBundle("model")
            //param1 = it.getString(ARG_PARAM1)
            //param2 = it.getString(ARG_PARAM2)
        //}
    }

    private fun updateContent(){
        val box_id = box_model.id
        val box_content = box_model.content
        val box_location = box_model.location
        val box_name = box_model.name
        val box_img = box_model.img
        val box_location_img = box_model.location_img

        box_name_field.text = box_name
        box_text_field.text = box_location

        if (box_img == "") {
            //val myLogo = (ResourcesCompat.getDrawable(this.resources, R.drawable.ic_baseline_photo_size_select_actual_24, null) as VectorDrawable).toBitmap()
            //box_image_field.setImageDrawable(resources.getDrawable(R.drawable.))

            //box_image_field.setImageBitmap(myLogo)
        } else {
            box_summary_image_field.setImageBitmap(Utils.StringToBitMap(box_img))
        }

        if (box_location_img == "") {
            //val myLogo = (ResourcesCompat.getDrawable(this.resources, R.drawable.ic_baseline_photo_size_select_actual_24, null) as VectorDrawable).toBitmap()
            //box_image_field.setImageDrawable(resources.getDrawable(R.drawable.))

            //box_image_field.setImageBitmap(myLogo)
        } else {
            box_location_image_field.setImageBitmap(Utils.StringToBitMap(box_location_img))
        }

        (activity as AppCompatActivity).supportActionBar?.title = box_model.id
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val v =  inflater.inflate(R.layout.fragment_box, container, false)

        previous_action_bar_title = (activity as AppCompatActivity).supportActionBar?.title.toString()
        // Get the activity and widget
        box_name_field = v.findViewById(R.id.box_summary_name)
        box_text_field = v.findViewById(R.id.box_location_name)
        box_summary_image_field = v.findViewById(R.id.box_summary_image)
        box_location_image_field = v.findViewById(R.id.box_location_image)

        // Get the arguments from the caller fragment/activity
        box_model = arguments?.getSerializable("model") as BoxModel
        updateContent()

        //Init Items View
        val recyclerview = v.findViewById<View>(R.id.box_summary_content) as RecyclerView
        box_item_adapter = BoxItemAdapter(itemList, false)
        recyclerview.layoutManager = LinearLayoutManager(activity)
        recyclerview.adapter = box_item_adapter

        initFirebase()

        return v
    }

    fun initFirebase(){
        firebase_listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot){
                Log.e("Error", "Data Change")
                itemList.clear()

                val boxes = dataSnapshot.child("boxes")
                for (box: DataSnapshot in boxes.children){
                    if (box.child("id").value.toString() == box_model.id){
                        val contentList = ArrayList<ContentItem>()
                        for (c: DataSnapshot in box.child("content").children){
                            val itemAmount = c.child("amount").value.toString()
                            val itemId = c.child("id").value.toString()
                            val itemInvNum = c.child("invnum").value.toString()
                            contentList.add(ContentItem(c.key.toString(), itemAmount, itemId, itemInvNum))
                        }

                        val image = box.child("image").value.toString()
                        val location_img = box.child("location_image").value.toString()
                        val location = box.child("location").value.toString()
                        val id = box.child("id").value.toString()
                        val name = box.child("name").value.toString()
                        val qrcode = box.child("qrcode").value.toString()
                        box_model = BoxModel(id, name, qrcode, location, image, location_img, contentList)
                        updateContent()
                        break
                    }
                }



                val items = dataSnapshot.child("items")
                for (contentItem: ContentItem in box_model.content){
                    for (item: DataSnapshot in items.children){
                        val image = item.child("image").value.toString()
                        val name = item.child("name").value.toString()
                        val description = item.child("description").value.toString()
                        val tags = item.child("tags").value.toString()
                        val itemid = item.child("id").value.toString()
                        if (itemid == contentItem.id) {
                            itemList.add(BoxItemModel(contentItem.key, contentItem.id, contentItem.amount, contentItem.invnum, name, description, tags, image))
                        }
                    }
                }
                box_item_adapter.setFilter(itemList)
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        }
        FirebaseDatabase.getInstance().reference.addValueEventListener(firebase_listener)
    }

    override fun onDestroyView() {
        Log.w("box","destroy")
        super.onDestroyView()
        (activity as AppCompatActivity).supportActionBar?.title = previous_action_bar_title
        FirebaseDatabase.getInstance().reference.removeEventListener(firebase_listener)
        //_binding = null
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param boxModel Parameter 1.
         * @return A new instance of fragment BoxFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(boxModel: BoxModel) =
            BoxFragment().apply {
                val args = Bundle()
                args.putSerializable("model", boxModel)
                val fragment = BoxFragment()
                fragment.arguments = args
                return fragment
                //arguments = Bundle().apply {
                //    putString(ARG_PARAM1, param1)
                //    putString(ARG_PARAM2, param2)
                //}
            }
    }
}