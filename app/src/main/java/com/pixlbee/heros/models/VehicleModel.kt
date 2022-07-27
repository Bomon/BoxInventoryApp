package com.pixlbee.heros.models

import java.io.Serializable

class VehicleModel(
    var id: String,
    var name: String,
    var callname: String,
    var description: String,
    var parking_spot: String,
    var image: String
): Serializable