package com.thw.inventory_app

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.view.menu.MenuView
import androidx.core.view.marginBottom
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.thw.inventory_app.ui.item.ItemFragment


class ItemAdapter(private val content: ArrayList<ItemModel>) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    lateinit var context: Context
    private var mItemList: ArrayList<ItemModel> = ArrayList()

    private lateinit var mListener: ItemAdapter.OnItemClickListener
    lateinit var holder: ItemAdapter.ItemViewHolder

    init {
        setFilter(content)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        context = parent.context
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.card_item, parent, false)
        return ItemViewHolder(view, mListener)

    }

    override fun getItemCount(): Int {
        return mItemList.size
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        this.holder = holder
        holder.item_name.text = mItemList[position].name
        //holder.item_tags.description = mItemList[position].description

        holder.item_tags.removeAllViews()
        if(mItemList[position].tags != ""){
            holder.item_tags_container.visibility = View.VISIBLE
            for (tag in mItemList[position].tags.split(";")){
                if (tag != ""){
                    val chip = Chip(context)
                    chip.text = tag
                    holder.item_tags.addView(chip)
                }
            }
        } else {
            holder.item_tags_container.visibility = View.GONE
        }

        //holder.item_description.text = mItemList[position].description
        if (mItemList[position].image != ""){
            val img = Utils.StringToBitMap(mItemList[position].image)
            if (img != null){
                holder.item_image.setImageBitmap(img)
            }
        }
        holder.item_image.transitionName = "itemTransition" + position
        Utils.setRecyclerViewCardAnimation(holder.itemView, context)
    }

    inner class ItemViewHolder(itemView: View, var mListener: ItemAdapter.OnItemClickListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener{

        var item_name: TextView = itemView.findViewById<TextView>(R.id.card_item_name)
        //var item_description: TextView = itemView.findViewById<TextView>(R.id.item_description)
        var item_image: ImageView = itemView.findViewById<ImageView>(R.id.card_item_img)
        val item_tags: ChipGroup = itemView.findViewById<ChipGroup>(R.id.card_item_tags)
        val item_tags_container: LinearLayout = itemView.findViewById<LinearLayout>(R.id.card_item_tags_container)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View?) {
            if(mListener != null){
                mListener.setOnCLickListener(adapterPosition, item_image)
            }
            true
        }
    }

    fun setFilter(itemList: List<ItemModel>) {
        mItemList.clear()
        mItemList.addAll(itemList)
        this.notifyDataSetChanged()
    }


    interface OnItemClickListener{
        fun setOnCLickListener(position: Int, view: View)
    }

    fun setOnItemClickListener(mListener: OnItemClickListener) {
        this.mListener = mListener
    }


}