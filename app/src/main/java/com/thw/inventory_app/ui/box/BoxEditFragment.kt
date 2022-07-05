package com.thw.inventory_app.ui.box

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.VectorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.activity.addCallback
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.allViews
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.thw.inventory_app.*
import java.time.Instant
import java.time.format.DateTimeFormatter


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

interface FragmentCallback {
    fun onDataSent(yourData: String?)
}

/**
 * A simple [Fragment] subclass.
 * Use the [BoxFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BoxEditFragment : FragmentCallback, Fragment(), BoxItemEditAdapter.Callbacks {

    private lateinit var box_model: BoxModel

    private lateinit var box_edit_image_field: ImageView
    private lateinit var box_edit_location_image_field: ImageView
    private lateinit var box_edit_name_field: TextInputEditText
    private lateinit var box_edit_id_field: TextInputEditText
    private lateinit var box_edit_description_field: TextInputEditText
    private lateinit var box_edit_location_field: TextInputEditText
    private lateinit var box_edit_status_chips: ChipGroup
    private lateinit var box_edit_status_input: TextInputEditText
    //private lateinit var box_edit_qrcode_field: EditText
    //private lateinit var box_edit_color_field: EditText
    //private lateinit var box_edit_status_field: NachoTextView


    private var is_new_box: Boolean = false
    lateinit var box_item_edit_adapter: BoxItemEditAdapter

    lateinit var itemList: ArrayList<BoxItemModel>

    //val items_to_add: ArrayList<String> = ArrayList<String>()
    val items_to_delete: ArrayList<ItemCardUpdate> = ArrayList<ItemCardUpdate>()
    val items_to_update = mutableMapOf<String, ItemCardUpdate>()
    val items_to_add = mutableMapOf<String, ItemCardUpdate>()
    val temp_added_items: ArrayList<String> = ArrayList<String>()
    val new_keys: ArrayList<String> = ArrayList<String>()

    private lateinit var image_bitmap: Bitmap


    fun applyChanges() {
        Log.e("Error", "Items to add: " + items_to_add.toString())
        Log.e("Error", "Items to delete: " + items_to_delete.toString())
        for ((temp_key, data) in items_to_add) {
            // add to items
            /*val itemsRef = FirebaseDatabase.getInstance().reference.child("items")
            itemsRef.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val items: DataSnapshot? = task.result
                    if (items != null) {
                        for (item: DataSnapshot in items.children) {
                            val id = item.child("id").value.toString()
                            if (id == data.item_id) {
                                val inBoxes = item.child("boxes").value.toString()
                                val itemKey: String = item.key.toString()
                                FirebaseDatabase.getInstance().reference.child("items").child(itemKey).child("boxes").setValue(inBoxes + "," + box_model.box_id)
                                break
                            }
                        }
                    }
                }
            }*/
            // add to boxes
            val boxesRef = FirebaseDatabase.getInstance().reference.child("boxes")
            boxesRef.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val boxes: DataSnapshot? = task.result
                    if (boxes != null) {
                        for (box: DataSnapshot in boxes.children) {
                            val id = box.child("id").value.toString()
                            if (id == box_model.id) {
                                val boxKey: String = box.key.toString()
                                var new_item = ContentItem(data.amount, data.item_id, data.invnum, data.status)
                                FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("content").push().setValue(new_item)
                                break
                            }
                        }
                    }
                }
            }
        }

        // remove items
        for (item_card: ItemCardUpdate in items_to_delete){
            // remove from items
            /*val itemsRef = FirebaseDatabase.getInstance().reference.child("items")
            itemsRef.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val items: DataSnapshot? = task.result
                    if (items != null) {
                        for (item: DataSnapshot in items.children) {
                            val id = item.child("id").value.toString()
                            if (id == item_card.item_id) {
                                Log.e("Error", "Found item to delete in")
                                val inBoxes = item.child("boxes").value.toString()
                                val updatedInBoxes = inBoxes.replaceFirst(","+box_model.box_id, "")
                                val itemKey: String = item.key.toString()
                                FirebaseDatabase.getInstance().reference.child("items").child(itemKey).child("boxes").setValue(updatedInBoxes)
                                break
                            }
                        }
                    }
                }
            }*/
            // remove from boxes
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
                                FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("content").child(item_card.item_key).removeValue()
                            }
                        }
                    }
                }
            }
        }
        Log.e("Error", "values to update: " + items_to_update.toString())
        // update remaining entries
        items_to_update.forEach { entry ->
            val update = entry.value
            val boxesRef = FirebaseDatabase.getInstance().reference.child("boxes")
            boxesRef.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val boxes: DataSnapshot? = task.result
                    if (boxes != null) {
                        for (box: DataSnapshot in boxes.children) {
                            val id = box.child("id").value.toString()
                            if (id == box_model.id) {
                                val boxKey: String = box.key.toString()
                                FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("content").child(update.item_key).child("amount").setValue(update.amount)
                                FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("content").child(update.item_key).child("invnum").setValue(update.invnum)
                                break
                            }
                        }
                    }
                }
            }
        }

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
                            //FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("qrcode").setValue(box_edit_qrcode_field.text.toString())
                            FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("location").setValue(box_edit_location_field.text.toString())
                            FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("description").setValue(box_edit_description_field.text.toString())
                            //FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("color").setValue(box_edit_color_field.text.toString())

                            FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("status").setValue(chipListToString())
                            if (::image_bitmap.isInitialized){
                                FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("image").setValue(Utils.getEncoded64ImageStringFromBitmap(image_bitmap))
                            }
                        }
                    }
                }
            }
        }
    }

    fun createBox(){
        box_model.id = box_edit_id_field.text.toString()
        box_model.name = box_edit_name_field.text.toString()
        //box_model.qrcode = box_edit_qrcode_field.text.toString()
        box_model.location = box_edit_location_field.text.toString()
        box_model.description = box_edit_description_field.text.toString()
        //box_model.color = box_edit_color_field.text.toString()
        box_model.status = chipListToString()
        box_model.img = ""
        if (::image_bitmap.isInitialized){
            box_model.img = Utils.getEncoded64ImageStringFromBitmap(image_bitmap)
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
            applyChanges()
            parentFragmentManager.popBackStack()
        }

        //builder.setNegativeButton("Nein") { dialog, which ->
        //    items_to_add.clear()
        //    items_to_update.clear()
        //    items_to_delete.clear()
        //    parentFragmentManager.popBackStack()
        //}

        builder.setNeutralButton("Abbrechen") { dialog, which ->
        }
        builder.show()
    }

    fun showDismissDialog() {
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle("Änderungen Verwerfen?")
        //builder.setMessage("Das ist ein Untertitel")

        builder.setPositiveButton("Ja") { dialog, which ->
            items_to_add.clear()
            items_to_update.clear()
            items_to_delete.clear()
            parentFragmentManager.popBackStack()
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

        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            showDismissDialog()
        }


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

    fun chipListToString(): String{
        var chipString = ""
        for (chip in box_edit_status_chips.allViews) {
            if (chip is Chip){
                chipString += ";" + chip.text.toString()
            }
        }
        return chipString.removePrefix(";")
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
        //box_edit_qrcode_field = v.findViewById(R.id.box_edit_qrcode)
        box_edit_description_field = v.findViewById(R.id.box_edit_description)
        //box_edit_color_field = v.findViewById(R.id.box_edit_color)
        val box_edit_add_button: Button = v.findViewById(R.id.box_edit_add_button)
        box_edit_image_field = v.findViewById(R.id.box_edit_image)
        box_edit_location_image_field = v.findViewById(R.id.box_edit_location_image)
        box_edit_status_chips = v.findViewById(R.id.box_edit_status_chips)
        box_edit_status_input = v.findViewById(R.id.box_edit_status_input)

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

        // Get the arguments from the caller fragment/activity
        box_model = arguments?.getSerializable("model") as BoxModel
        itemList = arguments?.getSerializable("items") as ArrayList<BoxItemModel>
        is_new_box = arguments?.getSerializable("isNewBox") as Boolean

        box_edit_name_field.setText(box_model.name)
        box_edit_id_field.setText(box_model.id)
        box_edit_location_field.setText(box_model.location)
        //box_edit_qrcode_field.setText(box_model.qrcode)
        box_edit_description_field.setText(box_model.description)
        //box_edit_color_field.setText(box_model.color)

        for (chip in box_model.status.split(";")){
            addChipToGroup(chip)
        }



        if (box_model.img == "") {
            val myLogo = (ResourcesCompat.getDrawable(this.resources, R.drawable.ic_baseline_photo_size_select_actual_24, null) as VectorDrawable).toBitmap()
            box_edit_image_field.setImageBitmap(myLogo)
        } else {
            box_edit_image_field.setImageBitmap(Utils.StringToBitMap(box_model.img))
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
                    val context = v?.getContext()


                    Utils.pushFragment(itemAddFragment, requireContext(), "itemAddFragment")
                    //val transaction: FragmentTransaction =
                    //    (context as FragmentActivity).supportFragmentManager.beginTransaction()
                    //transaction.replace(R.id.nav_host_fragment_activity_main, itemAddFragment)
                    //transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    //transaction.addToBackStack("test")
                    //transaction.commit()
                }
            }
        })

        //Init Items View
        val recyclerview = v.findViewById<View>(R.id.box_edit_content) as RecyclerView
        box_item_edit_adapter = BoxItemEditAdapter(itemList, false, this)
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

    override fun onDataSent(item_id: String?) {

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
                            itemList.add(BoxItemModel(temp_key, item_id, "1", "", item_name, item_description, item_tags, "", item_image))
                            box_item_edit_adapter.addToItemList(itemList)

                            items_to_add[temp_key] = ItemCardUpdate(temp_key, item_id, "1", "", "", -1)
                            new_keys.add(temp_key)
                            return@addOnCompleteListener;
                        }
                    }
                }
            }
        }
        //addItem(item_id.toString())
    }

    override fun handleItemCardUpdate(data: ItemCardUpdate) {
        if (data.delete_index != -1) {
            itemList.removeAt(data.delete_index)
            box_item_edit_adapter.removeFromItemList(itemList, data.delete_index)
            if (data.item_key in new_keys) {
                items_to_add.remove(data.item_key)
            } else {
                items_to_delete.add(data)
            }
            items_to_update.remove(data.item_key)
        } else {
            if (data.item_key in new_keys){
                items_to_add[data.item_key] = data
            } else {
                items_to_update[data.item_key] = data
            }
        }
    }
}