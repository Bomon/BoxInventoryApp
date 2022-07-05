package com.thw.inventory_app.ui.box

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.thw.inventory_app.ItemModel
import com.thw.inventory_app.R
import com.thw.inventory_app.Utils
import com.thw.inventory_app.ui.item.ItemFragment


class BoxItemAdapter(private val mDataList: ArrayList<BoxItemModel>, private val do_animate: Boolean) : RecyclerView.Adapter<BoxItemAdapter.BoxItemViewHolder>() {

    lateinit var context: Context
    private var mItemList: ArrayList<BoxItemModel> = ArrayList()

    init {
        setFilter(mDataList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoxItemViewHolder {
        context = parent.context
        val layoutInflater = LayoutInflater.from(parent.context)
        return BoxItemViewHolder(layoutInflater.inflate(R.layout.card_item_in_box, parent, false))

    }

    override fun getItemCount(): Int {
        return mItemList.size
    }

    override fun onBindViewHolder(holder: BoxItemViewHolder, position: Int) {
        holder.item_amount.text = mItemList[position].item_amount
        holder.item_name.text = mItemList[position].item_name

        holder.item_invnums.removeAllViews()
        for (tag in mItemList[position].item_invnum.split(";")){
            if (tag != ""){
                val chip = Chip(context)
                chip.setTextAppearance(android.R.style.TextAppearance_Material_Small)
                chip.text = tag
                holder.item_invnums.addView(chip)
            }
        }

        if (mItemList[position].item_status != "") {
            holder.box_status.setBackgroundColor(context.getColor(R.color.md_theme_light_secondary))
        }

        val img = Utils.StringToBitMap(mItemList[position].item_image)
        if (img != null){
            holder.item_image.setImageBitmap(img)
        }
        Utils.setRecyclerViewCardAnimation(holder.itemView, context);
    }

    inner class BoxItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        var box_status = itemView.findViewById<View>(R.id.item_status)
        var item_amount: TextView = itemView.findViewById<TextView>(R.id.item_amount)
        var item_name = itemView.findViewById<TextView>(R.id.item_name)
        var item_invnums = itemView.findViewById<ChipGroup>(R.id.item_invnums)
        var item_image = itemView.findViewById<ImageView>(R.id.item_img)
    }


    fun setFilter(itemList: List<BoxItemModel>) {
        mItemList.clear()
        mItemList.addAll(itemList)
        this.notifyDataSetChanged()

    }


}