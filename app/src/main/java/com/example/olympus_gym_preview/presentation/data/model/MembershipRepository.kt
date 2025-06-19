package com.example.olympus_gym_preview.presentation.data.model

import android.util.Log
import com.example.olympus_gym_preview.presentation.local.db.MembershipDomain
import com.example.olympus_gym_preview.presentation.local.db.MembershipEntity
import com.example.olympus_gym_preview.presentation.network.ApiClient
import com.example.olympus_gym_preview.presentation.parseDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class MembershipRepository(private val membershipDao: MembershipDao, private val apiClient: ApiClient) {
    suspend fun saveMembership(membership: MembershipDomain) {
        membershipDao.insertMembership(membership.toEntity())
    }

    fun getAllMemberships(): Flow<List<MembershipDomain>> {
        return membershipDao.getAllMemberships().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    suspend fun refreshMembershipFromBackend() {
        val membershipId = membershipDao.getAllMemberships().first().first().id

        Log.d("MembershipRepo", "Refrescando membresía con ID: $membershipId")

        try {
            val response = apiClient.instance.getMemberDetails(membershipId.toString())
            if (response.isSuccessful) {
                response.body()?.let { apiResponse ->
                    // Mapeo manual aquí
                    val entity = MembershipEntity(
                        id = apiResponse.membership.id,
                        verificationCode = apiResponse.membership.verification_code,
                        status = apiResponse.membership.status,
                        expirationDate = parseDate(apiResponse.membership.expiration_date),
                        firstName = apiResponse.membership.user.firstName,
                        lastName = apiResponse.membership.user.lastName
                    )

                    Log.d("MembershipRepo", "Actualizando membresía: $entity")

                    membershipDao.insertMembership(entity)
                }
            }
        } catch (e: Exception) {
            Log.e("MembershipRepo", "Error al actualizar membresía", e)
        }
    }

    suspend fun clearMemberships() {
        membershipDao.clearAll()
    }
}