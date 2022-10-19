package com.pixlbee.heros.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class BoxItemModel(
    var numeric_id: String,
    var item_id: String,
    var item_amount: String,
    var item_amount_taken: String,
    var item_invnum: String,
    var item_name: String,
    var item_color: Int,
    var item_image: String,
    var item_description: String,
    var item_tags: String,
    var item_compartment: String
): Parcelable