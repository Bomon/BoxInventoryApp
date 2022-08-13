package com.pixlbee.heros.fragments

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.pixlbee.heros.R
import com.pixlbee.heros.models.ItemModel
import com.pixlbee.heros.utility.Utils
import java.time.Instant
import java.time.format.DateTimeFormatter

class ItemEditFragment : Fragment() {

    private lateinit var mItemModel: ItemModel

    private lateinit var itemEditImageField: ImageView
    private lateinit var itemEditNameField: EditText
    private lateinit var itemEditNameLabel: TextInputLayout
    private lateinit var itemEditDescriptionField: EditText
    private lateinit var itemEditTagsField: EditText
    private lateinit var itemEditTagsChips: ChipGroup
    private lateinit var itemEditImageSpinner: ProgressBar

    private var isNewItem: Boolean = false

    private lateinit var imageBitmap: Bitmap

    private lateinit var animationType: String


    private fun checkFields(): Boolean {
        var status = true
        if (itemEditNameField.text.toString() == "") {
            itemEditNameLabel.isErrorEnabled = true
            itemEditNameLabel.error = resources.getString(R.string.error_field_empty)
            status = false
            Toast.makeText(context, resources.getString(R.string.error_item_edit_field), Toast.LENGTH_SHORT).show()
        }

        return status
    }


    private fun applyChanges(): Boolean {
        val fieldsOk: Boolean = checkFields()
        if (!fieldsOk) return false

        val itemsRef = FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("items")
        itemsRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val items: DataSnapshot? = task.result
                if (items != null) {
                    for (item: DataSnapshot in items.children) {
                        val id = item.child("id").value.toString()
                        if (id == mItemModel.id) {
                            val itemKey: String = item.key.toString()
                            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("items").child(itemKey).child("description").setValue(itemEditDescriptionField.text.toString().trim())
                            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("items").child(itemKey).child("name").setValue(itemEditNameField.text.toString().trim())
                            val chipString = Utils.chipListToString(itemEditTagsChips)
                            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("items").child(itemKey).child("tags").setValue(chipString)
                            if (::imageBitmap.isInitialized){
                                FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("items").child(itemKey).child("image").setValue(
                                    Utils.getEncoded64ImageStringFromBitmap(imageBitmap))
                            }
                        }
                    }
                }
            }
        }
        return true
    }


    private fun createItem(): Boolean {
        val fieldsOk: Boolean = checkFields()
        if (!fieldsOk) return false

        val itemDateKey = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).toString()
        mItemModel.id = itemDateKey
        mItemModel.name = itemEditNameField.text.toString()
        mItemModel.description = itemEditDescriptionField.text.toString()
        mItemModel.tags = itemEditTagsField.text.toString()
        mItemModel.image = ""
        if (::imageBitmap.isInitialized){
            mItemModel.image = Utils.getEncoded64ImageStringFromBitmap(imageBitmap)
        }
        FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("items").push().setValue(mItemModel)
        return true
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_item_edit, menu)
    }


    private fun showSaveDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(resources.getString(R.string.dialog_save_title))
        builder.setMessage(resources.getString(R.string.dialog_save_text))

        builder.setPositiveButton(resources.getString(R.string.dialog_yes)) { dialog, which ->
            val status: Boolean = applyChanges()
            if (status) {
                val navController = findNavController()
                navController.previousBackStackEntry?.savedStateHandle?.set(
                    "item",
                    mItemModel
                )
                navController.navigateUp()

            }
        }

        builder.setNegativeButton(resources.getString(R.string.dialog_no)) { dialog, which ->
            val navController: NavController = Navigation.findNavController(view!!)
            navController.navigateUp()
        }

        builder.setNeutralButton(resources.getString(R.string.dialog_cancel)) { dialog, which ->
        }
        builder.show()
    }


    private fun showDismissDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(resources.getString(R.string.dialog_dismiss_title))
        builder.setMessage(resources.getString(R.string.dialog_dismiss_text))

        builder.setPositiveButton(resources.getString(R.string.dialog_yes)) { dialog, which ->
            val navController: NavController = Navigation.findNavController(view!!)
            navController.navigateUp()
        }

        builder.setNegativeButton(resources.getString(R.string.dialog_no)) { dialog, which ->
        }
        builder.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.item_edit_btn_save) {
            // do not show save dialog for new items
            if (isNewItem){
                val status: Boolean = createItem()
                if (status) {
                    val navController = findNavController()
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        "item",
                        mItemModel
                    )
                    navController.popBackStack()
                }
            } else {
                showSaveDialog()
            }
        } else if (item.itemId == R.id.item_edit_btn_cancel) {
            showDismissDialog()
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        (activity as AppCompatActivity?)!!.supportActionBar!!.setDisplayHomeAsUpEnabled(false)
        (activity as AppCompatActivity?)!!.supportActionBar!!.setHomeButtonEnabled(false)


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
            returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)

        } else if (animationType == "simple"){
            enterTransition = MaterialFadeThrough()
            returnTransition = MaterialFadeThrough()
            exitTransition = MaterialFadeThrough()
        }

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            showDismissDialog()
        }

        // Get the arguments from the caller fragment/activity
        mItemModel = arguments?.getSerializable("itemModel") as ItemModel
        isNewItem = arguments?.getSerializable("isNewItem") as Boolean

        if (isNewItem){
            (activity as AppCompatActivity).supportActionBar?.title = resources.getString(R.string.fragment_item_edit_title)
        } else {
            (activity as AppCompatActivity).supportActionBar?.title = resources.getString(R.string.fragment_item_edit_title_new)
        }

        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        builder.detectFileUriExposure()
    }


    // For image picker
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode === RESULT_OK) {
            //Image Uri will not be null for RESULT_OK
            val uri: Uri = data?.data!!
            imageBitmap = MediaStore.Images.Media.getBitmap(context?.contentResolver, Uri.parse(uri.toString()))
            // Use Uri object instead of File to avoid storage permissions
            itemEditImageField.setImageURI(uri)
        } else if (resultCode == ImagePicker.RESULT_ERROR) {
            Toast.makeText(context, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, resources.getString(R.string.task_cancelled), Toast.LENGTH_SHORT).show()
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {

        val v =  inflater.inflate(R.layout.fragment_item_edit, container, false)

        // Get the activity and widget
        itemEditNameField = v.findViewById(R.id.item_edit_name)
        itemEditNameLabel = v.findViewById(R.id.item_edit_name_label)
        itemEditDescriptionField = v.findViewById(R.id.item_edit_description)
        itemEditTagsField = v.findViewById(R.id.item_edit_tags)
        itemEditImageField = v.findViewById(R.id.item_edit_image)
        itemEditTagsChips = v.findViewById(R.id.item_edit_tags_chips)
        itemEditImageSpinner = v.findViewById(R.id.item_edit_image_spinner)

        itemEditNameField.setText(mItemModel.name)
        itemEditDescriptionField.setText(mItemModel.description)

        if (mItemModel.image == "") {
            Glide.with(this).load(R.drawable.placeholder_with_bg_80_yellow).into(itemEditImageField)
        } else {
            itemEditImageField.setImageBitmap(Utils.stringToBitMap(mItemModel.image))
        }

        for (chip in mItemModel.tags.split(";")){
            if (chip != ""){
                addChipToGroup(chip)
            }
        }

        val thisFragment = this

        val confirmationChars = listOf(";", "\n")
        itemEditTagsField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                if (text.isNotEmpty() && (text.last().toString() in confirmationChars))
                    itemEditTagsField.setText("")
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
                        itemEditTagsField.setText("")
                    }
                }
            }
        })



        val startForImageResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                val resultCode = result.resultCode
                val data = result.data

                if (resultCode == RESULT_OK) {
                    //Image Uri will not be null for RESULT_OK
                    val uri: Uri = data?.data!!
                    imageBitmap = MediaStore.Images.Media.getBitmap(context?.contentResolver, Uri.parse(uri.toString()))
                    itemEditImageField.setImageURI(uri)
                    itemEditImageSpinner.visibility = View.GONE
                    //} else if (resultCode == ImagePicker.RESULT_ERROR) {
                    //Toast.makeText(context, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
                } else {
                    itemEditImageSpinner.visibility = View.GONE
                    Toast.makeText(context, resources.getString(R.string.task_cancelled), Toast.LENGTH_SHORT).show()
                }
            }


        itemEditImageField.setOnClickListener {
            ImagePicker.with(thisFragment)
                .crop()                    //Crop image(Optional), Check Customization for more option
                .compress(2048)            //Final image size will be less than 1 MB(Optional)
                .createIntent { intent ->
                    startForImageResult.launch(intent)
                    itemEditImageSpinner.visibility = View.VISIBLE
                }
        }

        return v
    }


    private fun addChipToGroup(chipText: String) {
        val chip = Chip(context)
        chip.text = chipText
        chip.isCloseIconVisible = true
        chip.isClickable = true
        chip.isCheckable = false
        itemEditTagsChips.addView(chip)
        chip.setOnCloseIconClickListener {
            itemEditTagsChips.removeView(chip as View)
        }
    }


}