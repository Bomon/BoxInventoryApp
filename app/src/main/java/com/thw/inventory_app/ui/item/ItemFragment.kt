package com.thw.inventory_app.ui.item

import android.app.AlertDialog
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.database.*
import com.stfalcon.imageviewer.StfalconImageViewer
import com.thw.inventory_app.*
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
class ItemFragment : Fragment() {
    private lateinit var item_model: ItemModel
    lateinit var containing_box_adapter: ContainingBoxAdapter
    lateinit var firebase_listener: ValueEventListener
    var boxList: ArrayList<BoxModel> = ArrayList<BoxModel>()

    lateinit var item_name_field: TextView
    lateinit var item_description_field: TextView
    lateinit var item_tags_field: ChipGroup
    lateinit var item_image_field: ImageView

    lateinit var previous_action_bar_title: String

    //override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    //    inflater.inflate(androidx.core.R.menu.example_menu2, menu)
    //}

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_item, menu)
    }

    fun deleteItem() {
        // remove from boxes
        val boxesRef = FirebaseDatabase.getInstance().reference.child("boxes")
        boxesRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val boxes: DataSnapshot? = task.result
                if (boxes != null) {
                    for (box: DataSnapshot in boxes.children) {
                        for (boxContent: DataSnapshot in box.child("content").children) {
                            val content_item_id = boxContent.child("id").value.toString()
                            if (content_item_id == item_model.id) {
                                val boxKey = box.key.toString()
                                val contentKey = boxContent.key.toString()
                                FirebaseDatabase.getInstance().reference.child("boxes")
                                    .child(boxKey).child("content").child(contentKey)
                                    .removeValue()
                            }
                        }
                    }
                }
            }
        }

        val itemsRef = FirebaseDatabase.getInstance().reference.child("items")
        itemsRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val items: DataSnapshot? = task.result
                if (items != null) {
                    for (item: DataSnapshot in items.children) {
                        val id = item.child("id").value.toString()
                        val itemKey = item.key.toString()
                        if (id == item_model.id) {
                            FirebaseDatabase.getInstance().reference.child("items").child(itemKey).removeValue()
                            break
                        }
                    }
                }
            }
        }
    }

    fun showDeleteDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Gegenstand Löschen?")
        builder.setMessage("Achtung: Beim Löschen wird der Geegenstand automatisch aus allen Boxen entfernt")

        builder.setPositiveButton("Ja") { dialog, which ->
            Toast.makeText(requireContext(),
                "yes", Toast.LENGTH_SHORT).show()
           deleteItem()
            parentFragmentManager.popBackStack()
        }

        builder.setNegativeButton("Nein") { dialog, which ->
            Toast.makeText(requireContext(),
                "no", Toast.LENGTH_SHORT).show()
        }
        builder.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.item_btn_edit) {
            if (view != null) {
                val editFragment: Fragment = ItemEditFragment.newInstance(item_model, false)
                Utils.pushFragment(editFragment, requireContext(), "itemEditFragment")
                //val transaction: FragmentTransaction =
                //    (context as FragmentActivity).supportFragmentManager.beginTransaction()
                //transaction.replace(R.id.nav_host_fragment_activity_main, editFragment)
                //transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                //transaction.addToBackStack("edit")
                //transaction.commit()
            }
            return true
        } else if (item.itemId == R.id.item_btn_delete) {
            showDeleteDialog()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    private fun updateContent(){
        val item_id = item_model.id
        val item_name = item_model.name
        val item_description = item_model.description
        val item_tags = item_model.tags
        val item_image = item_model.image

        if (item_description == ""){
            item_description_field.visibility = View.GONE
        } else {
            item_description_field.visibility = View.VISIBLE
        }

       // if (item_tags == ""){
        //    item_tags_field.visibility = View.GONE
       // }

        item_name_field.text = item_name
        item_description_field.text = item_description

        item_tags_field.removeAllViews()
        if(item_tags != ""){
            for (tag in item_tags.split(";")){
                if (tag != ""){
                    val chip = Chip(context)
                    chip.text = tag
                    item_tags_field.addView(chip)
                }
            }
        }

        if (item_image == "") {
            Glide.with(this).load(R.drawable.ic_placeholder).into(item_image_field)
        } else {
            item_image_field.setImageBitmap(Utils.StringToBitMap(item_image))
        }


        item_image_field.setOnClickListener {
            val drawables: ArrayList<Drawable> = ArrayList()
            drawables.add(item_image_field.drawable)

            StfalconImageViewer.Builder(
                context, drawables
            ) { imageView, image -> Glide.with(it.context).load(image).into(imageView) }
                .withTransitionFrom(item_image_field)
                .show(true)
        }

        (activity as AppCompatActivity).supportActionBar?.title = item_name
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val v =  inflater.inflate(R.layout.fragment_item_details, container, false)

        previous_action_bar_title = (activity as AppCompatActivity).supportActionBar?.title.toString()
        // Get the activity and widget
        item_name_field = v.findViewById(R.id.item_summary_name)
        item_tags_field = v.findViewById(R.id.item_summary_tags)
        item_description_field = v.findViewById(R.id.item_summary_description)
        item_image_field = v.findViewById(R.id.item_summary_image)

        // Get the arguments from the caller fragment/activity
        item_model = arguments?.getSerializable("model") as ItemModel

        var transitionName: String = "itemTransition" + (arguments?.getSerializable("position") as Int).toString()
        item_image_field.transitionName = transitionName

        updateContent()

        //Init Items View
        val recyclerview = v.findViewById<View>(R.id.item_summary_containing_boxes) as RecyclerView
        containing_box_adapter = ContainingBoxAdapter(boxList, item_model.id, false)
        recyclerview.layoutManager = LinearLayoutManager(activity)
        recyclerview.adapter = containing_box_adapter

        initFirebase()

        return v
    }

    fun initFirebase(){
        firebase_listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot){
                Log.e("Error", "Data Change")
                boxList.clear()

                val boxes = dataSnapshot.child("boxes")
                for (box: DataSnapshot in boxes.children){
                    for (boxContent: DataSnapshot in box.child("content").children) {
                        if (boxContent.child("id").value.toString() == item_model.id) {
                            val boxModel = Utils.readBoxModelFromDataSnapshot(box)
                            boxList.add(boxModel)
                        }
                    }
                }
                updateContent()
                containing_box_adapter.setFilter(boxList)
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
        fun newInstance(itemModel: ItemModel, position: Int) =
            ItemFragment().apply {
                val args = Bundle()
                args.putSerializable("model", itemModel)
                args.putSerializable("position", position)
                val fragment = ItemFragment()
                fragment.arguments = args
                return fragment
                //arguments = Bundle().apply {
                //    putString(ARG_PARAM1, param1)
                //    putString(ARG_PARAM2, param2)
                //}
            }
    }
}