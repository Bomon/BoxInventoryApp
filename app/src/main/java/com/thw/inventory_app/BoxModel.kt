package com.thw.inventory_app

import android.widget.ListView
import java.io.Serializable

data class ContentItem(
    val key: String = "",
    val amount: String = "?",
    val id: String = "",
    val invnum: String = ""
)

class BoxModel(
    var id: String,
    var name: String,
    var qrcode: String,
    var location: String,
    var img: String,
    var location_img: String,
    var content: ArrayList<ContentItem>): Serializable  {

}