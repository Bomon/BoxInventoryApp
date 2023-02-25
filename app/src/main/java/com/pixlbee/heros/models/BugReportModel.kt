package com.pixlbee.heros.models

import java.io.Serializable

data class DeviceInfoModel(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val board: String,
    val hardware: String,
    val version_release: String,
    val version_codename: String,
    val version_incremental: String,
    val version_sdk: String,
    val version_base_os: String,
    val display: String,
    val soc_manufacturer: String,
    val soc_model: String,
    val device: String,
    val host: String,
    val type: String,
    val build_time: String,
)

class BugReportModel(
    var bug_report: String,
    var app_version_name: String,
    var app_version_code: String,
    var timestamp: String,
    var device_info: DeviceInfoModel,
): Serializable