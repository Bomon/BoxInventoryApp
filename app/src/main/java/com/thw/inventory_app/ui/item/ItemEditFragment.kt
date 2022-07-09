package com.thw.inventory_app.ui.item

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.VectorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
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
class ItemEditFragment : Fragment() {

    private lateinit var item_model: ItemModel

    private lateinit var item_edit_image_field: ImageView
    private lateinit var item_edit_name_field: EditText
    private lateinit var item_edit_description_field: EditText
    private lateinit var item_edit_tags_field: EditText

    private var is_new_item: Boolean = false

    private lateinit var image_bitmap: Bitmap

    fun applyChanges() {

        val itemsRef = FirebaseDatabase.getInstance().reference.child("items")
        itemsRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val items: DataSnapshot? = task.result
                if (items != null) {
                    for (item: DataSnapshot in items.children) {
                        val id = item.child("id").value.toString()
                        if (id == item_model.id) {
                            Log.e("Error", "Found box to delete in")
                            val itemKey: String = item.key.toString()
                            FirebaseDatabase.getInstance().reference.child("items").child(itemKey).child("description").setValue(item_edit_description_field.text.toString())
                            FirebaseDatabase.getInstance().reference.child("items").child(itemKey).child("name").setValue(item_edit_name_field.text.toString())
                            FirebaseDatabase.getInstance().reference.child("items").child(itemKey).child("tags").setValue(item_edit_tags_field.text.toString())
                            if (::image_bitmap.isInitialized){
                                FirebaseDatabase.getInstance().reference.child("items").child(itemKey).child("image").setValue(Utils.getEncoded64ImageStringFromBitmap(image_bitmap))
                            }
                        }
                    }
                }
            }
        }

    }

    fun createItem(){
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
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_item_edit, menu)
    }

    fun showSaveDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Änderungen Speichern?")
        builder.setMessage("We have a message")

        builder.setPositiveButton("Ja") { dialog, which ->
            Toast.makeText(requireContext(),
                "yes", Toast.LENGTH_SHORT).show()
            if (is_new_item){
                createItem()
            }
            applyChanges()
            parentFragmentManager.popBackStack()
        }

        builder.setNegativeButton("Nein") { dialog, which ->
            Toast.makeText(requireContext(),
                "no", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }

        builder.setNeutralButton("Abbrechen") { dialog, which ->
            Toast.makeText(requireContext(),
                "cancel", Toast.LENGTH_SHORT).show()
        }
        builder.show()
    }

    fun showDismissDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Änderungen Verwerfen?")
        builder.setMessage("We have a message")

        builder.setPositiveButton("Ja") { dialog, which ->
            Toast.makeText(requireContext(),
                "yes", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }

        builder.setNegativeButton("Nein") { dialog, which ->
            Toast.makeText(requireContext(),
                "no", Toast.LENGTH_SHORT).show()
        }
        builder.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.item_edit_btn_save) {
            showSaveDialog()
        } else if (item.itemId == R.id.item_edit_btn_cancel) {
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
            item_edit_image_field.setImageURI(uri)
        } else if (resultCode == ImagePicker.RESULT_ERROR) {
            Toast.makeText(context, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Task Cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_box, container, false)

        val v =  inflater.inflate(R.layout.fragment_item_edit, container, false)

        // Get the activity and widget
        item_edit_name_field = v.findViewById(R.id.item_edit_name)
        item_edit_description_field = v.findViewById(R.id.item_edit_description)
        item_edit_tags_field = v.findViewById(R.id.item_edit_tags)
        item_edit_image_field = v.findViewById(R.id.item_edit_image)

        // Get the arguments from the caller fragment/activity
        item_model = arguments?.getSerializable("model") as ItemModel
        is_new_item = arguments?.getSerializable("isNewBox") as Boolean

        item_edit_name_field.setText(item_model.name)
        item_edit_description_field.setText(item_model.description)
        item_edit_tags_field.setText(item_model.tags)
        if (item_model.image == "") {
            Glide.with(this).load(R.drawable.ic_placeholder).into(item_edit_image_field)
        } else {
            item_edit_image_field.setImageBitmap(Utils.StringToBitMap(item_model.image))
        }

        val thisFragment = this

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
        fun newInstance(itemModel: ItemModel, isNewBox: Boolean) =
            ItemEditFragment().apply {
                val args = Bundle()
                args.putSerializable("model", itemModel)
                args.putSerializable("isNewBox", isNewBox)
                val fragment = ItemEditFragment()
                fragment.arguments = args
                return fragment
            }
    }
}