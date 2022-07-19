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
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.pixlbee.heros.R
import com.pixlbee.heros.models.ItemModel


class ItemAdapter : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    lateinit var context: Context
    private var mItemList: ArrayList<ItemModel> = ArrayList()

    private lateinit var mListener: OnItemClickListener
    private lateinit var holder: ItemViewHolder


    init {
        setHasStableIds(true)
    }


    override fun getItemId(position: Int): Long {
        return position.toLong()
    }


    override fun getItemViewType(position: Int): Int {
        return position
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
        holder.itemName.text = mItemList[position].name

        holder.itemTags.removeAllViews()
        if(mItemList[position].tags != ""){
            holder.itemTagsContainer.visibility = View.VISIBLE
            for (tag in mItemList[position].tags.split(";")){
                if (tag != ""){
                    val chip = Chip(context)
                    chip.text = tag
                    holder.itemTags.addView(chip)
                }
            }
        } else {
            holder.itemTagsContainer.visibility = View.GONE
        }

        //holder.item_description.text = mItemList[position].description
        if (mItemList[position].image != ""){
            val imageByteArray = Base64.decode(mItemList[position].image, Base64.DEFAULT)
            Glide.with(context)
                .load(imageByteArray)
                .into(holder.itemImage)
        }

        holder.itemContainer.transitionName = mItemList[position].id
    }


    inner class ItemViewHolder(itemView: View, private var mListener: OnItemClickListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener{

        var itemName: TextView = itemView.findViewById(R.id.card_item_name)
        var itemImage: ImageView = itemView.findViewById(R.id.card_item_img)
        val itemTags: ChipGroup = itemView.findViewById(R.id.card_item_tags)
        val itemTagsContainer: LinearLayout = itemView.findViewById(R.id.card_item_tags_container)
        val itemContainer: MaterialCardView = itemView.findViewById(R.id.card_item_small)

        init {
            itemView.setOnClickListener(this)
        }



        override fun onClick(view: View?) {
            if(mListener != null){
                // second argument is the element from which the transition will start
                mListener.onItemClicked(mItemList[adapterPosition], itemContainer)
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
        fun onItemClicked(item: ItemModel, view: View)
    }


    fun setOnItemClickListener(mListener: OnItemClickListener) {
        this.mListener = mListener
    }


}