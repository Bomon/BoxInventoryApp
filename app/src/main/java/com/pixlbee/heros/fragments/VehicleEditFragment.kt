package com.pixlbee.heros.fragments

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.StrictMode
import android.provider.MediaStore
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
import com.github.drjacky.imagepicker.ImagePicker
import com.github.drjacky.imagepicker.constant.ImageProvider
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.pixlbee.heros.R
import com.pixlbee.heros.models.VehicleModel
import com.pixlbee.heros.utility.Utils
import java.util.*

class VehicleEditFragment : Fragment() {

    private lateinit var mVehicleModel: VehicleModel

    private lateinit var vehicleEditImageField: ImageView
    private lateinit var vehicleEditNameField: EditText
    private lateinit var vehicleEditNameLabel: TextInputLayout
    private lateinit var vehicleEditCallnameField: EditText
    private lateinit var vehicleEditParkingSpotField: EditText
    private lateinit var vehicleEditDescriptionField: EditText
    private lateinit var vehicleEditImageSpinner: ProgressBar

    private var isNewVehicle: Boolean = false

    private lateinit var imageBitmap: Bitmap

    private lateinit var animationType: String


    private fun checkFields(): Boolean {
        var status = true
        if (vehicleEditNameField.text.toString() == "") {
            vehicleEditNameLabel.isErrorEnabled = true
            vehicleEditNameLabel.error = resources.getString(R.string.error_field_empty)
            status = false
            Toast.makeText(context, resources.getString(R.string.error_vehicle_edit_field), Toast.LENGTH_SHORT).show()
        }

        return status
    }


    private fun applyChanges(): Boolean {
        val fieldsOk: Boolean = checkFields()
        if (!fieldsOk) return false

        val vehiclesRef = FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("vehicles")
        vehiclesRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val vehicles: DataSnapshot? = task.result
                if (vehicles != null) {
                    for (vehicle: DataSnapshot in vehicles.children) {
                        val id = vehicle.child("id").value.toString()
                        if (id == mVehicleModel.id) {
                            val vehicleKey: String = vehicle.key.toString()
                            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("vehicles").child(vehicleKey).child("name").setValue(vehicleEditNameField.text.toString().trim())
                            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("vehicles").child(vehicleKey).child("callname").setValue(vehicleEditCallnameField.text.toString().trim())
                            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("vehicles").child(vehicleKey).child("description").setValue(vehicleEditDescriptionField.text.toString().trim())
                            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("vehicles").child(vehicleKey).child("parking_spot").setValue(vehicleEditParkingSpotField.text.toString().trim())
                            if (::imageBitmap.isInitialized){
                                FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("vehicles").child(vehicleKey).child("image").setValue(
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

        mVehicleModel.id = (UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE).toString()
        mVehicleModel.name = vehicleEditNameField.text.toString()
        mVehicleModel.callname = vehicleEditCallnameField.text.toString()
        mVehicleModel.description = vehicleEditDescriptionField.text.toString()
        mVehicleModel.parking_spot = vehicleEditParkingSpotField.text.toString()
        mVehicleModel.image = ""
        if (::imageBitmap.isInitialized){
            mVehicleModel.image = Utils.getEncoded64ImageStringFromBitmap(imageBitmap)
        }
        FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("vehicles").push().setValue(mVehicleModel)
        return true
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_vehicle_edit, menu)
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
                    "vehicle",
                    mVehicleModel
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
        if (item.itemId == R.id.vehicle_edit_btn_save) {
            // do not show save dialog for new items
            if (isNewVehicle){
                val status: Boolean = createItem()
                if (status) {
                    val navController = findNavController()
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        "vehicle",
                        mVehicleModel
                    )
                    navController.popBackStack()
                }
            } else {
                showSaveDialog()
            }
        } else if (item.itemId == R.id.vehicle_edit_btn_cancel) {
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
        mVehicleModel = arguments?.getSerializable("vehicleModel") as VehicleModel
        isNewVehicle = arguments?.getSerializable("isNewVehicle") as Boolean

        if (isNewVehicle){
            (activity as AppCompatActivity).supportActionBar?.title = resources.getString(R.string.fragment_vehicle_edit_title_new)
        } else {
            (activity as AppCompatActivity).supportActionBar?.title = resources.getString(R.string.fragment_vehicle_edit_title)
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
            vehicleEditImageField.setImageURI(uri)
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

        val v =  inflater.inflate(R.layout.fragment_vehicle_edit, container, false)

        // Get the activity and widget
        vehicleEditNameField = v.findViewById(R.id.vehicle_edit_name)
        vehicleEditCallnameField = v.findViewById(R.id.vehicle_edit_callname)
        vehicleEditDescriptionField = v.findViewById(R.id.vehicle_edit_description)
        vehicleEditParkingSpotField = v.findViewById(R.id.vehicle_edit_parking_spot)
        vehicleEditNameLabel = v.findViewById(R.id.vehicle_edit_name_label)
        vehicleEditImageField = v.findViewById(R.id.vehicle_edit_image)
        vehicleEditImageSpinner = v.findViewById(R.id.vehicle_edit_image_spinner)

        vehicleEditNameField.setText(mVehicleModel.name)
        vehicleEditCallnameField.setText(mVehicleModel.callname)
        vehicleEditParkingSpotField.setText(mVehicleModel.parking_spot)
        vehicleEditDescriptionField.setText(mVehicleModel.description)

        if (mVehicleModel.image == "") {
            Glide.with(this).load(R.drawable.placeholder_with_bg_80).into(vehicleEditImageField)
        } else {
            vehicleEditImageField.setImageBitmap(Utils.stringToBitMap(mVehicleModel.image))
        }

        val thisFragment = this

        val startForImageResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                val resultCode = result.resultCode
                val data = result.data

                if (resultCode == RESULT_OK) {
                    //Image Uri will not be null for RESULT_OK
                    val uri: Uri = data?.data!!
                    imageBitmap = MediaStore.Images.Media.getBitmap(context?.contentResolver, Uri.parse(uri.toString()))
                    vehicleEditImageField.setImageURI(uri)
                    vehicleEditImageSpinner.visibility = View.GONE
                    //} else if (resultCode == ImagePicker.RESULT_ERROR) {
                    //Toast.makeText(context, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
                } else {
                    vehicleEditImageSpinner.visibility = View.GONE
                    Toast.makeText(context, resources.getString(R.string.task_cancelled), Toast.LENGTH_SHORT).show()
                }
            }


        vehicleEditImageField.setOnClickListener {
            ImagePicker.with(activity as AppCompatActivity)
                .crop()
                .cropFreeStyle()
                .provider(ImageProvider.BOTH)
                .createIntentFromDialog { intent ->
                    startForImageResult.launch(intent)
                    vehicleEditImageSpinner.visibility = View.VISIBLE
                }
        }

        return v
    }


}