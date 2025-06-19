package com.example.olympus_gym_preview.presentation.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "memberships")
data class MembershipEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Int,
    val verificationCode: String,
    val status: String,
    val expirationDate: Date,
    val firstName: String,
    val lastName: String,
    val lastUpdated: Date = Date()
) {
    fun toDomainModel(): MembershipDomain {
        return MembershipDomain(
            id = id,
            verificationCode = verificationCode,
            status = status,
            expirationDate = expirationDate,
            firstName = firstName,
            lastName = lastName
        )
    }
}

data class MembershipDomain(
    val id: Int,
    val verificationCode: String,
    val status: String,
    val expirationDate: Date,
    val firstName: String,
    val lastName: String
) {
    fun toEntity(): MembershipEntity {
        return MembershipEntity(
            id = id,
            verificationCode = verificationCode,
            status = status,
            expirationDate = expirationDate,
            firstName = firstName,
            lastName = lastName
        )
    }
}