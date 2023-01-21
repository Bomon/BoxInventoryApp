package com.pixlbee.heros.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class ItemMoveModel(
    var item: BoxItemModel,
    var src_box_id: String,
    var src_compartment: String,
    var target_box_id: String,
    var target_compartment: String,
    var target_box_key: String,
): Parcelable