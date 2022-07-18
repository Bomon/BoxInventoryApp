package com.pixlbee.heros.fragments

import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.appcompat.app.AlertDialog
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
import android.widget.*
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
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
import com.google.android.material.transition.MaterialSharedAxis
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.pixlbee.heros.*
import com.pixlbee.heros.models.ItemModel
import com.pixlbee.heros.utility.Utils
import java.time.Instant
import java.time.format.DateTimeFormatter

class ItemEditFragment : Fragment() {

    private lateinit var item_model: ItemModel

    private lateinit var item_edit_image_field: ImageView
    private lateinit var item_edit_name_field: EditText
    private lateinit var item_edit_name_label: TextInputLayout
    private lateinit var item_edit_description_field: EditText
    private lateinit var item_edit_tags_field: EditText
    private lateinit var item_edit_tags_chips: ChipGroup

    private var is_new_item: Boolean = false

    private lateinit var image_bitmap: Bitmap


    fun checkFields(): Boolean {
        var status = true
        if (item_edit_name_field.text.toString() == "") {
            item_edit_name_label.isErrorEnabled = true
            item_edit_name_label.error = resources.getString(R.string.error_field_empty)
            status = false
        }

        return status
    }


    fun applyChanges(): Boolean {
        val fieldsOk: Boolean = checkFields()
        if (!fieldsOk) return false

        val itemsRef = FirebaseDatabase.getInstance().reference.child("items")
        itemsRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val items: DataSnapshot? = task.result
                if (items != null) {
                    for (item: DataSnapshot in items.children) {
                        val id = item.child("id").value.toString()
                        if (id == item_model.id) {
                            val itemKey: String = item.key.toString()
                            FirebaseDatabase.getInstance().reference.child("items").child(itemKey).child("description").setValue(item_edit_description_field.text.toString())
                            FirebaseDatabase.getInstance().reference.child("items").child(itemKey).child("name").setValue(item_edit_name_field.text.toString())
                            val chipString = Utils.chipListToString(item_edit_tags_chips)
                            FirebaseDatabase.getInstance().reference.child("items").child(itemKey).child("tags").setValue(chipString)
                            if (::image_bitmap.isInitialized){
                                FirebaseDatabase.getInstance().reference.child("items").child(itemKey).child("image").setValue(
                                    Utils.getEncoded64ImageStringFromBitmap(image_bitmap))
                            }
                        }
                    }
                }
            }
        }
        return true
    }


    fun createItem(): Boolean {
        val fieldsOk: Boolean = checkFields()
        if (!fieldsOk) return false

        var item_date_key = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).toString()
        item_model.id = item_date_key
        item_model.name = item_edit_name_field.text.toString()
        item_model.description = item_edit_description_field.text.toString()
        item_model.tags = item_edit_tags_field.text.toString()
        item_model.image = ""
        if (::image_bitmap.isInitialized){
            item_model.image = Utils.getEncoded64ImageStringFromBitmap(image_bitmap)
        }
        FirebaseDatabase.getInstance().reference.child("items").push().setValue(item_model)
        return true
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_item_edit, menu)
    }


    fun showSaveDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(resources.getString(R.string.dialog_save_title))
        builder.setMessage(resources.getString(R.string.dialog_save_text))

        builder.setPositiveButton(resources.getString(R.string.dialog_yes)) { dialog, which ->
            val status: Boolean = applyChanges()
            if (status) {
                val navController = findNavController()
                navController.previousBackStackEntry?.savedStateHandle?.set(
                    "item",
                    item_model
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


    fun showDismissDialog() {
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
            // dont show save dialog for new items
            if (is_new_item){
                val status: Boolean = createItem()
                if (status) {
                    val navController = findNavController()
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        "item",
                        item_model
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

        val transformEnter = MaterialContainerTransform(requireContext(), true)
        transformEnter.scrimColor = Color.TRANSPARENT
        sharedElementEnterTransition = transformEnter

        val transformReturn = MaterialContainerTransform(requireContext(), false)
        transformReturn.scrimColor = Color.TRANSPARENT
        sharedElementReturnTransition = transformReturn

        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            showDismissDialog()
        }

        // Get the arguments from the caller fragment/activity
        item_model = arguments?.getSerializable("itemModel") as ItemModel
        is_new_item = arguments?.getSerializable("isNewItem") as Boolean

        if (is_new_item){
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
            image_bitmap = MediaStore.Images.Media.getBitmap(context?.contentResolver, Uri.parse(uri.toString()))
            // Use Uri object instead of File to avoid storage permissions
            item_edit_image_field.setImageURI(uri)
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
        item_edit_name_field = v.findViewById(R.id.item_edit_name)
        item_edit_name_label = v.findViewById(R.id.item_edit_name_label)
        item_edit_description_field = v.findViewById(R.id.item_edit_description)
        item_edit_tags_field = v.findViewById(R.id.item_edit_tags)
        item_edit_image_field = v.findViewById(R.id.item_edit_image)
        item_edit_tags_chips = v.findViewById(R.id.item_edit_tags_chips)

        item_edit_name_field.setText(item_model.name)
        item_edit_description_field.setText(item_model.description)

        if (item_model.image == "") {
            Glide.with(this).load(R.drawable.placeholder_with_bg_80_yellow).into(item_edit_image_field)
        } else {
            item_edit_image_field.setImageBitmap(Utils.StringToBitMap(item_model.image))
        }

        for (chip in item_model.tags.split(";")){
            if (chip != ""){
                addChipToGroup(chip)
            }
        }

        val thisFragment = this

        val confirmationChars = listOf(";", "\n")
        item_edit_tags_field.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                if (text.isNotEmpty() && (text.last().toString() in confirmationChars))
                    item_edit_tags_field.setText("")
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
                        item_edit_tags_field.setText("")
                    }
                }
            }
        })

        item_edit_image_field.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                ImagePicker.with(thisFragment)
                    .crop(4f, 3f)	    			//Crop image(Optional), Check Customization for more option
                    .compress(1024)			//Final image size will be less than 1 MB(Optional)
                    .start()
            }
        })

        return v
    }


    private fun addChipToGroup(chipText: String) {
        val chip = Chip(context)
        chip.text = chipText
        chip.isCloseIconVisible = true
        chip.isClickable = true
        chip.isCheckable = false
        item_edit_tags_chips.addView(chip)
        chip.setOnCloseIconClickListener {
            item_edit_tags_chips.removeView(chip as View)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
    }


}