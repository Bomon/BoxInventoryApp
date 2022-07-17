package com.pixlbee.heros.utility

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Align
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.pixlbee.heros.R


class BoxDividerItemDecorator (private val mDivider: Drawable) : RecyclerView.ItemDecoration() {

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val dividerLeft = parent.paddingLeft
        val dividerRight = parent.width - parent.paddingRight
        val childCount = parent.childCount
        var currentChildLocation = ""


        val sharedPreferences = parent.context.getSharedPreferences("AppPreferences",
            Context.MODE_PRIVATE
        )
        val saved_order_by_btn = sharedPreferences.getInt("settings_box_order_by", R.id.radioButtonOrderId)

        if (saved_order_by_btn == R.id.radioButtonOrderLocation){
            for (i in 0..childCount - 1) {
                val child = parent.getChildAt(i)
                val childLocation = child.findViewById<Chip>(R.id.box_location).text.toString()
                if (childLocation != currentChildLocation){
                    currentChildLocation = childLocation

                    val params = child.layoutParams as RecyclerView.LayoutParams
                    val dividerTop = child.top - mDivider.intrinsicHeight - 110
                    val dividerBottom = dividerTop + mDivider.intrinsicHeight + 92
                    mDivider.setBounds(dividerLeft, dividerTop, dividerRight, dividerBottom)
                    mDivider.draw(canvas)

                    var mText = currentChildLocation
                    var mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                    mPaint.color = Color.BLUE
                    val textSize = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_SP,
                        20f, parent.resources.getDisplayMetrics()
                    )
                    mPaint.textSize = textSize
                    mPaint.textAlign = Align.LEFT

                    canvas.drawText(currentChildLocation, 100f, dividerTop.toFloat(), mPaint)
                }
            }
        }
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val childCount = parent.childCount
        var currentChildLocation = ""

        val sharedPreferences = parent.context.getSharedPreferences("AppPreferences",
            Context.MODE_PRIVATE
        )
        val saved_order_by_btn = sharedPreferences.getInt("settings_box_order_by", R.id.radioButtonOrderId)

        if (saved_order_by_btn == R.id.radioButtonOrderLocation){
            var childPosition: Int = parent.getChildAdapterPosition(view)
            Log.e("Error", childPosition.toString())
            if (childPosition == 0){
                outRect.set(0, 150, 0, 0)
            } else {
                if (parent.getChildAt(childPosition) != null){
                    var childLocation = parent.getChildAt(childPosition).findViewById<Chip>(R.id.box_location).text.toString()
                    var prevChildLocation = parent.getChildAt(childPosition - 1).findViewById<Chip>(R.id.box_location).text.toString()
                    if (childLocation != prevChildLocation){
                        outRect.set(0, 200, 0, 0)
                    } else {
                        outRect.set(0, 0, 0, 0)
                    }
                } else {
                    outRect.set(0, 0, 0, 0)
                }
            }
        }
    }
}