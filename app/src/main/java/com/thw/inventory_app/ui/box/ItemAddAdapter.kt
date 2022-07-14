package com.thw.inventory_app.ui.box

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.thw.inventory_app.*

class ItemAddAdapter(private val mDataList: ArrayList<ItemModel>) : RecyclerView.Adapter<ItemAddAdapter.ItemViewHolder>() {

    lateinit var context: Context
    private var mItemModel: ArrayList<ItemModel> = ArrayList()
    private lateinit var mListener: ItemAddAdapter.OnItemClickListener


    init {
        setFilter(mDataList)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        context = parent.context
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.card_item, parent, false)
        return ItemViewHolder(view)
    }


    override fun getItemCount(): Int {
        return mItemModel.size
    }


    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {

        holder.item_name.text = mItemModel[position].name
        val img = Utils.StringToBitMap(mItemModel[position].image)
        if (img != null){
            holder.item_image.setImageBitmap(img)
        }
    }


    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener{

        var item_name: TextView = itemView.findViewById<TextView>(R.id.card_item_name)
        var item_image: ImageView = itemView.findViewById<ImageView>(R.id.card_item_img)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View?) {
            if(mListener != null){
                mListener.onItemClicked(mItemModel[adapterPosition], item_image)
            }
            true
        }
    }


    fun setFilter(itemList: List<ItemModel>) {
        mItemModel.clear()
        mItemModel.addAll(itemList)
        this.notifyDataSetChanged()

    }


    interface OnItemClickListener{
        fun onItemClicked(item: ItemModel, view: View)
    }


    fun setOnItemClickListener(mListener: OnItemClickListener) {
        this.mListener = mListener
    }


}