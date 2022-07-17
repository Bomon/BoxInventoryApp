package com.pixlbee.heros.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.pixlbee.heros.R
import com.pixlbee.heros.utility.Utils
import com.pixlbee.heros.models.ItemModel


class ItemAdapter(private val content: ArrayList<ItemModel>) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    lateinit var context: Context
    private var mItemList: ArrayList<ItemModel> = ArrayList()

    private lateinit var mListener: OnItemClickListener
    lateinit var holder: ItemViewHolder


    init {
        setFilter(content)
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
        holder.item_name.text = mItemList[holder.adapterPosition].name
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
    }


    inner class ItemViewHolder(itemView: View, var mListener: OnItemClickListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener{

        var item_name: TextView = itemView.findViewById<TextView>(R.id.card_item_name)
        //var item_description: TextView = itemView.findViewById<TextView>(R.id.item_description)
        var item_image: ImageView = itemView.findViewById<ImageView>(R.id.card_item_img)
        val item_tags: ChipGroup = itemView.findViewById<ChipGroup>(R.id.card_item_tags)
        val item_tags_container: LinearLayout = itemView.findViewById<LinearLayout>(R.id.card_item_tags_container)
        val item_container: MaterialCardView = itemView.findViewById(R.id.card_item_small)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View?) {
            if(mListener != null){
                // second argument is the element from which the transition will start
                mListener.onItemClicked(mItemList[adapterPosition], item_name)
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