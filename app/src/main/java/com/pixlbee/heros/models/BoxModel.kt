package com.pixlbee.heros.models

import androidx.core.content.ContextCompat
import com.pixlbee.heros.R
import java.io.Serializable

data class ContentItem(
    val key: String = "",
    val amount: String = "?",
    val id: String = "",
    val invnum: String = "",
    val color: Int = -1
)

class BoxModel(
    var id: String,
    var name: String,
    var description: String,
    var qrcode: String,
    var location: String,
    var image: String,
    var location_image: String,
    var notes: String,
    var color: Int,
    var status: String,
    var content: ArrayList<ContentItem>): Serializable  {

}