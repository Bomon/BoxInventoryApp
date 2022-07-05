package com.thw.inventory_app

import android.widget.ListView
import java.io.Serializable

data class ContentItem(
    val key: String = "",
    val amount: String = "?",
    val id: String = "",
    val invnum: String = "",
    val status: String = ""
)

class BoxModel(
    var id: String,
    var name: String,
    var description: String,
    var qrcode: String,
    var location: String,
    var img: String,
    var location_img: String,
    var notes: String,
    var color: String,
    var status: String,
    var content: ArrayList<ContentItem>): Serializable  {

}