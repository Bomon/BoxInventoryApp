package com.pixlbee.heros.models

import java.io.Serializable

class ItemModel(
    var id: String,
    var name: String,
    var description: String,
    var tags: String,
    var image: String,
    var isSelected: Boolean = false): Serializable