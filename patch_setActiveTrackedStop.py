import re

with open('app/src/main/java/com/example/data/repository/BusRepository.kt', 'r') as f:
    content = f.read()

# setActiveTrackedStop
setActiveTrackedStop_new = """    suspend fun setActiveTrackedStop(
        co: String,
        route: String,
        bound: String,
        serviceType: String,
        stopId: String,
        stopNameEn: String,
        stopNameTc: String,
        destEn: String,
        destTc: String,
        seq: Int,
        stopLat: Double,
        stopLng: Double,
        radiusMeters: Float = 300f,
        triggerType: String = "RADIUS",
        areaId: Long? = null,
        isGeofenceEnabled: Boolean = true
    ) {
        pinnedDao.clearActiveFlag()

        val existing = pinnedDao.findPinnedStop(stopId, route, bound)
        val entity = if (existing != null) {
            existing.copy(
                co = co,
                isActive = true,
                radiusMeters = radiusMeters,
                triggerType = triggerType,
                areaId = areaId,
                isGeofenceEnabled = isGeofenceEnabled,
                stopLat = if (stopLat != 0.0) stopLat else existing.stopLat,
                stopLng = if (stopLng != 0.0) stopLng else existing.stopLng
            )
        } else {
            PinnedStopEntity(
                co = co,
                route = route,
                bound = bound,
                serviceType = serviceType,
                stopId = stopId,
                stopNameEn = stopNameEn,
                stopNameTc = stopNameTc,
                destEn = destEn,
                destTc = destTc,
                seq = seq,
                stopLat = stopLat,
                stopLng = stopLng,
                radiusMeters = radiusMeters,
                triggerType = triggerType,
                areaId = areaId,
                isGeofenceEnabled = isGeofenceEnabled,
                isActive = true
            )
        }
        val id = pinnedDao.insertPinnedStop(entity)
        pinnedDao.setActiveById(if (entity.id != 0L) entity.id else id)

        val updated = PinnedStopEntity(
            id = if (entity.id != 0L) entity.id else id,
            co = co,
            route = route,
            bound = bound,
            serviceType = serviceType,
            stopId = stopId,
            stopNameEn = stopNameEn,
            stopNameTc = stopNameTc,
            destEn = destEn,
            destTc = destTc,
            seq = seq,
            stopLat = stopLat,
            stopLng = stopLng,
            radiusMeters = radiusMeters,
            triggerType = triggerType,
            areaId = areaId,
            isGeofenceEnabled = isGeofenceEnabled,
            isActive = true
        )
        refreshTrackedBus(updated)
    }"""
content = re.sub(r'    suspend fun setActiveTrackedStop\(.*?refreshTrackedBus\(updated\)\n    \}', setActiveTrackedStop_new, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/data/repository/BusRepository.kt', 'w') as f:
    f.write(content)

