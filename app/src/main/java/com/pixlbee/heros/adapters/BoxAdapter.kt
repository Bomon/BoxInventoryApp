package com.pixlbee.heros.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.pixlbee.heros.R
import com.pixlbee.heros.fragments.BoxesFragmentDirections
import com.pixlbee.heros.models.BoxModel
import com.pixlbee.heros.models.VehicleModel
import com.pixlbee.heros.utility.Utils


class BoxAdapter(var content: ArrayList<BoxModel>, private var compactView: Boolean = false, private var locationClickable: Boolean = false, private var tagClickable: Boolean = false) : RecyclerView.Adapter<BoxAdapter.BoxViewHolder>() {

    private lateinit var mContext: Context
    private var mBoxList: ArrayList<BoxModel> = ArrayList()

    private lateinit var mListener: OnBoxClickListener
    private lateinit var holder: BoxViewHolder


    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return mBoxList[position].numeric_id.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoxViewHolder {
        mContext = parent.context
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.card_box, parent, false)
        return BoxViewHolder(view, mListener, compactView)
    }


    override fun getItemCount(): Int {
        return mBoxList.size
    }


    override fun onBindViewHolder(holder: BoxViewHolder, position: Int) {
        this.holder = holder
        holder.boxId.text = mBoxList[position].id
        holder.boxName.text = mBoxList[position].name

        val vehiclesRef = FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(mContext)).child("vehicles")
        vehiclesRef.get().addOnCompleteListener { task ->
            var foundVehicle = false
            var mVehicle: VehicleModel? = null
            if (task.isSuccessful) {
                val vehicles: DataSnapshot? = task.result
                if (vehicles != null) {
                    for (vehicle: DataSnapshot in vehicles.children) {
                        val id = vehicle.child("id").value.toString()
                        val vehicleKey = vehicle.key.toString()
                        if (id == mBoxList[position].in_vehicle) {
                            holder.boxVehicle.text = vehicle.child("name").value.toString()
                            foundVehicle = true
                            mVehicle = Utils.readVehicleModelFromDataSnapshot(vehicle)
                            break
                        }
                    }
                }
            }
            if (!foundVehicle){
                holder.boxVehicle.text = mContext.resources.getString(R.string.error_no_vehicle_assigned)
            } else if (locationClickable){
                holder.boxVehicle.setOnClickListener {
                    val navController: NavController = Navigation.findNavController(it)
                    navController.navigate(BoxesFragmentDirections.actionNavigationBoxesToVehicleDetailFragment(
                        mVehicle!!
                    ))
                }
            }
        }



        holder.boxColor.background.setTint(mBoxList[position].color)

        if (mBoxList[position].image != ""){
            Glide.with(mContext)
                .load(Utils.stringToBitMap(mBoxList[position].image))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.placeholder_with_bg_80)
                .into(holder.boxImg)
        } else {
            Glide.with(mContext)
                .load(R.drawable.placeholder_with_bg_80)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.boxImg)
        }

        holder.boxContainer.transitionName = mBoxList[position].id

        holder.boxStatusTags.removeAllViews()
        if(mBoxList[position].status != ""){
            for (tag in mBoxList[position].status.split(";")){
                if (tag != ""){
                    val chip = Chip(mContext)
                    chip.text = tag
                    chip.minHeight = 1
                    chip.setTextAppearance(R.style.SmallTextChip)
                    holder.boxStatusTags.addView(chip)
                    if (tagClickable){
                        chip.setOnClickListener {
                            mListener.onBoxTagClicked(tag)
                        }
                    }
                }
            }
        } else {
            holder.boxStatusContainer.visibility = View.GONE
        }

        // show incomplete icon if box has items taken out
        var isIncomplete = false
        for (ci in mBoxList[position].content){
            if (ci.amount_taken.toInt() != 0){
                isIncomplete = true
                break
            }
        }
        if (isIncomplete){
            holder.boxIncompleteIcon.visibility = View.VISIBLE
        } else {
            holder.boxIncompleteIcon.visibility = View.GONE
        }

    }


    inner class BoxViewHolder(itemView: View, private var mListener: OnBoxClickListener, compactView: Boolean) : RecyclerView.ViewHolder(itemView), View.OnClickListener{

        var boxId: TextView = itemView.findViewById(R.id.box_id)
        var boxName: TextView = itemView.findViewById(R.id.box_name)
        var boxVehicle: TextView = itemView.findViewById(R.id.box_vehicle)
        var boxImg: ImageView = itemView.findViewById(R.id.box_img)
        var boxStatusTags: ChipGroup = itemView.findViewById(R.id.box_status)
        var boxStatusContainer: LinearLayout = itemView.findViewById(R.id.box_status_container)
        var boxContainer: MaterialCardView = itemView.findViewById(R.id.box_card_small)
        var boxColor: View = itemView.findViewById(R.id.box_color)
        var boxIncompleteIcon: ImageView = itemView.findViewById(R.id.box_incomplete_icon)

        init {
            itemView.setOnClickListener(this)

            if (compactView) {
                boxStatusTags.visibility = View.GONE
                boxStatusContainer.visibility = View.GONE
            }
        }

        override fun onClick(view: View?) {
            if(mListener != null){
                // second argument is the element from which the transition will start
                mListener.onBoxClicked(mBoxList[adapterPosition], boxContainer)
            }
            true
        }
    }


    fun setFilter(itemList: List<BoxModel>) {
        mBoxList.clear()
        mBoxList.addAll(itemList)
        this.notifyDataSetChanged()
    }


    interface OnBoxClickListener{
        fun onBoxClicked(box: BoxModel, view: View)
        fun onBoxTagClicked(tag: String)
    }


    fun setOnBoxClickListener(mListener: OnBoxClickListener) {
        this.mListener = mListener
    }


}