package com.thw.inventory_app.ui.box

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.RecyclerView
import com.thw.inventory_app.R
import com.thw.inventory_app.Utils

data class ItemCardUpdate(val item_key: String, val item_id: String, val amount: String, val invnum: String, val delete_index: Int)

class BoxItemEditAdapter(private val mDataList: ArrayList<BoxItemModel>, private val do_animate: Boolean, private val handler: BoxItemEditAdapter.Callbacks) : RecyclerView.Adapter<BoxItemEditAdapter.BoxItemViewHolder>() {

    lateinit var context: Context
    private var mItemModel: ArrayList<BoxItemModel> = ArrayList()

    init {
        setFilter(mDataList)
    }

    interface Callbacks {
        fun handleItemCardUpdate(data: ItemCardUpdate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoxItemViewHolder {
        context = parent.context
        val layoutInflater = LayoutInflater.from(parent.context)
        return BoxItemViewHolder(layoutInflater.inflate(R.layout.card_item_in_box_edit, parent, false))

    }

    override fun getItemCount(): Int {
        return mItemModel.size
    }

    override fun onBindViewHolder(holder: BoxItemViewHolder, position: Int) {
        holder.bind(mItemModel[position])
        setAnimation(holder.itemView)
    }

    inner class BoxItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener{

        var item_amount: EditText
        var item_invnum: EditText
        var item_name: TextView
        var item_delete_button: Button

        init {
            itemView.setOnClickListener(this)
            item_amount = itemView.findViewById<EditText>(R.id.item_edit_amount)
            item_invnum = itemView.findViewById<EditText>(R.id.item_edit_invnum)
            item_name = itemView.findViewById<TextView>(R.id.item_edit_delete_btn)
            item_delete_button = itemView.findViewById<Button>(R.id.item_edit_img)
            item_delete_button.setOnClickListener {
                Log.e("Error", "Adapter pos: " + adapterPosition.toString())
                handler.handleItemCardUpdate(ItemCardUpdate(mItemModel[adapterPosition].item_key, mItemModel[adapterPosition].item_id, mItemModel[adapterPosition].item_amount, mItemModel[adapterPosition].item_invnum, adapterPosition))
                Log.e("Error", "Delete Card")
            }

            item_amount.doAfterTextChanged {  e ->
                e?.let {
                    mItemModel[adapterPosition].item_amount = it.toString()
                    handler.handleItemCardUpdate(ItemCardUpdate(mItemModel[adapterPosition].item_key, mItemModel[adapterPosition].item_id, it.toString(), mItemModel[adapterPosition].item_invnum, -1))
                }
            }

            item_invnum.doAfterTextChanged {  e ->
                e?.let {
                    mItemModel[adapterPosition].item_invnum = it.toString()
                    handler.handleItemCardUpdate(ItemCardUpdate(mItemModel[adapterPosition].item_key, mItemModel[adapterPosition].item_id, mItemModel[adapterPosition].item_amount, it.toString(), -1))
                }
            }
        }


        fun bind(model: BoxItemModel): Unit {
            item_name.text = model.item_name
            item_amount.setText(model.item_amount)
        }


        @SuppressLint("ResourceType")
        override fun onClick(view: View?) {
            if (view != null) {
                //val myFragment: Fragment = ItemsFragment.newInstance(mDataList[adapterPosition])
                //val context = view.getContext()
                //pushFragment(myFragment, context, view)
            }
        }
    }

    fun removeFromItemList(newlist: ArrayList<BoxItemModel>, position: Int) {
        this.mItemModel = newlist
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, mItemModel.size)
    }

    fun addToItemList(newlist: ArrayList<BoxItemModel>) {
        this.mItemModel = newlist
        notifyItemInserted(mItemModel.size-1)
        notifyItemRangeChanged(0, mItemModel.size)
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