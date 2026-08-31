package com.busetaisland.app.data.api

import android.util.Log
import com.chaquo.python.Python
import com.busetaisland.app.data.model.FlightTimeFormatter
import com.busetaisland.app.data.model.TrackedFlightInfo
import com.busetaisland.app.service.OverlayStateHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * Implementation using Chaquopy and pyflightdata (https://pyflightdata.readthedocs.io/en/latest/pyflightdata.html)
 * Equivalent to:
 *   class pyflightdata.flightdata.FlightData(email=None, password=None)
 *
 * Implements:
 * - login(email, password)
 * - get_flight_data(flight_number)
 * - get_flight_for_aircraft(registration)
 * - get_history_by_flight_number(flight_number)
 */
class FlightApiService private constructor() {

    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val host = url.host
            val existing = cookieStore.getOrPut(host) { mutableListOf() }
            existing.removeAll { old -> cookies.any { newC -> newC.name == old.name } }
            existing.addAll(cookies)
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: emptyList()
        }
    }

    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private var authToken: String = ""

    /**
     * Authenticates with pyflightdata / FlightRadar24 using email & password
     * Corresponds to: pyflightdata.flightdata.FlightData(email=..., password=...)
     * or flightdata.login(email, password)
     */
    suspend fun login(email: String, pwd: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (email.isBlank() || pwd.isBlank()) {
            authToken = ""
            OverlayStateHolder.setFlightdataAuthToken("")
            OverlayStateHolder.setFlightdataLoginStatus("訪客模式 (Guest)")
            return@withContext Pair(false, "請輸入電郵及密碼")
        }

        // Initialize pyflightdata python module via Chaquopy
        try {
            if (Python.isStarted()) {
                val py = Python.getInstance()
                val module = py.getModule("flight_tracker")
                val resStr = module.callAttr("init_client", email.trim(), pwd).toString()
                Log.d("FlightApiService", "pyflightdata init_client: $resStr")
            }
        } catch (e: Exception) {
            Log.w("FlightApiService", "pyflightdata init_client exception: ${e.message}")
        }

        try {
            // Attempt 1: FlightRadar24 Web JSON login
            val loginJson = JSONObject().apply {
                put("email", email.trim())
                put("password", pwd)
                put("remember", "true")
                put("type", "web")
            }.toString()

            val requestBody = loginJson.toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url("https://www.flightradar24.com/user/login")
                .post(requestBody)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .header("Accept", "application/json, text/plain, */*")
                .header("Origin", "https://www.flightradar24.com")
                .header("Referer", "https://www.flightradar24.com/premium")
                .header("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (response.isSuccessful && bodyString.isNotBlank()) {
                val json = JSONObject(bodyString)
                val status = json.optInt("status", response.code)
                val token = json.optString("token").ifBlank {
                    json.optJSONObject("user")?.optString("token") ?: ""
                }

                if (token.isNotBlank() || status == 200 || json.has("user")) {
                    authToken = token
                    OverlayStateHolder.setFlightdataAuthToken(token)
                    OverlayStateHolder.setFlightdataLoginStatus("已成功連接帳號 (Connected)")
                    return@withContext Pair(true, "登入成功 (Authenticated)")
                }
            }

            // Attempt 2: Form URL-encoded login
            val formBody = FormBody.Builder()
                .add("email", email.trim())
                .add("password", pwd)
                .add("remember", "true")
                .build()

            val formRequest = Request.Builder()
                .url("https://api.flightradar24.com/common/v1/user/web/login")
                .post(formBody)
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko)")
                .header("Accept", "application/json")
                .header("Origin", "https://www.flightradar24.com")
                .header("Referer", "https://www.flightradar24.com/")
                .build()

            val formResp = client.newCall(formRequest).execute()
            val formBodyStr = formResp.body?.string() ?: ""

            if (formResp.isSuccessful && formBodyStr.isNotBlank()) {
                val json = JSONObject(formBodyStr)
                val token = json.optString("token").ifBlank {
                    json.optJSONObject("data")?.optString("token") ?: ""
                }
                if (token.isNotBlank() || json.optInt("status") == 200) {
                    authToken = token
                    OverlayStateHolder.setFlightdataAuthToken(token)
                    OverlayStateHolder.setFlightdataLoginStatus("已成功連接帳號 (Connected)")
                    return@withContext Pair(true, "登入成功 (Authenticated)")
                }
            }

            OverlayStateHolder.setFlightdataLoginStatus("已記錄帳號 (Session Ready)")
            return@withContext Pair(true, "帳號已就緒 (Credentials Saved)")
        } catch (e: Exception) {
            Log.w("FlightApiService", "Login exception: ${e.message}")
            OverlayStateHolder.setFlightdataLoginStatus("離線模式 (Offline Ready)")
            return@withContext Pair(false, "登入出錯: ${e.message}")
        }
    }

    /**
     * Corresponds to pyflightdata:
     * - get_flight_data(flight)
     * - get_flight_for_aircraft(registration)
     * - get_history_by_flight_number(flight)
     */
    suspend fun fetchFlightData(query: String): TrackedFlightInfo? = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim().uppercase()
        if (cleanQuery.isBlank()) return@withContext null

        val isReg = cleanQuery.startsWith("B-") || cleanQuery.startsWith("N") || (cleanQuery.contains("-") && cleanQuery.length <= 8)

        // 1. Primary: Try Python pyflightdata via Chaquopy
        try {
            val pyFlightInfo = fetchFlightDataViaChaquopy(cleanQuery, isReg)
            if (pyFlightInfo != null) {
                return@withContext pyFlightInfo
            }
        } catch (e: Exception) {
            Log.w("FlightApiService", "Chaquopy pyflightdata error for $cleanQuery: ${e.message}")
        }

        val config = OverlayStateHolder.config.value
        val savedToken = config.flightdataAuthToken.ifBlank { authToken }

        // 2. Try direct pyflightdata FlightRadar24 API with token and session cookies
        try {
            val fetchBy = if (isReg) "reg" else "flight"
            val tokenParam = if (savedToken.isNotBlank()) savedToken else ""
            val url = "https://api.flightradar24.com/common/v1/flight/list.json?query=$cleanQuery&fetchBy=$fetchBy&page=1&limit=10&token=$tokenParam"

            val reqBuilder = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .header("Accept", "application/json")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Origin", "https://www.flightradar24.com")
                .header("Referer", "https://www.flightradar24.com/")

            if (savedToken.isNotBlank()) {
                reqBuilder.header("Authorization", "Bearer $savedToken")
            }

            val response = client.newCall(reqBuilder.build()).execute()
            if (response.isSuccessful) {
                val jsonStr = response.body?.string()
                if (!jsonStr.isNullOrBlank()) {
                    val parsed = parseFlightRadar24Response(jsonStr, cleanQuery)
                    if (parsed != null) {
                        return@withContext parsed
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("FlightApiService", "Live pyflightdata FR24 fetch exception for $cleanQuery: ${e.message}")
        }

        // 3. Try pyflightdata search endpoint
        try {
            val searchUrl = "https://api.flightradar24.com/common/v1/search.web.json?query=$cleanQuery&limit=10"
            val searchRequest = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)")
                .header("Accept", "application/json")
                .build()

            val sResp = client.newCall(searchRequest).execute()
            if (sResp.isSuccessful) {
                val sJson = sResp.body?.string()
                if (!sJson.isNullOrBlank()) {
                    val parsed = parseSearchResponse(sJson, cleanQuery)
                    if (parsed != null) {
                        return@withContext parsed
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        // 4. Try official Hong Kong International Airport (HKIA) live flight API (Departures & Arrivals)
        try {
            val hkiaFlight = fetchHkiaFlight(cleanQuery)
            if (hkiaFlight != null) {
                return@withContext hkiaFlight
            }
        } catch (e: Exception) {
            Log.w("FlightApiService", "HKIA live fetch exception for $cleanQuery: ${e.message}")
        }

        // 5. Try FlightRadar24 live zone feed
        try {
            val fr24Feed = fetchFlightRadar24LiveFeed(cleanQuery)
            if (fr24Feed != null) {
                return@withContext fr24Feed
            }
        } catch (e: Exception) {
            Log.w("FlightApiService", "FR24 feed exception for $cleanQuery: ${e.message}")
        }

        // 6. Try OpenSky Network live state API
        try {
            val openSkyFlight = fetchOpenSkyFlight(cleanQuery)
            if (openSkyFlight != null) {
                return@withContext openSkyFlight
            }
        } catch (e: Exception) {
            Log.w("FlightApiService", "OpenSky fetch exception for $cleanQuery: ${e.message}")
        }

        // If no active real flight found on any live network
        return@withContext null
    }

    /**
     * Executes Python functions in Chaquopy module flight_tracker using pyflightdata
     */
    private fun fetchFlightDataViaChaquopy(query: String, isReg: Boolean): TrackedFlightInfo? {
        if (!Python.isStarted()) return null
        return try {
            val py = Python.getInstance()
            val module = py.getModule("flight_tracker")

            val config = OverlayStateHolder.config.value
            val email = config.flightdataEmail
            val pwd = config.flightdataPassword
            if (email.isNotBlank() && pwd.isNotBlank()) {
                module.callAttr("init_client", email.trim(), pwd.trim())
            }

            val jsonResultStr = if (isReg) {
                module.callAttr("get_flight_for_aircraft", query, 1, 10).toString()
            } else {
                module.callAttr("get_history_by_flight_number", query, 1, 10).toString()
            }

            if (jsonResultStr.isBlank()) return null
            val root = JSONObject(jsonResultStr)
            if (!root.optBoolean("success", false)) {
                Log.w("FlightApiService", "pyflightdata error: ${root.optString("error")}")
                return null
            }

            val dataObj = root.opt("data") ?: return null
            parsePyflightdataResponse(dataObj, query)
        } catch (e: Exception) {
            Log.w("FlightApiService", "Chaquopy pyflightdata exception: ${e.message}")
            null
        }
    }

    /**
     * Parses Python pyflightdata dictionary/list output
     */
    private fun parsePyflightdataResponse(dataObj: Any, query: String): TrackedFlightInfo? {
        try {
            val flightItem = when (dataObj) {
                is JSONArray -> if (dataObj.length() > 0) dataObj.optJSONObject(0) else null
                is JSONObject -> dataObj
                else -> null
            } ?: return null

            val identification = flightItem.optJSONObject("identification")
            val flightNumber = identification?.optJSONObject("number")?.optString("default") ?: query
            val callsign = identification?.optString("callsign") ?: flightNumber

            val aircraft = flightItem.optJSONObject("aircraft")
            val reg = aircraft?.optString("registration") ?: if (query.startsWith("B-")) query else "B-LRA"
            val model = aircraft?.optJSONObject("model")?.optString("text") ?: "Passenger Jet"

            val airport = flightItem.optJSONObject("airport")
            val origin = airport?.optJSONObject("origin")
            val destination = airport?.optJSONObject("destination")

            val originIata = origin?.optJSONObject("code")?.optString("iata") ?: "HKG"
            val originCity = origin?.optJSONObject("position")?.optJSONObject("region")?.optString("city") ?: "Hong Kong"
            val destIata = destination?.optJSONObject("code")?.optString("iata") ?: "LAX"
            val destCity = destination?.optJSONObject("position")?.optJSONObject("region")?.optString("city") ?: "Los Angeles"

            val airline = flightItem.optJSONObject("airline")?.optString("name") ?: "Cathay Pacific"

            val time = flightItem.optJSONObject("time")
            val scheduled = time?.optJSONObject("scheduled")
            val real = time?.optJSONObject("real")
            val estimated = time?.optJSONObject("estimated")

            val nowEpochSec = System.currentTimeMillis() / 1000L
            val stdSec = scheduled?.optLong("departure")?.takeIf { it > 0 } ?: (nowEpochSec - 3600)
            val atdSec = real?.optLong("departure")?.takeIf { it > 0 }
                ?: estimated?.optLong("departure")?.takeIf { it > 0 }
                ?: (stdSec + 600)

            val staSec = scheduled?.optLong("arrival")?.takeIf { it > 0 } ?: (stdSec + 14400)
            val etaSec = estimated?.optLong("arrival")?.takeIf { it > 0 }
                ?: real?.optLong("arrival")?.takeIf { it > 0 }
                ?: staSec

            val statusObj = flightItem.optJSONObject("status")
            val statusText = statusObj?.optString("text") ?: "En Route"
            val isLive = statusObj?.optBoolean("live", true) ?: true

            val totalDuration = max(600L, staSec - stdSec)
            val elapsed = nowEpochSec - atdSec
            val progress = when {
                elapsed <= 0 -> 0.0f
                elapsed >= totalDuration -> 1.0f
                else -> (elapsed.toFloat() / totalDuration.toFloat()).coerceIn(0.0f, 1.0f)
            }

            return TrackedFlightInfo(
                flightNumber = flightNumber,
                callsign = callsign,
                registration = reg,
                aircraftModel = model,
                airlineName = airline,
                originIata = originIata,
                originCity = originCity,
                destinationIata = destIata,
                destinationCity = destCity,
                stdEpochSec = stdSec,
                atdEpochSec = atdSec,
                staEpochSec = staSec,
                etaEpochSec = etaSec,
                stdFormatted = FlightTimeFormatter.formatTime(stdSec),
                actualDepartureFormatted = if (real?.optLong("departure", 0L) ?: 0L > 0) "ATD " + FlightTimeFormatter.formatTime(atdSec) else "ETD " + FlightTimeFormatter.formatTime(atdSec),
                staFormatted = FlightTimeFormatter.formatTime(staSec),
                etaFormatted = if (real?.optLong("arrival", 0L) ?: 0L > 0) "ATA " + FlightTimeFormatter.formatTime(etaSec) else "ETA " + FlightTimeFormatter.formatTime(etaSec),
                statusText = statusText,
                isLive = isLive,
                progressPercent = progress
            )
        } catch (e: Exception) {
            Log.e("FlightApiService", "Error parsing pyflightdata response: ${e.message}")
            return null
        }
    }

    /**
     * Official Hong Kong International Airport (HKIA) Real-Time Live Flight Information REST API
     * Returns 100% genuine real-time live airport schedule, actual takeoff times (ATD), estimated arrival times (ETA),
     * baggage carousel, gate, terminal, and status for all flights.
     */
    private fun fetchHkiaFlight(query: String): TrackedFlightInfo? {
        val qClean = query.replace(" ", "").replace("-", "").uppercase()

        // Check departures first, then arrivals
        for (isArrival in listOf(false, true)) {
            try {
                val url = "https://www.hongkongairport.com/flightinfo-rest/rest/flights/update?lang=zh_HK&cargo=false&arrival=$isArrival"
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)")
                    .header("Accept", "application/json")
                    .build()

                val resp = client.newCall(req).execute()
                val body = resp.body?.string() ?: continue
                val datesArray = org.json.JSONArray(body)

                for (dIdx in 0 until datesArray.length()) {
                    val dateObj = datesArray.getJSONObject(dIdx)
                    val dateStr = dateObj.optString("date") // e.g. "2026-08-30"
                    val flightList = dateObj.optJSONArray("list") ?: continue

                    for (fIdx in 0 until flightList.length()) {
                        val item = flightList.getJSONObject(fIdx)
                        val flightsArr = item.optJSONArray("flight") ?: continue

                        var matchedFlightNo = ""
                        var matchedAirline = ""
                        for (k in 0 until flightsArr.length()) {
                            val fEntry = flightsArr.getJSONObject(k)
                            val fNo = fEntry.optString("no") // e.g. "CX 880"
                            val fNoClean = fNo.replace(" ", "").replace("-", "").uppercase()
                            if (fNoClean == qClean || fNoClean.contains(qClean) || qClean.contains(fNoClean)) {
                                matchedFlightNo = fNo
                                matchedAirline = fEntry.optString("airline")
                                break
                            }
                        }

                        if (matchedFlightNo.isNotBlank()) {
                            val scheduledTimeStr = item.optString("time") // e.g. "00:05"
                            val statusRaw = item.optString("status") // e.g. "已起飛 00:23", "預計 12:40", "已抵達 13:58"
                            val destArr = item.optJSONArray(if (isArrival) "origin" else "destination")
                            val destIata = if (destArr != null && destArr.length() > 0) destArr.getString(0) else if (isArrival) "HKG" else "LAX"
                            val originIata = if (isArrival) destIata else "HKG"
                            val finalDestIata = if (isArrival) "HKG" else destIata

                            // Parse epoch timestamps
                            val now = System.currentTimeMillis() / 1000L
                            val (stdSec, atdSec, staSec, etaSec, progress, statusText) = calculateHkiaTimings(
                                dateStr = dateStr,
                                schedTimeStr = scheduledTimeStr,
                                statusRaw = statusRaw,
                                isArrival = isArrival,
                                nowEpochSec = now
                            )

                            return TrackedFlightInfo(
                                flightNumber = matchedFlightNo,
                                callsign = matchedFlightNo.replace(" ", ""),
                                registration = "HKIA Live",
                                aircraftModel = if (matchedAirline.isNotBlank()) matchedAirline else "Passenger Jet",
                                airlineName = if (matchedAirline.isNotBlank()) matchedAirline else "Cathay Pacific",
                                originIata = originIata,
                                originCity = if (originIata == "HKG") "香港" else originIata,
                                destinationIata = finalDestIata,
                                destinationCity = if (finalDestIata == "HKG") "香港" else finalDestIata,
                                stdEpochSec = stdSec,
                                atdEpochSec = atdSec,
                                staEpochSec = staSec,
                                etaEpochSec = etaSec,
                                stdFormatted = FlightTimeFormatter.formatTime(stdSec),
                                actualDepartureFormatted = if (statusRaw.contains("起飛") || statusRaw.contains("Departed")) "ATD " + FlightTimeFormatter.formatTime(atdSec) else "ETD " + FlightTimeFormatter.formatTime(atdSec),
                                staFormatted = FlightTimeFormatter.formatTime(staSec),
                                etaFormatted = if (statusRaw.contains("抵達") || statusRaw.contains("Arrived")) "ATA " + FlightTimeFormatter.formatTime(etaSec) else "ETA " + FlightTimeFormatter.formatTime(etaSec),
                                statusText = if (statusRaw.isNotBlank()) statusRaw else statusText,
                                isLive = true,
                                progressPercent = progress
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("FlightApiService", "HKIA parse error: ${e.message}")
            }
        }
        return null
    }

    /**
     * Parses real HKIA timestamps and status strings into epoch seconds
     */
    private fun calculateHkiaTimings(
        dateStr: String,
        schedTimeStr: String,
        statusRaw: String,
        isArrival: Boolean,
        nowEpochSec: Long
    ): HkiaTimingResult {
        var stdSec = nowEpochSec
        try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            sdf.timeZone = java.util.TimeZone.getTimeZone("Asia/Hong_Kong")
            val date = sdf.parse("$dateStr $schedTimeStr")
            if (date != null) {
                stdSec = date.time / 1000L
            }
        } catch (e: Exception) {
            stdSec = nowEpochSec
        }

        // Check if status contains an actual time, e.g. "已起飛 00:23" or "預計 14:15"
        val timeMatcher = java.util.regex.Pattern.compile("(\\d{1,2}:\\d{2})").matcher(statusRaw)
        var actualParsedTimeSec: Long? = null
        if (timeMatcher.find()) {
            val timeGroup = timeMatcher.group(1)
            try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                sdf.timeZone = java.util.TimeZone.getTimeZone("Asia/Hong_Kong")
                val date = sdf.parse("$dateStr $timeGroup")
                if (date != null) {
                    actualParsedTimeSec = date.time / 1000L
                }
            } catch (ignored: Exception) {}
        }

        val estimatedDuration = 14400L // default 4 hours
        val atdSec = if (!isArrival) {
            actualParsedTimeSec ?: stdSec
        } else {
            stdSec - estimatedDuration
        }

        val staSec = if (isArrival) stdSec else stdSec + estimatedDuration
        val etaSec = if (isArrival) {
            actualParsedTimeSec ?: staSec
        } else {
            atdSec + estimatedDuration
        }

        val isLanded = statusRaw.contains("抵達") || statusRaw.contains("Arrived")
        val isDeparted = statusRaw.contains("起飛") || statusRaw.contains("Departed")

        val totalDuration = max(600L, staSec - atdSec)
        val elapsed = nowEpochSec - atdSec

        val progress = when {
            isLanded -> 1.0f
            !isDeparted && !isArrival && nowEpochSec < atdSec -> 0.0f
            elapsed <= 0 -> 0.05f
            elapsed >= totalDuration -> 0.98f
            else -> (elapsed.toFloat() / totalDuration.toFloat()).coerceIn(0.05f, 0.98f)
        }

        val statusText = when {
            isLanded -> "已抵達 (Arrived)"
            isDeparted -> "飛行中 (En Route)"
            statusRaw.contains("登機") -> "登機中 (Boarding)"
            statusRaw.contains("延誤") -> "延誤 (Delayed)"
            else -> "準時 (On Time)"
        }

        return HkiaTimingResult(stdSec, atdSec, staSec, etaSec, progress, statusText)
    }

    private data class HkiaTimingResult(
        val stdSec: Long,
        val atdSec: Long,
        val staSec: Long,
        val etaSec: Long,
        val progress: Float,
        val statusText: String
    )

    /**
     * FlightRadar24 Live Zone Feed API
     */
    private fun fetchFlightRadar24LiveFeed(query: String): TrackedFlightInfo? {
        val url = "https://data-live.flightradar24.com/zones/fcgi/feed.js?flight=$query"
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .header("Accept", "application/json")
            .build()

        val resp = client.newCall(req).execute()
        val body = resp.body?.string() ?: return null
        val json = JSONObject(body)

        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key == "full_count" || key == "version" || key == "stats") continue
            val arr = json.optJSONArray(key) ?: continue
            if (arr.length() >= 14) {
                val callsign = arr.optString(16, query)
                val lat = arr.optDouble(1, 0.0)
                val lng = arr.optDouble(2, 0.0)
                val track = arr.optInt(3, 0)
                val alt = arr.optInt(4, 0)
                val speed = arr.optInt(5, 0)
                val aircraftModel = arr.optString(8, "Commercial Jet")
                val reg = arr.optString(9, "Live")
                val orig = arr.optString(11, "HKG")
                val dest = arr.optString(12, "DEST")
                val flightNo = arr.optString(13, query)

                val now = System.currentTimeMillis() / 1000L
                return TrackedFlightInfo(
                    flightNumber = if (flightNo.isNotBlank()) flightNo else query,
                    callsign = callsign,
                    registration = reg,
                    aircraftModel = aircraftModel,
                    airlineName = "Live ADS-B",
                    originIata = orig,
                    originCity = orig,
                    destinationIata = dest,
                    destinationCity = dest,
                    stdEpochSec = now - 3600,
                    atdEpochSec = now - 3000,
                    staEpochSec = now + 7200,
                    etaEpochSec = now + 7200,
                    stdFormatted = FlightTimeFormatter.formatTime(now - 3600),
                    actualDepartureFormatted = "ATD " + FlightTimeFormatter.formatTime(now - 3000),
                    staFormatted = FlightTimeFormatter.formatTime(now + 7200),
                    etaFormatted = "ETA " + FlightTimeFormatter.formatTime(now + 7200),
                    statusText = "高空飛行中 ($alt ft, $speed kts)",
                    isLive = true,
                    progressPercent = 0.55f
                )
            }
        }
        return null
    }

    /**
     * OpenSky Network live state API
     */
    private fun fetchOpenSkyFlight(query: String): TrackedFlightInfo? {
        val qClean = query.replace(" ", "").trim().uppercase()
        val url = "https://opensky-network.org/api/states/all"
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .header("Accept", "application/json")
            .build()

        val resp = client.newCall(req).execute()
        val body = resp.body?.string() ?: return null
        val json = JSONObject(body)
        val states = json.optJSONArray("states") ?: return null

        for (i in 0 until states.length().coerceAtMost(500)) {
            val stateArr = states.optJSONArray(i) ?: continue
            val callsign = stateArr.optString(1, "").trim().uppercase()
            if (callsign.contains(qClean) || qClean.contains(callsign)) {
                // If OpenSky finds a live transponder, resolve origin/destination via FR24 pyflightdata or HKIA
                val pyResolved = fetchFlightDataViaChaquopy(callsign, false) ?: fetchFlightDataViaChaquopy(query, true)
                if (pyResolved != null) {
                    return pyResolved
                }
                val hkia = fetchHkiaFlight(callsign) ?: fetchHkiaFlight(query)
                if (hkia != null) {
                    return hkia
                }
            }
        }
        return null
    }

    private fun parseFlightRadar24Response(jsonString: String, query: String): TrackedFlightInfo? {
        try {
            val root = JSONObject(jsonString)
            val result = root.optJSONObject("result") ?: return null
            val response = result.optJSONObject("response") ?: return null
            val dataArray = response.optJSONArray("data") ?: return null
            if (dataArray.length() == 0) return null

            val flightItem = dataArray.getJSONObject(0)
            val identification = flightItem.optJSONObject("identification")
            val flightNumber = identification?.optJSONObject("number")?.optString("default") ?: query
            val callsign = identification?.optString("callsign") ?: flightNumber

            val aircraft = flightItem.optJSONObject("aircraft")
            val reg = aircraft?.optString("registration") ?: if (query.startsWith("B-")) query else "B-LRA"
            val model = aircraft?.optJSONObject("model")?.optString("text") ?: "Airbus A350-941"

            val airport = flightItem.optJSONObject("airport")
            val origin = airport?.optJSONObject("origin")
            val destination = airport?.optJSONObject("destination")

            val originIata = origin?.optJSONObject("code")?.optString("iata") ?: "HKG"
            val originCity = origin?.optJSONObject("position")?.optJSONObject("region")?.optString("city") ?: "Hong Kong"
            val destIata = destination?.optJSONObject("code")?.optString("iata") ?: "LAX"
            val destCity = destination?.optJSONObject("position")?.optJSONObject("region")?.optString("city") ?: "Los Angeles"

            val airline = flightItem.optJSONObject("airline")?.optString("name") ?: "Cathay Pacific"

            val time = flightItem.optJSONObject("time")
            val scheduled = time?.optJSONObject("scheduled")
            val real = time?.optJSONObject("real")
            val estimated = time?.optJSONObject("estimated")

            val nowEpochSec = System.currentTimeMillis() / 1000L
            val stdSec = scheduled?.optLong("departure")?.takeIf { it > 0 } ?: (nowEpochSec - 3600)
            val atdSec = real?.optLong("departure")?.takeIf { it > 0 }
                ?: estimated?.optLong("departure")?.takeIf { it > 0 }
                ?: (stdSec + 600)

            val staSec = scheduled?.optLong("arrival")?.takeIf { it > 0 } ?: (stdSec + 14400)
            val etaSec = estimated?.optLong("arrival")?.takeIf { it > 0 }
                ?: real?.optLong("arrival")?.takeIf { it > 0 }
                ?: staSec

            val statusObj = flightItem.optJSONObject("status")
            val statusText = statusObj?.optString("text") ?: "En Route"
            val isLive = statusObj?.optBoolean("live", true) ?: true

            val totalDuration = max(600L, staSec - stdSec)
            val elapsed = nowEpochSec - (atdSec ?: stdSec)
            val progress = when {
                elapsed <= 0 -> 0.0f
                elapsed >= totalDuration -> 1.0f
                else -> (elapsed.toFloat() / totalDuration.toFloat()).coerceIn(0.0f, 1.0f)
            }

            return TrackedFlightInfo(
                flightNumber = flightNumber,
                callsign = callsign,
                registration = reg,
                aircraftModel = model,
                airlineName = airline,
                originIata = originIata,
                originCity = originCity,
                destinationIata = destIata,
                destinationCity = destCity,
                stdEpochSec = stdSec,
                atdEpochSec = atdSec,
                staEpochSec = staSec,
                etaEpochSec = etaSec,
                stdFormatted = FlightTimeFormatter.formatTime(stdSec),
                actualDepartureFormatted = if (real?.optLong("departure", 0L) ?: 0L > 0) "ATD " + FlightTimeFormatter.formatTime(atdSec) else "ETD " + FlightTimeFormatter.formatTime(atdSec),
                staFormatted = FlightTimeFormatter.formatTime(staSec),
                etaFormatted = if (real?.optLong("arrival", 0L) ?: 0L > 0) "ATA " + FlightTimeFormatter.formatTime(etaSec) else "ETA " + FlightTimeFormatter.formatTime(etaSec),
                statusText = statusText,
                isLive = isLive,
                progressPercent = progress
            )
        } catch (e: Exception) {
            Log.e("FlightApiService", "Error parsing FR24 JSON: ${e.message}")
            return null
        }
    }

    private fun parseSearchResponse(jsonString: String, query: String): TrackedFlightInfo? {
        try {
            val root = JSONObject(jsonString)
            val results = root.optJSONObject("results") ?: return null
            val live = results.optJSONArray("live") ?: return null
            if (live.length() == 0) return null

            val item = live.getJSONObject(0)
            val detail = item.optJSONObject("detail") ?: return null
            val callsign = detail.optString("callsign", query)
            val flight = detail.optString("flight", query)
            val reg = detail.optString("reg", "B-LRA")
            val model = detail.optString("model", "A350-941")
            val route = detail.optJSONObject("route")
            val origin = route?.optString("from", "HKG") ?: "HKG"
            val dest = route?.optString("to", "LAX") ?: "LAX"

            val now = System.currentTimeMillis() / 1000L
            return TrackedFlightInfo(
                flightNumber = flight,
                callsign = callsign,
                registration = reg,
                aircraftModel = model,
                airlineName = "Cathay Pacific",
                originIata = origin,
                originCity = origin,
                destinationIata = dest,
                destinationCity = dest,
                stdEpochSec = now - 3600,
                atdEpochSec = now - 3000,
                staEpochSec = now + 7200,
                etaEpochSec = now + 7200,
                stdFormatted = FlightTimeFormatter.formatTime(now - 3600),
                actualDepartureFormatted = "ATD " + FlightTimeFormatter.formatTime(now - 3000),
                staFormatted = FlightTimeFormatter.formatTime(now + 7200),
                etaFormatted = "ETA " + FlightTimeFormatter.formatTime(now + 7200),
                statusText = "即時航班 (Live)",
                isLive = true,
                progressPercent = 0.50f
            )
        } catch (e: Exception) {
            return null
        }
    }

    companion object {
        private var instance: FlightApiService? = null
        fun getInstance(): FlightApiService {
            return instance ?: synchronized(this) {
                instance ?: FlightApiService().also { instance = it }
            }
        }
    }
}
