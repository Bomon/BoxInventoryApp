package com.pixlbee.heros.preferences

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.pixlbee.heros.R
import com.pixlbee.heros.utility.Utils


class ImagePreference : Preference {

    constructor(context: Context?) : super(context!!)
    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs)

    private lateinit var imageField: ImageView
    private lateinit var imageSpinner: ProgressBar

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        imageField = holder.findViewById(R.id.image) as ImageView
        imageSpinner = holder.findViewById(R.id.image_spinner) as ProgressBar

        val orgRef = FirebaseDatabase.getInstance().reference.child(Utils.getCurrentlySelectedOrg(
            context
        ))
        orgRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val org: DataSnapshot = task.result
                val image = org.child(key).value.toString()
                if (image == ""){
                    Glide.with(context).load(R.drawable.placeholder_with_bg_80).into(imageField)
                } else {
                    imageField.scaleType= ImageView.ScaleType.FIT_CENTER
                    imageField.setImageBitmap(Utils.stringToBitMap(image))
                }
            }
        }

    }

    fun showSpinner(){
        imageSpinner.visibility = View.VISIBLE
    }

    fun hideSpinner(){
        imageSpinner.visibility = View.GONE
    }

    fun setImageURI(uri: Uri?){
         imageField.setImageURI(uri)
    }


}