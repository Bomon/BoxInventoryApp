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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.dhaval2404.imagepicker.ImagePicker
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
import com.pixlbee.heros.adapters.BoxItemEditAdapter
import com.pixlbee.heros.adapters.VehicleAdapter
import com.pixlbee.heros.models.BoxItemModel
import com.pixlbee.heros.models.BoxModel
import com.pixlbee.heros.models.ContentItem
import com.pixlbee.heros.models.VehicleModel
import com.pixlbee.heros.utility.Utils
import dev.sasikanth.colorsheet.ColorSheet
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*


class BoxEditFragment : Fragment() {

    private lateinit var mBoxModel: BoxModel
    private lateinit var mItemList: ArrayList<BoxItemModel>
    private lateinit var mVehicle: VehicleModel

    private lateinit var boxEditImageField: ImageView
    private lateinit var boxEditLocationImageField: ImageView
    private lateinit var boxEditVehicleField: ImageView
    private lateinit var boxEditLocationDetailsFieldContainer: TextInputLayout
    private lateinit var boxEditNameField: TextInputEditText
    private lateinit var boxEditNameFieldContainer: TextInputLayout
    private lateinit var boxEditIdField: TextInputEditText
    private lateinit var boxEditIdFieldContainer: TextInputLayout
    private lateinit var boxEditTypeField: TextInputEditText
    private lateinit var boxEditTypeFieldContainer: TextInputLayout
    private lateinit var boxEditDescriptionField: TextInputEditText
    private lateinit var boxEditLocationDetailsField: TextInputEditText
    private lateinit var boxEditStatusChips: ChipGroup
    private lateinit var boxEditStatusInput: TextInputEditText
    private lateinit var boxEditQrcodeField: EditText
    private lateinit var boxEditQrcodeFieldContainer: TextInputLayout
    private lateinit var boxEditColorBtn: MaterialButton
    private lateinit var boxEditColorPreview: View
    private lateinit var boxEditImageSpinner: ProgressBar
    private lateinit var boxEditLocationImageSpinner: ProgressBar
    private var boxEditColor: Int = -1

    private var qrList: ArrayList<String> = ArrayList()
    private var idList: ArrayList<String> = ArrayList()

    private var isNewBox: Boolean = false
    private lateinit var mBoxItemEditAdapter: BoxItemEditAdapter
    private lateinit var mVehicleAdapter: VehicleAdapter

    private lateinit var navController: NavController

    private lateinit var imageBitmap: Bitmap
    private lateinit var locationImageBitmap: Bitmap

    private lateinit var animationType: String


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


    private fun applyChanges(): Boolean {
        val fieldsOk: Boolean = checkFields()
        if (!fieldsOk) return false

        val boxesRef = FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("boxes")
        boxesRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val boxes: DataSnapshot? = task.result
                if (boxes != null) {
                    for (box: DataSnapshot in boxes.children) {
                        val id = box.child("id").value.toString()
                        if (id == mBoxModel.id) {
                            val boxKey: String = box.key.toString()
                            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("boxes").child(boxKey).child("id").setValue(boxEditIdField.text.toString().trim())
                            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("boxes").child(boxKey).child("name").setValue(boxEditNameField.text.toString().trim())
                            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("boxes").child(boxKey).child("description").setValue(boxEditDescriptionField.text.toString().trim())
                            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("boxes").child(boxKey).child("type").setValue(boxEditTypeField.text.toString().trim())
                            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("boxes").child(boxKey).child("qrcode").setValue(boxEditQrcodeField.text.toString().trim())
                            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("boxes").child(boxKey).child("in_vehicle").setValue(mVehicle.id)
                            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("boxes").child(boxKey).child("location_details").setValue(boxEditLocationDetailsField.text.toString().trim())
                            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("boxes").child(boxKey).child("color").setValue(boxEditColor)

                            val chipString = Utils.chipListToString(boxEditStatusChips)
                            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("boxes").child(boxKey).child("status").setValue(chipString)

                            if (::imageBitmap.isInitialized){
                                val updatedImage = Utils.getEncoded64ImageStringFromBitmap(imageBitmap)
                                FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("boxes").child(boxKey).child("image").setValue(
                                    updatedImage)
                            }


                            if (::locationImageBitmap.isInitialized){
                                val updatedLocationImage = Utils.getEncoded64ImageStringFromBitmap(locationImageBitmap)
                                FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("boxes").child(boxKey).child("location_image").setValue(
                                    updatedLocationImage)
                            }

                            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("boxes").child(boxKey).child("content").removeValue()
                            val itemList = mBoxItemEditAdapter.getCurrentStatus()
                            val updatedContentItems = ArrayList<ContentItem>()
                            for (item: BoxItemModel in itemList) {
                                val newItem = ContentItem(item.numeric_id, "", item.item_amount, item.item_amount_taken, item.item_id, item.item_invnum, item.item_color)
                                updatedContentItems.add(newItem)
                                FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("boxes").child(boxKey).child("content").push().setValue(newItem)
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
        mBoxModel.type = boxEditTypeField.text.toString()
        mBoxModel.in_vehicle = mVehicle.id
        mBoxModel.description = boxEditDescriptionField.text.toString()
        mBoxModel.color = boxEditColor
        mBoxModel.status = Utils.chipListToString(boxEditStatusChips)
        mBoxModel.image = ""
        if (::imageBitmap.isInitialized){
            mBoxModel.image = Utils.getEncoded64ImageStringFromBitmap(imageBitmap)
        }
        if (::locationImageBitmap.isInitialized){
            mBoxModel.location_image = Utils.getEncoded64ImageStringFromBitmap(locationImageBitmap)
        }
        val itemList = mBoxItemEditAdapter.getCurrentStatus()
        val newContentItems = ArrayList<ContentItem>()
        for (item: BoxItemModel in itemList) {
            val newItem = ContentItem(item.numeric_id,"", item.item_amount, item.item_amount_taken, item.item_id, item.item_invnum, item.item_color)
            newContentItems.add(newItem)
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

        // Get the arguments from the caller fragment/activity
        mBoxModel = arguments?.getSerializable("boxModel") as BoxModel
        val itemListArray: Array<BoxItemModel> =
            arguments?.getSerializable("items") as Array<BoxItemModel>
        mItemList = itemListArray.toCollection(ArrayList())
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
        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<String>("item_id")?.observe(
            viewLifecycleOwner) { result ->
            addSelectedItem(result)
        }
        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<VehicleModel>("vehicleModel")?.observe(
            viewLifecycleOwner) { result ->
            mVehicle = result
            mVehicleAdapter.setFilter(listOf(mVehicle))
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
        boxEditTypeField = v.findViewById(R.id.box_edit_type)
        boxEditQrcodeField = v.findViewById(R.id.box_edit_qrcode)
        boxEditDescriptionField = v.findViewById(R.id.box_edit_description)
        boxEditColorBtn = v.findViewById(R.id.box_edit_color_btn)
        boxEditColorPreview = v.findViewById(R.id.box_edit_color_preview)
        val boxEditAddButton: Button = v.findViewById(R.id.box_edit_add_button)
        boxEditImageField = v.findViewById(R.id.box_edit_image)
        boxEditLocationImageField = v.findViewById(R.id.box_edit_location_image)
        boxEditStatusChips = v.findViewById(R.id.box_edit_status_chips)
        boxEditStatusInput = v.findViewById(R.id.box_edit_status_input)
        boxEditVehicleField = v.findViewById(R.id.box_edit_vehicle_overlay)

        boxEditIdFieldContainer = v.findViewById(R.id.box_edit_id_container)
        boxEditNameFieldContainer = v.findViewById(R.id.box_edit_name_container)
        boxEditQrcodeFieldContainer = v.findViewById(R.id.box_edit_qrcode_container)
        boxEditLocationDetailsFieldContainer = v.findViewById(R.id.box_edit_location_details_container)
        boxEditTypeFieldContainer = v.findViewById(R.id.box_edit_type_container)

        boxEditImageSpinner = v.findViewById(R.id.bod_edit_image_spinner)
        boxEditLocationImageSpinner = v.findViewById(R.id.bod_edit_location_image_spinner)

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
        boxEditTypeField.setText(if (mBoxModel.type == "null") "" else mBoxModel.type)
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
            Glide.with(this).load(R.drawable.placeholder_with_bg_80).into(boxEditImageField)
        } else {
            boxEditImageField.setImageBitmap(Utils.stringToBitMap(mBoxModel.image))
        }

        if (mBoxModel.location_image == "") {
            Glide.with(this).load(R.drawable.placeholder_with_bg_80).into(boxEditLocationImageField)
        } else {
            boxEditLocationImageField.setImageBitmap(Utils.stringToBitMap(mBoxModel.location_image))
        }

        val thisFragment = this


        val startForMainImageResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                val resultCode = result.resultCode
                val data = result.data

                if (resultCode == AppCompatActivity.RESULT_OK) {
                    //Image Uri will not be null for RESULT_OK
                    val uri: Uri = data?.data!!
                    imageBitmap = MediaStore.Images.Media.getBitmap(context?.contentResolver, Uri.parse(uri.toString()))
                    boxEditImageField.setImageURI(uri)
                    boxEditImageSpinner.visibility = View.GONE
                //} else if (resultCode == ImagePicker.RESULT_ERROR) {
                    //Toast.makeText(context, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
                } else {
                    boxEditImageSpinner.visibility = View.GONE
                    Toast.makeText(context, resources.getString(R.string.task_cancelled), Toast.LENGTH_SHORT).show()
                }
            }

        boxEditImageField.setOnClickListener {
            ImagePicker.with(thisFragment)
                .crop()                    //Crop image(Optional), Check Customization for more option
                .compress(2048)            //Final image size will be less than 1 MB(Optional)
                .createIntent { intent ->
                    boxEditImageSpinner.visibility = View.VISIBLE
                    startForMainImageResult.launch(intent)
                }
        }

        val startForLocationImageResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                val resultCode = result.resultCode
                val data = result.data

                if (resultCode == AppCompatActivity.RESULT_OK) {
                    //Image Uri will not be null for RESULT_OK
                    val uri: Uri = data?.data!!
                    locationImageBitmap = MediaStore.Images.Media.getBitmap(context?.contentResolver, Uri.parse(uri.toString()))
                    boxEditLocationImageField.setImageURI(uri)
                    boxEditLocationImageSpinner.visibility = View.GONE
                //} else if (resultCode == ImagePicker.RESULT_ERROR) {
                    //Toast.makeText(context, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
                } else {
                    boxEditLocationImageSpinner.visibility = View.GONE
                    Toast.makeText(context, resources.getString(R.string.task_cancelled), Toast.LENGTH_SHORT).show()
                }
            }


        boxEditLocationImageField.setOnClickListener {
            ImagePicker.with(thisFragment)
                .crop()                    //Crop image(Optional), Check Customization for more option
                .compress(2048)            //Final image size will be less than 1 MB(Optional)
                .createIntent { intent ->
                    boxEditLocationImageSpinner.visibility = View.VISIBLE
                    startForLocationImageResult.launch(intent)
                }
        }


        //Init vehicle view
        mVehicleAdapter = VehicleAdapter(ArrayList<VehicleModel>())
        val recyclerviewVehicle = v.findViewById<View>(R.id.box_edit_vehicle_rv) as RecyclerView
        recyclerviewVehicle.layoutManager = LinearLayoutManager(activity)
        recyclerviewVehicle.adapter = mVehicleAdapter
        mVehicleAdapter.setFilter(listOf(mVehicle))

        //Init Items View
        val recyclerview = v.findViewById<View>(R.id.box_edit_content) as RecyclerView
        //box_item_edit_adapter = BoxItemEditAdapter(itemList, false, this)
        mBoxItemEditAdapter = BoxItemEditAdapter(mItemList)
        recyclerview.layoutManager = LinearLayoutManager(activity)
        recyclerview.adapter = mBoxItemEditAdapter

        boxEditAddButton.setOnClickListener { view ->
            if (view != null) {
                if (animationType == "elegant") {
                    exitTransition = Hold()
                }
                // Temp store elements for when item was added
                mBoxModel.status = Utils.chipListToString(boxEditStatusChips)
                mBoxModel.color = boxEditColor
                mItemList = mBoxItemEditAdapter.getCurrentStatus()

                val extras = FragmentNavigatorExtras(view to "transition_to_items")
                findNavController().navigate(
                    BoxEditFragmentDirections.actionBoxEditFragmentToNavigationItems(
                        returnItemInsteadOfShowDetails = true
                    ), extras
                )
            }
        }

        boxEditVehicleField.setOnClickListener { view ->
            if (view != null) {
                if (animationType == "elegant") {
                    exitTransition = Hold()
                }
                // Temp store elements for when item was added
                mBoxModel.status = Utils.chipListToString(boxEditStatusChips)
                mBoxModel.color = boxEditColor
                mItemList = mBoxItemEditAdapter.getCurrentStatus()

                val extras = FragmentNavigatorExtras(view to "transition_to_vehicles")
                findNavController().navigate(
                    BoxEditFragmentDirections.actionBoxEditFragmentToNavigationVehicles(
                        returnVehicleInsteadOfShowDetails = true
                    ), extras
                )
            }
        }


        val itemTouchHelper by lazy {
            val simpleItemTouchCallback = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.START or ItemTouchHelper.END, 0) {

                override fun onMove(recyclerView: RecyclerView,
                                    viewHolder: RecyclerView.ViewHolder,
                                    target: RecyclerView.ViewHolder): Boolean {
                    val adapter = recyclerView.adapter as BoxItemEditAdapter
                    val from = viewHolder.adapterPosition
                    val to = target.adapterPosition

                    adapter.notifyItemMoved(from, to)

                    return true
                }

                override fun onMoved(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    fromPos: Int,
                    target: RecyclerView.ViewHolder,
                    toPos: Int,
                    x: Int,
                    y: Int
                ) {
                    super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y)
                    val adapter = recyclerView.adapter as BoxItemEditAdapter
                    adapter.moveItem(fromPos, toPos)
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
        itemTouchHelper.attachToRecyclerView(recyclerview)


        return v
    }


    private fun addSelectedItem(item_id: String?) {
        val tempKey = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).toString()
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
                            mItemList.add(BoxItemModel(
                                (UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE).toString(),
                                item_id,
                                "1",
                                "0",
                                "",
                                itemName,
                                ContextCompat.getColor(requireContext(), R.color.default_item_color),
                                itemImage,
                                itemDescription,
                                itemTags
                            ))
                            mBoxItemEditAdapter.addToItemList(mItemList)
                            return@addOnCompleteListener
                        }
                    }
                }
            }
        }
        //addItem(item_id.toString())
    }


}