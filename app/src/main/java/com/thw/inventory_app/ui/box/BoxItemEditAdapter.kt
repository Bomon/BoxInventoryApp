package com.thw.inventory_app.ui.box

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.children
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.thw.inventory_app.R
import dev.sasikanth.colorsheet.ColorSheet
import it.sephiroth.android.library.numberpicker.NumberPicker


data class ItemCardUpdate(val item_key: String, val item_id: String, val amount: String, val invnum: String, val status: String, val delete_index: Int)

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

        var item_edit_amount: NumberPicker
        //var item_edit_invnum: EditText
        //var item_edit_status: EditText
        var item_name: TextView
        var item_color_button: MaterialButton
        var item_delete_button: MaterialButton

        init {
            itemView.setOnClickListener(this)
            item_edit_amount = itemView.findViewById<NumberPicker>(R.id.box_item_edit_amount)
            //item_edit_invnum = itemView.findViewById<EditText>(R.id.box_item_edit_invnum)
            //item_edit_status = itemView.findViewById<EditText>(R.id.box_item_edit_status)
            item_name = itemView.findViewById<EditText>(R.id.box_item_name)
            item_delete_button = itemView.findViewById<MaterialButton>(R.id.box_item_delete_btn)
            item_color_button = itemView.findViewById<MaterialButton>(R.id.box_item_color_btn)
            item_delete_button.setOnClickListener {
                Log.e("Error", "Adapter pos: " + adapterPosition.toString())
                handler.handleItemCardUpdate(ItemCardUpdate(mItemModel[adapterPosition].item_key, mItemModel[adapterPosition].item_id, mItemModel[adapterPosition].item_amount, mItemModel[adapterPosition].item_invnum, mItemModel[adapterPosition].item_status, adapterPosition))
                Log.e("Error", "Delete Card")
            }

            for( v in item_edit_amount.children) {
                if(v is EditText) {
                    v.setOnEditorActionListener { _, _, _ ->
                        v.clearFocus()
                        val imm = v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(v.windowToken, 0)
                        true
                    }
                }
            }

            val fragmentManager = (context as FragmentActivity).supportFragmentManager
            val colors = context.resources.getIntArray(R.array.demo_colors)
            item_color_button.setOnClickListener {
                ColorSheet().colorPicker(
                    colors = colors,
                    listener = { color ->
                        item_color_button.background.setTint(color)
                        val red = (color shr 16 and 0xFF).toFloat()
                        val green = (color shr 8 and 0xFF).toFloat()
                        val blue = (color and 0xFF).toFloat()
                        val alpha = (color shr 24 and 0xFF).toFloat()
                        if ((red * 0.299 + green * 0.587 + blue * 0.114) > 186) {
                            item_color_button.setTextColor(Color.BLACK)
                            item_color_button.setIconTintResource(R.color.black)
                        } else {
                            item_color_button.setTextColor(Color.WHITE)
                            item_color_button.setIconTintResource(R.color.white)
                        }



                    })
                    .show(fragmentManager)
            }


            //item_edit_amount.setOnClickListener({
            //    show()
            //})

            //item_edit_amount.doAfterTextChanged {  e ->
            //    e?.let {
           //         mItemModel[adapterPosition].item_amount = it.toString()
            //        handler.handleItemCardUpdate(ItemCardUpdate(mItemModel[adapterPosition].item_key, mItemModel[adapterPosition].item_id, it.toString(), mItemModel[adapterPosition].item_invnum, mItemModel[adapterPosition].item_status, -1))
           //     }
            //}

            //item_edit_invnum.doAfterTextChanged {  e ->
            //    e?.let {
            //        mItemModel[adapterPosition].item_invnum = it.toString()
            //        handler.handleItemCardUpdate(ItemCardUpdate(mItemModel[adapterPosition].item_key, mItemModel[adapterPosition].item_id, mItemModel[adapterPosition].item_amount, it.toString(), mItemModel[adapterPosition].item_status, -1))
            //    }
            // }

            //item_edit_status.doAfterTextChanged {  e ->
            //    e?.let {
            //        mItemModel[adapterPosition].item_invnum = it.toString()
            //        handler.handleItemCardUpdate(ItemCardUpdate(mItemModel[adapterPosition].item_key, mItemModel[adapterPosition].item_id, mItemModel[adapterPosition].item_amount, mItemModel[adapterPosition].item_invnum, it.toString(), -1))
            //    }
            //}
        }

        fun bind(model: BoxItemModel): Unit {
            item_name.text = model.item_name
            //item_edit_amount.setText(model.item_amount)
            item_edit_amount.progress = model.item_amount.toInt()
            //item_edit_invnum.setText(model.item_invnum)
            //item_edit_status.setText(model.item_status)
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