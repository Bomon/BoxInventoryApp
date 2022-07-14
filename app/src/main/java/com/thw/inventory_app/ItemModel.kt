package com.thw.inventory_app

import java.io.Serializable

class ItemModel(
    var id: String,
    var name: String,
    var description: String,
    var tags: String,
    var image: String): Serializable  {

}