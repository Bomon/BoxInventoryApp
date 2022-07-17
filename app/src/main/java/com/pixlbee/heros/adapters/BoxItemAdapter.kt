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
import com.pixlbee.heros.utility.Utils
import com.pixlbee.heros.models.BoxItemModel


class BoxItemAdapter(private val mDataList: ArrayList<BoxItemModel>, boxId: String) : RecyclerView.Adapter<BoxItemAdapter.BoxItemViewHolder>() {

    lateinit var context: Context
    private var mItemList: ArrayList<BoxItemModel> = ArrayList()
    private lateinit var box_id: String


    init {
        box_id = boxId
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
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
        holder.item_color.background.setTint(mItemList[position].item_color)

        holder.item_invnums.removeAllViews()
        for (tag in mItemList[position].item_invnum.split(";")){
            if (tag != ""){
                val chip = Chip(context)
                chip.setTextAppearance(android.R.style.TextAppearance_Material_Small)
                chip.text = tag
                holder.item_invnums.addView(chip)
            }
        }

        val img = Utils.StringToBitMap(mItemList[position].item_image)
        if (img != null){
            holder.item_image.setImageBitmap(img)
        }
    }


    inner class BoxItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        var item_color = itemView.findViewById<View>(R.id.item_color)
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

    fun updateColorInFirebase(position: Int) {
        notifyItemChanged(position)
        val boxesRef = FirebaseDatabase.getInstance().reference.child("boxes")
        boxesRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val boxes: DataSnapshot? = task.result
                if (boxes != null) {
                    for (box: DataSnapshot in boxes.children) {
                        val id = box.child("id").value.toString()
                        if (id == box_id) {
                            val boxKey: String = box.key.toString()
                            for (item: DataSnapshot in box.child("content").children){
                                val item_id = item.child("id").value.toString()
                                if (mItemList[position].item_id == item_id) {
                                    val itemKey: String = item.key.toString()
                                    FirebaseDatabase.getInstance().reference.child("boxes")
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