package com.thw.inventory_app.ui.item

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
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.thw.inventory_app.BoxModel
import com.thw.inventory_app.R
import com.thw.inventory_app.Utils
import com.thw.inventory_app.ui.box.BoxFragment


class ContainingBoxAdapter(
    private val mDataList: ArrayList<BoxModel>,
    item_id: String,
    private val do_animate: Boolean
) : RecyclerView.Adapter<ContainingBoxAdapter.BoxItemViewHolder>() {

    lateinit var context: Context
    lateinit var item_id: String
    private var mBoxModel: ArrayList<BoxModel> = ArrayList()

    init {
        setFilter(mDataList)
        this.item_id = item_id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoxItemViewHolder {
        context = parent.context
        val layoutInflater = LayoutInflater.from(parent.context)
        return BoxItemViewHolder(layoutInflater.inflate(R.layout.card_box_with_inv, parent, false))

    }

    override fun getItemCount(): Int {
        return mBoxModel.size
    }

    override fun onBindViewHolder(holder: BoxItemViewHolder, position: Int) {
        (holder as BoxItemViewHolder).bind(mBoxModel[position]);
        setAnimation(holder.itemView);
    }

    inner class BoxItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener{

        var box_id: TextView? = null
        var box_name: TextView? = null
        var box_location: TextView? = null
        var box_image: ImageView? = null
        var box_inv_label: TextView? = null
        var box_invnums: ChipGroup? = null
        var box_status: ChipGroup? = null

        init {
            itemView.setOnClickListener(this)
            box_id = itemView.findViewById<TextView>(R.id.box_inv_id)
            box_name = itemView.findViewById<TextView>(R.id.box_inv_name)
            box_location = itemView.findViewById<TextView>(R.id.box_inv_location)
            box_image = itemView.findViewById<ImageView>(R.id.box_inv_img)
            box_inv_label = itemView.findViewById<TextView>(R.id.box_inv_label)
            box_invnums = itemView.findViewById<ChipGroup>(R.id.box_inv_invnums)
            box_status = itemView.findViewById<ChipGroup>(R.id.box_inv_status)
        }

        fun bind(model: BoxModel): Unit {
            box_id?.text = model.id
            box_name?.text = model.name
            box_location?.text = model.location
            val img = Utils.StringToBitMap(model.image)
            if (img != null){
                box_image?.setImageBitmap(img)
            }

            box_status?.removeAllViews()
            if(model.status != ""){
                for (tag in model.status.split(";")){
                    if (tag != ""){
                        val chip = Chip(context)
                        chip.text = tag
                        chip.minHeight = 1
                        chip.setTextAppearance(R.style.BoxStatusChip)
                        box_status?.addView(chip)
                    }
                }
            }

            if (box_inv_label != null && box_invnums != null) {
                var invnums = ArrayList<String>()
                for (ci in model.content){
                    if (ci.id == item_id){
                        if (ci.invnum != ""){
                            invnums.add(ci.invnum)
                        }
                    }
                }

                if (invnums.size > 0){
                    box_inv_label!!.visibility = View.VISIBLE
                    box_invnums!!.visibility = View.VISIBLE
                    box_invnums?.removeAllViews()
                    for (invnum in invnums){
                        if (invnum != ""){
                            val chip = Chip(context)
                            chip.setTextAppearance(android.R.style.TextAppearance_Material_Small)
                            chip.text = invnum
                            box_invnums?.addView(chip)
                        }
                    }
                } else {
                    box_inv_label!!.visibility = View.GONE
                    box_invnums!!.visibility = View.GONE
                }
            }


        }

        @SuppressLint("ResourceType")
        override fun onClick(view: View?) {
            if (view != null) {
                //val action = MainFragmentDirections.actionMainFragmentToReportsFragment()
                //navController.navigate(action)
                val myFragment: Fragment = BoxFragment.newInstance(mDataList[adapterPosition], adapterPosition)
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


    fun setFilter(itemList: List<BoxModel>) {
        mBoxModel.clear()
        mBoxModel.addAll(itemList)
        this.notifyDataSetChanged()

    }


}