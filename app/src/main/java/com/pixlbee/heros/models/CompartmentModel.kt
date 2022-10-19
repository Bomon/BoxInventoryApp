package com.pixlbee.heros.models

import java.io.Serializable

class CompartmentModel(
    var name: String,
    var content: ArrayList<BoxItemModel>,
    var is_expanded: Boolean): Serializable