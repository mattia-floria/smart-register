package com.afloria.smartregister.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val ident: String,
    val pass: String,
    val uid: String
)

@Serializable
data class LoginResponse(
    val ident: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val token: String? = null,
    val release: String? = null,
    val expire: String? = null,
    val errors: List<String>? = null,
    val requestedAction: String? = null,
    val choices: List<LoginChoice>? = null
)

@Serializable
data class LoginChoice(
    val cid: String? = null,
    val ident: String? = null,
    val name: String? = null,
    val school: String? = null
)
