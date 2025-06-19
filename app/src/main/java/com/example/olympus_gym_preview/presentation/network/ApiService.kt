package com.example.olympus_gym_preview.presentation.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface OlympusGymApiService {
    @POST("members/verify")
    suspend fun verifyMember(
        @Body request: VerificationRequest
    ): Response<MembershipVerificationResponse> // Cambiado el tipo de retorno

    @GET("members/{id}")
    suspend fun getMemberDetails(@Path("id") id: String): Response<MembershipVerificationResponse>
}

data class VerificationRequest(
    val verification_code: String
)

// Nueva estructura de datos para la respuesta
data class MembershipVerificationResponse(
    val membership: MembershipData
)

data class MembershipData(
    val id: Int,
    val verification_code: String,
    val status: String,
    val expiration_date: String,
    val createdAt: String,
    val user: UserData
)

data class UserData(
    val firstName: String,
    val lastName: String
)