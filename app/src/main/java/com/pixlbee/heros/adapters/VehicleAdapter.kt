package com.pixlbee.heros.adapters

import android.content.Context
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.pixlbee.heros.R
import com.pixlbee.heros.models.VehicleModel


class VehicleAdapter(var content: ArrayList<VehicleModel>, private var compactView: Boolean = false) : RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder>() {

    private lateinit var mContext: Context
    private var mVehicleList: ArrayList<VehicleModel> = ArrayList()

    private lateinit var mListener: OnVehicleClickListener
    private lateinit var holder: VehicleViewHolder


    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return mVehicleList[position].id.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleViewHolder {
        mContext = parent.context
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.card_vehicle_compact, parent, false)
        return VehicleViewHolder(view, mListener, compactView)
    }


    override fun getItemCount(): Int {
        return mVehicleList.size
    }


    override fun onBindViewHolder(holder: VehicleViewHolder, position: Int) {
        this.holder = holder
        holder.vehicleName.text = mVehicleList[position].name
        holder.vehicleCallname.text = mVehicleList[position].callname
        holder.vehicleParkingSpot.text = mVehicleList[position].parking_spot

        if (mVehicleList[position].callname == "") {
            holder.vehicleCallname.visibility = View.GONE
        } else {
            holder.vehicleCallname.visibility = View.VISIBLE
        }

        if (mVehicleList[position].parking_spot == "") {
            holder.vehicleParkingSpotContainer.visibility = View.GONE
        } else {
            holder.vehicleParkingSpotContainer.visibility = View.VISIBLE
        }

        if (mVehicleList[position].image != ""){
            val imageByteArray = Base64.decode(mVehicleList[position].image, Base64.DEFAULT)
            Glide.with(mContext)
                .load(imageByteArray)
                .into(holder.vehicleImg)
        }

        holder.vehicleContainer.transitionName = mVehicleList[position].id
    }


    inner class VehicleViewHolder(itemView: View, private var mListener: OnVehicleClickListener, compactView: Boolean) : RecyclerView.ViewHolder(itemView), View.OnClickListener{

        var vehicleName: TextView = itemView.findViewById(R.id.vehicle_name)
        var vehicleCallname: TextView = itemView.findViewById(R.id.vehicle_callname)
        var vehicleParkingSpot: TextView = itemView.findViewById(R.id.vehicle_parking_spot)
        var vehicleImg: ImageView = itemView.findViewById(R.id.vehicle_img)
        var vehicleContainer: MaterialCardView = itemView.findViewById(R.id.vehicle_card_small)
        var vehicleParkingSpotContainer: LinearLayout = itemView.findViewById(R.id.vehicle_parking_spot_container)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View?) {
            if(mListener != null){
                // second argument is the element from which the transition will start
                mListener.onVehicleClicked(mVehicleList[adapterPosition], vehicleContainer)
            }
            true
        }
    }


    fun setFilter(vehicleList: List<VehicleModel>) {
        mVehicleList.clear()
        mVehicleList.addAll(vehicleList)
        this.notifyDataSetChanged()
    }


    interface OnVehicleClickListener{
        fun onVehicleClicked(vehicle: VehicleModel, view: View)
    }


    fun setOnVehicleClickListener(mListener: OnVehicleClickListener) {
        this.mListener = mListener
    }

}