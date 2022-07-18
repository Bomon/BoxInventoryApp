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


class ContainingBoxAdapter(
    mDataList: ArrayList<BoxModel>,
    item_id: String,
) : RecyclerView.Adapter<ContainingBoxAdapter.ContainingBoxViewHolder>() {

    lateinit var context: Context
    lateinit var item_id: String
    private var mBoxList: ArrayList<BoxModel> = ArrayList()


    private lateinit var mListener: OnContainingBoxClickListener


    init {
        setFilter(mDataList)
        this.item_id = item_id
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContainingBoxViewHolder {
        context = parent.context
        val layoutInflater = LayoutInflater.from(parent.context)
        return ContainingBoxViewHolder(layoutInflater.inflate(R.layout.card_box_with_inv, parent, false))
    }


    override fun getItemCount(): Int {
        return mBoxList.size
    }


    override fun onBindViewHolder(holder: ContainingBoxViewHolder, position: Int) {
        holder.bind(mBoxList[position])
    }


    inner class ContainingBoxViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener{

        private var box_id: TextView = itemView.findViewById(R.id.box_inv_id)
        private var box_name: TextView = itemView.findViewById(R.id.box_inv_name)
        private var box_location: TextView = itemView.findViewById(R.id.box_inv_location)
        private var box_image: ImageView = itemView.findViewById(R.id.box_inv_img)
        private var box_inv_label: TextView = itemView.findViewById(R.id.box_inv_label)
        private var box_invnums: ChipGroup = itemView.findViewById(R.id.box_inv_invnums)
        private var box_status: ChipGroup = itemView.findViewById(R.id.box_inv_status)
        private var box_color: View = itemView.findViewById(R.id.box_inv_color)
        private var box_container: MaterialCardView = itemView.findViewById(R.id.box_inv_card_small)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(model: BoxModel): Unit {
            box_id.text = model.id
            box_name.text = model.name
            box_location.text = model.location
            box_color.background.setTint(model.color)

            val img = Utils.StringToBitMap(model.image)
            if (img != null){
                box_image.setImageBitmap(img)
            }

            box_status.removeAllViews()
            if(model.status != ""){
                for (tag in model.status.split(";")){
                    if (tag != ""){
                        val chip = Chip(context)
                        chip.text = tag
                        chip.minHeight = 1
                        chip.setTextAppearance(R.style.BoxStatusChip)
                        box_status.addView(chip)
                    }
                }
            }

            if (box_inv_label != null && box_invnums != null) {
                val invnums = ArrayList<String>()
                for (ci in model.content){
                    if (ci.id == item_id){
                        if (ci.invnum != ""){
                            invnums.add(ci.invnum)
                        }
                    }
                }

                if (invnums.size > 0){
                    box_inv_label.visibility = View.VISIBLE
                    box_invnums.visibility = View.VISIBLE
                    box_invnums.removeAllViews()
                    for (invnum in invnums){
                        if (invnum != ""){
                            val chip = Chip(context)
                            chip.setTextAppearance(android.R.style.TextAppearance_Material_Small)
                            chip.text = invnum
                            box_invnums.addView(chip)
                        }
                    }
                } else {
                    box_inv_label.visibility = View.GONE
                    box_invnums.visibility = View.GONE
                }
            }
            box_container.transitionName = model.id
        }

        override fun onClick(view: View?) {
            if(mListener != null){
                // second argument is the element from which the transition will start
                mListener.onContainingBoxClicked(mBoxList[adapterPosition], box_container)
            }
            true
        }
    }


    fun setFilter(itemList: List<BoxModel>) {
        mBoxList.clear()
        mBoxList.addAll(itemList)
        this.notifyDataSetChanged()
    }


    interface OnContainingBoxClickListener{
        fun onContainingBoxClicked(box: BoxModel, view: View)
    }


    fun setOnBoxClickListener(mListener: OnContainingBoxClickListener) {
        this.mListener = mListener
    }


}