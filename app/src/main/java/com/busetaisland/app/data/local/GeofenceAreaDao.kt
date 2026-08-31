package com.busetaisland.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GeofenceAreaDao {

    @Query("SELECT * FROM geofence_areas ORDER BY createdAt DESC")
    fun getAllAreas(): Flow<List<GeofenceAreaEntity>>

    @Query("SELECT * FROM geofence_areas WHERE id = :id LIMIT 1")
    fun getAreaById(id: Long): Flow<GeofenceAreaEntity?>

    @Query("SELECT * FROM geofence_areas WHERE id = :id LIMIT 1")
    suspend fun getAreaByIdSync(id: Long): GeofenceAreaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArea(area: GeofenceAreaEntity): Long

    @Update
    suspend fun updateArea(area: GeofenceAreaEntity)

    @Delete
    suspend fun deleteArea(area: GeofenceAreaEntity)

    @Query("DELETE FROM geofence_areas WHERE id = :id")
    suspend fun deleteAreaById(id: Long)
}
