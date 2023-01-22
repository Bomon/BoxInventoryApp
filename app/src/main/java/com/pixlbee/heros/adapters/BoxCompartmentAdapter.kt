package com.pixlbee.heros.adapters

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.pixlbee.heros.R
import com.pixlbee.heros.models.BoxItemModel
import com.pixlbee.heros.models.CompartmentModel
import com.pixlbee.heros.utility.Animations
import com.pixlbee.heros.utility.Utils
import kotlin.math.min


class BoxCompartmentAdapter(boxId: String) : RecyclerView.Adapter<BoxCompartmentAdapter.BoxItemViewHolder>() {

    private lateinit var mContext: Context
    private var mCompartmentList: ArrayList<CompartmentModel> = ArrayList()
    private lateinit var mBoxId: String
    private lateinit var animationType: String
    private var skipUpdate = false

    private lateinit var mListener: BoxCompartmentAdapter.OnCompartmentItemClickListener


    init {
        mBoxId = boxId
        setHasStableIds(true)
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
        return BoxItemViewHolder(layoutInflater.inflate(R.layout.card_compartment_in_box, parent, false))
    }


    override fun getItemCount(): Int {
        return mCompartmentList.size
    }


    override fun onBindViewHolder(holder: BoxItemViewHolder, position: Int) {
        val compartment = mCompartmentList[position]

        holder.compartmentName.text = compartment.name
        holder.compartmentContainer.transitionName = compartment.name

        holder.mBoxItemAdapter = BoxItemAdapter(mBoxId)
        holder.mBoxItemAdapter.setOnBoxItemClickListener(object: BoxItemAdapter.OnBoxItemClickListener{
            override fun onBoxItemClicked(item: BoxItemModel, view: View) {
                mListener.onCompartmentItemClicked(item, view)
            }
        })
        holder.recyclerview.layoutManager = LinearLayoutManager(mContext)
        holder.recyclerview.adapter = holder.mBoxItemAdapter

        if (compartment.is_expanded) {
            holder.rvContainer.visibility = View.VISIBLE
        } else {
            holder.rvContainer.visibility = View.GONE
        }

        val mItemList = compartment.content
        holder.mBoxItemAdapter.setFilter(mItemList)

        // Swipe functionality
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT + ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            private val clearPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }


            private fun clearCanvas(c: Canvas?, left: Float, top: Float, right: Float, bottom: Float) {
                c?.drawRect(left, top, right, bottom, clearPaint)
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (Utils.checkHasWritePermission(mContext, false)){

                    val itemView = viewHolder.itemView
                    val itemHeight = itemView.bottom - itemView.top
                    val isCanceled = dX == 0f && !isCurrentlyActive

                    if (isCanceled) {
                        clearCanvas(c, itemView.left + dX, itemView.top.toFloat(), itemView.left.toFloat(), itemView.bottom.toFloat())
                        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                        return
                    }

                    if (dX>0){
                        val backgroundLeft = GradientDrawable()
                        //val backgroundLeftColor = ResourcesCompat.getColor(resources, R.color.thw_blue, context?.theme)

                        val backgroundLeftColor = mContext!!.obtainStyledAttributes(
                            TypedValue().data,
                            intArrayOf(com.google.android.material.R.attr.colorPrimaryContainer)
                        ).getColor(0, 0)

                        backgroundLeft.setColor(backgroundLeftColor)
                        backgroundLeft.cornerRadius = 40F
                        backgroundLeft.setBounds(itemView.left + 27, itemView.top + 10, itemView.left + (itemView.right-itemView.left)/2, itemView.bottom - 10)
                        backgroundLeft.draw(c)

                        val iconColor = mContext!!.obtainStyledAttributes(
                            TypedValue().data,
                            intArrayOf(com.google.android.material.R.attr.colorOnPrimaryContainer)
                        ).getColor(0, 0)
                        val editIcon = ContextCompat.getDrawable(mContext!!, R.drawable.ic_baseline_exposure_plus_1_24)
                        editIcon!!.setTint(iconColor)
                        val intrinsicWidth = editIcon.intrinsicWidth
                        val intrinsicHeight = editIcon.intrinsicHeight
                        // Calculate position of minus icon
                        val editIconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
                        val editIconMargin = (itemHeight - intrinsicHeight)
                        val editIconLeft = itemView.left - intrinsicWidth + 150
                        val editIconRight = itemView.left + 150
                        val editIconBottom = editIconTop + intrinsicHeight
                        // Draw the minus icon
                        editIcon.setBounds(editIconLeft, editIconTop, editIconRight, editIconBottom)
                        editIcon.draw(c)

                    } else {
                        val backgroundRight = GradientDrawable()
                        //val backgroundRightColor = ResourcesCompat.getColor(resources, R.color.thw_blue, context?.theme)

                        val backgroundRightColor = mContext!!.obtainStyledAttributes(
                            TypedValue().data,
                            intArrayOf(com.google.android.material.R.attr.colorPrimaryContainer)
                        ).getColor(0, 0)

                        backgroundRight.setColor(backgroundRightColor)
                        backgroundRight.cornerRadius = 40F
                        backgroundRight.setBounds(itemView.left + (itemView.right-itemView.left)/2, itemView.top + 10, itemView.right-27, itemView.bottom - 10)
                        backgroundRight.draw(c)

                        val iconColor = mContext!!.obtainStyledAttributes(
                            TypedValue().data,
                            intArrayOf(com.google.android.material.R.attr.colorOnPrimaryContainer)
                        ).getColor(0, 0)
                        val editIcon = ContextCompat.getDrawable(mContext!!, R.drawable.ic_baseline_exposure_neg_1_24)
                        editIcon!!.setTint(iconColor)
                        val intrinsicWidth = editIcon.intrinsicWidth
                        val intrinsicHeight = editIcon.intrinsicHeight
                        // Calculate position of plus icon
                        val editIconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
                        val editIconMargin = (itemHeight - intrinsicHeight)
                        val editIconLeft = itemView.right - intrinsicWidth - 100
                        val editIconRight = itemView.right - 100
                        val editIconBottom = editIconTop + intrinsicHeight
                        // Draw the plus icon
                        editIcon.setBounds(editIconLeft, editIconTop, editIconRight, editIconBottom)
                        editIcon.draw(c)
                    }
                    super.onChildDraw(c, recyclerView, viewHolder, dX/3, dY, actionState, isCurrentlyActive)
                } else {
                    super.onChildDraw(c, recyclerView, viewHolder, 0f, dY, actionState, isCurrentlyActive)
                }
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (Utils.checkHasWritePermission(mContext, false)){
                    val position = viewHolder.bindingAdapterPosition

                    var newTakenAmount = "0"
                    if (direction == ItemTouchHelper.RIGHT){
                        newTakenAmount = Integer.max(0, mItemList[position].item_amount_taken.toInt() - 1)
                            .toString()
                    } else {
                        newTakenAmount = min(
                            mItemList[position].item_amount.toInt(),
                            mItemList[position].item_amount_taken.toInt() + 1
                        ).toString()
                    }
                    mItemList[position].item_amount_taken = newTakenAmount

                    skipUpdate = true;
                    holder.mBoxItemAdapter.updateAmountTaken(position, newTakenAmount, mContext)
                }
            }
        }).attachToRecyclerView(holder.recyclerview)

        holder.itemView.setOnClickListener {
            if (compartment.is_expanded) {
                holder.collapseContent(it)
            } else {
                holder.expandContent(it)
            }
            compartment.is_expanded = !compartment.is_expanded
        }
    }


    inner class BoxItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        var compartmentName: TextView = itemView.findViewById(R.id.compartment_name)
        var compartmentContainer: MaterialCardView = itemView.findViewById(R.id.box_compartment_card_small)
        val recyclerview = itemView.findViewById<View>(R.id.compartment_content) as RecyclerView
        val compartmentArrow: ImageView = itemView.findViewById<ImageView>(R.id.compartment_arrow)
        val rvContainer: RelativeLayout = itemView.findViewById<RelativeLayout>(R.id.compartment_content_container)
        lateinit var mBoxItemAdapter: BoxItemAdapter

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

    interface OnCompartmentItemClickListener{
        fun onCompartmentItemClicked(item: BoxItemModel, view: View)
    }

    fun setOnCompartmentItemClickListener(mListener: OnCompartmentItemClickListener) {
        this.mListener = mListener
    }


    fun setFilter(compartmentItems: ArrayList<CompartmentModel>) {
        // skip update is only true if a swipe was performed on an item
        // reason is that a full mCompartmentList Update would redraw the whole box
        // if we disable this update, we get a smooth back-swipe transition of the item
        if (skipUpdate) {
            skipUpdate = false
        } else {
            mCompartmentList.clear()
            mCompartmentList.addAll(compartmentItems)
            this.notifyDataSetChanged()
        }
    }

}