package com.example.data.model

data class ZyronUser(
    val id: String,
    val displayName: String,
    val email: String,
    val photoUrl: String? = null,
    val isGuest: Boolean = false
)
