package com.pixlbee.heros.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
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


    init {
        mBoxId = boxId
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
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
        holder.itemAmount.text = mItemList[position].item_amount
        holder.itemName.text = mItemList[position].item_name
        holder.itemColor.background.setTint(mItemList[position].item_color)

        holder.itemInvnums.removeAllViews()
        for (tag in mItemList[position].item_invnum.split(";")){
            if (tag != ""){
                val chip = Chip(mContext)
                chip.setTextAppearance(android.R.style.TextAppearance_Material_Small)
                chip.text = tag
                holder.itemInvnums.addView(chip)
            }
        }

        val img = Utils.stringToBitMap(mItemList[position].item_image)
        if (img != null){
            holder.itemImage.setImageBitmap(img)
        }
    }


    inner class BoxItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        var itemColor: View = itemView.findViewById(R.id.item_color)
        var itemAmount: TextView = itemView.findViewById(R.id.item_amount)
        var itemName: TextView = itemView.findViewById(R.id.item_name)
        var itemInvnums: ChipGroup = itemView.findViewById(R.id.item_invnums)
        var itemImage: ImageView = itemView.findViewById(R.id.item_img)
    }


    fun setFilter(itemList: List<BoxItemModel>) {
        mItemList.clear()
        mItemList.addAll(itemList)
        this.notifyDataSetChanged()

    }

    fun updateColorInFirebase(position: Int) {
        notifyItemChanged(position)
        val boxesRef = FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(mContext)).child("boxes")
        boxesRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val boxes: DataSnapshot? = task.result
                if (boxes != null) {
                    for (box: DataSnapshot in boxes.children) {
                        val id = box.child("id").value.toString()
                        if (id == mBoxId) {
                            val boxKey: String = box.key.toString()
                            for (item: DataSnapshot in box.child("content").children){
                                val itemId = item.child("id").value.toString()
                                if (mItemList[position].item_id == itemId) {
                                    val itemKey: String = item.key.toString()
                                    FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(mContext)).child("boxes")
                                        .child(boxKey).child("content").child(itemKey)
                                        .child("color").setValue(mItemList[position].item_color)
                                }
                            }
                        }
                    }
                }
            }
        }
    }


}