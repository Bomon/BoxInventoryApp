package com.pixlbee.heros.models

import android.util.ArrayMap
import java.io.Serializable

class ItemAvailabilityModel(
    var item: ItemModel,
    var taken: ArrayMap<String, Int>
): Serializable