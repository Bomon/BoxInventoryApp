package com.thw.inventory_app.ui.box

import android.annotation.SuppressLint
import android.content.Context
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.thw.inventory_app.ContentItem
import com.thw.inventory_app.ItemModel
import com.thw.inventory_app.R
import com.thw.inventory_app.Utils

data class ItemSelection(val item_id: String)

class ItemAddAdapter(private val mDataList: ArrayList<ItemModel>, private val box_id: String, private val do_animate: Boolean, val handler: ItemAddAdapter.Callbacks) : RecyclerView.Adapter<ItemAddAdapter.ItemViewHolder>() {

    lateinit var context: Context
    private var mItemModel: ArrayList<ItemModel> = ArrayList()

    init {
        setFilter(mDataList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        context = parent.context
        val layoutInflater = LayoutInflater.from(parent.context)
        return ItemViewHolder(layoutInflater.inflate(R.layout.card_item, parent, false))

    }

    interface Callbacks {
        fun handleItemSelection(data: ItemSelection)
    }

    override fun getItemCount(): Int {
        return mItemModel.size
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        (holder as ItemViewHolder).bind(mItemModel[position]);
        setAnimation(holder.itemView);
    }

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener{

        var item_name: TextView? = null
        var item_image: ImageView? = null
        var item_id: TextView? = null
        var item_description: TextView? = null
        var item_tags: TextView? = null

        init {
            itemView.setOnClickListener(this)
            item_name = itemView.findViewById<TextView>(R.id.item_name)
            item_image = itemView.findViewById<ImageView>(R.id.item_img)
            //item_id = itemView.findViewById<TextView>(R.id.box_id)
            //item_description = itemView.findViewById<TextView>(R.id.box_location)
            //item_tags = itemView.findViewById<TextView>(R.id.box_location)
        }


        fun bind(model: ItemModel): Unit {
            item_name?.text = model.name
            val img = Utils.StringToBitMap(model.image)
            if (img != null){
                item_image?.setImageBitmap(img)
            }
        }

        @SuppressLint("ResourceType")
        override fun onClick(view: View?) {
            var item_id = mItemModel[adapterPosition].id
            handler.handleItemSelection(ItemSelection(item_id))
            (context as FragmentActivity).supportFragmentManager.popBackStack()
        }
    }

    private fun setAnimation(viewToAnimate: View) {
        if (do_animate) {
            val animation: Animation =
                AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left)
            viewToAnimate.startAnimation(animation)
        }
    }


    fun setFilter(itemList: List<ItemModel>) {
        mItemModel.clear()
        mItemModel.addAll(itemList)
        this.notifyDataSetChanged()

    }


}