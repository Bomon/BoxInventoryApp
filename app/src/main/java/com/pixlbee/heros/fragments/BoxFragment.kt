package com.pixlbee.heros.fragments

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialSharedAxis
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.pixlbee.heros.R
import com.pixlbee.heros.adapters.BoxItemAdapter
import com.pixlbee.heros.models.BoxItemModel
import com.pixlbee.heros.models.BoxModel
import com.pixlbee.heros.models.ContentItem
import com.pixlbee.heros.utility.BoxPdfCreator
import com.pixlbee.heros.utility.Utils
import com.stfalcon.imageviewer.StfalconImageViewer


class BoxFragment : Fragment(){

    private lateinit var mBoxModel: BoxModel
    lateinit var mBoxItemAdapter: BoxItemAdapter
    private lateinit var mFirebaseListener: ValueEventListener
    var mItemList: ArrayList<BoxItemModel> = ArrayList()

    private lateinit var boxNameField: TextView
    private lateinit var boxDescriptionField: TextView
    private lateinit var boxLocationField: TextView
    private lateinit var boxStatusField: ChipGroup
    private lateinit var boxColorField: View

    private lateinit var boxSummaryImageField: ImageView
    private lateinit var boxLocationImageField: ImageView


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_box, menu)
    }


    private fun deleteBox() {
        val boxesRef = FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("boxes")
        boxesRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val boxes: DataSnapshot? = task.result
                if (boxes != null) {
                    for (box: DataSnapshot in boxes.children) {
                        val id = box.child("id").value.toString()
                        val boxKey = box.key.toString()
                        if (id == mBoxModel.id) {
                            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("boxes").child(boxKey).removeValue()
                            break
                        }
                    }
                }
            }
        }
    }


    private fun showDeleteDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(resources.getString(R.string.dialog_delete_box_title))
        builder.setMessage(resources.getString(R.string.dialog_delete_box_text))

        builder.setPositiveButton(resources.getString(R.string.dialog_yes)) { _, _ ->
            deleteBox()
            val navController: NavController = Navigation.findNavController(view!!)
            navController.navigateUp()
        }

        builder.setNegativeButton(resources.getString(R.string.dialog_no)) { _, _ ->
        }
        builder.show()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.box_btn_edit -> {
                if (Utils.checkHasWritePermission(context)) {
                    if (view != null) {
                        val bundle = Bundle()
                        bundle.putSerializable("boxModel", mBoxModel)
                        bundle.putSerializable("items", mItemList.toTypedArray())
                        bundle.putSerializable("isNewBox", false)
                        val navController: NavController = Navigation.findNavController(view!!)
                        navController.navigate(R.id.action_boxFragment_to_boxEditFragment, bundle)
                    }
                }
                return true
            }
            R.id.box_btn_delete -> {
                if (Utils.checkHasWritePermission(context)) {
                    showDeleteDialog()
                }
                return true
            }
            R.id.box_btn_print -> {
                if (checkPermission()) {
                    val creator = BoxPdfCreator()
                    creator.createPdf(context, mBoxModel, viewLifecycleOwner)
                } else {
                    requestPermission()
                }
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }


    private fun checkPermission(): Boolean {
        // checking of permissions.
        val permission1 =
            ContextCompat.checkSelfPermission(context!!, WRITE_EXTERNAL_STORAGE)
        val permission2 =
            ContextCompat.checkSelfPermission(context!!, READ_EXTERNAL_STORAGE)
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

        val transformEnter = MaterialContainerTransform(requireContext(), true)
        transformEnter.scrimColor = Color.TRANSPARENT
        sharedElementEnterTransition = transformEnter

        val transformReturn = MaterialContainerTransform(requireContext(), false)
        transformReturn.scrimColor = Color.TRANSPARENT
        sharedElementReturnTransition = transformReturn

        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)

        // Get the arguments from the caller fragment/activity
        mBoxModel = arguments?.getSerializable("boxModel") as BoxModel
    }


    private fun updateContent(){
        val boxId = mBoxModel.id
        val boxContent = mBoxModel.content
        val boxLocation = mBoxModel.location
        val boxName = mBoxModel.name
        val boxImg: String = mBoxModel.image
        val boxDescription = mBoxModel.description
        val boxStatus = mBoxModel.status
        val boxColor = mBoxModel.color
        val boxLocationImg = mBoxModel.location_image

        //box_id_field.text = box_id
        boxNameField.text = boxName
        boxDescriptionField.text = boxDescription
        boxColorField.background.setTint(boxColor)

        if(boxDescription == "")
            boxDescriptionField.visibility = View.GONE
        else
            boxDescriptionField.visibility = View.VISIBLE

        if(boxStatus == "")
            boxStatusField.visibility = View.GONE
        else
            boxStatusField.visibility = View.VISIBLE

        //box_notes_field.text = box_notes
        boxLocationField.text = boxLocation

        boxStatusField.removeAllViews()
        for (tag in boxStatus.split(";")){
            if (tag != ""){
                val chip = Chip(context)
                chip.text = tag
                chip.setTextAppearance(R.style.BoxStatusChip)
                boxStatusField.addView(chip)
            }
        }

        if (boxImg == "") {
            Glide.with(this).load(R.drawable.placeholder_with_bg_80).into(boxSummaryImageField)
        } else {
            boxSummaryImageField.scaleType=ImageView.ScaleType.CENTER_CROP
            boxSummaryImageField.setImageBitmap(Utils.stringToBitMap(boxImg))
        }

        if (boxLocationImg == "") {
            Glide.with(this).load(R.drawable.placeholder_with_bg_80).into(boxLocationImageField)
        } else {
            boxLocationImageField.scaleType=ImageView.ScaleType.CENTER_CROP
            boxLocationImageField.setImageBitmap(Utils.stringToBitMap(boxLocationImg))
        }

        (activity as AppCompatActivity).supportActionBar?.title = boxId
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {

        val v =  inflater.inflate(R.layout.fragment_box, container, false)

        // Get the activity and widget
        //box_id_field = v.findViewById(R.id.box_summary_id)
        boxNameField = v.findViewById(R.id.box_summary_name)
        boxDescriptionField = v.findViewById(R.id.box_summary_description)
        //box_notes_field = v.findViewById(R.id.box_summary_notes)
        boxLocationField = v.findViewById(R.id.box_location_name)
        boxStatusField = v.findViewById(R.id.box_summary_status)
        boxSummaryImageField = v.findViewById(R.id.box_summary_image)
        boxLocationImageField = v.findViewById(R.id.box_location_image)
        boxColorField = v.findViewById(R.id.box_summary_color)
        val boxContainer: View = v.findViewById(R.id.box_fragment_container)

        // Transition taget element
        boxContainer.transitionName = mBoxModel.id

        updateContent()

        //Init Image Fullscreen on click
        boxSummaryImageField.setOnClickListener {
            val drawables: ArrayList<Drawable> = ArrayList()
            drawables.add(boxSummaryImageField.drawable)

            StfalconImageViewer.Builder(
                context, drawables
            ) { imageView, image -> Glide.with(it.context).load(image).into(imageView) }
                .withTransitionFrom(boxSummaryImageField)
                .show(true)
        }

        boxLocationImageField.setOnClickListener {
            val drawables: ArrayList<Drawable> = ArrayList()
            drawables.add(boxLocationImageField.drawable)

            StfalconImageViewer.Builder(
                context, drawables
            ) { imageView, image -> Glide.with(it.context).load(image).into(imageView) }
                .withTransitionFrom(boxLocationImageField)
                .show(true)
        }

        //Init Items View
        val recyclerview = v.findViewById<View>(R.id.box_summary_content) as RecyclerView
        mBoxItemAdapter = BoxItemAdapter(mBoxModel.id)
        recyclerview.layoutManager = LinearLayoutManager(activity)
        recyclerview.adapter = mBoxItemAdapter

        // Swipe functionality
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT + ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (Utils.checkHasWritePermission(context, false)){
                    super.onChildDraw(
                        c,
                        recyclerView,
                        viewHolder,
                        dX/5,
                        dY,
                        actionState,
                        isCurrentlyActive
                    )
                } else {
                    super.onChildDraw(
                        c,
                        recyclerView,
                        viewHolder,
                        0f,
                        dY,
                        actionState,
                        isCurrentlyActive
                    )
                }
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (Utils.checkHasWritePermission(context, false)){
                    val oldBoxModel: BoxItemModel = mItemList[viewHolder.adapterPosition]
                    val position = viewHolder.adapterPosition

                    var color: Int = Utils.getNextColor(context!!, oldBoxModel.item_color)
                    if (direction == ItemTouchHelper.RIGHT){
                        color = Utils.getPreviousColor(context!!, oldBoxModel.item_color)
                    }
                    oldBoxModel.item_color = color

                    mBoxItemAdapter.updateColorInFirebase(position)
                }
            }
        }).attachToRecyclerView(recyclerview)

        (activity as AppCompatActivity).supportActionBar?.title = mBoxModel.id

        initFirebase()

        return v
    }


    private fun initFirebase(){
        mFirebaseListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot){
                val boxes = dataSnapshot.child(Utils.getCurrentlySelectedOrg(context!!)).child("boxes")
                for (box: DataSnapshot in boxes.children){
                    if (box.child("id").value.toString() == mBoxModel.id){
                        mBoxModel = Utils.readBoxModelFromDataSnapshot(context, box)
                        updateContent()
                        break
                    }
                }

                mItemList.clear()
                val items = dataSnapshot.child(Utils.getCurrentlySelectedOrg(context!!)).child("items")
                for (contentItem: ContentItem in mBoxModel.content){
                    for (item: DataSnapshot in items.children){
                        val image = item.child("image").value.toString()
                        val name = item.child("name").value.toString()
                        val description = item.child("description").value.toString()
                        val tags = item.child("tags").value.toString()
                        val itemId = item.child("id").value.toString()
                        if (itemId == contentItem.id) {
                            mItemList.add(BoxItemModel(
                                contentItem.id,
                                contentItem.amount,
                                contentItem.invnum,
                                name,
                                contentItem.color,
                                image
                            ))
                        }
                    }
                }
                mBoxItemAdapter.setFilter(mItemList)
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        }
        FirebaseDatabase.getInstance().reference.addValueEventListener(mFirebaseListener)
    }


    override fun onDestroyView() {
        Log.w("box","destroy")
        super.onDestroyView()
        FirebaseDatabase.getInstance().reference.removeEventListener(mFirebaseListener)
    }


}