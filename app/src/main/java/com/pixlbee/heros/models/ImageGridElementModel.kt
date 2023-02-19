package com.pixlbee.heros.models

import java.io.Serializable

enum class GridElementType {
    IMAGE, ADD_BTN
}

class ImageGridElementModel(
    var image: String,
    var grid_element_type: GridElementType): Serializable