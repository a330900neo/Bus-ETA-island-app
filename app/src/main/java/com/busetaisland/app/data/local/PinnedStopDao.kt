package com.busetaisland.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PinnedStopDao {

    @Query("SELECT * FROM pinned_stops ORDER BY createdAt DESC")
    fun getAllPinnedStops(): Flow<List<PinnedStopEntity>>

    @Query("SELECT * FROM pinned_stops WHERE isActive = 1 LIMIT 1")
    fun getActiveTrackedStop(): Flow<PinnedStopEntity?>

    @Query("SELECT * FROM pinned_stops WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveTrackedStopSync(): PinnedStopEntity?

    @Query("SELECT * FROM pinned_stops WHERE stopId = :stopId AND route = :route AND bound = :bound LIMIT 1")
    suspend fun findPinnedStop(stopId: String, route: String, bound: String): PinnedStopEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPinnedStop(stop: PinnedStopEntity): Long

    @Update
    suspend fun updatePinnedStop(stop: PinnedStopEntity)

    @Query("UPDATE pinned_stops SET isActive = 0")
    suspend fun clearActiveFlag()

    @Query("UPDATE pinned_stops SET isActive = 1 WHERE id = :id")
    suspend fun setActiveById(id: Long)

    @Query("UPDATE pinned_stops SET radiusMeters = :radius WHERE id = :id")
    suspend fun updateRadius(id: Long, radius: Float)

    @Query("UPDATE pinned_stops SET isGeofenceEnabled = :enabled WHERE id = :id")
    suspend fun updateGeofenceEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE pinned_stops SET triggerType = :triggerType, areaId = :areaId WHERE id = :id")
    suspend fun updateTriggerMode(id: Long, triggerType: String, areaId: Long?)

    @Query("SELECT * FROM pinned_stops WHERE areaId = :areaId")
    suspend fun getStopsByAreaId(areaId: Long): List<PinnedStopEntity>

    @Delete
    suspend fun deletePinnedStop(stop: PinnedStopEntity)

    @Query("DELETE FROM pinned_stops WHERE id = :id")
    suspend fun deleteById(id: Long)
}
