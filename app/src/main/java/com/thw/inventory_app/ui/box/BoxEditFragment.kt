package com.thw.inventory_app.ui.box

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.Navigation
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.thw.inventory_app.*
import dev.sasikanth.colorsheet.ColorSheet
import java.time.Instant
import java.time.format.DateTimeFormatter


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

interface FragmentCallback {
    fun passSelectedItem(yourData: String?)
}

/**
 * A simple [Fragment] subclass.
 * Use the [BoxFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
//class BoxEditFragment : FragmentCallback, Fragment(), BoxItemEditAdapter.Callbacks {
class BoxEditFragment() : FragmentCallback, Fragment() {

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
    private var box_edit_color: Int = R.color.default_box_color


    private var is_new_box: Boolean = false
    lateinit var box_item_edit_adapter: BoxItemEditAdapter

    lateinit var itemList: ArrayList<BoxItemModel>

    //val items_to_add: ArrayList<String> = ArrayList<String>()
    //val items_to_delete: ArrayList<ItemCardUpdate> = ArrayList<ItemCardUpdate>()
    //val items_to_update = mutableMapOf<String, ItemCardUpdate>()
    //val items_to_add = mutableMapOf<String, ItemCardUpdate>()
    //val temp_added_items: ArrayList<String> = ArrayList<String>()
    //val new_keys: ArrayList<String> = ArrayList<String>()

    private lateinit var image_bitmap: Bitmap

    fun checkFields(): Boolean {
        var status = true
        if (box_edit_id_field.text.toString() == "") {
            box_edit_id_field_container.isErrorEnabled = true
            box_edit_id_field_container.error = "Feld darf nicht leer sein"
            status = false
        }
        Log.e("Error", Utils.getAllBoxIds().joinToString(", ") )
        if (box_edit_id_field.text.toString() in Utils.getAllBoxIds()) {
            box_edit_id_field_container.isErrorEnabled = true
            box_edit_id_field_container.error = "Es existiert bereits eine Box mit dieser ID"
            status = false
        }
        if (box_edit_name_field.text.toString() == "") {
            box_edit_name_field_container.isErrorEnabled = true
            box_edit_name_field_container.error = "Feld darf nicht leer sein"
            status = false
        }
        if (box_edit_qrcode_field.text.toString() == "") {
            box_edit_qrcode_field_container.isErrorEnabled = true
            box_edit_qrcode_field_container.error = "Feld darf nicht leer sein"
            status = false
        }
        if (box_edit_qrcode_field.text.toString() in Utils.getAllBoxQRcodes()) {
            box_edit_qrcode_field_container.isErrorEnabled = true
            box_edit_qrcode_field_container.error = "Es existiert bereits eine Box mit diesem QR-Code"
            status = false
        }
        if (box_edit_location_field.text.toString() == "") {
            box_edit_location_field_container.isErrorEnabled = true
            box_edit_location_field_container.error = "Feld darf nicht leer sein"
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
                            Log.e("Error", "Found box to delete in")
                            val boxKey: String = box.key.toString()
                            FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("id").setValue(box_edit_id_field.text.toString())
                            FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("name").setValue(box_edit_name_field.text.toString())
                            FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("description").setValue(box_edit_description_field.text.toString())
                            FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("qrcode").setValue(box_edit_qrcode_field.text.toString())
                            FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("status").setValue(Utils.chipListToString(box_edit_status_chips))
                            FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("location").setValue(box_edit_location_field.text.toString())
                            FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("color").setValue(box_edit_color)
                            if (::image_bitmap.isInitialized){
                                FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("image").setValue(Utils.getEncoded64ImageStringFromBitmap(image_bitmap))
                            }
                            val itemList = box_item_edit_adapter.getCurrentStatus()
                            FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("content").removeValue()
                            for (item: BoxItemModel in itemList) {
                                Log.e("Error", "Set item color " + item.item_name + " to " + item.item_color)
                                var new_item = ContentItem("", item.item_amount, item.item_id, item.item_invnum, item.item_color)
                                FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("content").push().setValue(new_item)
                            }
                        }
                    }
                }
            }
        }
        return true
    }

    fun createBox(){
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
        FirebaseDatabase.getInstance().reference.child("boxes").push().setValue(box_model)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_box_edit, menu)
    }

    fun showSaveDialog() {
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle("Änderungen Speichern?")

        builder.setPositiveButton("Ja") { dialog, which ->
            if (is_new_box){
                createBox()
            }
            val status: Boolean = applyChanges()

            val navController: NavController = Navigation.findNavController(view!!)
            navController.navigateUp()
            //if (status) parentFragmentManager.popBackStack()
        }

        builder.setNeutralButton("Abbrechen") { dialog, which ->
        }
        builder.show()
    }

    fun showDismissDialog() {
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle("Änderungen Verwerfen?")
        //builder.setMessage("Das ist ein Untertitel")

        builder.setPositiveButton("Ja") { dialog, which ->

            val navController: NavController = Navigation.findNavController(view!!)
            navController.navigateUp()
            //parentFragmentManager.popBackStack()
        }

        builder.setNegativeButton("Nein") { dialog, which ->
        }
        builder.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.box_edit_btn_save) {
            showSaveDialog()
        } else if (item.itemId == R.id.box_edit_btn_cancel) {
            showDismissDialog()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            val builder = MaterialAlertDialogBuilder(requireContext())
            builder.setTitle("Änderungen Verwerfen?")
            //builder.setMessage("Das ist ein Untertitel")

            builder.setPositiveButton("Ja") { dialog, which ->
                val navController: NavController = Navigation.findNavController(view!!)
                navController.navigateUp()
                //findNavController().popBackStack()
            }

            builder.setNegativeButton("Nein") { dialog, which ->
            }
            builder.show()
            // Now actually go back()
        }
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                Log.e("Error", "Back was pressed")
                val builder = MaterialAlertDialogBuilder(requireContext())
                builder.setTitle("Änderungen Verwerfen?")
                //builder.setMessage("Das ist ein Untertitel")

                builder.setPositiveButton("Ja") { dialog, which ->
                    setEnabled(false); //this is important line
                    requireActivity().onBackPressed();
                }

                builder.setNegativeButton("Nein") { dialog, which ->
                    setEnabled(true)
                }
                builder.show()
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);

        //val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
        //    Log.e("Error", "Back was pressed")
        //    showDismissDialog()
        //}


        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())
        builder.detectFileUriExposure()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.e("Error", "onActivityResult")
        //super.onActivityResult(requestCode, resultCode, data)
        //if (resultCode == RESULT_OK && requestCode == pickImage) {
        //    imageUri = data?.data
        //    box_image_field.setImageURI(imageUri)
        //}
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode === RESULT_OK) {
            //Image Uri will not be null for RESULT_OK
            val uri: Uri = data?.data!!
            image_bitmap = MediaStore.Images.Media.getBitmap(context?.contentResolver, Uri.parse(uri.toString()))
            // Use Uri object instead of File to avoid storage permissions
            box_edit_image_field.setImageURI(uri)
        } else if (resultCode == ImagePicker.RESULT_ERROR) {
            Toast.makeText(context, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Task Cancelled", Toast.LENGTH_SHORT).show()
        }
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
        val navController: NavController = Navigation.findNavController(view!!)
        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<String>("item_id")?.observe(
            viewLifecycleOwner) { result ->
            Log.e("Error", "Received from prev: " + result )
            passSelectedItem(result)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_box, container, false)

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

        val fragmentManager = (context as FragmentActivity).supportFragmentManager
        val colors = (context as FragmentActivity).resources.getIntArray(R.array.demo_colors)

        fun onColorClick(it: View){
            ColorSheet().colorPicker(
                colors = colors,
                listener = { color ->
                    box_edit_color_preview.background.setTint(color)
                    box_edit_color = color
                })
                .show(fragmentManager)
        }

        box_edit_color_btn.setOnClickListener {
            onColorClick(it)
        }
        box_edit_color_preview.setOnClickListener {
            onColorClick(it)
        }

        // Get the arguments from the caller fragment/activity
        box_model = arguments?.getSerializable("boxModel") as BoxModel
        var itemListArray: Array<BoxItemModel> = arguments?.getSerializable("items") as Array<BoxItemModel>
        itemList = itemListArray.toCollection(ArrayList())
        is_new_box = arguments?.getSerializable("isNewBox") as Boolean

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

        if (is_new_box){
            (activity as AppCompatActivity).supportActionBar?.title = "Box erstellen"

        } else {
            (activity as AppCompatActivity).supportActionBar?.title = "Box bearbeiten"
        }

        if (box_model.image == "") {
            Glide.with(this).load(R.drawable.placeholder_with_bg).into(box_edit_image_field)
        } else {
            box_edit_image_field.setImageBitmap(Utils.StringToBitMap(box_model.image))
        }

        val thisFragment = this

        box_edit_image_field.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                ImagePicker.with(thisFragment)
                    .crop(4f, 3f)	    			//Crop image(Optional), Check Customization for more option
                    .compress(1024)			//Final image size will be less than 1 MB(Optional)
                    .start()
            }
        })

        val this_frag_callback: FragmentCallback = this

        box_edit_add_button.setOnClickListener(object: View.OnClickListener {
            override fun onClick(v: View?) {
                if (view != null) {
                    val itemAddFragment: ItemsAddFragment = ItemsAddFragment.newInstance(box_model.id)
                    itemAddFragment.setFragmentCallback(this_frag_callback)

                    val navController: NavController = Navigation.findNavController(view!!)
                    val bundle = Bundle()
                    val itemModel: ItemModel = ItemModel("", "", "", "", "")
                    bundle.putSerializable("itemModel", itemModel)
                    bundle.putSerializable("isNewBox", true)
                    navController.navigate(R.id.action_boxEditFragment_to_itemsAddFragment, bundle)
                }
            }
        })

        //Init Items View
        val recyclerview = v.findViewById<View>(R.id.box_edit_content) as RecyclerView
        //box_item_edit_adapter = BoxItemEditAdapter(itemList, false, this)
        box_item_edit_adapter = BoxItemEditAdapter(itemList, false)
        recyclerview.layoutManager = LinearLayoutManager(activity)
        recyclerview.adapter = box_item_edit_adapter

        return v
    }

    override fun onDestroyView() {
        Log.w("box","destroy")
        super.onDestroyView()
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
        fun newInstance(boxModel: BoxModel, itemList: ArrayList<BoxItemModel>, isNewBox: Boolean) =
            BoxEditFragment().apply {
                val args = Bundle()
                args.putSerializable("model", boxModel)
                args.putSerializable("items", itemList)
                Log.e("Error", itemList.toString())
                args.putSerializable("isNewBox", isNewBox)
                val fragment = BoxEditFragment()
                fragment.arguments = args
                return fragment
            }
    }

    override fun passSelectedItem(item_id: String?) {

        var temp_key = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).toString()
        val itemsRef = FirebaseDatabase.getInstance().reference.child("items")
        itemsRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val items: DataSnapshot? = task.result
                if (items != null) {
                    for (item: DataSnapshot in items.children) {
                        val id = item.child("id").value.toString()
                        if (id == item_id) {
                            Log.e("Error", "Found item to delete in")
                            val item_name = item.child("name").value.toString()
                            val item_image = item.child("images").value.toString()
                            val item_description = item.child("description").value.toString()
                            val item_tags = item.child("tags").value.toString()
                            itemList.add(BoxItemModel(temp_key, item_id, "1", "", item_name, item_description, item_tags, R.color.default_item_color, item_image))
                            box_item_edit_adapter.addToItemList(itemList)

                            //items_to_add[temp_key] = ItemCardUpdate(temp_key, item_id, "1", "", "", -1)
                            //new_keys.add(temp_key)
                            return@addOnCompleteListener;
                        }
                    }
                }
            }
        }
        //addItem(item_id.toString())
    }
}