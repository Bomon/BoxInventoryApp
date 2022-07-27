package com.pixlbee.heros.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.pixlbee.heros.R
import com.pixlbee.heros.models.BoxModel
import com.pixlbee.heros.utility.Utils


class VehicleBoxesAdapter(
    mDataList: ArrayList<BoxModel>,
    vehicle_id: String,
) : RecyclerView.Adapter<VehicleBoxesAdapter.ContainingBoxViewHolder>() {

    lateinit var mContext: Context
    lateinit var mVehicleId: String
    private var mBoxList: ArrayList<BoxModel> = ArrayList()


    private lateinit var mListener: OnVehicleBoxClickListener


    init {
        setFilter(mDataList)
        this.mVehicleId = vehicle_id
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContainingBoxViewHolder {
        mContext = parent.context
        val layoutInflater = LayoutInflater.from(parent.context)
        return ContainingBoxViewHolder(layoutInflater.inflate(R.layout.card_box, parent, false))
    }


    override fun getItemCount(): Int {
        return mBoxList.size
    }


    override fun onBindViewHolder(holder: ContainingBoxViewHolder, position: Int) {
        holder.bind(mBoxList[position])
    }


    inner class ContainingBoxViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener{

        private var boxId: TextView = itemView.findViewById(R.id.box_inv_id)
        private var boxName: TextView = itemView.findViewById(R.id.box_inv_name)
        private var boxVehicle: TextView = itemView.findViewById(R.id.box_inv_vehicle)
        private var boxImage: ImageView = itemView.findViewById(R.id.box_inv_img)
        private var boxInvLabel: TextView = itemView.findViewById(R.id.box_inv_label)
        private var boxInvnums: ChipGroup = itemView.findViewById(R.id.box_inv_invnums)
        private var boxStatus: ChipGroup = itemView.findViewById(R.id.box_inv_status)
        private var boxColor: View = itemView.findViewById(R.id.box_inv_color)
        private var boxContainer: MaterialCardView = itemView.findViewById(R.id.box_inv_card_small)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(model: BoxModel) {
            boxId.text = model.id
            boxName.text = model.name
            boxVehicle.visibility = View.GONE
            boxColor.background.setTint(model.color)

            val img = Utils.stringToBitMap(model.image)
            if (img != null){
                boxImage.setImageBitmap(img)
            }

            boxStatus.removeAllViews()
            if(model.status != ""){
                for (tag in model.status.split(";")){
                    if (tag != ""){
                        val chip = Chip(mContext)
                        chip.text = tag
                        chip.minHeight = 1
                        chip.setTextAppearance(R.style.BoxStatusChip)
                        boxStatus.addView(chip)
                    }
                }
            }

            boxContainer.transitionName = model.id
        }

        override fun onClick(view: View?) {
            if(mListener != null){
                // second argument is the element from which the transition will start
                mListener.onVehicleBoxClicked(mBoxList[adapterPosition], boxContainer)
            }
            true
        }
    }


    fun setFilter(boxList: List<BoxModel>) {
        mBoxList.clear()
        mBoxList.addAll(boxList)
        this.notifyDataSetChanged()
    }


    interface OnVehicleBoxClickListener{
        fun onVehicleBoxClicked(box: BoxModel, view: View)
    }


    fun setOnBoxClickListener(mListener: OnVehicleBoxClickListener) {
        this.mListener = mListener
    }


}