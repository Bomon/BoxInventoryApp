package com.thw.inventory_app.ui.box

import android.annotation.SuppressLint
import android.content.Context
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
import com.thw.inventory_app.ItemModel
import com.thw.inventory_app.R
import com.thw.inventory_app.Utils
import com.thw.inventory_app.ui.item.ItemFragment


class BoxItemAdapter(private val mDataList: ArrayList<BoxItemModel>, private val do_animate: Boolean) : RecyclerView.Adapter<BoxItemAdapter.BoxItemViewHolder>() {

    lateinit var context: Context
    private var mItemModel: ArrayList<BoxItemModel> = ArrayList()

    init {
        setFilter(mDataList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoxItemViewHolder {
        context = parent.context
        val layoutInflater = LayoutInflater.from(parent.context)
        return BoxItemViewHolder(layoutInflater.inflate(R.layout.card_item_in_box, parent, false))

    }

    override fun getItemCount(): Int {
        return mItemModel.size
    }

    override fun onBindViewHolder(holder: BoxItemViewHolder, position: Int) {
        (holder as BoxItemViewHolder).bind(mItemModel[position]);
        setAnimation(holder.itemView);
    }

    inner class BoxItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener{

        var item_amount: TextView? = null
        var item_name: TextView? = null
        var item_image: ImageView? = null

        init {
            itemView.setOnClickListener(this)
            item_amount = itemView.findViewById<TextView>(R.id.item_amount)
            item_name = itemView.findViewById<TextView>(R.id.item_name)
            item_image = itemView.findViewById<ImageView>(R.id.item_img)
        }


        fun bind(model: BoxItemModel): Unit {
            item_name?.text = model.item_name
            val img = Utils.StringToBitMap(model.item_image)
            if (img != null){
                item_image?.setImageBitmap(img)
            }
            item_amount?.text = model.item_amount
        }

        @SuppressLint("ResourceType")
        override fun onClick(view: View?) {
            if (view != null) {
                val im: BoxItemModel = mItemModel[adapterPosition]
                val item: ItemModel = ItemModel(im.item_id, im.item_name, im.item_description, im.item_tags, im.item_image)
                val myFragment: Fragment = ItemFragment.newInstance(item)
                val context = view.getContext()
                Utils.pushFragment(myFragment, context, "ItemView")
            }
        }
    }

    private fun setAnimation(viewToAnimate: View) {
        if (do_animate) {
            val animation: Animation =
                AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left)
            viewToAnimate.startAnimation(animation)
        }
    }


    fun setFilter(itemList: List<BoxItemModel>) {
        mItemModel.clear()
        mItemModel.addAll(itemList)
        this.notifyDataSetChanged()

    }


}