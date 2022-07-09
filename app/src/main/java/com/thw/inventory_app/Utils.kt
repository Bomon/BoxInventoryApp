package com.thw.inventory_app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.core.view.allViews
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream

class Utils {

    companion object {
        fun StringToBitMap(encodedString: String?): Bitmap? {
            return try {
                val encodeByte: ByteArray = Base64.decode(encodedString, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.size)
            } catch (e: Exception) {
                e.message
                null
            }
        }

        fun getEncoded64ImageStringFromBitmap(bitmap: Bitmap): String {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
            val byteFormat: ByteArray = stream.toByteArray()
            // get the base 64 string
            return Base64.encodeToString(byteFormat, Base64.NO_WRAP)
        }

        fun pushFragment(newFragment: Fragment, context: Context, backStackName: String) {
            val fragmentManager = (context as FragmentActivity).supportFragmentManager
            val transaction: FragmentTransaction = fragmentManager.beginTransaction()
            //transaction.setCustomAnimations(
            //    R.anim.slide_in,
            //    R.anim.fade_out,
            //    R.anim.fade_in,
            //    R.anim.slide_out)
            transaction.replace(R.id.nav_host_fragment_activity_main, newFragment)
            //transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            transaction.addToBackStack(backStackName)
            transaction.commit()
        }

        fun readBoxModelFromDataSnapshot(box: DataSnapshot): BoxModel {
            val image = box.child("image").value.toString()
            val location_image = box.child("location_image").value.toString()
            val location = box.child("location").value.toString()
            val id = box.child("id").value.toString()
            val name = box.child("name").value.toString()
            val description = box.child("description").value.toString()
            val qrcode = box.child("qrcode").value.toString()
            val notes = box.child("notes").value.toString()

            var color: Int? = box.child("color").value.toString().toIntOrNull()
            if (color == null) {
                Log.e("Error", "Box color is null")
                color = R.color.default_box_color
            }

            val status = box.child("status").value.toString()
            val content = box.child("content")

            val contentList = ArrayList<ContentItem>()
            for (c: DataSnapshot in content.children){
                val itemAmount = c.child("amount").value.toString()
                val itemId = c.child("id").value.toString()
                val itemInvNum = c.child("invnum").value.toString()
                var itemColor: Int? = c.child("color").value.toString().toIntOrNull()
                if (itemColor == null) {
                    itemColor = R.color.default_item_color
                }
                contentList.add(ContentItem(c.key.toString(), itemAmount, itemId, itemInvNum, itemColor))
            }
            return BoxModel(id, name, description, qrcode, location, image, location_image, notes, color, status, contentList)
        }

        fun getAllBoxIds(): ArrayList<String> {
            var idList: ArrayList<String> = ArrayList<String>()
            val boxesRef = FirebaseDatabase.getInstance().reference.child("boxes")
            boxesRef.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val boxes: DataSnapshot? = task.result
                    if (boxes != null) {
                        for (box: DataSnapshot in boxes.children) {
                            idList.add(box.child("id").value.toString())
                        }
                    }
                }
            }
            Log.e("Error", idList.joinToString(", "))
            return idList
        }

        fun getAllBoxQRcodes(): ArrayList<String> {
            var qrList: ArrayList<String> = ArrayList<String>()
            val boxesRef = FirebaseDatabase.getInstance().reference.child("boxes")
            boxesRef.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val boxes: DataSnapshot? = task.result
                    if (boxes != null) {
                        for (box: DataSnapshot in boxes.children) {
                            qrList.add(box.child("qrcode").value.toString())
                        }
                    }
                }
            }
            return qrList
        }

        fun updateFirebaseEntities(){
            val itemsRef = FirebaseDatabase.getInstance().reference.child("boxes")
            itemsRef.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val boxes: DataSnapshot? = task.result
                    if (boxes != null) {
                        for (box: DataSnapshot in boxes.children) {
                            val boxKey = box.key.toString()
                            if (!box.hasChild("status")) {
                                FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("status").setValue("")
                            }
                            if (!box.hasChild("notes")) {
                                FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("notes").setValue("")
                            }
                            if (!box.hasChild("color")) {
                                FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("color").setValue("")
                            }
                            for (contentItem: DataSnapshot in box.child("content").children){
                                val contentItemKey = contentItem.key.toString()
                                if (!contentItem.hasChild("color")) {
                                    FirebaseDatabase.getInstance().reference.child("boxes").child(boxKey).child("content").child(contentItemKey).child("color").setValue(R.color.default_item_color)
                                }
                            }
                        }
                    }
                }
            }
        }

        fun chipListToString(chipGroup: ChipGroup): String{
            var chipString = ""
            for (chip in chipGroup.allViews) {
                if (chip is Chip){
                    chipString += ";" + chip.text.toString()
                }
            }
            return chipString.removePrefix(";")
        }

        fun setRecyclerViewCardAnimation(viewToAnimate: View, context: Context) {
            //val animation: Animation =
            //    AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left)
            //viewToAnimate.startAnimation(animation)
        }
    }

}