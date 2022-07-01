package com.thw.inventory_app

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
import com.thw.inventory_app.ui.box.BoxFragment


class BoxAdapter(private val mDataList: ArrayList<BoxModel>, private val do_animate: Boolean, private val layout: Int) : RecyclerView.Adapter<BoxAdapter.BoxViewHolder>() {

    lateinit var context: Context
    private var mBoxModelList: ArrayList<BoxModel> = ArrayList()

    init {
        setFilter(mDataList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoxViewHolder {
        context = parent.context
        val layoutInflater = LayoutInflater.from(parent.context)
        return BoxViewHolder(layoutInflater.inflate(layout, parent, false))

    }

    override fun getItemCount(): Int {
        return mBoxModelList.size
    }

    override fun onBindViewHolder(holder: BoxViewHolder, position: Int) {
        (holder as BoxViewHolder).bind(mBoxModelList[position]);
        setAnimation(holder.itemView);
    }

    inner class BoxViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener{

        var box_id: TextView? = null
        var box_name: TextView? = null
        var box_location: TextView? = null
        var box_img: ImageView? = null

        init {
            itemView.setOnClickListener(this)
            box_id = itemView.findViewById<TextView>(R.id.box_id)
            box_name = itemView.findViewById<TextView>(R.id.box_name)
            box_location = itemView.findViewById<TextView>(R.id.box_location)
            box_img = itemView.findViewById<ImageView>(R.id.box_img)
        }


        fun bind(model: BoxModel): Unit {
            box_id?.text = model.id
            box_name?.text = model.name
            box_location?.text = model.location
            val img = Utils.StringToBitMap(model.img)
            if (img != null){
                box_img?.setImageBitmap(img)
            }
        }

        @SuppressLint("ResourceType")
        override fun onClick(view: View?) {
            if (view != null) {
                val myFragment: Fragment = BoxFragment.newInstance(mBoxModelList[adapterPosition])
                val context = view.getContext()
                Utils.pushFragment(myFragment, context, "BoxAdapter")
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
        mBoxModelList.clear()
        mBoxModelList.addAll(itemList)
        this.notifyDataSetChanged()

    }


}