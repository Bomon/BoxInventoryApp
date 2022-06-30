package com.thw.inventory_app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
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

        fun pushFragment(newFragment: Fragment, context: Context, view: View, backStackName: String) {
            val transaction: FragmentTransaction =
                (context as FragmentActivity).supportFragmentManager.beginTransaction()
            transaction.replace(R.id.nav_host_fragment_activity_main, newFragment)
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            transaction.addToBackStack(backStackName)
            transaction.commit()
        }

        fun getItemModel(item_id: String): ItemModel {
            val itemsRef = FirebaseDatabase.getInstance().reference.child("items")
            var id: String = ""
            var description: String = ""
            var name: String = ""
            var tags: String = ""
            var image: String = ""


            var response: ItemModel = ItemModel("", "", "", "", "")
            itemsRef.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val items: DataSnapshot? = task.result
                    if (items != null) {
                        for (item: DataSnapshot in items.children) {
                            id = item.child("id").value.toString()
                            if (id == item_id) {
                                Log.e("Error", "Found box to delete in")
                                description = item.child("description").value.toString()
                                name = item.child("name").value.toString()
                                tags = item.child("tags").value.toString()
                                image = item.child("image").value.toString()
                                response.id = id
                                response.name = name
                                response.tags = tags
                                response.image = image
                                response.image = image
                            }
                        }
                    }
                }
            }
            return response


            itemsRef.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val items: DataSnapshot? = task.result
                    if (items != null) {
                        for (item: DataSnapshot in items.children) {
                            id = item.child("id").value.toString()
                            if (id == item_id) {
                                Log.e("Error", "Found box to delete in")
                                description = item.child("description").value.toString()
                                name = item.child("name").value.toString()
                                tags = item.child("tags").value.toString()
                                image = item.child("image").value.toString()
                            }
                        }
                    }
                }
            }
            Log.e("Error", id )
            Log.e("Error", name)
            Log.e("Error", description)
            Log.e("Error", tags)
            return ItemModel(id, name, description, tags, image)
        }
    }

}