package com.pixlbee.heros.utility

import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.LinearLayout


class Animations {
    companion object {

        //Kotlin
        fun expand(v: View){
            v.measure(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT)
            val targetHeight: Int = v.measuredHeight

            v.layoutParams.height = 1
            v.visibility = View.VISIBLE

            val a: Animation = object : Animation(){
                override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                    v.layoutParams.height = if (interpolatedTime == 1f)
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    else
                        (targetHeight * interpolatedTime).toInt()
                    v.requestLayout()

                }

                override fun willChangeBounds(): Boolean {
                    return true
                }
            }

            a.duration = (targetHeight / v.context.resources.displayMetrics.density).toInt().toLong()
            v.startAnimation(a)
        }

        fun collapse(v: View) {
            val initialHeight : Int = v.measuredHeight
            val a : Animation = object : Animation(){
                override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                    if (interpolatedTime == 1f){
                        v.visibility = View.GONE
                    }else{
                        v.layoutParams.height = initialHeight - (initialHeight * interpolatedTime).toInt()
                        v.requestLayout()
                    }
                }

                override fun willChangeBounds(): Boolean {
                    return true
                }
            }

            a.duration = (initialHeight / v.context.resources.displayMetrics.density).toInt().toLong()
            v.startAnimation(a)
        }
    }
}