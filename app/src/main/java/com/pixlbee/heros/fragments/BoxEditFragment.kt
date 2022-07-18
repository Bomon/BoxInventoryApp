package com.pixlbee.heros.fragments

import android.app.Activity
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
import com.pixlbee.heros.*
import com.pixlbee.heros.adapters.BoxItemEditAdapter
import com.pixlbee.heros.models.BoxItemModel
import com.pixlbee.heros.models.BoxModel
import com.pixlbee.heros.models.ContentItem
import com.pixlbee.heros.utility.Utils
import dev.sasikanth.colorsheet.ColorSheet
import java.time.Instant
import java.time.format.DateTimeFormatter


class BoxEditFragment : Fragment() {

    private lateinit var box_model: BoxModel

    private lateinit var box_edit_image_field: ImageView
    private lateinit var box_edit_location_image_field: ImageView
    private lateinit var box_edit_location_field_container: TextInputLayout
    private lateinit var box_edit_name_field: TextInputEditText
    private lateinit var box_edit_name_field_container: TextInputLayout
    private lateinit var box_edit_id_field: TextInputEditText
    private lateinit var box_edit_id_field_container: TextInputLayout
    private lateinit var box_edit_description_field: TextInputEditText
    private lateinit var box_edit_location_field: TextInputEditText
    private lateinit var box_edit_status_chips: ChipGroup
    private lateinit var box_edit_status_input: TextInputEditText
    private lateinit var box_edit_qrcode_field: EditText
    private lateinit var box_edit_qrcode_field_container: TextInputLayout
    private lateinit var box_edit_color_btn: MaterialButton
    private lateinit var box_edit_color_preview: View
    private var box_edit_color: Int = -1

    var qrList: ArrayList<String> = ArrayList<String>()
    var idList: ArrayList<String> = ArrayList<String>()

    private var is_new_box: Boolean = false
    lateinit var box_item_edit_adapter: BoxItemEditAdapter

    lateinit var itemList: ArrayList<BoxItemModel>
    lateinit var navController: NavController

    private lateinit var image_bitmap: Bitmap
    private lateinit var location_image_bitmap: Bitmap


    fun checkFields(): Boolean {
        var status = true
        if (box_edit_id_field.text.toString() == "") {
            box_edit_id_field_container.isErrorEnabled = true
            box_edit_id_field_container.error = resources.getString(R.string.error_field_empty)
            status = false
        }
        if (box_edit_id_field.text.toString() in idList) {
            box_edit_id_field_container.isErrorEnabled = true
            box_edit_id_field_container.error = resources.getString(R.string.error_box_already_exists_id)
            status = false
        }
        if (box_edit_name_field.text.toString() == "") {
            box_edit_name_field_container.isErrorEnabled = true
            box_edit_name_field_container.error = resources.getString(R.string.error_field_empty)
            status = false
        }
        if (box_edit_qrcode_field.text.toString() == "") {
            box_edit_qrcode_field_container.isErrorEnabled = true
            box_edit_qrcode_field_container.error = resources.getString(R.string.error_field_empty)
            status = false
        }
        if (box_edit_qrcode_field.text.toString() in qrList) {
            box_edit_qrcode_field_container.isErrorEnabled = true
            box_edit_qrcode_field_container.error = resources.getString(R.string.error_box_already_exists_qrcode)
            status = false
        }
        if (box_edit_location_field.text.toString() == "") {
            box_edit_location_field_container.isErrorEnabled = true
            box_edit_location_field_container.error = resources.getString(R.string.error_field_empty)
            status = false
        }

        return status
    }


    fun applyChanges(): Boolean {
        val fieldsOk: Boolean = checkFields()
        if (!fieldsOk) return false

        val boxesRef = FirebaseDatabase.getInstance().reference.child("boxes")
        boxesRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val boxes: DataSnapshot? = task.result
                if (boxes != null) {
                    for (box: DataSnapshot in boxes.children) {
                        val id = box.child("id").value.toString()
                        if (id == box_model.id) {
                            val boxKey: String = box.key.toString()
                            FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("id").setValue(box_edit_id_field.text.toString())
                            FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("name").setValue(box_edit_name_field.text.toString())
                            FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("description").setValue(box_edit_description_field.text.toString())
                            FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("qrcode").setValue(box_edit_qrcode_field.text.toString())
                            FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("location").setValue(box_edit_location_field.text.toString())
                            FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("color").setValue(box_edit_color)

                            val chipString = Utils.chipListToString(box_edit_status_chips)
                            FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("status").setValue(chipString)

                            if (::image_bitmap.isInitialized){
                                val updated_image = Utils.getEncoded64ImageStringFromBitmap(image_bitmap)
                                FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("image").setValue(
                                    updated_image)
                            }


                            if (::location_image_bitmap.isInitialized){
                                val updated_location_image = Utils.getEncoded64ImageStringFromBitmap(location_image_bitmap)
                                FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("location_image").setValue(
                                    updated_location_image)
                            }

                            FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("content").removeValue()
                            val itemList = box_item_edit_adapter.getCurrentStatus()
                            val updatedContentItems = ArrayList<ContentItem>()
                            for (item: BoxItemModel in itemList) {
                                val new_item = ContentItem("", item.item_amount, item.item_id, item.item_invnum, item.item_color)
                                updatedContentItems.add(new_item)
                                FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("content").push().setValue(new_item)
                            }
                            // This is just a workaround
                            // Problem: when ID is changed, Box Fragment no longer can find the right box
                            // Hence we go directly back to home after editing where the box is udpated
                            // If user clicks it again, he sees the box with the new ID
                            if (box_edit_id_field.text.toString() != box_model.id){
                                navController.navigateUp()
                            }
                        }
                    }
                }
            }
        }
        return true
    }


    fun createBox(): Boolean {
        val fieldsOk: Boolean = checkFields()
        if (!fieldsOk) return false

        box_model.id = box_edit_id_field.text.toString()
        box_model.name = box_edit_name_field.text.toString()
        box_model.qrcode = box_edit_qrcode_field.text.toString()
        box_model.location = box_edit_location_field.text.toString()
        box_model.description = box_edit_description_field.text.toString()
        box_model.color = box_edit_color
        box_model.status = ""
        box_model.image = ""
        if (::image_bitmap.isInitialized){
            box_model.image = Utils.getEncoded64ImageStringFromBitmap(image_bitmap)
        }
        if (::location_image_bitmap.isInitialized){
            box_model.location_image = Utils.getEncoded64ImageStringFromBitmap(location_image_bitmap)
        }
        FirebaseDatabase.getInstance().reference.child("boxes").push().setValue(box_model)
        return true
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_box_edit, menu)
    }


    fun showSaveDialog() {
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


    fun showDismissDialog() {
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
            if (is_new_box){
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


    fun initQrCodeList(){
        val boxesRef = FirebaseDatabase.getInstance().reference.child("boxes")
        boxesRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val boxes: DataSnapshot? = task.result
                if (boxes != null) {
                    qrList.clear()
                    for (box: DataSnapshot in boxes.children) {
                        if (box.child("id").value.toString() != box_model.id){
                            qrList.add(box.child("qrcode").value.toString())
                        }
                    }
                }
            }
        }
    }

    fun initBoxIdList(){
        val boxesRef = FirebaseDatabase.getInstance().reference.child("boxes")
        boxesRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val boxes: DataSnapshot? = task.result
                if (boxes != null) {
                    idList.clear()
                    for (box: DataSnapshot in boxes.children) {
                        if (box.child("id").value.toString() != box_model.id){
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
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)

        // This task is done once on fragment creation
        // Reason: each qrcode / boxId must only exist once. The task is async and so we start it here to have the result when user clicks save
        initQrCodeList()
        initBoxIdList()

        // Get the arguments from the caller fragment/activity
        box_model = arguments?.getSerializable("boxModel") as BoxModel
        val itemListArray: Array<BoxItemModel> = arguments?.getSerializable("items") as Array<BoxItemModel>
        itemList = itemListArray.toCollection(ArrayList())
        is_new_box = arguments?.getSerializable("isNewBox") as Boolean


        if (is_new_box){
            (activity as AppCompatActivity).supportActionBar?.title = resources.getString(R.string.fragment_box_edit_title_new)
        } else {
            (activity as AppCompatActivity).supportActionBar?.title = resources.getString(R.string.fragment_box_edit_title)
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
        box_edit_status_chips.addView(chip)
        chip.setOnCloseIconClickListener {
            box_edit_status_chips.removeView(chip as View)
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        navController = Navigation.findNavController(view)
        // Receive data from ItemAddFragment (selected Item)
        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<String>("item_id")?.observe(
            viewLifecycleOwner) { result ->
            addSelectedItem(result)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {

        exitTransition = MaterialFadeThrough()

        val v =  inflater.inflate(R.layout.fragment_box_edit, container, false)

        // Get the activity and widget
        box_edit_name_field = v.findViewById(R.id.box_edit_name)
        box_edit_id_field = v.findViewById(R.id.box_edit_id)
        box_edit_location_field = v.findViewById(R.id.box_edit_location)
        box_edit_qrcode_field = v.findViewById(R.id.box_edit_qrcode)
        box_edit_description_field = v.findViewById(R.id.box_edit_description)
        box_edit_color_btn = v.findViewById(R.id.box_edit_color_btn)
        box_edit_color_preview = v.findViewById(R.id.box_edit_color_preview)
        val box_edit_add_button: Button = v.findViewById(R.id.box_edit_add_button)
        box_edit_image_field = v.findViewById(R.id.box_edit_image)
        box_edit_location_image_field = v.findViewById(R.id.box_edit_location_image)
        box_edit_status_chips = v.findViewById(R.id.box_edit_status_chips)
        box_edit_status_input = v.findViewById(R.id.box_edit_status_input)

        box_edit_id_field_container = v.findViewById(R.id.box_edit_id_container)
        box_edit_name_field_container = v.findViewById(R.id.box_edit_name_container)
        box_edit_qrcode_field_container = v.findViewById(R.id.box_edit_qrcode_container)
        box_edit_location_field_container = v.findViewById(R.id.box_edit_location_container)

        val confirmationChars = listOf(";", "\n")
        box_edit_status_input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                if (text.isNotEmpty() && (text.last().toString() in confirmationChars))
                    box_edit_status_input.setText("")
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
                        box_edit_status_input.setText("")
                    }
                }
            }
        })

        box_edit_id_field.onFocusChangeListener = object: View.OnFocusChangeListener{
            override fun onFocusChange(v: View?, hasFocus: Boolean) {
                if (!hasFocus) {
                    if (box_edit_qrcode_field.text.toString() == ""){
                        box_edit_qrcode_field.text = box_edit_id_field.text
                    }
                }
            }

        }

        val fragmentManager = (context as FragmentActivity).supportFragmentManager
        val colors = (context as FragmentActivity).resources.getIntArray(R.array.picker_colors)

        fun onColorClick() {
            ColorSheet().colorPicker(
                colors = colors,
                listener = { color ->
                    box_edit_color_preview.background.setTint(color)
                    box_edit_color = color
                })
                .show(fragmentManager)
        }

        box_edit_color_btn.setOnClickListener {
            onColorClick()
        }
        box_edit_color_preview.setOnClickListener {
            onColorClick()
        }

        box_edit_name_field.setText(box_model.name)
        box_edit_id_field.setText(box_model.id)
        box_edit_location_field.setText(box_model.location)
        box_edit_qrcode_field.setText(box_model.qrcode)
        box_edit_description_field.setText(box_model.description)
        box_edit_color_preview.background.setTint(box_model.color)
        box_edit_color = box_model.color

        for (chip in box_model.status.split(";")){
            if (chip != ""){
                addChipToGroup(chip)
            }
        }

        if (box_model.image == "") {
            Glide.with(this).load(R.drawable.placeholder_with_bg_80).into(box_edit_image_field)
        } else {
            box_edit_image_field.setImageBitmap(Utils.StringToBitMap(box_model.image))
        }

        val thisFragment = this


        val startForMainImageResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                val resultCode = result.resultCode
                val data = result.data

                if (resultCode == Activity.RESULT_OK) {
                    //Image Uri will not be null for RESULT_OK
                    val uri: Uri = data?.data!!
                    image_bitmap = MediaStore.Images.Media.getBitmap(context?.contentResolver, Uri.parse(uri.toString()))
                    box_edit_image_field.setImageURI(uri)
                //} else if (resultCode == ImagePicker.RESULT_ERROR) {
                    //Toast.makeText(context, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, resources.getString(R.string.task_cancelled), Toast.LENGTH_SHORT).show()
                }
            }

        box_edit_image_field.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                ImagePicker.with(thisFragment)
                    .crop(4f, 3f)	    			//Crop image(Optional), Check Customization for more option
                    .compress(1024)			//Final image size will be less than 1 MB(Optional)
                    .createIntent { intent ->
                        startForMainImageResult.launch(intent)
                    }
            }
        })

        val startForLocationImageResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                val resultCode = result.resultCode
                val data = result.data

                if (resultCode == Activity.RESULT_OK) {
                    //Image Uri will not be null for RESULT_OK
                    val uri: Uri = data?.data!!
                    location_image_bitmap = MediaStore.Images.Media.getBitmap(context?.contentResolver, Uri.parse(uri.toString()))
                    box_edit_location_image_field.setImageURI(uri)
                //} else if (resultCode == ImagePicker.RESULT_ERROR) {
                    //Toast.makeText(context, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, resources.getString(R.string.task_cancelled), Toast.LENGTH_SHORT).show()
                }
            }


        box_edit_location_image_field.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                ImagePicker.with(thisFragment)
                    .crop()	    			//Crop image(Optional), Check Customization for more option
                    .compress(1024)			//Final image size will be less than 1 MB(Optional)
                    .createIntent { intent ->
                        startForLocationImageResult.launch(intent)
                    }
            }
        })

        //Init Items View
        val recyclerview = v.findViewById<View>(R.id.box_edit_content) as RecyclerView
        //box_item_edit_adapter = BoxItemEditAdapter(itemList, false, this)
        box_item_edit_adapter = BoxItemEditAdapter(itemList)
        recyclerview.layoutManager = LinearLayoutManager(activity)
        recyclerview.adapter = box_item_edit_adapter

        box_edit_add_button.setOnClickListener(object: View.OnClickListener {
            override fun onClick(view: View?) {
                if (view != null) {
                    exitTransition = Hold()
                    // Temp store elements for when item was added
                    box_model.status = Utils.chipListToString(box_edit_status_chips)
                    box_model.color = box_edit_color
                    itemList = box_item_edit_adapter.getCurrentStatus()

                    val extras = FragmentNavigatorExtras(view to "transition_to_items")
                    findNavController().navigate(BoxEditFragmentDirections.actionBoxEditFragmentToNavigationItems(returnItemInsteadOfShowDetails = true), extras)
                }
            }
        })

        return v
    }


    fun addSelectedItem(item_id: String?) {
        val temp_key = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).toString()
        val itemsRef = FirebaseDatabase.getInstance().reference.child("items")
        itemsRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val items: DataSnapshot? = task.result
                if (items != null) {
                    for (item: DataSnapshot in items.children) {
                        val id = item.child("id").value.toString()
                        if (id == item_id) {
                            val item_name = item.child("name").value.toString()
                            val item_image = item.child("images").value.toString()
                            val item_description = item.child("description").value.toString()
                            val item_tags = item.child("tags").value.toString()
                            itemList.add(BoxItemModel(temp_key, item_id, "1", "", item_name, item_description, item_tags, ContextCompat.getColor(requireContext(), R.color.default_item_color), item_image))
                            box_item_edit_adapter.addToItemList(itemList)
                            return@addOnCompleteListener
                        }
                    }
                }
            }
        }
        //addItem(item_id.toString())
    }


}