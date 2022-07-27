package com.pixlbee.heros.models

import java.io.Serializable

data class ContentItem(
    val key: String = "",
    val amount: String = "?",
    val id: String = "",
    val invnum: String = "",
    val color: Int = -1
)

class BoxModel(
    var numeric_id: Long,
    var id: String,
    var name: String,
    var description: String,
    var qrcode: String,
    var vehicle: String,
    var location_details: String,
    var image: String,
    var location_image: String,
    var color: Int,
    var status: String,
    var content: ArrayList<ContentItem>
): Serializable