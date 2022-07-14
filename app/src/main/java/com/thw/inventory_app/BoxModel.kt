package com.thw.inventory_app

import java.io.Serializable

data class ContentItem(
    val key: String = "",
    val amount: String = "?",
    val id: String = "",
    val invnum: String = "",
    val color: Int = R.color.default_item_color
)

class BoxModel(
    var id: String,
    var name: String,
    var description: String,
    var qrcode: String,
    var location: String,
    var image: String,
    var location_img: String,
    var notes: String,
    var color: Int,
    var status: String,
    var content: ArrayList<ContentItem>): Serializable  {

}