package com.pixlbee.heros.utility

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.ArrayMap
import android.util.Base64
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.allViews
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
import com.pixlbee.heros.models.VehicleModel
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.ExecutionException
import kotlin.math.roundToInt


class Utils {

    companion object {
        fun stringToBitMap(encodedString: String?): Bitmap? {
            return try {
                val encodeByte: ByteArray = Base64.decode(encodedString, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.size)
            } catch (e: Exception) {
                e.message
                null
            }
        }


        fun dpToPx(dp: Int, context: Context?): Int {
            var density = context?.resources?.displayMetrics?.density
            if (density == null) {
                density = 1f
            }
            return (dp.toFloat() * density).roundToInt()
        }


        fun getEncoded64ImageStringFromBitmap(bitmap: Bitmap): String {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
            val byteFormat: ByteArray = stream.toByteArray()
            // get the base 64 string
            return Base64.encodeToString(byteFormat, Base64.NO_WRAP)
        }


        fun getCurrentlySelectedOrg(context: Context): String {
            val sharedPreferences: SharedPreferences =
                context.getSharedPreferences("AppPreferences", MODE_PRIVATE)
            return sharedPreferences.getString("selected_organization", "").toString()
        }


        fun readBoxModelFromDataSnapshot(context: Context?, box: DataSnapshot): BoxModel {
            val image = box.child("image").value.toString()
            val locationImage = box.child("location_image").value.toString()
            val locationDetails = box.child("location_details").value.toString()
            val type = box.child("type").value.toString()
            val id = box.child("id").value.toString()
            val numericId = box.child("numeric_id").value.toString()
            val name = box.child("name").value.toString()
            val description = box.child("description").value.toString()
            val qrcode = box.child("qrcode").value.toString()
            val inVehicle = box.child("in_vehicle").value.toString()
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
                var itemAmountTaken = c.child("amount_taken").value.toString()
                if (itemAmountTaken == "null"){
                    itemAmountTaken = "0"
                }
                val itemId = c.child("id").value.toString()
                val itemNumericId = c.child("numeric_id").value.toString()
                val itemInvNum = c.child("invnum").value.toString()
                var itemColor: Int? = c.child("color").value.toString().toIntOrNull()
                if (itemColor == null) {
                    itemColor = -1
                    if (context != null){
                        itemColor = ContextCompat.getColor(context, R.color.default_item_color)
                    }
                }
                val itemCompartment = c.child("compartment").value.toString()
                contentList.add(ContentItem(itemNumericId, c.key.toString(), itemAmount, itemAmountTaken, itemId, itemInvNum, itemColor, itemCompartment))
            }

            val compartments = box.child("compartmentList").value.toString()
            val compartmentList = ArrayList<String>()
            if (compartments != "null") {
                val compartmentList = ArrayList(compartments.split(";"))
            }

            return BoxModel(
                numericId,
                id,
                name,
                type,
                description,
                qrcode,
                inVehicle,
                locationDetails,
                image,
                locationImage,
                color,
                status,
                contentList,
                compartmentList
            )
        }


        fun readItemModelFromDataSnapshot(item: DataSnapshot): ItemModel {
            val id = item.child("id").value.toString()
            val name = item.child("name").value.toString()
            val description = item.child("description").value.toString()
            val image = item.child("image").value.toString()
            val tags = item.child("tags").value.toString()
            return ItemModel(id, name, description, tags, image)
        }


        fun readVehicleModelFromDataSnapshot(item: DataSnapshot): VehicleModel {
            val id = item.child("id").value.toString()
            val name = item.child("name").value.toString()
            val callname = item.child("callname").value.toString()
            val parkingSpot = item.child("parking_spot").value.toString()
            val description = item.child("description").value.toString()
            val image = item.child("image").value.toString()
            return VehicleModel(id, name, callname, description, parkingSpot, image)
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


        fun getAllItemNames(context: Context): ArrayMap<String, String> {
            val itemsMap: ArrayMap<String, String> = ArrayMap<String, String>()
            val itemsRef = FirebaseDatabase.getInstance().reference
            val task: Task<DataSnapshot> = itemsRef.child(getCurrentlySelectedOrg(context)).child("items").get()

            itemsRef.get().addOnCompleteListener { task ->

            }

            try {
                Tasks.await(task)
                if (task.isSuccessful) {
                    val items: DataSnapshot? = task.result
                    if (items != null) {
                        for (item: DataSnapshot in items.children) {
                            val itemName = item.child("name").value.toString()
                            val itemId = item.child("id").value.toString()
                            itemsMap[itemId] = itemName
                        }
                    }
                }
                var success = task.isSuccessful
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
                    chipString += ";" + chip.text.toString().trim()
                }
            }
            return chipString.removePrefix(";")
        }


        fun getButtonForSortSetting(btnText: String?): Int {
            when (btnText) {
                "order_by_id" -> { return R.id.radioButtonOrderId }
                "order_by_name" -> { return R.id.radioButtonOrderName }
                "order_by_vehicle" -> { return R.id.radioButtonOrderVehicle }
                "order_by_status" -> { return R.id.radioButtonOrderStatus }
                "order_by_completeness" -> { return R.id.radioButtonCompleteness }
                "order_by_color" -> { return R.id.radioButtonOrderColor }
                "order_by_latest" -> { return R.id.radioButtonOrderLatest }
                "vehicles_order_by_name" -> { return R.id.radioButtonVehiclesOrderName }
                "vehicles_order_by_callname" -> { return R.id.radioButtonVehiclesOrderCallname }
                "vehicles_order_by_parking_spot" -> { return R.id.radioButtonVehiclesOrderParkingSpot }
            }
            return R.id.radioButtonOrderId
        }

        fun getSortSettingForButton(btnId: Int): String {
            when (btnId) {
                R.id.radioButtonOrderId -> { return "order_by_id" }
                R.id.radioButtonOrderName -> { return "order_by_name" }
                R.id.radioButtonOrderVehicle -> { return "order_by_vehicle" }
                R.id.radioButtonOrderStatus -> { return "order_by_status" }
                R.id.radioButtonCompleteness -> { return "order_by_completeness" }
                R.id.radioButtonOrderColor -> { return "order_by_color" }
                R.id.radioButtonOrderLatest -> { return "order_by_latest" }
                R.id.radioButtonVehiclesOrderName -> { return "vehicles_order_by_name" }
                R.id.radioButtonVehiclesOrderCallname -> { return "vehicles_order_by_callname" }
                R.id.radioButtonVehiclesOrderParkingSpot -> { return "vehicles_order_by_parking_spot" }
            }
            return "order_by_id"
        }

        fun getButtonForSortSettingAscDesc(btnText: String?): Int {
            when (btnText) {
                "order_ascending" -> { return R.id.radioButtonOrderAscending }
                "order_descending" -> { return R.id.radioButtonOrderDescending }
                "vehicles_order_ascending" -> { return R.id.radioButtonVehiclesOrderAscending }
                "vehicles_order_descending" -> { return R.id.radioButtonVehiclesOrderDescending }
            }
            return R.id.radioButtonOrderAscending
        }

        fun getSortSettingAscDescForButton(btnId: Int): String {
            when (btnId) {
                R.id.radioButtonOrderAscending -> { return "order_ascending" }
                R.id.radioButtonOrderDescending -> { return "order_descending" }
                R.id.radioButtonVehiclesOrderAscending -> { return "vehicles_order_ascending" }
                R.id.radioButtonVehiclesOrderDescending -> { return "vehicles_order_descending" }
            }
            return "order_ascending"
        }



        fun replaceUmlauteForSorting(text: String): String{
            var text = text.lowercase(Locale.getDefault()).replace("ä","ae")
            text = text.replace("ö","oe")
            text = text.replace("ü","ue")
            return text
        }


        fun getNextColor(context: Context, currentColor: Int): Int{
            val colorCircle = ArrayList<Int>()
            colorCircle.add(context.resources.getColor(R.color.thw_blue))
            colorCircle.add(context.resources.getColor(R.color.md_green_500))
            colorCircle.add(context.resources.getColor(R.color.md_yellow_500))
            colorCircle.add(context.resources.getColor(R.color.md_red_500))

            val colorPos = colorCircle.indexOf(currentColor)
            if (colorPos == -1){
                return colorCircle[0]
            } else if (colorPos == colorCircle.size - 1)
                return colorCircle[0]
            return colorCircle[colorPos + 1]
        }


        fun getPreviousColor(context: Context, currentColor: Int): Int {
            val colorCircle = ArrayList<Int>()
            colorCircle.add(context.resources.getColor(R.color.thw_blue))
            colorCircle.add(context.resources.getColor(R.color.md_green_500))
            colorCircle.add(context.resources.getColor(R.color.md_yellow_500))
            colorCircle.add(context.resources.getColor(R.color.md_red_500))

            val colorPos = colorCircle.indexOf(currentColor)
            if (colorPos == -1) {
                return colorCircle[0]
            } else if (colorPos == 0)
                return colorCircle[colorCircle.size - 1]
            return colorCircle[colorPos - 1]
        }
    }

}