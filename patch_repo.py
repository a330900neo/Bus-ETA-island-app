import re

with open('app/src/main/java/com/example/data/repository/BusRepository.kt', 'r') as f:
    content = f.read()

# Replace loadRoutes
load_routes_new = """    suspend fun loadRoutes(): List<KmbRouteData> {
        if (_routesCache.value.isNotEmpty()) return _routesCache.value
        _isLoadingRoutes.value = true
        val combinedRoutes = mutableListOf<KmbRouteData>()
        try {
            val response = apiService.getAllRoutes()
            response.data?.let { combinedRoutes.addAll(it) }
        } catch (e: Exception) {
            // Log/Fallback
        }
        try {
            val responseCtb = ctbApiService.getAllRoutes("CTB")
            responseCtb.data?.let { list -> 
                combinedRoutes.addAll(list.flatMap { 
                    listOf(it.toKmbRouteData("O"), it.toKmbRouteData("I"))
                })
            }
            val responseNwfb = ctbApiService.getAllRoutes("NWFB")
            responseNwfb.data?.let { list -> 
                combinedRoutes.addAll(list.flatMap { 
                    listOf(it.toKmbRouteData("O"), it.toKmbRouteData("I"))
                })
            }
        } catch (e: Exception) {}
        
        if (combinedRoutes.isNotEmpty()) {
            _routesCache.value = combinedRoutes
            _isLoadingRoutes.value = false
            return combinedRoutes
        }
        val fallback = getFallbackRoutes()
        _routesCache.value = fallback
        _isLoadingRoutes.value = false
        return fallback
    }"""

content = re.sub(r'    suspend fun loadRoutes\(\): List<KmbRouteData> \{.*?(?=    suspend fun getStopsForRoute)', load_routes_new + '\n\n', content, flags=re.DOTALL)

# Replace getStopsForRoute
get_stops_new = """    suspend fun getStopsForRoute(co: String, route: String, bound: String, serviceType: String = "1"): List<Pair<KmbRouteStopData, KmbStopDetail?>> {
        val directionParam = if (bound.equals("O", ignoreCase = true) || bound.equals("outbound", ignoreCase = true)) "outbound" else "inbound"
        val stopsResult = mutableListOf<Pair<KmbRouteStopData, KmbStopDetail?>>()

        try {
            val routeStops = if (co == "CTB" || co == "NWFB") {
                val dir = if (bound.equals("O", ignoreCase = true)) "outbound" else "inbound"
                val res = ctbApiService.getRouteStops(route, dir, co)
                res.data?.map { it.toKmbRouteStopData() }
            } else {
                val res = apiService.getRouteStops(route, directionParam, serviceType)
                res.data
            }
            
            if (!routeStops.isNullOrEmpty()) {
                for (rs in routeStops) {
                    val detail = getStopDetail(co, rs.stop)
                    stopsResult.add(Pair(rs, detail))
                }
                return stopsResult
            }
        } catch (e: Exception) {
            // Fallback
        }

        // Return fallback stops for route
        return getFallbackRouteStops(route, bound)
    }"""
content = re.sub(r'    suspend fun getStopsForRoute\(route: String, bound: String, serviceType: String = "1"\): List<Pair<KmbRouteStopData, KmbStopDetail\?>> \{.*?(?=    suspend fun getStopDetail)', get_stops_new + '\n\n', content, flags=re.DOTALL)

# Replace getStopDetail
get_stop_detail_new = """    suspend fun getStopDetail(co: String, stopId: String): KmbStopDetail? {
        stopDetailCache[stopId]?.let { return it }
        try {
            if (co == "CTB" || co == "NWFB") {
                val response = ctbApiService.getStopDetail(stopId)
                response.data?.toKmbStopDetail()?.let {
                    stopDetailCache[stopId] = it
                    return it
                }
            } else {
                val response = apiService.getStopDetail(stopId)
                response.data?.let {
                    stopDetailCache[stopId] = it
                    return it
                }
            }
        } catch (e: Exception) {
            // Return fallback
        }
        return getFallbackStopDetail(stopId)
    }"""
content = re.sub(r'    suspend fun getStopDetail\(stopId: String\): KmbStopDetail\? \{.*?(?=    suspend fun getEta)', get_stop_detail_new + '\n\n', content, flags=re.DOTALL)


# Replace getEta
get_eta_new = """    suspend fun getEta(co: String, stopId: String, route: String, serviceType: String = "1"): List<FormattedEtaItem> {
        try {
            val list = if (co == "CTB" || co == "NWFB") {
                val res = ctbApiService.getEtaForStopRoute(stopId, route, co)
                res.data?.map { it.toKmbEtaData() }
            } else {
                val res = apiService.getEtaForStopRoute(stopId, route, serviceType)
                res.data
            }
            if (!list.isNullOrEmpty()) {
                val formatted = list
                    .filter { it.route.equals(route, ignoreCase = true) && !it.eta.isNullOrBlank() }
                    .sortedBy { it.etaSeq }
                    .map { parseEtaItem(it) }
                return formatted
            }
        } catch (e: Exception) {
            // API failed or no network
        }
        return emptyList()
    }"""
content = re.sub(r'    suspend fun getEta\(stopId: String, route: String, serviceType: String = "1"\): List<FormattedEtaItem> \{.*?(?=    private fun parseEtaItem)', get_eta_new + '\n\n', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/data/repository/BusRepository.kt', 'w') as f:
    f.write(content)

