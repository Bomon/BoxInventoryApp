package com.thw.inventory_app.ui.box

import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.stfalcon.imageviewer.StfalconImageViewer
import com.thw.inventory_app.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [BoxFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BoxFragment : Fragment(){
    // TODO: Rename and change types of parameters

    //private val box_id=itemView.findViewById<TextView>(R.id.box_id)
    //private val box_name=itemView.findViewById<TextView>(R.id.box_name)
    //private val box_location=itemView.findViewById<TextView>(R.id.box_location)
    //private val box_img=itemView.findViewById<ImageView>(R.id.box_img)

    private lateinit var box_model: BoxModel
    lateinit var box_item_adapter: BoxItemAdapter
    lateinit var firebase_listener: ValueEventListener
    var itemList: ArrayList<BoxItemModel> = ArrayList<BoxItemModel>()

    lateinit var box_id_field: TextView
    lateinit var box_name_field: TextView
    lateinit var box_description_field: TextView
    lateinit var box_location_field: TextView
    lateinit var box_status_field: ChipGroup
    lateinit var box_color_field: View

    lateinit var box_summary_image_field: ImageView
    lateinit var box_location_image_field: ImageView

    //lateinit var previous_action_bar_title: String

    //override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    //    inflater.inflate(androidx.core.R.menu.example_menu2, menu)
    //}

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_box, menu)
    }

    fun deleteBox() {
        val boxesRef = FirebaseDatabase.getInstance().reference.child("boxes")
        boxesRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val boxes: DataSnapshot? = task.result
                if (boxes != null) {
                    for (box: DataSnapshot in boxes.children) {
                        val id = box.child("id").value.toString()
                        val boxKey = box.key.toString()
                        if (id == box_model.id) {
                            FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).removeValue()
                            break
                        }
                    }
                }
            }
        }
    }

    fun showDeleteDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Box Löschen?")
        builder.setMessage("Soll die Box wirklich gelöscht werden?")

        builder.setPositiveButton("Ja") { dialog, which ->
            Toast.makeText(requireContext(),
                "yes", Toast.LENGTH_SHORT).show()
            deleteBox()
            val navController: NavController = Navigation.findNavController(view!!)
            navController.navigateUp()
            //navController.back
            //parentFragmentManager.popBackStack()
        }

        builder.setNegativeButton("Nein") { dialog, which ->
            Toast.makeText(requireContext(),
                "no", Toast.LENGTH_SHORT).show()
        }
        builder.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        var isFront =true
        if (item.itemId == R.id.box_btn_edit) {
            if (view != null) {
                val bundle = Bundle()
                bundle.putSerializable("boxModel", box_model)
                bundle.putSerializable("items", itemList.toTypedArray())
                bundle.putSerializable("isNewBox", false)
                val navController: NavController = Navigation.findNavController(view!!)
                navController.navigate(R.id.action_boxFragment_to_boxEditFragment, bundle)
            }
            return true
        } else if (item.itemId == R.id.box_btn_delete) {
            showDeleteDialog()
            return true
        } else if (item.itemId == R.id.box_btn_print) {
            if (checkPermission()) {
                var creator = BoxPdfCreator()
                creator.createPdf(context, box_model, viewLifecycleOwner)
            } else {
                requestPermission();
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun checkPermission(): Boolean {
        // checking of permissions.
        val permission1 =
            ContextCompat.checkSelfPermission(context!!, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val permission2 =
            ContextCompat.checkSelfPermission(context!!, Manifest.permission.READ_EXTERNAL_STORAGE)
        return permission1 == PackageManager.PERMISSION_GRANTED && permission2 == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        // requesting permissions if not provided.
        val PERMISSION_REQUEST_CODE = 200
        activity?.let {
            ActivityCompat.requestPermissions(
                it,
                arrayOf(WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        //val transform = MaterialContainerTransform()
        //transform.scrimColor = Color.TRANSPARENT
        //sharedElementEnterTransition = transform
        //sharedElementReturnTransition = transform
    }

    private fun updateContent(){
        val box_id = box_model.id
        val box_content = box_model.content
        val box_location = box_model.location
        val box_name = box_model.name
        val box_img: String = box_model.image
        val box_description = box_model.description
        val box_status = box_model.status
        val box_color = box_model.color
        val box_location_img = box_model.location_img

        //box_id_field.text = box_id
        box_name_field.text = box_name
        box_description_field.text = box_description
        box_color_field.background.setTint(box_color)

        if(box_description == "")
            box_description_field.visibility = View.GONE
        else
            box_description_field.visibility = View.VISIBLE

        if(box_status == "")
            box_status_field.visibility = View.GONE
        else
            box_status_field.visibility = View.VISIBLE

        //box_notes_field.text = box_notes
        box_location_field.text = box_location

        box_status_field.removeAllViews()
        for (tag in box_status.split(";")){
            if (tag != ""){
                val chip = Chip(context)
                chip.text = tag
                chip.setTextAppearance(R.style.BoxStatusChip)
                box_status_field.addView(chip)
            }
        }

        if (box_img == "") {
            Glide.with(this).load(R.drawable.placeholder_with_bg).into(box_summary_image_field)
        } else {
            box_summary_image_field.scaleType=ImageView.ScaleType.CENTER_CROP
            box_summary_image_field.setImageBitmap(Utils.StringToBitMap(box_img))
        }

        if (box_location_img == "") {
            Glide.with(this).load(R.drawable.placeholder_with_bg).into(box_location_image_field)
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

        //previous_action_bar_title = (activity as AppCompatActivity).supportActionBar?.title.toString()

        // Get the activity and widget
        //box_id_field = v.findViewById(R.id.box_summary_id)
        box_name_field = v.findViewById(R.id.box_summary_name)
        box_description_field = v.findViewById(R.id.box_summary_description)
        //box_notes_field = v.findViewById(R.id.box_summary_notes)
        box_location_field = v.findViewById(R.id.box_location_name)
        box_status_field = v.findViewById(R.id.box_summary_status)
        box_summary_image_field = v.findViewById(R.id.box_summary_image)
        box_location_image_field = v.findViewById(R.id.box_location_image)
        box_color_field = v.findViewById(R.id.box_summary_color)
        var box_container: View = v.findViewById(R.id.box_card)

        // Get the arguments from the caller fragment/activity
        box_model = arguments?.getSerializable("boxModel") as BoxModel
        updateContent()

        //Init Image Fullscreen on click
        box_summary_image_field.setOnClickListener {
            val drawables: ArrayList<Drawable> = ArrayList()
            drawables.add(box_summary_image_field.drawable)

            StfalconImageViewer.Builder(
                context, drawables
            ) { imageView, image -> Glide.with(it.context).load(image).into(imageView) }
                .withTransitionFrom(box_summary_image_field)
                .show(true)
        }
        box_location_image_field.setOnClickListener {
            val drawables: ArrayList<Drawable> = ArrayList()
            drawables.add(box_location_image_field.drawable)

            StfalconImageViewer.Builder(
                context, drawables
            ) { imageView, image -> Glide.with(it.context).load(image).into(imageView) }
                .withTransitionFrom(box_location_image_field)
                .show(true)
        }

        //Init Items View
        val recyclerview = v.findViewById<View>(R.id.box_summary_content) as RecyclerView
        box_item_adapter = BoxItemAdapter(itemList, false)
        recyclerview.layoutManager = LinearLayoutManager(activity)
        recyclerview.adapter = box_item_adapter

        (activity as AppCompatActivity).supportActionBar?.title = box_model.id

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
                        box_model = Utils.readBoxModelFromDataSnapshot(box)
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
                            itemList.add(BoxItemModel(contentItem.key, contentItem.id, contentItem.amount, contentItem.invnum, name, description, tags, contentItem.color, image))
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
        //(activity as AppCompatActivity).supportActionBar?.title = previous_action_bar_title
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
        fun newInstance(boxModel: BoxModel, position: Int) =
            BoxFragment().apply {
                val args = Bundle()
                args.putSerializable("boxModel", boxModel)
                args.putSerializable("position", position)
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