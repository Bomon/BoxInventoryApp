package com.thw.inventory_app

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.get
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class BoxAdapter(var content: ArrayList<BoxModel>) : RecyclerView.Adapter<BoxAdapter.BoxViewHolder>() {

    lateinit var context: Context
    private var mBoxList: ArrayList<BoxModel> = ArrayList()

    private lateinit var mListener: OnBoxClickListener
    lateinit var holder: BoxViewHolder

    init {
        setFilter(content)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoxViewHolder {
        context = parent.context
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.card_box, parent, false)
        return BoxViewHolder(view, mListener)
    }

    override fun getItemCount(): Int {
        return mBoxList.size
    }

    override fun onBindViewHolder(holder: BoxViewHolder, position: Int) {
        this.holder = holder
        holder.box_id.text = mBoxList[position].id
        holder.box_name.text = mBoxList[position].name
        holder.box_location.text = mBoxList[position].location
        holder.box_color.background.setTint(mBoxList[position].color)
        val img = Utils.StringToBitMap(mBoxList[position].image)
        if (img != null){
            holder.box_img.setImageBitmap(img)
        }

        holder.box_status_tags.removeAllViews()

        if(mBoxList[position].status != ""){
            for (tag in mBoxList[position].status.split(";")){
                if (tag != ""){
                    val chip = Chip(context)
                    chip.text = tag
                    chip.minHeight = 1
                    chip.setTextAppearance(R.style.BoxStatusChip)
                    holder.box_status_tags.addView(chip)
                }
            }
        }

        holder.box_container.transitionName = "boxTransition" + position
        Utils.setRecyclerViewCardAnimation(holder.itemView, context)
    }

    inner class BoxViewHolder(itemView: View, var mListener: OnBoxClickListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener{

        var box_id: TextView = itemView.findViewById<TextView>(R.id.box_id)
        var box_name: TextView = itemView.findViewById<TextView>(R.id.box_name)
        var box_location: TextView = itemView.findViewById<TextView>(R.id.box_location)
        var box_img: ImageView = itemView.findViewById<ImageView>(R.id.box_img)
        var box_status_tags: ChipGroup = itemView.findViewById<ChipGroup>(R.id.box_status)
        var box_container: MaterialCardView = itemView.findViewById<MaterialCardView>(R.id.box_card_small)
        var box_color: View = itemView.findViewById<View>(R.id.box_color)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View?) {
            if(mListener != null){
                mListener.setOnCLickListener(adapterPosition, box_container)
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
        fun setOnCLickListener(position: Int, view: View)
    }

    fun setOnBoxClickListener(mListener: OnBoxClickListener) {
        this.mListener = mListener
    }


}