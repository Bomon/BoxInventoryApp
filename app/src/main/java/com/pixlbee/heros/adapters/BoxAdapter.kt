package com.pixlbee.heros.adapters

import android.content.Context
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.pixlbee.heros.R
import com.pixlbee.heros.models.BoxModel
import com.pixlbee.heros.utility.Utils


class BoxAdapter(var content: ArrayList<BoxModel>, private var compactView: Boolean = false) : RecyclerView.Adapter<BoxAdapter.BoxViewHolder>() {

    private lateinit var mContext: Context
    private var mBoxList: ArrayList<BoxModel> = ArrayList()

    private lateinit var mListener: OnBoxClickListener
    private lateinit var holder: BoxViewHolder


    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return mBoxList[position].numeric_id
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
            if (task.isSuccessful) {
                val vehicles: DataSnapshot? = task.result
                if (vehicles != null) {
                    for (vehicle: DataSnapshot in vehicles.children) {
                        val id = vehicle.child("id").value.toString()
                        val vehicleKey = vehicle.key.toString()
                        if (id == mBoxList[position].vehicle) {
                            holder.boxVehicle.text = vehicle.child("name").value.toString()
                            foundVehicle = true
                            break
                        }
                    }
                }
            }
            if (!foundVehicle){
                holder.boxVehicle.text = mContext.resources.getString(R.string.error_no_vehicle_assigned)
            }
        }


        holder.boxColor.background.setTint(mBoxList[position].color)

        if (mBoxList[position].image != ""){
            val imageByteArray = Base64.decode(mBoxList[position].image, Base64.DEFAULT)
            Glide.with(mContext)
                .load(imageByteArray)
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
                    chip.setTextAppearance(R.style.BoxStatusChip)
                    holder.boxStatusTags.addView(chip)
                }
            }
        }
    }


    inner class BoxViewHolder(itemView: View, private var mListener: OnBoxClickListener, compactView: Boolean) : RecyclerView.ViewHolder(itemView), View.OnClickListener{

        var boxId: TextView = itemView.findViewById(R.id.box_id)
        var boxName: TextView = itemView.findViewById(R.id.box_name)
        var boxVehicle: TextView = itemView.findViewById(R.id.box_vehicle)
        var boxImg: ImageView = itemView.findViewById(R.id.box_img)
        var boxStatusTags: ChipGroup = itemView.findViewById(R.id.box_status)
        var boxContainer: MaterialCardView = itemView.findViewById(R.id.box_card_small)
        var boxColor: View = itemView.findViewById(R.id.box_color)

        init {
            itemView.setOnClickListener(this)

            if (compactView) {
                boxStatusTags.visibility = View.GONE
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
    }


    fun setOnBoxClickListener(mListener: OnBoxClickListener) {
        this.mListener = mListener
    }

    fun updateColorInFirebase(position: Int) {
        notifyItemChanged(position)
        val boxesRef = FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(mContext)).child("boxes")
        boxesRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val boxes: DataSnapshot? = task.result
                if (boxes != null) {
                    for (box: DataSnapshot in boxes.children) {
                        val id = box.child("id").value.toString()
                        if (id == holder.boxId.text.toString()) {
                            val boxKey: String = box.key.toString()
                            FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(mContext)).child("boxes").child(boxKey).child("color").setValue(mBoxList[position].color)
                        }
                    }
                }
            }
        }
    }


}