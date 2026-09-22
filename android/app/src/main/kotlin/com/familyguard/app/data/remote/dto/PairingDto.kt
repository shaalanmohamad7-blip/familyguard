package com.familyguard.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PairingRedeemRequest(
    val code: String,
    val platform: String,
    val appVersion: String
)

@Serializable
data class PairingRedeemResponse(
    val deviceToken: String,
    val deviceId: String,
    val familyId: String
)
