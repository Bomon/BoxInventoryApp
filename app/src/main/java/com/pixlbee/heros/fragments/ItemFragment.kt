package com.pixlbee.heros.fragments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.pixlbee.heros.R
import com.pixlbee.heros.adapters.ContainingBoxAdapter
import com.pixlbee.heros.models.BoxModel
import com.pixlbee.heros.models.ItemModel
import com.pixlbee.heros.utility.Utils
import com.stfalcon.imageviewer.StfalconImageViewer


class ItemFragment : Fragment() {
    private lateinit var mItemModel: ItemModel
    lateinit var mAdapter: ContainingBoxAdapter
    private lateinit var mFirebaseListener: ValueEventListener
    var mBoxList: ArrayList<BoxModel> = ArrayList()

    private lateinit var itemNameField: TextView
    private lateinit var itemDescriptionField: TextView
    private lateinit var itemTagsField: ChipGroup
    private lateinit var itemImageField: ImageView
    private lateinit var itemImageOverlayField: LinearLayout
    lateinit var itemContainingBoxesEmptyLabel: TextView
    private lateinit var itemDescriptionDivider: LinearLayout
    private lateinit var itemTagsDivider: LinearLayout

    private lateinit var animationType: String


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_item, menu)
    }


    private fun deleteItem() {
        // remove from boxes
        val boxesRef = FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("boxes")
        boxesRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val boxes: DataSnapshot? = task.result
                if (boxes != null) {
                    for (box: DataSnapshot in boxes.children) {
                        for (boxContent: DataSnapshot in box.child("content").children) {
                            val contentItemId = boxContent.child("id").value.toString()
                            if (contentItemId == mItemModel.id) {
                                val boxKey = box.key.toString()
                                val contentKey = boxContent.key.toString()
                                FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("boxes")
                                    .child(boxKey).child("content").child(contentKey)
                                    .removeValue()
                            }
                        }
                    }
                }
            }
        }

        val itemsRef = FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("items")
        itemsRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val items: DataSnapshot? = task.result
                if (items != null) {
                    for (item: DataSnapshot in items.children) {
                        val id = item.child("id").value.toString()
                        val itemKey = item.key.toString()
                        if (id == mItemModel.id) {
                            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("items").child(itemKey).removeValue()
                            break
                        }
                    }
                }
            }
        }
    }


    private fun showDeleteDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(resources.getString(R.string.dialog_delete_item_title))
        builder.setMessage(resources.getString(R.string.dialog_delete_item_text))

        builder.setPositiveButton(R.string.dialog_yes) { dialog, which ->
            deleteItem()
            val navController: NavController = Navigation.findNavController(view!!)
            navController.navigateUp()
        }

        builder.setNegativeButton(R.string.dialog_no) { dialog, which ->
        }
        builder.show()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.item_btn_edit) {
            if(Utils.checkHasWritePermission(context)) {
                if (view != null) {
                    val bundle = Bundle()
                    bundle.putSerializable("itemModel", mItemModel)
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

        val sharedPreferences = context!!.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        animationType = sharedPreferences.getString("animation_type", "simple").toString()

        if (animationType == "elegant") {
            val transformEnter = MaterialContainerTransform(requireContext(), true)
            transformEnter.scrimColor = Color.TRANSPARENT
            sharedElementEnterTransition = transformEnter

            val transformReturn = MaterialContainerTransform(requireContext(), false)
            transformReturn.scrimColor = Color.TRANSPARENT
            sharedElementReturnTransition = transformReturn

            exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
            enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)

        } else if (animationType == "simple"){
            enterTransition = MaterialFadeThrough()
            returnTransition = MaterialFadeThrough()
            exitTransition = MaterialFadeThrough()
        }

        (activity as AppCompatActivity).supportActionBar?.title = resources.getString(R.string.item_details_title)

        // Get the arguments from the caller fragment/activity
        mItemModel = arguments?.getSerializable("itemModel") as ItemModel
    }


    private fun updateContent(){
        val itemName = mItemModel.name
        val itemDescription = mItemModel.description
        val itemTags = mItemModel.tags
        val itemImage = mItemModel.image

        if (itemDescription == ""){
            itemDescriptionDivider.visibility = View.GONE
            itemDescriptionField.visibility = View.GONE
        } else {
            itemDescriptionDivider.visibility = View.VISIBLE
            itemDescriptionField.visibility = View.VISIBLE
        }

        itemNameField.text = itemName
        itemDescriptionField.text = itemDescription

        itemTagsField.removeAllViews()
        if(itemTags != ""){
            itemTagsDivider.visibility = View.VISIBLE
            itemTagsField.visibility = View.VISIBLE
            for (tag in itemTags.split(";")){
                if (tag != ""){
                    val chip = Chip(context)
                    chip.text = tag
                    itemTagsField.addView(chip)
                }
            }
        } else {
            itemTagsDivider.visibility = View.GONE
            itemTagsField.visibility = View.GONE
        }

        if (itemImage == "") {
            Glide.with(this).load(R.drawable.placeholder_with_bg_80_yellow).into(itemImageField)
        } else {
            itemImageField.scaleType=ImageView.ScaleType.CENTER_CROP
            Glide.with(this).load(Utils.stringToBitMap(itemImage)).into(itemImageField)
        }

        itemImageOverlayField.setOnClickListener {
            val drawables: ArrayList<Bitmap?> = ArrayList()

            if (itemImage == "") {
                drawables.add(BitmapFactory.decodeResource(resources, R.drawable.placeholder_with_bg_80_yellow))
            } else {
                drawables.add(Utils.stringToBitMap(itemImage))
            }

            StfalconImageViewer.Builder(
                context, drawables
            ) { imageView, image -> Glide.with(it.context).load(image).into(imageView) }
                .withTransitionFrom(itemImageField)
                .show(true)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {

        val v =  inflater.inflate(R.layout.fragment_item_details, container, false)

        // Get the activity and widget
        itemNameField = v.findViewById(R.id.item_summary_name)
        itemTagsField = v.findViewById(R.id.item_summary_tags)
        itemDescriptionField = v.findViewById(R.id.item_summary_description)
        itemImageField = v.findViewById(R.id.item_summary_image)
        itemImageOverlayField = v.findViewById(R.id.item_summary_image_overlay)
        itemContainingBoxesEmptyLabel = v.findViewById(R.id.item_summary_content_empty_label)
        itemTagsDivider = v.findViewById(R.id.item_summary_tags_divider)
        itemDescriptionDivider = v.findViewById(R.id.item_summary_description_divider)

        val itemContainer: ConstraintLayout = v.findViewById(R.id.item_fragment_container)

        // Transition target element
        val subId = arguments?.getSerializable("subId") as String
        itemContainer.transitionName = mItemModel.id + subId

        updateContent()

        //Init Items View
        val recyclerview = v.findViewById<View>(R.id.item_summary_containing_boxes) as RecyclerView
        mAdapter = ContainingBoxAdapter(mBoxList, mItemModel.id)
        mAdapter.setOnBoxClickListener(object: ContainingBoxAdapter.OnContainingBoxClickListener{
            override fun onContainingBoxClicked(box: BoxModel, view: View) {
                val extras = FragmentNavigatorExtras(
                    view to box.id
                )
                // second argument is the animation start view
                val navController: NavController = Navigation.findNavController(view)
                navController.navigate(ItemFragmentDirections.actionItemFragmentToBoxFragment(box), extras)
            }
        })


        recyclerview.layoutManager = LinearLayoutManager(activity)
        recyclerview.adapter = mAdapter

        initFirebase()

        (activity as AppCompatActivity).supportActionBar?.title = resources.getString(R.string.item_details_title)

        return v
    }


    private fun initFirebase(){
        mFirebaseListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot){
                val items = dataSnapshot.child(Utils.getCurrentlySelectedOrg(context!!)).child("items")
                for (item: DataSnapshot in items.children){
                    if (item.child("id").value.toString() == mItemModel.id){
                        mItemModel = Utils.readItemModelFromDataSnapshot(item)
                        updateContent()
                        break
                    }
                }

                val addedIds: ArrayList<String> = ArrayList()
                mBoxList.clear()
                val boxes = dataSnapshot.child(Utils.getCurrentlySelectedOrg(context!!)).child("boxes")
                for (box: DataSnapshot in boxes.children){
                    for (boxContent: DataSnapshot in box.child("content").children) {
                        if (boxContent.child("id").value.toString() == mItemModel.id) {
                            val boxModel = Utils.readBoxModelFromDataSnapshot(context, box)
                            if (boxModel.id !in addedIds) {
                                mBoxList.add(boxModel)
                                addedIds.add(boxModel.id)
                            }
                        }
                    }
                }

                mBoxList.sortWith(
                    compareBy(String.CASE_INSENSITIVE_ORDER) { Utils.replaceUmlauteForSorting(it.name) }
                )

                mAdapter.setFilter(mBoxList)
                if (mBoxList.size == 0){
                    itemContainingBoxesEmptyLabel.visibility = View.VISIBLE
                } else {
                    itemContainingBoxesEmptyLabel.visibility = View.GONE
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        }
        FirebaseDatabase.getInstance().reference.addValueEventListener(mFirebaseListener)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        if (animationType == "elegant") {
            postponeEnterTransition()
            view.doOnPreDraw { startPostponedEnterTransition() }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        FirebaseDatabase.getInstance().reference.removeEventListener(mFirebaseListener)
    }


}