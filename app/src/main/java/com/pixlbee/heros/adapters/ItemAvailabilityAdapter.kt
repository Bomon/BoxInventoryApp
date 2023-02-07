package com.pixlbee.heros.adapters

import android.content.Context
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.imageview.ShapeableImageView
import com.pixlbee.heros.R
import com.pixlbee.heros.models.ItemAvailabilityModel


class ItemAvailabilityAdapter : RecyclerView.Adapter<ItemAvailabilityAdapter.ItemViewHolder>() {

    lateinit var context: Context
    private var mItemList: ArrayList<ItemAvailabilityModel> = ArrayList()

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
        val view = layoutInflater.inflate(R.layout.card_item_availability, parent, false)
        return ItemViewHolder(view, mListener)
    }


    override fun getItemCount(): Int {
        return mItemList.size
    }


    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        this.holder = holder
        holder.itemName.text = mItemList[position].item.name

        holder.itemTags.removeAllViews()
        if(mItemList[position].item.tags != ""){
            holder.itemTags.visibility = View.VISIBLE
            holder.itemTagIcon.visibility = View.VISIBLE
            for (tag in mItemList[position].item.tags.split(";")){
                if (tag != ""){
                    val chip = Chip(context)
                    chip.text = tag
                    holder.itemTags.addView(chip)
                    chip.setTextAppearance(R.style.SmallTextChip)
                    chip.setOnClickListener {
                        mListener.onItemTagClicked(tag)
                    }
                }
            }
        } else {
            holder.itemTagIcon.visibility = View.GONE
            holder.itemTags.visibility = View.GONE
        }

        //holder.item_description.text = mItemList[position].description
        if (mItemList[position].item.image != ""){
            val imageByteArray = Base64.decode(mItemList[position].item.image, Base64.DEFAULT)
            Glide.with(context)
                .load(imageByteArray)
                .into(holder.itemImage)
        }

        holder.itemContainer.transitionName = mItemList[position].item.id

        // remove rows
        while (holder.itemAvailabilityTable.childCount > 1) {
            holder.itemAvailabilityTable.removeViewAt(1)
        }

        // Add rows
        mItemList[position].taken.forEach { (key, value) ->
            Log.e("Error", "Add row " + key.toString() + " with value " + value);
            val row = TableRow(context)

            val textViewBox = TextView(context)
            textViewBox.text = key
            row.addView(textViewBox)


            val textViewAmount = TextView(context)
            textViewAmount.text = value.toString()
            row.addView(textViewAmount)

            holder.itemAvailabilityTable.addView(row)
        }
    }


    inner class ItemViewHolder(itemView: View, private var mListener: OnItemClickListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener{

        var itemName: TextView = itemView.findViewById(R.id.card_item_name)
        var itemImage: ImageView = itemView.findViewById(R.id.card_item_img)
        val itemTags: ChipGroup = itemView.findViewById(R.id.card_item_tags)
        val itemTagsContainer: LinearLayout = itemView.findViewById(R.id.card_item_tags_container)
        val itemContainer: MaterialCardView = itemView.findViewById(R.id.card_item_small)
        val itemAvailabilityTable: TableLayout = itemView.findViewById(R.id.item_availability_table)
        val itemTagIcon: ShapeableImageView = itemView.findViewById(R.id.card_item_tags_icon)

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


    fun setFilter(itemList: List<ItemAvailabilityModel>) {
        mItemList.clear()
        mItemList.addAll(itemList)
        this.notifyDataSetChanged()
    }


    interface OnItemClickListener{
        fun onItemClicked(item: ItemAvailabilityModel, view: View)
        fun onItemTagClicked(tag: String)
    }


    fun setOnItemClickListener(mListener: OnItemClickListener) {
        this.mListener = mListener
    }


}