package com.pixlbee.heros.adapters

import android.content.Context
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.elevation.SurfaceColors
import com.pixlbee.heros.R
import com.pixlbee.heros.models.ItemModel


class ItemSelectionAdapter : RecyclerView.Adapter<ItemSelectionAdapter.ItemSelectionViewHolder>() {

    lateinit var context: Context
    private var mItemList: ArrayList<ItemModel> = ArrayList()

    private lateinit var mListener: OnItemClickListener
    private lateinit var holder: ItemSelectionViewHolder

    fun toggleSelection(position: Int) {
        mItemList[position].isSelected = !mItemList[position].isSelected
    }

    fun getSelectedItems(): ArrayList<String> {
        var selectedItems: ArrayList<String> = ArrayList()
        for (item in mItemList) {
            if (item.isSelected) {
                selectedItems.add(item.id)
            }
        }
        return selectedItems
    }


    init {
        setHasStableIds(true)
    }


    override fun getItemId(position: Int): Long {
        return mItemList[position].id.hashCode().toLong()
    }


    override fun getItemViewType(position: Int): Int {
        return position
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemSelectionViewHolder {
        context = parent.context
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.card_item_selection, parent, false)
        return ItemSelectionViewHolder(view, mListener)
    }


    override fun getItemCount(): Int {
        return mItemList.size
    }


    override fun onBindViewHolder(holder: ItemSelectionViewHolder, position: Int) {
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
                    chip.setOnClickListener {
                        mListener.onItemTagClicked(tag)
                    }
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


    inner class ItemSelectionViewHolder(itemView: View, private var mListener: OnItemClickListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener, View.OnLongClickListener{

        var itemName: TextView = itemView.findViewById(R.id.card_item_selection_name)
        var itemImage: ImageView = itemView.findViewById(R.id.card_item_selection_img)
        val itemTags: ChipGroup = itemView.findViewById(R.id.card_item_selection_tags)
        val itemSelectionMarker: ImageView = itemView.findViewById(R.id.card_item_selection_check)
        val itemTagsContainer: LinearLayout = itemView.findViewById(R.id.card_item_selection_tags_container)
        val itemContainer: MaterialCardView = itemView.findViewById(R.id.card_item_selection_small)

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        override fun onLongClick(view: View?): Boolean {
            mListener.onItemClicked(mItemList[absoluteAdapterPosition], itemContainer, absoluteAdapterPosition, true)
            itemSelectionMarker.isVisible = mItemList[absoluteAdapterPosition].isSelected
            if (mItemList[absoluteAdapterPosition].isSelected) itemContainer.setBackgroundColor(SurfaceColors.SURFACE_5.getColor(context)) else itemContainer.setBackgroundColor(SurfaceColors.SURFACE_1.getColor(context))
            return true
        }

        override fun onClick(view: View?) {
            // second argument is the element from which the transition will start
            mListener.onItemClicked(mItemList[absoluteAdapterPosition], itemContainer, absoluteAdapterPosition, false)
            itemSelectionMarker.isVisible = mItemList[absoluteAdapterPosition].isSelected
            if (mItemList[absoluteAdapterPosition].isSelected) itemContainer.setBackgroundColor(SurfaceColors.SURFACE_5.getColor(context)) else itemContainer.setBackgroundColor(SurfaceColors.SURFACE_1.getColor(context))
            true
        }
    }


    fun setFilter(itemList: List<ItemModel>) {
        mItemList.clear()
        mItemList.addAll(itemList)
        this.notifyDataSetChanged()
    }


    interface OnItemClickListener{
        fun onItemClicked(item: ItemModel, view: View, position: Int, isLongClick: Boolean)
        fun onItemTagClicked(tag: String)
    }


    fun setOnItemClickListener(mListener: OnItemClickListener) {
        this.mListener = mListener
    }


}