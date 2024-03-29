@file:Suppress("LocalVariableName")

package com.pixlbee.heros.fragments

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialSharedAxis
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.pixlbee.heros.R
import com.pixlbee.heros.adapters.BoxCompartmentAdapter
import com.pixlbee.heros.adapters.VehicleAdapter
import com.pixlbee.heros.models.*
import com.pixlbee.heros.utility.BoxPdfCreator
import com.pixlbee.heros.utility.Utils
import com.stfalcon.imageviewer.StfalconImageViewer
import java.util.*


class BoxFragment : Fragment(){

    private lateinit var mVehicleAdapter: VehicleAdapter
    private lateinit var mVehicle: VehicleModel
    private lateinit var mBoxModel: BoxModel
    lateinit var mBoxCompartmentAdapter: BoxCompartmentAdapter
    private lateinit var mFirebaseListener: ValueEventListener
    var mItemList: ArrayList<BoxItemModel> = ArrayList()
    var mCompartmentList: ArrayList<CompartmentModel> = ArrayList()

    private lateinit var boxIdField: TextView
    private lateinit var boxNameField: TextView
    private lateinit var boxDescriptionField: TextView
    private lateinit var boxLocationDetailsField: TextView
    private lateinit var boxStatusField: ChipGroup
    private lateinit var boxColorField: View
    private lateinit var boxHeaderCard: MaterialCardView
    private lateinit var boxImageSlider: LinearLayout
    private lateinit var boxImagesContainer: LinearLayout

    private lateinit var boxSummaryImageField: ImageView
    private lateinit var boxSummaryImageFieldOverlay: LinearLayout

    private lateinit var animationType: String
    private var sliderImageViews: ArrayList<ImageView> = ArrayList()
    private var sliderImages: ArrayList<Bitmap?> = ArrayList()


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
                        bundle.putSerializable("compartments", mCompartmentList.toTypedArray())
                        bundle.putSerializable("vehicleModel", mVehicle)
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
        val permission2 =
            ContextCompat.checkSelfPermission(context!!, READ_EXTERNAL_STORAGE)
        Log.e("Error", (permission2 == PackageManager.PERMISSION_GRANTED).toString())
        return permission2 == PackageManager.PERMISSION_GRANTED
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

        val sharedPreferences = context!!.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        animationType = sharedPreferences.getString("animation_type", "simple").toString()
        if (animationType == "elegant") {
            val transformEnter = MaterialContainerTransform(requireContext(), true)
            transformEnter.scrimColor = Color.TRANSPARENT
            sharedElementEnterTransition = transformEnter

            val transformReturn = MaterialContainerTransform(requireContext(), false)
            transformReturn.scrimColor = Color.TRANSPARENT
            sharedElementReturnTransition = transformReturn

        } else if (animationType == "simple") {
            exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
            enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
            returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
        }


        // Get the arguments from the caller fragment/activity
        mBoxModel = arguments?.getSerializable("boxModel") as BoxModel
    }


    private fun updateVehicleModel(vehicle_id: String){
        val vehiclesRef = FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("vehicles")
        vehiclesRef.get().addOnCompleteListener { task ->
            var foundVehicle = false
            if (task.isSuccessful) {
                val vehicles: DataSnapshot? = task.result
                if (vehicles != null) {
                    for (vehicle: DataSnapshot in vehicles.children) {
                        val id = vehicle.child("id").value.toString()
                        val vehicleKey = vehicle.key.toString()
                        if (id == vehicle_id) {
                            mVehicle = Utils.readVehicleModelFromDataSnapshot(vehicle)
                            foundVehicle = true
                            break
                        }
                    }
                }
            }
            if (!foundVehicle){
                mVehicle = VehicleModel("-1", resources.getString(R.string.error_no_vehicle_assigned), "", "", "", "")
            }
            mVehicleAdapter.setFilter(listOf(mVehicle))
        }
    }


    private fun updateContent(){
        val boxId = mBoxModel.id
        val boxVehicleId = mBoxModel.in_vehicle
        val boxLocationDetails = mBoxModel.location_details
        val boxName = mBoxModel.name
        val boxImg: String = mBoxModel.image
        val boxDescription = mBoxModel.description
        val boxStatus = mBoxModel.status
        val boxColor = mBoxModel.color
        val boxLocationImg = mBoxModel.location_image

        updateVehicleModel(boxVehicleId)

        //box_id_field.text = box_id
        boxNameField.text = boxName
        boxIdField.text = boxId
        boxDescriptionField.text = boxDescription
        boxColorField.background.setTint(boxColor)
        boxHeaderCard.strokeColor = boxColor

        if(boxDescription == "")
            boxDescriptionField.visibility = View.GONE
        else
            boxDescriptionField.visibility = View.VISIBLE

        if(boxStatus == "")
            boxStatusField.visibility = View.GONE
        else
            boxStatusField.visibility = View.VISIBLE

        //box_notes_field.text = box_notes
        boxLocationDetailsField.text = if (boxLocationDetails == "null") "" else boxLocationDetails
        if (listOf("", "null").contains(boxLocationDetails)) {
            boxLocationDetailsField.visibility = View.GONE
        }

        boxStatusField.removeAllViews()
        for (tag in boxStatus.split(";")){
            if (tag != ""){
                val chip = Chip(context)
                chip.text = tag
                chip.setTextAppearance(R.style.SmallTextChip)
                boxStatusField.addView(chip)
            }
        }

        boxImageSlider.removeAllViews()
        sliderImageViews.clear()
        sliderImages.clear()

        var sliderImageOffset = 0

        if (boxImg == "") {
            Glide.with(this).load(R.drawable.placeholder_with_bg_80_elevated).into(boxSummaryImageField)
            //addImageToSlider(BitmapFactory.decodeResource(resources, R.drawable.placeholder_with_bg_80))
        } else {
            boxSummaryImageField.scaleType= ScaleType.CENTER_CROP
            Glide.with(this).load(Utils.stringToBitMap(boxImg)).into(boxSummaryImageField)
            addImageToSlider(Utils.stringToBitMap(boxImg), false)
            sliderImageOffset = 1
        }

        if (boxLocationImg != "") {
            for (image in boxLocationImg.split(";")){
                addImageToSlider(Utils.stringToBitMap(image), true)
            }
        } else {
            boxImagesContainer.visibility = View.GONE
        }

        for (iv in sliderImageViews) {
            iv.setOnClickListener {
                StfalconImageViewer.Builder(
                    context, sliderImages
                ) { imageView, image -> Glide.with(it.context).load(image).into(imageView) }
                    .withTransitionFrom(iv)
                    .withStartPosition(sliderImageViews.indexOf(iv) + sliderImageOffset)
                    .show(true)
            }
        }

        (activity as AppCompatActivity).supportActionBar?.title = boxId
        // Alternative: context?.resources?.getString(R.id.container)
    }


    private fun addImageToSlider(image: Bitmap?, displayInPreview: Boolean){
        if (displayInPreview) {
            val imageView = ShapeableImageView(context)
            //imageView.id = i

            val radius = Utils.dpToPx(8, context).toFloat()
            val shapeAppearanceModel = imageView.shapeAppearanceModel.toBuilder()
                .setAllCornerSizes(radius)
                .build()
            imageView.shapeAppearanceModel = shapeAppearanceModel

            imageView.setPadding(Utils.dpToPx(4, context))

            imageView.scaleType = ScaleType.CENTER_CROP

            val hw = Utils.dpToPx(100, context)
            val params = LinearLayout.LayoutParams(hw, hw)
            params.bottomMargin = Utils.dpToPx(5, context)

            imageView.layoutParams = params
            boxImageSlider.addView(imageView)

            Glide.with(this).load(image).into(imageView)

            sliderImageViews.add(imageView)
        }

        sliderImages.add(image)
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        val v =  inflater.inflate(R.layout.fragment_box, container, false)

        // Get the activity and widget
        boxNameField = v.findViewById(R.id.box_summary_name)
        boxIdField = v.findViewById(R.id.box_summary_id)
        boxDescriptionField = v.findViewById(R.id.box_summary_description)
        boxLocationDetailsField = v.findViewById(R.id.box_summary_location_details)
        boxStatusField = v.findViewById(R.id.box_summary_status)
        boxSummaryImageField = v.findViewById(R.id.box_summary_image)
        //boxLocationImageField = v.findViewById(R.id.box_location_image)
        boxColorField = v.findViewById(R.id.box_summary_color)
        boxSummaryImageFieldOverlay = v.findViewById(R.id.box_summary_image_overlay)
        boxHeaderCard = v.findViewById(R.id.headerCard)
        boxImageSlider = v.findViewById(R.id.imageSlider)
        boxImagesContainer = v.findViewById(R.id.imagesContainer)
        val boxContainer: View = v.findViewById(R.id.box_fragment_container)

        // Transition target element
        boxContainer.transitionName = mBoxModel.id

        //Init Compartments View
        val rvCompartments = v.findViewById<View>(R.id.box_compartments) as RecyclerView
        mBoxCompartmentAdapter = BoxCompartmentAdapter(mBoxModel.id)
        mBoxCompartmentAdapter.setOnCompartmentItemClickListener(object: BoxCompartmentAdapter.OnCompartmentItemClickListener{
            override fun onCompartmentItemClicked(item: BoxItemModel, view: View) {
                if (animationType == "elegant") {
                    exitTransition = Hold()
                }
                val extras = FragmentNavigatorExtras(
                    view to item.item_id + item.numeric_id
                )
                // second argument is the animation start view
                val itemModel = ItemModel(item.item_id, item.item_name, item.item_description, item.item_tags, item.item_image)
                val navController: NavController = Navigation.findNavController(view)
                navController.navigate(BoxFragmentDirections.actionBoxFragmentToItemFragment(itemModel, item.numeric_id), extras)
            }
        })
        rvCompartments.layoutManager = LinearLayoutManager(activity)
        rvCompartments.adapter = mBoxCompartmentAdapter

        // Init vehicle view
        val recyclerviewVehicle = v.findViewById<View>(R.id.box_summary_vehicle_rv) as RecyclerView
        recyclerviewVehicle.transitionName = "vehicleTransition"
        mVehicleAdapter = VehicleAdapter(ArrayList<VehicleModel>(), true)
        mVehicleAdapter.setOnVehicleClickListener(object: VehicleAdapter.OnVehicleClickListener{
            override fun onVehicleClicked(vehicle: VehicleModel, view: View) {
                if (vehicle.id != "-1"){
                    if (animationType == "elegant") {
                        exitTransition = Hold()
                    }
                    val extras = FragmentNavigatorExtras(
                        recyclerviewVehicle to "vehicleTransition"
                    )
                    val navController: NavController = Navigation.findNavController(view)
                    navController.navigate(BoxFragmentDirections.actionBoxFragmentToVehicleDetailFragment(vehicle), extras)
                }
            }
        })
        recyclerviewVehicle.layoutManager = LinearLayoutManager(activity)
        recyclerviewVehicle.adapter = mVehicleAdapter

        //Init Image Fullscreen on click
        boxSummaryImageFieldOverlay.setOnClickListener {
            val drawables: ArrayList<Bitmap?> = ArrayList()
            if (mBoxModel.image == "") {
                drawables.add(BitmapFactory.decodeResource(resources, R.drawable.placeholder_with_bg_80))
            } else {
                drawables.add(Utils.stringToBitMap(mBoxModel.image))
            }

            StfalconImageViewer.Builder(
                context, drawables
            ) { imageView, image -> Glide.with(it.context).load(image).into(imageView) }
                .withTransitionFrom(boxSummaryImageField)
                .show(true)
        }

        (activity as AppCompatActivity).supportActionBar?.title = mBoxModel.id

        initFirebase()

        return v
    }


    private fun initFirebase(){
        mFirebaseListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot){

                // store which compartments were expanded (e.g. needed if go back from selection)
                val expandedCompartments: ArrayList<String> = ArrayList<String>()
                for (c in mCompartmentList) {
                    if (c.is_expanded) {
                        expandedCompartments.add(c.name)
                    }
                }

                // clear old lists
                mItemList.clear()
                mCompartmentList.clear()

                val boxes = dataSnapshot.child(Utils.getCurrentlySelectedOrg(context!!)).child("boxes")
                for (box: DataSnapshot in boxes.children){
                    if (box.child("id").value.toString() == mBoxModel.id){
                        mBoxModel = Utils.readBoxModelFromDataSnapshot(context, box)
                        for (c in mBoxModel.compartments) {
                            mCompartmentList.add(CompartmentModel(c, ArrayList<BoxItemModel>(), c in expandedCompartments))
                        }
                        updateContent()
                        break
                    }
                }

                val items = dataSnapshot.child(Utils.getCurrentlySelectedOrg(context!!)).child("items")
                for (contentItem: ContentItem in mBoxModel.content){
                    for (item: DataSnapshot in items.children){
                        val image = item.child("image").value.toString()
                        val name = item.child("name").value.toString()
                        val description = item.child("description").value.toString()
                        val tags = item.child("tags").value.toString()
                        val itemId = item.child("id").value.toString()
                        var numericId = contentItem.numeric_id
                        if (numericId == "null") {
                            numericId = (UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE).toString()
                        }
                        if (itemId == contentItem.id) {
                            val compartmentName = if (contentItem.compartment == "null") "" else contentItem.compartment

                            val newItem = BoxItemModel(
                                numericId,
                                contentItem.id,
                                contentItem.amount,
                                contentItem.amount_taken,
                                contentItem.invnum,
                                name,
                                contentItem.color,
                                image,
                                description,
                                tags,
                                compartmentName
                            )

                            // Add compartment if not exist
                            if (compartmentName !in (mCompartmentList.map {model -> model.name })) {
                                val newCompName = if (compartmentName == "null") "" else compartmentName
                                mCompartmentList.add(CompartmentModel(newCompName, ArrayList<BoxItemModel>(), false))
                            }

                            mCompartmentList.filter { model -> model.name == compartmentName }[0].content.add(newItem)


                            mItemList.add(newItem)
                        }

                    }
                }

                // if there is only one compartment, expand it by default
                if (mCompartmentList.size == 1) {
                    mCompartmentList[0].is_expanded = true
                }

                mBoxCompartmentAdapter.setFilter(mCompartmentList)
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        }
        FirebaseDatabase.getInstance().reference.addValueEventListener(mFirebaseListener)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        val sharedPreferences = context!!.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val animationType = sharedPreferences.getString("animation_type", "simple")
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