package com.pixlbee.heros.fragments

import android.app.AlertDialog
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.database.*
import com.stfalcon.imageviewer.StfalconImageViewer
import com.pixlbee.heros.*
import com.pixlbee.heros.R
import com.pixlbee.heros.adapters.ContainingBoxAdapter
import com.pixlbee.heros.models.BoxModel
import com.pixlbee.heros.models.ItemModel
import com.pixlbee.heros.utility.Utils


class ItemFragment : Fragment() {
    private lateinit var item_model: ItemModel
    lateinit var containing_box_adapter: ContainingBoxAdapter
    lateinit var firebase_listener: ValueEventListener
    var boxList: ArrayList<BoxModel> = ArrayList<BoxModel>()

    lateinit var item_name_field: TextView
    lateinit var item_description_field: TextView
    lateinit var item_tags_field: ChipGroup
    lateinit var item_image_field: ImageView
    lateinit var item_containing_boxes_empty_label: TextView


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
        builder.setTitle(resources.getString(R.string.dialog_delete_item_title))
        builder.setMessage(resources.getString(R.string.dialog_delete_item_text))

        builder.setPositiveButton(R.string.dialog_yes) { dialog, which ->
            Toast.makeText(requireContext(),
                "yes", Toast.LENGTH_SHORT).show()
            deleteItem()

            val navController: NavController = Navigation.findNavController(view!!)
            navController.navigateUp()
        }

        builder.setNegativeButton(R.string.dialog_no) { dialog, which ->
            Toast.makeText(requireContext(),
                "no", Toast.LENGTH_SHORT).show()
        }
        builder.show()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.item_btn_edit) {
            if(Utils.checkHasWritePermission(context)) {
                if (view != null) {
                    val bundle = Bundle()
                    bundle.putSerializable("itemModel", item_model)
                    bundle.putSerializable("isNewItem", false)
                    val navController: NavController = Navigation.findNavController(view!!)
                    navController.navigate(R.id.action_itemFragment_to_itemEditFragment, bundle)
                }
            }
            return true
        } else if (item.itemId == R.id.item_btn_delete) {
            if(Utils.checkHasWritePermission(context)) {
                showDeleteDialog()
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        (activity as AppCompatActivity).supportActionBar?.title = resources.getString(R.string.item_details_title)

        // Get the arguments from the caller fragment/activity
        item_model = arguments?.getSerializable("itemModel") as ItemModel
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
            Glide.with(this).load(R.drawable.placeholder_with_bg).into(item_image_field)
        } else {
            item_image_field.scaleType=ImageView.ScaleType.CENTER_CROP
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
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {

        val v =  inflater.inflate(R.layout.fragment_item_details, container, false)

        // Get the activity and widget
        item_name_field = v.findViewById(R.id.item_summary_name)
        item_tags_field = v.findViewById(R.id.item_summary_tags)
        item_description_field = v.findViewById(R.id.item_summary_description)
        item_image_field = v.findViewById(R.id.item_summary_image)
        item_containing_boxes_empty_label = v.findViewById(R.id.item_summary_content_empty_label)

        updateContent()

        //Init Items View
        val recyclerview = v.findViewById<View>(R.id.item_summary_containing_boxes) as RecyclerView
        containing_box_adapter = ContainingBoxAdapter(boxList, item_model.id)
        recyclerview.layoutManager = LinearLayoutManager(activity)
        recyclerview.adapter = containing_box_adapter

        initFirebase()

        return v
    }


    fun initFirebase(){
        firebase_listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot){
                boxList.clear()

                val boxes = dataSnapshot.child("boxes")
                for (box: DataSnapshot in boxes.children){
                    for (boxContent: DataSnapshot in box.child("content").children) {
                        if (boxContent.child("id").value.toString() == item_model.id) {
                            val boxModel = Utils.readBoxModelFromDataSnapshot(context, box)
                            boxList.add(boxModel)
                        }
                    }
                }
                updateContent()
                containing_box_adapter.setFilter(boxList)
                if (boxList.size == 0){
                    item_containing_boxes_empty_label.visibility = View.VISIBLE
                } else {
                    item_containing_boxes_empty_label.visibility = View.GONE
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        }
        FirebaseDatabase.getInstance().reference.addValueEventListener(firebase_listener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        FirebaseDatabase.getInstance().reference.removeEventListener(firebase_listener)
    }


}