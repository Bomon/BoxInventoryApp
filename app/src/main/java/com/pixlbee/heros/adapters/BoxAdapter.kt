package com.pixlbee.heros.adapters

import android.content.Context
import android.util.Base64
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
import com.pixlbee.heros.models.BoxModel


class BoxAdapter(var content: ArrayList<BoxModel>, var compactView: Boolean = false) : RecyclerView.Adapter<BoxAdapter.BoxViewHolder>() {

    lateinit var context: Context
    private var mBoxList: ArrayList<BoxModel> = ArrayList()

    private lateinit var mListener: OnBoxClickListener
    lateinit var holder: BoxViewHolder


    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return mBoxList[position].numeric_id
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoxViewHolder {
        context = parent.context
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.card_box, parent, false)
        return BoxViewHolder(view, mListener, compactView)
    }


    override fun getItemCount(): Int {
        return mBoxList.size
    }


    override fun onBindViewHolder(holder: BoxViewHolder, position: Int) {
        this.holder = holder
        holder.box_id.text = mBoxList[position].id
        holder.box_name.text = mBoxList[position].name
        holder.box_location.text = mBoxList[position].location
        holder.box_color.background.setTint(mBoxList[position].color)

        if (mBoxList[position].image != ""){
            var imageByteArray = Base64.decode(mBoxList[position].image, Base64.DEFAULT);
            Glide.with(context)
                .load(imageByteArray)
                .into(holder.box_img);
        }


        //val img = Utils.StringToBitMap(mBoxList[position].image)
        //if (img != null){
        //    holder.box_img.setImageBitmap(img)
        //}

        holder.box_status_tags.removeAllViews()

        if(mBoxList[position].status != ""){
            for (tag in mBoxList[position].status.split(";")){
                if (tag != ""){
                    val chip = Chip(context)
                    chip.text = tag
                    chip.minHeight = 1
                    chip.setTextAppearance(R.style.BoxStatusChip)
                    holder.box_status_tags.addView(chip)
                }
            }
        }
    }


    inner class BoxViewHolder(itemView: View, var mListener: OnBoxClickListener, var compactView: Boolean) : RecyclerView.ViewHolder(itemView), View.OnClickListener{

        var box_id: TextView = itemView.findViewById<TextView>(R.id.box_id)
        var box_name: TextView = itemView.findViewById<TextView>(R.id.box_name)
        var box_location: TextView = itemView.findViewById<TextView>(R.id.box_location)
        var box_img: ImageView = itemView.findViewById<ImageView>(R.id.box_img)
        var box_status_tags: ChipGroup = itemView.findViewById<ChipGroup>(R.id.box_status)
        var box_container: MaterialCardView = itemView.findViewById<MaterialCardView>(R.id.box_card_small)
        var box_color: View = itemView.findViewById<View>(R.id.box_color)

        init {
            itemView.setOnClickListener(this)

            if (compactView == true) {
                box_status_tags.visibility = View.GONE
            }
        }

        override fun onClick(view: View?) {
            if(mListener != null){
                // second argument is the element from which the transition will start
                mListener.onBoxClicked(mBoxList[adapterPosition], box_img)
            }
            true
        }
    }


    fun setFilter(itemList: List<BoxModel>) {
        mBoxList.clear()
        mBoxList.addAll(itemList)
        this.notifyDataSetChanged()
    }


    interface OnBoxClickListener{
        fun onBoxClicked(box: BoxModel, view: View)
    }


    fun setOnBoxClickListener(mListener: OnBoxClickListener) {
        this.mListener = mListener
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
                        if (id == holder.box_id.text.toString()) {
                            val boxKey: String = box.key.toString()
                            FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("color").setValue(mBoxList[position].color)
                        }
                    }
                }
            }
        }
    }


}