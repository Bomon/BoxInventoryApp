package com.pixlbee.heros.fragments

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialSharedAxis
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.pixlbee.heros.R
import com.pixlbee.heros.adapters.BoxAdapter
import com.pixlbee.heros.models.BoxModel
import com.pixlbee.heros.models.VehicleModel
import com.pixlbee.heros.utility.Utils
import com.stfalcon.imageviewer.StfalconImageViewer


class VehicleDetailFragment : Fragment() {
    private lateinit var mVehicleModel: VehicleModel
    lateinit var mAdapter: BoxAdapter
    private lateinit var mFirebaseListener: ValueEventListener
    var mBoxList: ArrayList<BoxModel> = ArrayList()

    private lateinit var vehicleNameField: TextView
    private lateinit var vehicleCallnameField: ChipGroup
    private lateinit var vehicleParkingSpotField: TextView
    private lateinit var vehicleDescriptionField: TextView
    private lateinit var vehicleImageField: ImageView
    lateinit var vehicleContainedBoxesEmptyLabel: TextView


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_vehicle_details, menu)
    }


    private fun deleteVehicle() {
        // remove from boxes
        val boxesRef = FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("boxes")
        boxesRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val boxes: DataSnapshot? = task.result
                if (boxes != null) {
                    for (box: DataSnapshot in boxes.children) {
                        val vehicleId = box.child("in_vehicle").value.toString()
                        if (vehicleId == mVehicleModel.id.toString()) {
                            val boxKey = box.key.toString()
                            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("boxes")
                                .child(boxKey).child("in_vehicle").setValue("")
                            break
                        }
                    }
                }
            }
        }

        val vehiclesRef = FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("vehicles")
        vehiclesRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val vehicles: DataSnapshot? = task.result
                if (vehicles != null) {
                    for (vehicle: DataSnapshot in vehicles.children) {
                        val id = vehicle.child("id").value.toString()
                        val vehicleKey = vehicle.key.toString()
                        if (id == mVehicleModel.id.toString()) {
                            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(context!!)).child("vehicles").child(vehicleKey).removeValue()
                            break
                        }
                    }
                }
            }
        }
    }


    private fun showDeleteDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(resources.getString(R.string.dialog_delete_vehicle_title))
        builder.setMessage(resources.getString(R.string.dialog_delete_vehicle_text))

        builder.setPositiveButton(R.string.dialog_yes) { dialog, which ->
            deleteVehicle()
            val navController: NavController = Navigation.findNavController(view!!)
            navController.navigateUp()
        }

        builder.setNegativeButton(R.string.dialog_no) { dialog, which ->
        }
        builder.show()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.vehicle_btn_edit) {
            if(Utils.checkHasWritePermission(context)) {
                if (view != null) {
                    val bundle = Bundle()
                    bundle.putSerializable("vehicleModel", mVehicleModel)
                    bundle.putSerializable("isNewVehicle", false)
                    val navController: NavController = Navigation.findNavController(view!!)
                    navController.navigate(R.id.action_vehicleDetailFragment_to_vehicleEditFragment, bundle)
                }
            }
            return true
        } else if (item.itemId == R.id.vehicle_btn_delete) {
            if(Utils.checkHasWritePermission(context)) {
                showDeleteDialog()
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        val transform = MaterialContainerTransform()
        transform.scrimColor = Color.TRANSPARENT
        sharedElementEnterTransition = transform
        sharedElementReturnTransition = transform

        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)

        (activity as AppCompatActivity).supportActionBar?.title = resources.getString(R.string.vehicle_details_title)

        // Get the arguments from the caller fragment/activity
        mVehicleModel = arguments?.getSerializable("vehicleModel") as VehicleModel
    }


    private fun updateContent(){
        val vehicleName = mVehicleModel.name
        val vehicleCallname = mVehicleModel.callname
        val vehicleDescription = mVehicleModel.description
        val vehicleParkingSpot = mVehicleModel.parking_spot
        val vehicleImage = mVehicleModel.image

        if (vehicleDescription == ""){
            vehicleDescriptionField.visibility = View.GONE
        } else {
            vehicleDescriptionField.visibility = View.VISIBLE
        }

        vehicleNameField.text = vehicleName
        vehicleDescriptionField.text = vehicleDescription
        vehicleParkingSpotField.text = vehicleParkingSpot

        if (vehicleDescription == "") {
            vehicleDescriptionField.visibility = View.GONE
        } else {
            vehicleDescriptionField.visibility = View.VISIBLE
        }

        vehicleCallnameField.removeAllViews()
        if(vehicleCallname != ""){
            vehicleCallnameField.visibility = View.VISIBLE
            val chip = Chip(context)
            chip.text = vehicleCallname
            chip.isClickable = false
            vehicleCallnameField.addView(chip)
        } else {
            vehicleCallnameField.visibility = View.GONE
        }

        if (vehicleImage == "") {
            Glide.with(this).load(R.drawable.placeholder_with_bg_80).into(vehicleImageField)
        } else {
            vehicleImageField.scaleType=ImageView.ScaleType.CENTER_CROP
            vehicleImageField.setImageBitmap(Utils.stringToBitMap(vehicleImage))
        }

        vehicleImageField.setOnClickListener {
            val drawables: ArrayList<Drawable> = ArrayList()
            drawables.add(vehicleImageField.drawable)

            StfalconImageViewer.Builder(
                context, drawables
            ) { imageView, image -> Glide.with(it.context).load(image).into(imageView) }
                .withTransitionFrom(vehicleImageField)
                .show(true)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View? {

        val v =  inflater.inflate(R.layout.fragment_vehicle_details, container, false)

        // Get the activity and widget
        vehicleNameField = v.findViewById(R.id.vehicle_details_name)
        vehicleDescriptionField = v.findViewById(R.id.vehicle_details_description)
        vehicleImageField = v.findViewById(R.id.vehicle_details_image)
        vehicleCallnameField = v.findViewById(R.id.vehicle_details_callname)
        vehicleParkingSpotField = v.findViewById(R.id.vehicle_details_parking_spot)
        vehicleContainedBoxesEmptyLabel = v.findViewById(R.id.vehicle_details_content_empty_label)

        val vehicleContainer: ConstraintLayout = v.findViewById(R.id.vehicle_details_fragment_container)

        // Transition taget element
        vehicleContainer.transitionName = mVehicleModel.id.toString()

        updateContent()

        //Init Items View
        val recyclerview = v.findViewById<View>(R.id.vehicle_details_content) as RecyclerView
        mAdapter = BoxAdapter(mBoxList)
        mAdapter.setOnBoxClickListener(object: BoxAdapter.OnBoxClickListener{
            override fun onBoxClicked(box: BoxModel, view: View) {
                val extras = FragmentNavigatorExtras(
                    view to box.id
                )
                // second argument is the animation start view
                val navController: NavController = Navigation.findNavController(view)
                navController.navigate(VehicleDetailFragmentDirections.actionVehicleDetailFragmentToBoxFragment(box), extras)
            }
        })


        recyclerview.layoutManager = LinearLayoutManager(activity)
        recyclerview.adapter = mAdapter

        initFirebase()

        (activity as AppCompatActivity).supportActionBar?.title = resources.getString(R.string.vehicle_details_title)

        return v
    }


    private fun initFirebase(){
        mFirebaseListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot){
                val vehicles = dataSnapshot.child(Utils.getCurrentlySelectedOrg(context!!)).child("vehicles")
                for (vehicle: DataSnapshot in vehicles.children){
                    if (vehicle.child("id").value.toString() == mVehicleModel.id.toString()){
                        mVehicleModel = Utils.readVehicleModelFromDataSnapshot(context, vehicle)
                        updateContent()
                        break
                    }
                }

                Log.e("Error", "looking for boxes with id " + mVehicleModel.id.toString())
                mBoxList.clear()
                val boxes = dataSnapshot.child(Utils.getCurrentlySelectedOrg(context!!)).child("boxes")
                for (box: DataSnapshot in boxes.children){
                    if (box.child("in_vehicle").value.toString() == mVehicleModel.id.toString()) {
                        val boxModel = Utils.readBoxModelFromDataSnapshot(context, box)
                        mBoxList.add(boxModel)
                        Log.e("Error", "found box")
                    }
                }

                mBoxList.sortWith(
                    compareBy(String.CASE_INSENSITIVE_ORDER) { Utils.replaceUmlauteForSorting(it.name) }
                )

                mAdapter.setFilter(mBoxList)
                if (mBoxList.size == 0){
                    vehicleContainedBoxesEmptyLabel.visibility = View.VISIBLE
                } else {
                    vehicleContainedBoxesEmptyLabel.visibility = View.GONE
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        }
        FirebaseDatabase.getInstance().reference.addValueEventListener(mFirebaseListener)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }

    }


    override fun onDestroyView() {
        super.onDestroyView()
        FirebaseDatabase.getInstance().reference.removeEventListener(mFirebaseListener)
    }


}