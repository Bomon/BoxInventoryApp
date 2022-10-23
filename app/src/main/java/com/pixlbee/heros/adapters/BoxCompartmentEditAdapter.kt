package com.pixlbee.heros.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.pixlbee.heros.R
import com.pixlbee.heros.models.CompartmentModel
import com.pixlbee.heros.utility.Animations


class BoxCompartmentEditAdapter(boxId: String) : RecyclerView.Adapter<BoxCompartmentEditAdapter.BoxItemViewHolder>() {

    private lateinit var mContext: Context
    private var mCompartmentList: ArrayList<CompartmentModel> = ArrayList()
    private lateinit var mBoxId: String
    private lateinit var animationType: String

    private lateinit var mListener: BoxCompartmentEditAdapter.OnCompartmentItemAddListener


    init {
        mBoxId = boxId
        setHasStableIds(true)
    }

    fun getCurrentStatus(): ArrayList<CompartmentModel> {

        return mCompartmentList
    }

    override fun getItemId(position: Int): Long {
        return mCompartmentList[position].name.hashCode().toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoxItemViewHolder {
        mContext = parent.context
        val layoutInflater = LayoutInflater.from(parent.context)
        return BoxItemViewHolder(layoutInflater.inflate(R.layout.card_compartment_in_box_edit, parent, false))
    }


    override fun getItemCount(): Int {
        return mCompartmentList.size
    }


    override fun onBindViewHolder(holder: BoxItemViewHolder, position: Int) {
        val compartment = mCompartmentList[position]

        holder.compartmentName.text = compartment.name
        holder.compartmentContainer.transitionName = compartment.name

        holder.mBoxItemEditAdapter = BoxItemEditAdapter(ArrayList())
        holder.recyclerview.layoutManager = LinearLayoutManager(mContext)
        holder.recyclerview.adapter = holder.mBoxItemEditAdapter

        if (compartment.is_expanded) {
            holder.rvContainer.visibility = View.VISIBLE
        } else {
            holder.rvContainer.visibility = View.GONE
        }

        val mItemList = compartment.content
        holder.mBoxItemEditAdapter.setFilter(mItemList)

        // On Compartment Clicked collapase / ellapse
        holder.itemView.setOnClickListener {
            if (compartment.is_expanded) {
                holder.collapseContent(it)
            } else {
                holder.expandContent(it)
            }
            compartment.is_expanded = !compartment.is_expanded
        }

        // On Item Added
        holder.compartmentAddItemButton.setOnClickListener { view ->
            if (view != null) {
                Log.e("Error", "Add item to comaprtment " + compartment.name)
                mListener.onCompartmentItemAdd(compartment.name, view)
            }
        }

        holder.isExpanded = compartment.is_expanded


        val itemTouchHelper by lazy {
            val simpleItemTouchCallback = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.START or ItemTouchHelper.END, 0) {

                override fun onMove(recyclerView: RecyclerView,
                                    viewHolder: RecyclerView.ViewHolder,
                                    target: RecyclerView.ViewHolder): Boolean {
                    val adapter = recyclerView.adapter as BoxItemEditAdapter
                    val from = viewHolder.adapterPosition
                    val to = target.adapterPosition

                    adapter.notifyItemMoved(from, to)

                    return true
                }

                override fun onMoved(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    fromPos: Int,
                    target: RecyclerView.ViewHolder,
                    toPos: Int,
                    x: Int,
                    y: Int
                ) {
                    super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y)
                    val adapter = recyclerView.adapter as BoxItemEditAdapter
                    val updatedItemList = adapter.moveItem(fromPos, toPos)
                    mCompartmentList[holder.absoluteAdapterPosition].content = updatedItemList
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                }

                override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                    super.onSelectedChanged(viewHolder, actionState)

                    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                        viewHolder?.itemView?.alpha = 0.5f
                    }
                }

                override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                    super.clearView(recyclerView, viewHolder)

                    viewHolder.itemView.alpha = 1.0f
                }
            }

            ItemTouchHelper(simpleItemTouchCallback)
        }
        itemTouchHelper.attachToRecyclerView(holder.recyclerview)

    }


    inner class BoxItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        var compartmentName: TextView = itemView.findViewById(R.id.compartment_name)
        var compartmentContainer: MaterialCardView = itemView.findViewById(R.id.box_compartment_card_small)
        val recyclerview = itemView.findViewById<View>(R.id.compartment_content) as RecyclerView
        val compartmentArrow: ImageView = itemView.findViewById<ImageView>(R.id.compartment_arrow)
        val compartmentAddItemButton: MaterialButton = itemView.findViewById<MaterialButton>(R.id.compartment_add_item_button)
        val rvContainer: RelativeLayout = itemView.findViewById<RelativeLayout>(R.id.compartment_content_container)
        var isExpanded: Boolean = false

        lateinit var mBoxItemEditAdapter: BoxItemEditAdapter

        fun expandContent(p0: View?) {
            compartmentArrow.animate().setDuration(200).rotation(180F)
            Animations.expand(rvContainer)
            //recyclerview.visibility = View.VISIBLE
            //recyclerview.alpha = 0.0f
            //recyclerview.animate()
                //.translationY(recyclerview.height.toFloat())
            //    .alpha(1.0f)
           //     .setDuration(500)
        }

        fun collapseContent(p0: View?) {
            compartmentArrow.animate().setDuration(200).rotation(0F)
            Animations.collapse(rvContainer)
            //recyclerview.visibility = View.GONE
            //recyclerview.alpha = 1.0f
            //recyclerview.animate()
                //.translationY(recyclerview.height.toFloat())
            //    .alpha(0.0f)
            //    .setDuration(500)
        }
    }

    interface OnCompartmentItemAddListener{
        fun onCompartmentItemAdd(compartmentName: String, view: View)
    }

    fun setOnCompartmentItemAddListener(mListener: OnCompartmentItemAddListener) {
        this.mListener = mListener
    }


    fun setFilter(compartmentItems: ArrayList<CompartmentModel>) {
        mCompartmentList.clear()
        mCompartmentList.addAll(compartmentItems)
        this.notifyDataSetChanged()
    }

}