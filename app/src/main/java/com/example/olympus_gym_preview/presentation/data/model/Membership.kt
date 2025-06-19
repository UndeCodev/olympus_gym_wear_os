package com.example.olympusgym.domain.model

data class Membership(
    val id: String,
    val userName: String,
    val status: String, // "active", "expired", "pending"
    val expirationDate: String, // Formato: "dd/MM/yyyy"
    val daysRemaining: Int
)