package com.pixlbee.heros.fragments

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.activity.addCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.drjacky.imagepicker.ImagePicker
import com.github.drjacky.imagepicker.constant.ImageProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.pixlbee.heros.R
import com.pixlbee.heros.adapters.BoxCompartmentEditAdapter
import com.pixlbee.heros.adapters.ImageGridAdapter
import com.pixlbee.heros.adapters.VehicleAdapter
import com.pixlbee.heros.models.*
import com.pixlbee.heros.utility.Utils
import dev.sasikanth.colorsheet.ColorSheet
import java.util.*


class BoxEditFragment : Fragment() {

    private lateinit var mBoxModel: BoxModel
    private var mCompartmentList: ArrayList<CompartmentModel> = ArrayList()
    private lateinit var mVehicle: VehicleModel

    private lateinit var boxEditImageField: ImageView
    private lateinit var boxEditVehicleField: ImageView
    private lateinit var boxEditLocationDetailsFieldContainer: TextInputLayout
    private lateinit var boxEditNameField: TextInputEditText
    private lateinit var boxEditNameFieldContainer: TextInputLayout
    private lateinit var boxEditIdField: TextInputEditText
    private lateinit var boxEditIdFieldContainer: TextInputLayout
    private lateinit var boxEditDescriptionField: TextInputEditText
    private lateinit var boxEditLocationDetailsField: TextInputEditText
    private lateinit var boxEditStatusChips: ChipGroup
    private lateinit var boxEditStatusInput: TextInputEditText
    private lateinit var boxEditQrcodeField: EditText
    private lateinit var boxEditQrcodeFieldContainer: TextInputLayout
    private lateinit var boxEditColorBtn: MaterialButton
    private lateinit var boxEditColorPreview: View
    private lateinit var boxEditImageSpinner: ProgressBar

    private lateinit var boxImgAddBtn: MaterialButton
    private lateinit var boxImgChangeBtn: MaterialButton
    private lateinit var boxImgDeleteBtn: MaterialButton

    private lateinit var boxVehicleAddBtn: MaterialButton
    private lateinit var boxVehicleChangeBtn: MaterialButton
    private lateinit var boxVehicleDeleteBtn: MaterialButton

    private var boxEditColor: Int = -1

    private var qrList: ArrayList<String> = ArrayList()
    private var idList: ArrayList<String> = ArrayList()

    private var newTempCompartments: ArrayList<String> = ArrayList()

    private var movedItemTracker: ArrayList<ItemMoveModel> = ArrayList()

    private var isNewBox: Boolean = false
    private lateinit var mBoxCompartmentEditAdapter: BoxCompartmentEditAdapter
    private lateinit var mVehicleAdapter: VehicleAdapter
    private lateinit var mimageGridAdapter: ImageGridAdapter

    private lateinit var navController: NavController

    private lateinit var imageBitmap: Bitmap
    private var imageBitmapEncoded: String = ""
    private lateinit var locationImageBitmap: Bitmap

    private lateinit var animationType: String

    private lateinit var rvCompartments: RecyclerView
    private lateinit var rvImageGrid: RecyclerView


    private var gridImages = ArrayList<ImageGridElementModel>()


    private fun checkFields(): Boolean {
        var status = true
        if (boxEditIdField.text.toString() == "") {
            boxEditIdFieldContainer.isErrorEnabled = true
            boxEditIdFieldContainer.error = resources.getString(R.string.error_field_empty)
            status = false
        }
        if (boxEditIdField.text.toString() in idList) {
            boxEditIdFieldContainer.isErrorEnabled = true
            boxEditIdFieldContainer.error = resources.getString(R.string.error_box_already_exists_id)
            status = false
        }
        if (boxEditNameField.text.toString() == "") {
            boxEditNameFieldContainer.isErrorEnabled = true
            boxEditNameFieldContainer.error = resources.getString(R.string.error_field_empty)
            status = false
        }
        if (boxEditQrcodeField.text.toString() == "") {
            boxEditQrcodeFieldContainer.isErrorEnabled = true
            boxEditQrcodeFieldContainer.error = resources.getString(R.string.error_field_empty)
            status = false
        }
        if (boxEditQrcodeField.text.toString() in qrList) {
            boxEditQrcodeFieldContainer.isErrorEnabled = true
            boxEditQrcodeFieldContainer.error = resources.getString(R.string.error_box_already_exists_qrcode)
            status = false
        }
        if (!status){
            Toast.makeText(context, resources.getString(R.string.error_box_edit_field), Toast.LENGTH_SHORT).show()
        }

        return status
    }


    fun moveItem(movedItem: ItemMoveModel) {
        if (!movedItemTracker.map { model -> model.item.numeric_id }.contains(movedItem.item.numeric_id)) {
            movedItemTracker.add(movedItem)
        }
    }


    private fun applyChanges(): Boolean {
        val fieldsOk: Boolean = checkFields()
        if (!fieldsOk) return false
        val tempContext = requireContext()
        val boxesRef = FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("boxes")
        boxesRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val boxes: DataSnapshot? = task.result
                if (boxes != null) {
                    for (box: DataSnapshot in boxes.children) {
                        val id = box.child("id").value.toString()
                        if (id == mBoxModel.id) {
                            // Update Box Fields
                            val boxKey: String = box.key.toString()
                            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(tempContext)).child("boxes").child(boxKey).child("id").setValue(boxEditIdField.text.toString().trim())
                            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(tempContext)).child("boxes").child(boxKey).child("name").setValue(boxEditNameField.text.toString().trim())
                            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(tempContext)).child("boxes").child(boxKey).child("description").setValue(boxEditDescriptionField.text.toString().trim())
                            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(tempContext)).child("boxes").child(boxKey).child("qrcode").setValue(boxEditQrcodeField.text.toString().trim())
                            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(tempContext)).child("boxes").child(boxKey).child("in_vehicle").setValue(mVehicle.id)
                            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(tempContext)).child("boxes").child(boxKey).child("location_details").setValue(boxEditLocationDetailsField.text.toString().trim())
                            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(tempContext)).child("boxes").child(boxKey).child("color").setValue(boxEditColor)

                            // Update Status Chips
                            val chipString = Utils.chipListToString(boxEditStatusChips)
                            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(tempContext)).child("boxes").child(boxKey).child("status").setValue(chipString)

                            // Update main image
                            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(tempContext)).child("boxes").child(boxKey).child("image").setValue(imageBitmapEncoded)

                            // Update Location image
                            val additionalImages = mimageGridAdapter.getImages().filter { im ->
                                im.grid_element_type == GridElementType.IMAGE && im.image != ""
                            }.joinToString(";") { im ->
                                im.image
                            }
                            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(tempContext)).child("boxes").child(boxKey).child("location_image").setValue(additionalImages)

                            // Update compartment items
                            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(tempContext)).child("boxes").child(boxKey).child("content").removeValue()
                            val compartmentList = mBoxCompartmentEditAdapter.getCurrentStatus()
                            val updatedContentItems = ArrayList<ContentItem>()
                            for (compartment: CompartmentModel in compartmentList) {
                                for (item: BoxItemModel in compartment.content) {
                                    val newItem = ContentItem(item.numeric_id, "", item.item_amount, item.item_amount_taken, item.item_id, item.item_invnum, item.item_color, compartment.name)
                                    updatedContentItems.add(newItem)
                                    FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(tempContext)).child("boxes").child(boxKey).child("content").push().setValue(newItem)
                                }
                            }
                            val allCompartmentList = (compartmentList.map { c -> c.name }).filter { c -> c != "" }.joinToString(";")
                            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(tempContext)).child("boxes").child(boxKey).child("compartmentList").setValue(allCompartmentList)

                            // Insert items that were moved out of the box
                            for (movedItem in movedItemTracker) {
                                val newItem = movedItem.item
                                val newContentItem = ContentItem(newItem.numeric_id, "", newItem.item_amount, newItem.item_amount_taken, newItem.item_id, newItem.item_invnum, newItem.item_color, movedItem.target_compartment)
                                FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(tempContext)).child("boxes").child(movedItem.target_box_key).child("content").push().setValue(newContentItem)
                            }

                            // This is just a workaround
                            // Problem: when ID is changed, Box Fragment no longer can find the right box
                            // Hence we go directly back to home after editing where the box is updated
                            // If user clicks it again, he sees the box with the new ID
                            if (boxEditIdField.text.toString() != mBoxModel.id){
                                navController.navigateUp()
                            }
                        }
                    }
                }
            }
        }
        return true
    }


    private fun createBox(): Boolean {
        val fieldsOk: Boolean = checkFields()
        if (!fieldsOk) return false

        mBoxModel.id = boxEditIdField.text.toString()
        mBoxModel.name = boxEditNameField.text.toString()
        mBoxModel.qrcode = boxEditQrcodeField.text.toString()
        mBoxModel.in_vehicle = mVehicle.id
        mBoxModel.description = boxEditDescriptionField.text.toString()
        mBoxModel.color = boxEditColor
        mBoxModel.status = Utils.chipListToString(boxEditStatusChips)
        mBoxModel.image = imageBitmapEncoded
        if (::locationImageBitmap.isInitialized){
            mBoxModel.location_image = Utils.getEncoded64ImageStringFromBitmap(locationImageBitmap)
        }
        val compartmentList = mBoxCompartmentEditAdapter.getCurrentStatus()
        val newContentItems = ArrayList<ContentItem>()
        for (compartment: CompartmentModel in compartmentList){
            for (item: BoxItemModel in compartment.content) {
                val newItem = ContentItem(item.numeric_id,"", item.item_amount, item.item_amount_taken, item.item_id, item.item_invnum, item.item_color, compartment.name)
                newContentItems.add(newItem)
            }
        }
        mBoxModel.content = newContentItems
        FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("boxes").push().setValue(mBoxModel)
        return true
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_box_edit, menu)
    }


    private fun showSaveDialog() {
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle(resources.getString(R.string.dialog_save_title))
        builder.setMessage(R.string.dialog_save_text)

        builder.setPositiveButton(resources.getString(R.string.dialog_yes)) { _, _ ->
            val status = applyChanges()
            if (status) {
                navController.navigateUp()
            }
        }

        builder.setNeutralButton(resources.getString(R.string.dialog_cancel)) { _, _ ->
        }
        builder.show()
    }


    private fun showDismissDialog() {
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle(resources.getString(R.string.dialog_dismiss_title))
        builder.setMessage(resources.getString(R.string.dialog_dismiss_text))

        builder.setPositiveButton(resources.getString(R.string.dialog_yes)) { _, _ ->
            navController.navigateUp()
        }

        builder.setNegativeButton(resources.getString(R.string.dialog_no)) { _, _ ->
        }
        builder.show()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.box_edit_btn_save) {
            if (isNewBox){
                val status = createBox()
                if (status) {
                    navController.navigateUp()
                }
            } else {
                showSaveDialog()
            }
        } else if (item.itemId == R.id.box_edit_btn_cancel) {
            showDismissDialog()
        } else if (item.itemId == android.R.id.home) {
            showDismissDialog()
        }
        return super.onOptionsItemSelected(item)
    }


    private fun initQrCodeList(){
        val boxesRef = FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("boxes")
        boxesRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val boxes: DataSnapshot? = task.result
                if (boxes != null) {
                    qrList.clear()
                    for (box: DataSnapshot in boxes.children) {
                        if (box.child("id").value.toString() != mBoxModel.id){
                            qrList.add(box.child("qrcode").value.toString())
                        }
                    }
                }
            }
        }
    }

    private fun initBoxIdList(){
        val boxesRef = FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("boxes")
        boxesRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val boxes: DataSnapshot? = task.result
                if (boxes != null) {
                    idList.clear()
                    for (box: DataSnapshot in boxes.children) {
                        if (box.child("id").value.toString() != mBoxModel.id){
                            idList.add(box.child("id").value.toString())
                        }
                    }
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            showDismissDialog()
        }

        val sharedPreferences = context!!.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        animationType = sharedPreferences.getString("animation_type", "simple").toString()
        if (animationType == "elegant") {
            enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
            returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
        } else if (animationType == "simple"){
            enterTransition = MaterialFadeThrough()
            returnTransition = MaterialFadeThrough()
        }

        // This task is done once on fragment creation
        // Reason: each qrcode / boxId must only exist once. The task is async and so we start it here to have the result when user clicks save
        initQrCodeList()
        initBoxIdList()

        // Get the arguments from the caller fragment/activity and init compartment list
        mBoxModel = arguments?.getSerializable("boxModel") as BoxModel
        val itemListArray: Array<BoxItemModel> =
            arguments?.getSerializable("items") as Array<BoxItemModel>

        mCompartmentList.add(CompartmentModel("", ArrayList<BoxItemModel>(), false))
        for (c in mBoxModel.compartments) {
            mCompartmentList.add(CompartmentModel(c, ArrayList<BoxItemModel>(), false))
        }

        for (item in itemListArray) {
            // Add compartment if not exist
            if (item.item_compartment !in (mCompartmentList.map {model -> model.name })) {
                mCompartmentList.add(CompartmentModel(item.item_compartment, ArrayList<BoxItemModel>(), false))
            }
            mCompartmentList.filter { model -> model.name == item.item_compartment }[0].content.add(item)
        }

        mVehicle = arguments?.getSerializable("vehicleModel") as VehicleModel?
            ?: VehicleModel(
                "-1",
                resources.getString(R.string.error_no_vehicle_assigned),
                "",
                "",
                "",
                ""
            )
        isNewBox = arguments?.getSerializable("isNewBox") as Boolean


        if (isNewBox) {
            (activity as AppCompatActivity).supportActionBar?.title =
                resources.getString(R.string.fragment_box_edit_title_new)
        } else {
            (activity as AppCompatActivity).supportActionBar?.title =
                resources.getString(R.string.fragment_box_edit_title)
        }

        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        builder.detectFileUriExposure()

        // prepare image grid
        gridImages.clear()
        mBoxModel.location_image.split(";").forEach { image ->
            if (image != ""){
                gridImages.add(ImageGridElementModel(image, GridElementType.IMAGE))
            }
        }
    }


    private fun addChipToGroup(chipText: String) {
        val chip = Chip(context)
        chip.text = chipText
        chip.isCloseIconVisible = true
        chip.isClickable = true
        chip.isCheckable = false
        boxEditStatusChips.addView(chip)
        chip.setOnCloseIconClickListener {
            boxEditStatusChips.removeView(chip as View)
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        navController = Navigation.findNavController(view)
        // Receive data from ItemAddFragment (selected Item)
        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<MutableList<String>>("itemIdList")?.observe(
            viewLifecycleOwner) { result ->
            navController.currentBackStackEntry?.savedStateHandle?.getLiveData<String>("targetCompartmentName")?.observe(viewLifecycleOwner) { targetCompartment ->
                for (item in result){
                    addSelectedItem(item, targetCompartment)
                }
            }
        }
        navController.currentBackStackEntry?.savedStateHandle?.remove<MutableList<String>>("itemIdList")

        // Receive data from VehicleAddFragment (selected vehicle)
        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<VehicleModel>("vehicleModel")?.observe(
            viewLifecycleOwner) { result ->
            mVehicle = result
            mVehicleAdapter.setFilter(listOf(mVehicle))

            boxVehicleAddBtn.visibility = View.GONE
            boxVehicleDeleteBtn.visibility = View.VISIBLE
            boxVehicleChangeBtn.visibility = View.VISIBLE
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {

        if (isNewBox){
            (activity as AppCompatActivity).supportActionBar?.title = resources.getString(R.string.fragment_box_edit_title_new)
        } else {
            (activity as AppCompatActivity).supportActionBar?.title = resources.getString(R.string.fragment_box_edit_title)
        }

        (activity as AppCompatActivity?)!!.supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        (activity as AppCompatActivity?)!!.supportActionBar!!.setHomeButtonEnabled(false)

        if (animationType in listOf("simple", "elegant")) {
            exitTransition = MaterialFadeThrough()
        }

        val v =  inflater.inflate(R.layout.fragment_box_edit, container, false)

        // Get the activity and widget
        boxEditNameField = v.findViewById(R.id.box_edit_name)
        boxEditIdField = v.findViewById(R.id.box_edit_id)
        boxEditLocationDetailsField = v.findViewById(R.id.box_edit_location_details)
        boxEditQrcodeField = v.findViewById(R.id.box_edit_qrcode)
        boxEditDescriptionField = v.findViewById(R.id.box_edit_description)
        boxEditColorBtn = v.findViewById(R.id.box_edit_color_btn)
        boxEditColorPreview = v.findViewById(R.id.box_edit_color_preview)
        val boxEditAddButton: Button = v.findViewById(R.id.box_edit_add_button)
        boxEditImageField = v.findViewById(R.id.box_edit_image)
        //boxEditLocationImageField = v.findViewById(R.id.box_edit_location_image)
        boxEditStatusChips = v.findViewById(R.id.box_edit_status_chips)
        boxEditStatusInput = v.findViewById(R.id.box_edit_status_input)
        boxEditVehicleField = v.findViewById(R.id.box_edit_vehicle_overlay)

        boxEditIdFieldContainer = v.findViewById(R.id.box_edit_id_container)
        boxEditNameFieldContainer = v.findViewById(R.id.box_edit_name_container)
        boxEditQrcodeFieldContainer = v.findViewById(R.id.box_edit_qrcode_container)
        boxEditLocationDetailsFieldContainer = v.findViewById(R.id.box_edit_location_details_container)

        boxImgAddBtn = v.findViewById(R.id.btn_add_image)
        boxImgChangeBtn = v.findViewById(R.id.btn_change_image)
        boxImgDeleteBtn = v.findViewById(R.id.btn_remove_image)

        boxVehicleAddBtn = v.findViewById(R.id.btn_add_vehicle)
        boxVehicleChangeBtn = v.findViewById(R.id.btn_change_vehicle)
        boxVehicleDeleteBtn = v.findViewById(R.id.btn_remove_vehicle)

        boxEditImageSpinner = v.findViewById(R.id.bod_edit_image_spinner)
        //boxEditLocationImageSpinner = v.findViewById(R.id.bod_edit_location_image_spinner)

        val confirmationChars = listOf(";", "\n")
        boxEditStatusInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                if (text.isNotEmpty() && (text.last().toString() in confirmationChars))
                    boxEditStatusInput.setText("")
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty()) {
                    if (s.length > 1 && s.last().toString() in confirmationChars) {
                        var text = s.toString()
                        for (cf in confirmationChars) {
                            text = text.replace(cf, "")
                        }
                        addChipToGroup(text)
                        boxEditStatusInput.setText("")
                    }
                }
            }
        })

        boxEditIdField.onFocusChangeListener =
            View.OnFocusChangeListener { v, hasFocus ->
                if (!hasFocus) {
                    if (boxEditQrcodeField.text.toString() == ""){
                        boxEditQrcodeField.text = boxEditIdField.text
                    }
                }
            }

        val fragmentManager = (context as FragmentActivity).supportFragmentManager
        val colors = (context as FragmentActivity).resources.getIntArray(R.array.picker_colors)

        fun onColorClick() {
            ColorSheet().colorPicker(
                colors = colors,
                listener = { color ->
                    boxEditColorPreview.background.setTint(color)
                    boxEditColor = color
                })
                .show(fragmentManager)
        }

        boxEditColorBtn.setOnClickListener {
            onColorClick()
        }
        boxEditColorPreview.setOnClickListener {
            onColorClick()
        }

        boxEditNameField.setText(mBoxModel.name)
        boxEditIdField.setText(mBoxModel.id)
        boxEditLocationDetailsField.setText(if (mBoxModel.location_details == "null") "" else mBoxModel.location_details)
        boxEditQrcodeField.setText(mBoxModel.qrcode)
        boxEditDescriptionField.setText(mBoxModel.description)
        boxEditColorPreview.background.setTint(mBoxModel.color)
        boxEditColor = mBoxModel.color

        for (chip in mBoxModel.status.split(";")){
            if (chip != ""){
                addChipToGroup(chip)
            }
        }

        if (mBoxModel.image == "") {
            boxImgAddBtn.visibility = View.VISIBLE
            boxImgDeleteBtn.visibility = View.GONE
            boxImgChangeBtn.visibility = View.GONE
        } else {
            boxImgAddBtn.visibility = View.GONE
            boxImgDeleteBtn.visibility = View.VISIBLE
            boxImgChangeBtn.visibility = View.VISIBLE
            Glide.with(this).load(Utils.stringToBitMap(mBoxModel.image)).into(boxEditImageField)
        }

        val startForMainImageResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                val resultCode = result.resultCode
                val data = result.data

                if (resultCode == AppCompatActivity.RESULT_OK) {
                    val uri: Uri = data?.data!!
                    imageBitmap = MediaStore.Images.Media.getBitmap(context?.contentResolver, Uri.parse(uri.toString()))
                    imageBitmapEncoded = Utils.getEncoded64ImageStringFromBitmap(imageBitmap)

                    Glide.with(context!!)
                        .load(imageBitmap)
                        .into(boxEditImageField)

                    boxEditImageSpinner.visibility = View.GONE
                    boxImgAddBtn.visibility = View.GONE
                    boxImgDeleteBtn.visibility = View.VISIBLE
                    boxImgChangeBtn.visibility = View.VISIBLE
                } else {
                    boxEditImageSpinner.visibility = View.GONE
                    Toast.makeText(context, resources.getString(R.string.task_cancelled), Toast.LENGTH_SHORT).show()
                }
            }

        val imageClickElements = listOf(boxEditImageField, boxImgAddBtn, boxImgChangeBtn)

        imageClickElements.forEach { elem ->
            elem.setOnClickListener {
                ImagePicker.with(activity as AppCompatActivity)
                    .crop()
                    .cropFreeStyle()
                    .provider(ImageProvider.BOTH)
                    .createIntentFromDialog { intent ->
                        startForMainImageResult.launch(intent)
                        boxEditImageSpinner.visibility = View.VISIBLE
                    }
            }
        }

        // action for on additional iamge select
        val startForGridImageResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                val resultCode = result.resultCode
                val data = result.data

                if (resultCode == AppCompatActivity.RESULT_OK) {
                    //Image Uri will not be null for RESULT_OK
                    val uri: Uri = data?.data!!
                    locationImageBitmap = MediaStore.Images.Media.getBitmap(context?.contentResolver, Uri.parse(uri.toString()))
                    val imageString = Utils.getEncoded64ImageStringFromBitmap(locationImageBitmap)

                    mimageGridAdapter.addImage(ImageGridElementModel(imageString, GridElementType.IMAGE))
                } else {
                    Toast.makeText(context, resources.getString(R.string.task_cancelled), Toast.LENGTH_SHORT).show()
                }
            }

        // Init image grid
        mimageGridAdapter = ImageGridAdapter(gridImages)
        rvImageGrid = v.findViewById(R.id.box_edit_additional_images)
        rvImageGrid.layoutManager = GridLayoutManager(activity, 3)
        rvImageGrid.adapter = mimageGridAdapter

        mimageGridAdapter.setOnImageGridClickListener(object: ImageGridAdapter.OnImageGridClickListener {

            override fun onImageAdd(view: View) {

                ImagePicker.with(activity as AppCompatActivity)
                    .crop()
                    .cropFreeStyle()
                    .provider(ImageProvider.BOTH)
                    .createIntentFromDialog { intent ->
                        startForGridImageResult.launch(intent)
                    }
            }
        })


        //Init vehicle view
        mVehicleAdapter = VehicleAdapter(ArrayList<VehicleModel>(), true)
        val recyclerviewVehicle = v.findViewById<View>(R.id.box_edit_vehicle_rv) as RecyclerView
        recyclerviewVehicle.layoutManager = LinearLayoutManager(activity)
        recyclerviewVehicle.adapter = mVehicleAdapter
        mVehicleAdapter.setFilter(listOf(mVehicle))

        if (mVehicle.id == "-1") {
            boxVehicleAddBtn.visibility = View.VISIBLE
            boxVehicleDeleteBtn.visibility = View.GONE
            boxVehicleChangeBtn.visibility = View.GONE
        } else {
            boxVehicleAddBtn.visibility = View.GONE
            boxVehicleDeleteBtn.visibility = View.VISIBLE
            boxVehicleChangeBtn.visibility = View.VISIBLE
        }

        //Init Compartment View
        rvCompartments = v.findViewById<View>(R.id.box_edit_compartments) as RecyclerView
        mBoxCompartmentEditAdapter = BoxCompartmentEditAdapter(mBoxModel.id, this)
        rvCompartments.layoutManager = LinearLayoutManager(activity)
        rvCompartments.adapter = mBoxCompartmentEditAdapter

        // if there is only one compartment, expand it by default
        if (mCompartmentList.size == 1) {
            mCompartmentList[0].is_expanded = true
        }
        mBoxCompartmentEditAdapter.setFilter(mCompartmentList)
        mBoxCompartmentEditAdapter.setOnCompartmentItemAddListener(object: BoxCompartmentEditAdapter.OnCompartmentItemAddListener{
            override fun onCompartmentItemAdd(targetCompartmentName: String, view: View) {
                if (animationType == "elegant") {
                    exitTransition = Hold()
                }
                // Temp store elements for when item was added
                mBoxModel.status = Utils.chipListToString(boxEditStatusChips)
                mBoxModel.color = boxEditColor
                mCompartmentList = mBoxCompartmentEditAdapter.getCurrentStatus()

                val extras = FragmentNavigatorExtras(view to "transition_to_items")
                findNavController().navigate(
                    BoxEditFragmentDirections.actionBoxEditFragmentToItemsSelectionFragment(targetCompartmentName), extras
                )
            }
        })

        boxEditAddButton.setOnClickListener { view ->
            if (view != null) {
                val builder = MaterialAlertDialogBuilder(context!!)
                builder.setTitle(context!!.resources.getString(R.string.dialog_add_compartment_title))

                val viewInflated: View = LayoutInflater.from(context)
                    .inflate(R.layout.dialog_create_compartment, container, false)
                val input = viewInflated.findViewById<View>(R.id.dialog_input_compartment_name) as EditText
                val container = viewInflated.findViewById<View>(R.id.dialog_input_compartment_name_container) as TextInputLayout

                builder.setView(viewInflated)
                builder.setPositiveButton(context!!.resources.getString(R.string.dialog_add), null)
                builder.setNegativeButton(context!!.resources.getString(R.string.dialog_cancel)) { dialog, _ -> dialog.cancel() }

                val mAlertDialog: AlertDialog = builder.create()
                mAlertDialog.setOnShowListener {
                    val b: Button = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    b.setOnClickListener {
                        val inputText = input.text.toString()
                        if (inputText != "" && !inputText.contains(";") && inputText !in mCompartmentList.map { t -> t.name } && inputText !in newTempCompartments) {
                            mCompartmentList.add(CompartmentModel(inputText, ArrayList<BoxItemModel>(), true))
                            newTempCompartments.add(inputText)
                            mBoxCompartmentEditAdapter.setFilter(mCompartmentList)
                            mAlertDialog.dismiss()
                        } else {
                            if (inputText == "") {
                                container.isErrorEnabled = true
                                container.error =
                                    context!!.resources.getString(R.string.error_dialog_field_empty)
                            } else if (inputText == ";") {
                                    container.isErrorEnabled = true
                                    container.error =
                                        context!!.resources.getString(R.string.error_dialog_field_illegal)
                            } else if (inputText in mCompartmentList.map { t -> t.name }) {
                                container.isErrorEnabled = true
                                container.error =
                                    context!!.resources.getString(R.string.error_dialog_compartment_already_exists)
                            }
                        }
                    }
                }
                mAlertDialog.show()

            }
        }


        //Init Vehicle View
        val vehicleClickElements = listOf(boxEditVehicleField, boxVehicleAddBtn, boxVehicleChangeBtn)
        vehicleClickElements.forEach { elem ->
            elem.setOnClickListener { view ->
                if (boxEditVehicleField != null) {
                    if (animationType == "elegant") {
                        exitTransition = Hold()
                    }
                    // Temp store elements for when item was added
                    mBoxModel.status = Utils.chipListToString(boxEditStatusChips)
                    mBoxModel.color = boxEditColor
                    mCompartmentList = mBoxCompartmentEditAdapter.getCurrentStatus()

                    val extras = FragmentNavigatorExtras(boxEditVehicleField to "transition_to_vehicles")
                    findNavController().navigate(
                        BoxEditFragmentDirections.actionBoxEditFragmentToNavigationVehicles(
                            returnVehicleInsteadOfShowDetails = true
                        ), extras
                    )
                }
            }
        }

        boxImgDeleteBtn.setOnClickListener {
            imageBitmapEncoded = ""
            boxImgAddBtn.visibility = View.VISIBLE
            boxImgDeleteBtn.visibility = View.GONE
            boxImgChangeBtn.visibility = View.GONE
            Glide.with(this).load(R.drawable.ic_outline_add_photo_alternate_24_padding).into(boxEditImageField)
        }

        boxVehicleDeleteBtn.setOnClickListener {
            mVehicle = VehicleModel(
                    "-1",
                    resources.getString(R.string.error_no_vehicle_assigned),
                    "",
                    "",
                    "",
                    ""
                )
            mVehicleAdapter.setFilter(listOf(mVehicle))
            boxVehicleAddBtn.visibility = View.VISIBLE
            boxVehicleDeleteBtn.visibility = View.GONE
            boxVehicleChangeBtn.visibility = View.GONE
        }


        // init move of images
        val itemTouchHelper by lazy {
            val simpleItemTouchCallback = object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT, 0) {

                override fun onMove(recyclerView: RecyclerView,
                                    viewHolder: RecyclerView.ViewHolder,
                                    target: RecyclerView.ViewHolder): Boolean {
                    val from = viewHolder.absoluteAdapterPosition
                    val to = target.absoluteAdapterPosition
                    val adapter = recyclerView.adapter as ImageGridAdapter

                    adapter.onRowMoved(from, to)

                    return true
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                }

                override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                    super.onSelectedChanged(viewHolder, actionState)

                    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                        viewHolder?.itemView?.alpha = 0.5f
                    }
                }

                override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                    super.clearView(recyclerView, viewHolder)

                    viewHolder.itemView.alpha = 1.0f
                }
            }

            ItemTouchHelper(simpleItemTouchCallback)
        }
        itemTouchHelper.attachToRecyclerView(rvImageGrid)


        return v
    }


    private fun addSelectedItem(item_id: String?, targetCompartment: String) {
        val itemsRef = FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("items")
        itemsRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val items: DataSnapshot? = task.result
                if (items != null) {
                    for (item: DataSnapshot in items.children) {
                        val id = item.child("id").value.toString()
                        if (id == item_id) {
                            val itemName = item.child("name").value.toString()
                            val itemImage = item.child("images").value.toString()
                            val itemDescription = item.child("description").value.toString()
                            val itemTags = item.child("tags").value.toString()
                            for (compartment in mCompartmentList) {
                                if (compartment.name == targetCompartment) {
                                    compartment.content.add(BoxItemModel(
                                        (UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE).toString(),
                                        item_id,
                                        "1",
                                        "0",
                                        "",
                                        itemName,
                                        ContextCompat.getColor(requireContext(), R.color.default_item_color),
                                        itemImage,
                                        itemDescription,
                                        itemTags,
                                        targetCompartment
                                    ))
                                }
                            }
                            mBoxCompartmentEditAdapter.setFilter(mCompartmentList)
                            return@addOnCompleteListener
                        }
                    }
                }
            }
        }
        //addItem(item_id.toString())
    }

    fun getTempCompartments(): ArrayList<String> {
        return newTempCompartments
    }

    fun removeTempCompartment(name: String){
        newTempCompartments.remove(name)
        mCompartmentList.removeIf { it.name == name }
    }

}