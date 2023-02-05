package com.pixlbee.heros.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.pixlbee.heros.R
import com.pixlbee.heros.models.BoxItemModel
import com.pixlbee.heros.utility.Utils


class BoxItemAdapter(boxId: String) : RecyclerView.Adapter<BoxItemAdapter.BoxItemViewHolder>() {

    private lateinit var mContext: Context
    private var mItemList: ArrayList<BoxItemModel> = ArrayList()
    private lateinit var mBoxId: String

    private lateinit var mListener: OnBoxItemClickListener


    init {
        mBoxId = boxId
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return mItemList[position].numeric_id.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoxItemViewHolder {
        mContext = parent.context
        val layoutInflater = LayoutInflater.from(parent.context)
        return BoxItemViewHolder(layoutInflater.inflate(R.layout.card_item_in_box, parent, false))

    }


    override fun getItemCount(): Int {
        return mItemList.size
    }


    override fun onBindViewHolder(holder: BoxItemViewHolder, position: Int) {
        val itemAmount = mItemList[position].item_amount.toInt()
        val itemAvailAmount = itemAmount - mItemList[position].item_amount_taken.toInt()

        if (itemAvailAmount == itemAmount) {
            holder.itemAmount.text = itemAmount.toString()
        } else {
            holder.itemAmount.text = "$itemAvailAmount / $itemAmount"
        }
        holder.itemName.text = mItemList[position].item_name
        //holder.itemColor.background.setTint(mItemList[position].item_color)
        holder.itemContainer.strokeColor = mItemList[position].item_color

        holder.itemInvnums.removeAllViews()
        for (tag in mItemList[position].item_invnum.split(";")){
            if (tag != ""){
                val chip = Chip(mContext)
                chip.setTextAppearance(android.R.style.TextAppearance_Material_Small)
                chip.text = tag
                chip.isClickable = false
                holder.itemInvnums.addView(chip)
            }
        }

        if (mItemList[position].item_image != "") {
            val img = Utils.stringToBitMap(mItemList[position].item_image)
            if (img != null) {
                Glide.with(mContext).load(img).into(holder.itemImage)
            }
        }

        holder.itemContainer.transitionName = mItemList[position].numeric_id
    }


    inner class BoxItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener{
        //var itemColor: View = itemView.findViewById(R.id.item_color)
        var itemAmount: TextView = itemView.findViewById(R.id.item_amount)
        var itemName: TextView = itemView.findViewById(R.id.item_name)
        var itemInvnums: ChipGroup = itemView.findViewById(R.id.item_invnums)
        var itemImage: ImageView = itemView.findViewById(R.id.item_img)
        var itemContainer: MaterialCardView = itemView.findViewById(R.id.box_item_card_small)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View?) {
            if(mListener != null){
                // second argument is the element from which the transition will start
                mListener.onBoxItemClicked(mItemList[adapterPosition], itemContainer)
            }
            true
        }
    }


    fun setFilter(itemList: List<BoxItemModel>) {
        mItemList.clear()
        mItemList.addAll(itemList)
        this.notifyDataSetChanged()

    }


    fun updateAmountTaken(position: Int, newTakenAmount: String, iContext: Context) {
        Log.e("Error", "update amount")
        val boxesRef = FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(iContext)).child("boxes")
        boxesRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val boxes: DataSnapshot? = task.result
                if (boxes != null) {
                    for (box: DataSnapshot in boxes.children) {
                        val id = box.child("id").value.toString()
                        if (id == mBoxId) {
                            val boxKey: String = box.key.toString()
                            for (item: DataSnapshot in box.child("content").children){
                                val itemId = item.child("numeric_id").value.toString()
                                if (mItemList[position].numeric_id == itemId) {
                                    val itemKey: String = item.key.toString()
                                    FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(iContext)).child("boxes")
                                        .child(boxKey).child("content").child(itemKey)
                                        .child("amount_taken").setValue(newTakenAmount)
                                    break
                                }
                            }
                        }
                    }
                }
            }
        }
        notifyItemChanged(position)
    }


    interface OnBoxItemClickListener{
        fun onBoxItemClicked(item: BoxItemModel, view: View)
    }

    fun setOnBoxItemClickListener(mListener: OnBoxItemClickListener) {
        this.mListener = mListener
    }

}