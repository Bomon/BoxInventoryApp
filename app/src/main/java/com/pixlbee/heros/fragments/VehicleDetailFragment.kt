package com.pixlbee.heros.fragments

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
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
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialFadeThrough
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
    private lateinit var mAdapter: BoxAdapter
    private lateinit var mFirebaseListener: ValueEventListener
    var mBoxList: ArrayList<BoxModel> = ArrayList()

    private lateinit var vehicleNameField: TextView
    private lateinit var vehicleCallnameField: TextView
    private lateinit var vehicleCallnameContainer: LinearLayout
    private lateinit var vehicleParkingSpotField: TextView
    private lateinit var vehicleDescriptionField: TextView
    private lateinit var vehicleImageField: ImageView
    private lateinit var vehicleContainedBoxesEmptyLabel: TextView
    private lateinit var vehicleParkingSpotContainer: LinearLayout
    private lateinit var vehicleDetailsContainer: LinearLayout
    private lateinit var vehicleDescriptionDivider: LinearLayout
    private lateinit var vehicleDetailsDivider: LinearLayout

    private lateinit var animationType: String


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
                        if (vehicleId == mVehicleModel.id) {
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
                        if (id == mVehicleModel.id) {
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

        } else if (animationType == "simple"){
            enterTransition = MaterialFadeThrough()
            returnTransition = MaterialFadeThrough()
            exitTransition = MaterialFadeThrough()
        }

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
            vehicleDescriptionDivider.visibility = View.GONE
            vehicleDescriptionField.visibility = View.GONE
        } else {
            vehicleDescriptionDivider.visibility = View.VISIBLE
            vehicleDescriptionField.visibility = View.VISIBLE
        }

        vehicleNameField.text = vehicleName
        vehicleDescriptionField.text = vehicleDescription
        vehicleParkingSpotField.text = vehicleParkingSpot

        if (vehicleParkingSpot == "") {
            vehicleParkingSpotContainer.visibility = View.GONE
        } else {
            vehicleParkingSpotContainer.visibility = View.VISIBLE
        }

        if(vehicleCallname != ""){
            vehicleCallnameContainer.visibility = View.VISIBLE
            vehicleCallnameField.text = vehicleCallname
        } else {
            vehicleCallnameContainer.visibility = View.GONE
        }

        if (vehicleCallname == "" && vehicleParkingSpot == "") {
            vehicleDetailsDivider.visibility = View.GONE
            vehicleDetailsContainer.visibility = View.GONE
        }
        else {
            vehicleDetailsDivider.visibility = View.VISIBLE
            vehicleDetailsContainer.visibility = View.VISIBLE
        }

        if (vehicleImage == "") {
            Glide.with(this).load(R.drawable.placeholder_with_bg_80).into(vehicleImageField)
        } else {
            vehicleImageField.scaleType=ImageView.ScaleType.CENTER_CROP
            Glide.with(this).load(Utils.stringToBitMap(vehicleImage)).into(vehicleImageField)
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
        vehicleParkingSpotContainer = v.findViewById(R.id.vehicle_details_parking_spot_container)
        vehicleDescriptionDivider = v.findViewById(R.id.vehicle_description_divider)
        vehicleDetailsContainer = v.findViewById(R.id.vehicle_details_container)
        vehicleDetailsDivider = v.findViewById(R.id.vehicle_details_divider)
        vehicleCallnameContainer = v.findViewById(R.id.vehicle_details_callname_container)

        val vehicleContainer: ConstraintLayout = v.findViewById(R.id.vehicle_details_fragment_container)

        // Transition target element
        vehicleContainer.transitionName = mVehicleModel.id

        //Init Items View
        val recyclerview = v.findViewById<View>(R.id.vehicle_details_content) as RecyclerView
        mAdapter = BoxAdapter(mBoxList)
        mAdapter.setOnBoxClickListener(object: BoxAdapter.OnBoxClickListener{
            override fun onBoxClicked(box: BoxModel, view: View) {
                if (animationType == "elegant") {
                    exitTransition = Hold()
                }
                val extras = FragmentNavigatorExtras(
                    view to box.id
                )
                // second argument is the animation start view
                val navController: NavController = Navigation.findNavController(view)
                navController.navigate(VehicleDetailFragmentDirections.actionVehicleDetailFragmentToBoxFragment(box), extras)
            }

            override fun onBoxTagClicked(tag: String) {
                //do nothing
            }
        })


        recyclerview.layoutManager = LinearLayoutManager(activity)
        recyclerview.adapter = mAdapter

        (activity as AppCompatActivity).supportActionBar?.title = resources.getString(R.string.vehicle_details_title)

        return v
    }


    private fun initFirebase(){
        mFirebaseListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot){
                val vehicles = dataSnapshot.child(Utils.getCurrentlySelectedOrg(context!!)).child("vehicles")
                for (vehicle: DataSnapshot in vehicles.children){
                    if (vehicle.child("id").value.toString() == mVehicleModel.id){
                        mVehicleModel = Utils.readVehicleModelFromDataSnapshot(vehicle)
                        updateContent()
                        break
                    }
                }

                mBoxList.clear()
                val boxes = dataSnapshot.child(Utils.getCurrentlySelectedOrg(context!!)).child("boxes")
                for (box: DataSnapshot in boxes.children){
                    if (box.child("in_vehicle").value.toString() == mVehicleModel.id) {
                        val boxModel = Utils.readBoxModelFromDataSnapshot(context, box)
                        mBoxList.add(boxModel)
                    }
                }

                mBoxList.sortWith(
                    compareBy(String.CASE_INSENSITIVE_ORDER) { Utils.replaceUmlauteForSorting(it.id) }
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

        initFirebase()

        if (animationType == "elegant") {
            postponeEnterTransition()
            view.doOnPreDraw { startPostponedEnterTransition() }
        }

    }


    override fun onDestroyView() {
        super.onDestroyView()
        FirebaseDatabase.getInstance().reference.removeEventListener(mFirebaseListener)
    }


}