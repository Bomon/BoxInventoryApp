package com.pixlbee.heros.utility

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.ArrayMap
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.allViews
import androidx.preference.PreferenceManager
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.pixlbee.heros.R
import com.pixlbee.heros.models.BoxModel
import com.pixlbee.heros.models.ContentItem
import com.pixlbee.heros.models.ItemModel
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutionException


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


        fun readBoxModelFromDataSnapshot(context: Context?, box: DataSnapshot): BoxModel {
            val image = box.child("image").value.toString()
            val location_image = box.child("location_image").value.toString()
            val location = box.child("location").value.toString()
            val id = box.child("id").value.toString()
            val numeric_id = box.child("numeric_id").value.toString().toLong()
            val name = box.child("name").value.toString()
            val description = box.child("description").value.toString()
            val qrcode = box.child("qrcode").value.toString()
            val notes = box.child("notes").value.toString()

            var color: Int? = box.child("color").value.toString().toIntOrNull()
            if (color == null) {
                color = -1
                if (context != null){
                    color = ContextCompat.getColor(context, R.color.default_box_color)
                }
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
                    itemColor = -1
                    if (context != null){
                        itemColor = ContextCompat.getColor(context, R.color.default_item_color)
                    }
                }
                contentList.add(ContentItem(c.key.toString(), itemAmount, itemId, itemInvNum, itemColor))
            }
            return BoxModel(numeric_id, id, name, description, qrcode, location, image, location_image, notes, color, status, contentList)
        }


        fun readItemModelFromDataSnapshot(context: Context?, item: DataSnapshot): ItemModel {
            val id = item.child("id").value.toString()
            val name = item.child("name").value.toString()
            val description = item.child("description").value.toString()
            val image = item.child("image").value.toString()
            val tags = item.child("tags").value.toString()
            return ItemModel(id, name, description, tags, image)
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
            return idList
        }


        fun checkHasWritePermission(context: Context?, showToast: Boolean = true): Boolean {
            if (context != null) {
                val sharedPreferences = context.getSharedPreferences("AppPreferences", MODE_PRIVATE)
                val hasWritePermission = sharedPreferences.getBoolean("write_permission", false)
                if (hasWritePermission) {
                    return true
                }
            }
            if (showToast)
                Toast.makeText(context,
                    context?.resources?.getString(R.string.error_no_write_permission) ?: "NO_WRITE_PERMISSION", Toast.LENGTH_SHORT).show()
            return false
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


        fun getAllItemNames(): ArrayMap<String, String> {
            var itemsMap: ArrayMap<String, String> = ArrayMap<String, String>()
            val itemsRef = FirebaseDatabase.getInstance().reference
            val task: Task<DataSnapshot> = itemsRef.child("items").get()

            itemsRef.get().addOnCompleteListener { task ->

            }

            try {
                Tasks.await<DataSnapshot>(task)
                if (task.isSuccessful) {
                    val items: DataSnapshot? = task.result
                    if (items != null) {
                        for (item: DataSnapshot in items.children) {
                            var itemName = item.child("name").value.toString()
                            var itemId = item.child("id").value.toString()
                            itemsMap[itemId] = itemName
                        }
                    }
                }
                var success = task.isSuccessful()
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            return itemsMap
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


        fun getButtonForSortSetting(btnText: String?): Int {
            when (btnText) {
                "order_by_id" -> { return R.id.radioButtonOrderId }
                "order_by_name" -> { return R.id.radioButtonOrderName }
                "order_by_location" -> { return R.id.radioButtonOrderLocation }
                "order_by_status" -> { return R.id.radioButtonOrderStatus }
                "order_by_color" -> { return R.id.radioButtonOrderColor }
                "order_by_latest" -> { return R.id.radioButtonOrderLatest }
            }
            return R.id.radioButtonOrderId
        }

        fun getSortSettingForButton(btnId: Int): String {
            when (btnId) {
                R.id.radioButtonOrderId -> { return "order_by_id" }
                R.id.radioButtonOrderName -> { return "order_by_name" }
                R.id.radioButtonOrderLocation -> { return "order_by_location" }
                R.id.radioButtonOrderStatus -> { return "order_by_status" }
                R.id.radioButtonOrderColor -> { return "order_by_color" }
                R.id.radioButtonOrderLatest -> { return "order_by_latest" }
            }
            return "order_by_id"
        }

        fun getButtonForSortSettingAscDesc(btnText: String?): Int {
            when (btnText) {
                "order_ascending" -> { return R.id.radioButtonOrderAscending }
                "order_descending" -> { return R.id.radioButtonOrderDescending }
            }
            return R.id.radioButtonOrderAscending
        }

        fun getSortSettingAscDescForButton(btnId: Int): String {
            when (btnId) {
                R.id.radioButtonOrderAscending -> { return "order_ascending" }
                R.id.radioButtonOrderDescending -> { return "order_descending" }
            }
            return "order_ascending"
        }


        fun getItemNameForId(id: String): String {
            var itemName = "?"
            var done = false
            val itemsRef = FirebaseDatabase.getInstance().reference.child("items")
            itemsRef.get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val items: DataSnapshot? = task.result
                    if (items != null) {
                        for (item: DataSnapshot in items.children) {
                            if (item.child("id").value.toString() == id){
                                itemName = item.child("name").value.toString()
                                done = true
                                break
                            }
                        }
                    }
                }
                done = true
            }
            while (!done){

            }
            return itemName
        }


        fun getNextColor(context: Context, currentColor: Int): Int{
            var colorCircle = ArrayList<Int>()
            colorCircle.add(context.resources.getColor(R.color.thw_blue))
            colorCircle.add(context.resources.getColor(R.color.md_green_500))
            colorCircle.add(context.resources.getColor(R.color.md_yellow_500))
            colorCircle.add(context.resources.getColor(R.color.md_red_500))

            var colorPos = colorCircle.indexOf(currentColor)
            if (colorPos == -1){
                return colorCircle[0]
            } else if (colorPos == colorCircle.size - 1)
                return colorCircle[0]
            return colorCircle[colorPos + 1]
        }


        fun getPreviousColor(context: Context, currentColor: Int): Int {
            var colorCircle = ArrayList<Int>()
            colorCircle.add(context.resources.getColor(R.color.thw_blue))
            colorCircle.add(context.resources.getColor(R.color.md_green_500))
            colorCircle.add(context.resources.getColor(R.color.md_yellow_500))
            colorCircle.add(context.resources.getColor(R.color.md_orange_500))
            colorCircle.add(context.resources.getColor(R.color.md_red_500))

            var colorPos = colorCircle.indexOf(currentColor)
            if (colorPos == -1) {
                return colorCircle[0]
            } else if (colorPos == 0)
                return colorCircle[colorCircle.size - 1]
            return colorCircle[colorPos - 1]
        }
    }

}