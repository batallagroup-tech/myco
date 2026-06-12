package com.batallagroup.myco.domain.model

data class MycoNode(
    val deviceAddress: String,
    val nodeId: String,
    val rssi: Int,
    val lastSeen: Long = System.currentTimeMillis(),
    val messagesRelayed: Int = 0
)
