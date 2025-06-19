package com.example.olympus_gym_preview.presentation.data.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.olympus_gym_preview.presentation.local.db.MembershipEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MembershipDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembership(membership: MembershipEntity)

    @Query("SELECT * FROM memberships LIMIT 1")
    fun getAllMemberships(): Flow<List<MembershipEntity>>


    @Query("DELETE FROM memberships")
    suspend fun clearAll()
}