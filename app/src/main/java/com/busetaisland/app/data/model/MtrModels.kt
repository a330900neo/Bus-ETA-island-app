package com.busetaisland.app.data.model

import androidx.compose.ui.graphics.Color
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MtrApiResponse(
    @Json(name = "status") val status: Int? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "sys_time") val sysTime: String? = null,
    @Json(name = "curr_time") val currTime: String? = null,
    @Json(name = "isdelay") val isdelay: String? = null,
    @Json(name = "data") val data: Map<String, MtrScheduleStationData>? = null
)

@JsonClass(generateAdapter = true)
data class MtrScheduleStationData(
    @Json(name = "curr_time") val currTime: String? = null,
    @Json(name = "sys_time") val sysTime: String? = null,
    @Json(name = "UP") val up: List<MtrTrainItem>? = null,
    @Json(name = "DOWN") val down: List<MtrTrainItem>? = null
)

@JsonClass(generateAdapter = true)
data class MtrTrainItem(
    @Json(name = "ttnt") val ttnt: String? = null, // time to next train (minutes)
    @Json(name = "valid") val valid: String? = null, // "Y" or "N"
    @Json(name = "plat") val plat: String? = null, // platform number e.g. "1"
    @Json(name = "time") val time: String? = null, // "yyyy-MM-dd HH:mm:ss"
    @Json(name = "source") val source: String? = null,
    @Json(name = "dest") val dest: String? = null, // station code e.g. "POA", "CEN"
    @Json(name = "seq") val seq: String? = null,
    @Json(name = "timetype") val timetype: String? = null,
    @Json(name = "route") val route: String? = null
)

@JsonClass(generateAdapter = true)
data class MtrLrtApiResponse(
    @Json(name = "status") val status: Int? = null,
    @Json(name = "system_time") val systemTime: String? = null,
    @Json(name = "platform_list") val platformList: List<MtrLrtPlatform>? = null
)

@JsonClass(generateAdapter = true)
data class MtrLrtPlatform(
    @Json(name = "platform_id") val platformId: Int? = null,
    @Json(name = "route_list") val routeList: List<MtrLrtRouteItem>? = null
)

@JsonClass(generateAdapter = true)
data class MtrLrtRouteItem(
    @Json(name = "route_no") val routeNo: String? = null,
    @Json(name = "dest_tc") val destTc: String? = null,
    @Json(name = "dest_en") val destEn: String? = null,
    @Json(name = "time_en") val timeEn: String? = null,
    @Json(name = "time_tc") val timeTc: String? = null,
    @Json(name = "arrival_departure") val arrivalDeparture: String? = null
)

data class MtrStationInfo(
    val code: String,
    val nameEn: String,
    val nameTc: String,
    val lat: Double,
    val lng: Double,
    val lrtStationId: Int? = null
)

data class MtrLineDefinition(
    val lineCode: String,
    val nameEn: String,
    val nameTc: String,
    val colorHex: String,
    val color: Color,
    val isLightContent: Boolean = true,
    val upDestinationEn: String,
    val upDestinationTc: String,
    val downDestinationEn: String,
    val downDestinationTc: String,
    val stations: List<MtrStationInfo>
)

object MtrRegistry {

    // Colors as specified in user request:
    // Tsuen Wan line: #ED1D24 (Red)
    // Kwun Tong line: #00AB4E (Green)
    // Island line: #007DC5 (Dark Blue)
    // East Rail line: #53B7E8 (Light Blue)
    // Tung Chung line: #F7943E (Orange)
    // Tuen Ma line: #923011 (Brown)
    // Tseung Kwan O line: #7D499D (Purple)
    // South Island line: #BAC429 (Light Green/Olive)
    // Airport Express: #00888A (Teal)
    // Disneyland Resort line: #F173AC (Pink)
    // Light Rail: #D3A809 (Goldenrod)

    val COLOR_TWL = Color(0xFFED1D24)
    val COLOR_KTL = Color(0xFF00AB4E)
    val COLOR_ISL = Color(0xFF007DC5)
    val COLOR_EAL = Color(0xFF53B7E8)
    val COLOR_TCL = Color(0xFFF7943E)
    val COLOR_TML = Color(0xFF923011)
    val COLOR_TKL = Color(0xFF7D499D)
    val COLOR_SIL = Color(0xFFBAC429)
    val COLOR_AEL = Color(0xFF00888A)
    val COLOR_DRL = Color(0xFFF173AC)
    val COLOR_LRT = Color(0xFFD3A809)

    val LINES: List<MtrLineDefinition> = listOf(
        MtrLineDefinition(
            lineCode = "TWL",
            nameEn = "Tsuen Wan Line",
            nameTc = "荃灣綫",
            colorHex = "#ED1D24",
            color = COLOR_TWL,
            isLightContent = true,
            upDestinationEn = "Tsuen Wan",
            upDestinationTc = "荃灣",
            downDestinationEn = "Central",
            downDestinationTc = "中環",
            stations = listOf(
                MtrStationInfo("CEN", "Central", "中環", 22.2818, 114.1585),
                MtrStationInfo("ADM", "Admiralty", "金鐘", 22.2789, 114.1650),
                MtrStationInfo("TST", "Tsim Sha Tsui", "尖沙咀", 22.2974, 114.1722),
                MtrStationInfo("JOR", "Jordan", "佐敦", 22.3049, 114.1717),
                MtrStationInfo("YMT", "Yau Ma Tei", "油麻地", 22.3130, 114.1706),
                MtrStationInfo("MOK", "Mong Kok", "旺角", 22.3193, 114.1694),
                MtrStationInfo("PRE", "Prince Edward", "太子", 22.3246, 114.1683),
                MtrStationInfo("SSP", "Sham Shui Po", "深水埗", 22.3308, 114.1622),
                MtrStationInfo("CSW", "Cheung Sha Wan", "長沙灣", 22.3355, 114.1565),
                MtrStationInfo("LCK", "Lai Chi Kok", "茘枝角", 22.3375, 114.1487),
                MtrStationInfo("MEF", "Mei Foo", "美孚", 22.3370, 114.1408),
                MtrStationInfo("LAK", "Lai King", "茘景", 22.3486, 114.1264),
                MtrStationInfo("KWF", "Kwai Fong", "葵芳", 22.3568, 114.1281),
                MtrStationInfo("KWH", "Kwai Hing", "葵興", 22.3631, 114.1312),
                MtrStationInfo("TWH", "Tai Wo Hau", "大窩口", 22.3705, 114.1245),
                MtrStationInfo("TSW", "Tsuen Wan", "荃灣", 22.3736, 114.1178)
            )
        ),
        MtrLineDefinition(
            lineCode = "KTL",
            nameEn = "Kwun Tong Line",
            nameTc = "觀塘綫",
            colorHex = "#00AB4E",
            color = COLOR_KTL,
            isLightContent = true,
            upDestinationEn = "Tiu Keng Leng",
            upDestinationTc = "調景嶺",
            downDestinationEn = "Whampoa",
            downDestinationTc = "黃埔",
            stations = listOf(
                MtrStationInfo("WHA", "Whampoa", "黃埔", 22.3051, 114.1884),
                MtrStationInfo("HOM", "Ho Man Tin", "何文田", 22.3096, 114.1824),
                MtrStationInfo("YMT", "Yau Ma Tei", "油麻地", 22.3130, 114.1706),
                MtrStationInfo("MOK", "Mong Kok", "旺角", 22.3193, 114.1694),
                MtrStationInfo("PRE", "Prince Edward", "太子", 22.3246, 114.1683),
                MtrStationInfo("SKM", "Shek Kip Mei", "石硤尾", 22.3323, 114.1688),
                MtrStationInfo("KOT", "Kowloon Tong", "九龍塘", 22.3372, 114.1764),
                MtrStationInfo("LOF", "Lok Fu", "樂富", 22.3385, 114.1873),
                MtrStationInfo("WTS", "Wong Tai Sin", "黃大仙", 22.3418, 114.1932),
                MtrStationInfo("DIH", "Diamond Hill", "鑽石山", 22.3402, 114.2014),
                MtrStationInfo("CHH", "Choi Hung", "彩虹", 22.3347, 114.2089),
                MtrStationInfo("KOB", "Kowloon Bay", "九龍灣", 22.3235, 114.2140),
                MtrStationInfo("NTK", "Ngau Tau Kok", "牛頭角", 22.3155, 114.2191),
                MtrStationInfo("KWT", "Kwun Tong", "觀塘", 22.3120, 114.2257),
                MtrStationInfo("LAT", "Lam Tin", "藍田", 22.3069, 114.2332),
                MtrStationInfo("YAT", "Yau Tong", "油塘", 22.2978, 114.2374),
                MtrStationInfo("TIK", "Tiu Keng Leng", "調景嶺", 22.3045, 114.2527)
            )
        ),
        MtrLineDefinition(
            lineCode = "ISL",
            nameEn = "Island Line",
            nameTc = "港島綫",
            colorHex = "#007DC5",
            color = COLOR_ISL,
            isLightContent = true,
            upDestinationEn = "Chai Wan",
            upDestinationTc = "柴灣",
            downDestinationEn = "Kennedy Town",
            downDestinationTc = "堅尼地城",
            stations = listOf(
                MtrStationInfo("KET", "Kennedy Town", "堅尼地城", 22.2810, 114.1288),
                MtrStationInfo("HKU", "HKU", "香港大學", 22.2842, 114.1354),
                MtrStationInfo("SYP", "Sai Ying Pun", "西營盤", 22.2863, 114.1432),
                MtrStationInfo("SHW", "Sheung Wan", "上環", 22.2868, 114.1522),
                MtrStationInfo("CEN", "Central", "中環", 22.2818, 114.1585),
                MtrStationInfo("ADM", "Admiralty", "金鐘", 22.2789, 114.1650),
                MtrStationInfo("WAC", "Wan Chai", "灣仔", 22.2777, 114.1731),
                MtrStationInfo("CAB", "Causeway Bay", "銅鑼灣", 22.2801, 114.1848),
                MtrStationInfo("TIH", "Tin Hau", "天后", 22.2825, 114.1923),
                MtrStationInfo("FOH", "Fortress Hill", "炮台山", 22.2884, 114.1942),
                MtrStationInfo("NOP", "North Point", "北角", 22.2912, 114.1997),
                MtrStationInfo("QUB", "Quarry Bay", "鰂魚涌", 22.2880, 114.2096),
                MtrStationInfo("TAK", "Tai Koo", "太古", 22.2846, 114.2162),
                MtrStationInfo("SWH", "Sai Wan Ho", "西灣河", 22.2822, 114.2222),
                MtrStationInfo("SKW", "Shau Kei Wan", "筲箕灣", 22.2796, 114.2291),
                MtrStationInfo("HFC", "Heng Fa Chuen", "杏花邨", 22.2758, 114.2403),
                MtrStationInfo("CHW", "Chai Wan", "柴灣", 22.2642, 114.2372)
            )
        ),
        MtrLineDefinition(
            lineCode = "EAL",
            nameEn = "East Rail Line",
            nameTc = "東鐵綫",
            colorHex = "#53B7E8",
            color = COLOR_EAL,
            isLightContent = false,
            upDestinationEn = "Lo Wu / Lok Ma Chau",
            upDestinationTc = "羅湖 / 落馬洲",
            downDestinationEn = "Admiralty",
            downDestinationTc = "金鐘",
            stations = listOf(
                MtrStationInfo("ADM", "Admiralty", "金鐘", 22.2789, 114.1650),
                MtrStationInfo("EXC", "Exhibition Centre", "會展", 22.2820, 114.1758),
                MtrStationInfo("HUH", "Hung Hom", "紅磡", 22.3029, 114.1818),
                MtrStationInfo("MKK", "Mong Kok East", "旺角東", 22.3225, 114.1724),
                MtrStationInfo("KOT", "Kowloon Tong", "九龍塘", 22.3372, 114.1764),
                MtrStationInfo("TAW", "Tai Wai", "大圍", 22.3727, 114.1786),
                MtrStationInfo("SHT", "Sha Tin", "沙田", 22.3837, 114.1873),
                MtrStationInfo("FOT", "Fo Tan", "火炭", 22.3957, 114.1952),
                MtrStationInfo("RAC", "Racecourse", "馬場", 22.4005, 114.2045),
                MtrStationInfo("UNI", "University", "大學", 22.4132, 114.2104),
                MtrStationInfo("TAP", "Tai Po Market", "大埔墟", 22.4447, 114.1706),
                MtrStationInfo("TWO", "Tai Wo", "太和", 22.4507, 114.1607),
                MtrStationInfo("FAN", "Fanling", "粉嶺", 22.4925, 114.1384),
                MtrStationInfo("SHS", "Sheung Shui", "上水", 22.5012, 114.1278),
                MtrStationInfo("LOW", "Lo Wu", "羅湖", 22.5285, 114.1130),
                MtrStationInfo("LMC", "Lok Ma Chau", "落馬洲", 22.5152, 114.0658)
            )
        ),
        MtrLineDefinition(
            lineCode = "TCL",
            nameEn = "Tung Chung Line",
            nameTc = "東涌綫",
            colorHex = "#F7943E",
            color = COLOR_TCL,
            isLightContent = true,
            upDestinationEn = "Tung Chung",
            upDestinationTc = "東涌",
            downDestinationEn = "Hong Kong",
            downDestinationTc = "香港",
            stations = listOf(
                MtrStationInfo("HOK", "Hong Kong", "香港", 22.2840, 114.1581),
                MtrStationInfo("KOW", "Kowloon", "九龍", 22.3045, 114.1615),
                MtrStationInfo("OLY", "Olympic", "奧運", 22.3178, 114.1601),
                MtrStationInfo("NAC", "Nam Cheong", "南昌", 22.3259, 114.1542),
                MtrStationInfo("LAK", "Lai King", "茘景", 22.3486, 114.1264),
                MtrStationInfo("TSY", "Tsing Yi", "青衣", 22.3585, 114.1070),
                MtrStationInfo("SUN", "Sunny Bay", "欣澳", 22.3308, 114.0289),
                MtrStationInfo("TUC", "Tung Chung", "東涌", 22.2890, 113.9412)
            )
        ),
        MtrLineDefinition(
            lineCode = "TML",
            nameEn = "Tuen Ma Line",
            nameTc = "屯馬綫",
            colorHex = "#923011",
            color = COLOR_TML,
            isLightContent = true,
            upDestinationEn = "Tuen Mun",
            upDestinationTc = "屯門",
            downDestinationEn = "Wu Kai Sha",
            downDestinationTc = "烏溪沙",
            stations = listOf(
                MtrStationInfo("WKS", "Wu Kai Sha", "烏溪沙", 22.4287, 114.2442),
                MtrStationInfo("MOS", "Ma On Shan", "馬鞍山", 22.4243, 114.2319),
                MtrStationInfo("HEO", "Heng On", "恆安", 22.4173, 114.2281),
                MtrStationInfo("TSH", "Tai Shui Hang", "大水坑", 22.4089, 114.2238),
                MtrStationInfo("SHM", "Shek Mun", "石門", 22.3879, 114.2084),
                MtrStationInfo("CIO", "City One", "第一城", 22.3846, 114.2039),
                MtrStationInfo("STW", "Sha Tin Wai", "沙田圍", 22.3770, 114.1952),
                MtrStationInfo("CKT", "Che Kung Temple", "車公廟", 22.3748, 114.1862),
                MtrStationInfo("TAW", "Tai Wai", "大圍", 22.3727, 114.1786),
                MtrStationInfo("HIK", "Hin Keng", "顯徑", 22.3627, 114.1718),
                MtrStationInfo("DIH", "Diamond Hill", "鑽石山", 22.3402, 114.2014),
                MtrStationInfo("KAT", "Kai Tak", "啟德", 22.3308, 114.2001),
                MtrStationInfo("SUW", "Sung Wong Toi", "宋皇臺", 22.3276, 114.1895),
                MtrStationInfo("TKW", "To Kwa Wan", "土瓜灣", 22.3175, 114.1884),
                MtrStationInfo("HOM", "Ho Man Tin", "何文田", 22.3096, 114.1824),
                MtrStationInfo("HUH", "Hung Hom", "紅磡", 22.3029, 114.1818),
                MtrStationInfo("ETS", "East Tsim Sha Tsui", "尖東", 22.2952, 114.1739),
                MtrStationInfo("AUS", "Austin", "柯士甸", 22.3040, 114.1666),
                MtrStationInfo("NAC", "Nam Cheong", "南昌", 22.3259, 114.1542),
                MtrStationInfo("MEF", "Mei Foo", "美孚", 22.3370, 114.1408),
                MtrStationInfo("TWW", "Tsuen Wan West", "荃灣西", 22.3685, 114.1112),
                MtrStationInfo("KSR", "Kam Sheung Road", "錦上路", 22.4344, 114.0620),
                MtrStationInfo("YUL", "Yuen Long", "元朗", 22.4452, 114.0357),
                MtrStationInfo("LOP", "Long Ping", "朗屏", 22.4475, 114.0253),
                MtrStationInfo("TIS", "Tin Shui Wai", "天水圍", 22.4489, 114.0040),
                MtrStationInfo("SIH", "Siu Hong", "兆康", 22.4118, 113.9781),
                MtrStationInfo("TUM", "Tuen Mun", "屯門", 22.3952, 113.9734)
            )
        ),
        MtrLineDefinition(
            lineCode = "TKL",
            nameEn = "Tseung Kwan O Line",
            nameTc = "將軍澳綫",
            colorHex = "#7D499D",
            color = COLOR_TKL,
            isLightContent = true,
            upDestinationEn = "Po Lam / LOHAS Park",
            upDestinationTc = "寶琳 / 康城",
            downDestinationEn = "North Point",
            downDestinationTc = "北角",
            stations = listOf(
                MtrStationInfo("NOP", "North Point", "北角", 22.2912, 114.1997),
                MtrStationInfo("QUB", "Quarry Bay", "鰂魚涌", 22.2880, 114.2096),
                MtrStationInfo("YAT", "Yau Tong", "油塘", 22.2978, 114.2374),
                MtrStationInfo("TIK", "Tiu Keng Leng", "調景嶺", 22.3045, 114.2527),
                MtrStationInfo("TKO", "Tseung Kwan O", "將軍澳", 22.3079, 114.2600),
                MtrStationInfo("HAH", "Hang Hau", "坑口", 22.3155, 114.2644),
                MtrStationInfo("POA", "Po Lam", "寶琳", 22.3228, 114.2575),
                MtrStationInfo("LHP", "LOHAS Park", "康城", 22.2963, 114.2704)
            )
        ),
        MtrLineDefinition(
            lineCode = "SIL",
            nameEn = "South Island Line",
            nameTc = "南港島綫",
            colorHex = "#BAC429",
            color = COLOR_SIL,
            isLightContent = false,
            upDestinationEn = "South Horizons",
            upDestinationTc = "海怡半島",
            downDestinationEn = "Admiralty",
            downDestinationTc = "金鐘",
            stations = listOf(
                MtrStationInfo("ADM", "Admiralty", "金鐘", 22.2789, 114.1650),
                MtrStationInfo("OCP", "Ocean Park", "海洋公園", 22.2478, 114.1706),
                MtrStationInfo("WCH", "Wong Chuk Hang", "黃竹坑", 22.2482, 114.1681),
                MtrStationInfo("LET", "Lei Tung", "利東", 22.2425, 114.1565),
                MtrStationInfo("SOH", "South Horizons", "海怡半島", 22.2429, 114.1487)
            )
        ),
        MtrLineDefinition(
            lineCode = "AEL",
            nameEn = "Airport Express",
            nameTc = "機場快綫",
            colorHex = "#00888A",
            color = COLOR_AEL,
            isLightContent = true,
            upDestinationEn = "AsiaWorld-Expo / Airport",
            upDestinationTc = "博覽館 / 機場",
            downDestinationEn = "Hong Kong",
            downDestinationTc = "香港",
            stations = listOf(
                MtrStationInfo("HOK", "Hong Kong", "香港", 22.2840, 114.1581),
                MtrStationInfo("KOW", "Kowloon", "九龍", 22.3045, 114.1615),
                MtrStationInfo("TSY", "Tsing Yi", "青衣", 22.3585, 114.1070),
                MtrStationInfo("AIR", "Airport", "機場", 22.3160, 113.9360),
                MtrStationInfo("AWE", "AsiaWorld-Expo", "博覽館", 22.3207, 113.9416)
            )
        ),
        MtrLineDefinition(
            lineCode = "DRL",
            nameEn = "Disneyland Resort Line",
            nameTc = "迪士尼綫",
            colorHex = "#F173AC",
            color = COLOR_DRL,
            isLightContent = true,
            upDestinationEn = "Disneyland Resort",
            upDestinationTc = "迪士尼",
            downDestinationEn = "Sunny Bay",
            downDestinationTc = "欣澳",
            stations = listOf(
                MtrStationInfo("SUN", "Sunny Bay", "欣澳", 22.3308, 114.0289),
                MtrStationInfo("DIS", "Disneyland Resort", "迪士尼", 22.3155, 114.0450)
            )
        ),
        MtrLineDefinition(
            lineCode = "LRT",
            nameEn = "Light Rail",
            nameTc = "輕鐵",
            colorHex = "#D3A809",
            color = COLOR_LRT,
            isLightContent = false,
            upDestinationEn = "Tuen Mun / Yuen Long",
            upDestinationTc = "屯門 / 元朗",
            downDestinationEn = "Tin Shui Wai / Ferry Pier",
            downDestinationTc = "天水圍 / 屯門碼頭",
            stations = listOf(
                MtrStationInfo("1", "Tuen Mun Ferry Pier", "屯門碼頭", 22.3725, 113.9660, 1),
                MtrStationInfo("10", "Town Centre", "市中心", 22.3920, 113.9760, 10),
                MtrStationInfo("100", "Siu Hong", "兆康", 22.4118, 113.9781, 100),
                MtrStationInfo("600", "Yuen Long", "元朗", 22.4452, 114.0357, 600),
                MtrStationInfo("430", "Tin Shui Wai", "天水圍", 22.4489, 114.0040, 430),
                MtrStationInfo("75", "On Ting", "安定", 22.3888, 113.9762, 75),
                MtrStationInfo("80", "Goodview Garden", "豐景園", 22.3831, 113.9734, 80),
                MtrStationInfo("200", "Ming Kum", "鳴琴", 22.3995, 113.9680, 200),
                MtrStationInfo("220", "Leung King", "良景", 22.4080, 113.9640, 220),
                MtrStationInfo("230", "Tin King", "田景", 22.4105, 113.9662, 230),
                MtrStationInfo("500", "Wetland Park", "濕地公園", 22.4660, 114.0050, 500),
                MtrStationInfo("510", "Tin Yat", "天逸", 22.4690, 114.0000, 510)
            )
        )
    )

    private val lineMap = LINES.associateBy { it.lineCode }
    private val lineByNameTc = LINES.associateBy { it.nameTc }
    private val lineByNameEn = LINES.associateBy { it.nameEn.lowercase() }

    fun findLine(identifier: String): MtrLineDefinition? {
        val clean = identifier.trim()
        return lineMap[clean.uppercase()]
            ?: lineByNameTc[clean]
            ?: lineByNameEn[clean.lowercase()]
            ?: LINES.firstOrNull { it.nameTc.contains(clean) || it.nameEn.contains(clean, ignoreCase = true) }
    }

    /**
     * Resolves the route badge color for any route string and company.
     */
    fun getRouteColor(co: String, route: String): Color {
        if (co.equals("MTR", ignoreCase = true)) {
            val line = findLine(route)
            if (line != null) return line.color
        }
        val line = findLine(route)
        if (line != null) return line.color

        return when (co.uppercase()) {
            "GMB" -> Color(0xFF00C853)
            "CTB" -> Color(0xFFFFD600)
            "NWFB" -> Color(0xFFFF6D00)
            else -> Color(0xFFD0BCFF)
        }
    }

    /**
     * Determines text color on top of route badge.
     */
    fun getRouteTextColor(co: String, route: String): Color {
        if (co.equals("MTR", ignoreCase = true)) {
            val line = findLine(route)
            if (line != null) {
                return if (line.isLightContent) Color.White else Color.Black
            }
        }
        val line = findLine(route)
        if (line != null) {
            return if (line.isLightContent) Color.White else Color.Black
        }
        return when (co.uppercase()) {
            "GMB" -> Color.Black
            "CTB" -> Color.Black
            "NWFB" -> Color.White
            else -> Color(0xFF381E72)
        }
    }

    /**
     * Get Station Chinese Name from station code across all lines
     */
    fun getStationNameTc(code: String): String {
        for (line in LINES) {
            val st = line.stations.find { it.code.equals(code, ignoreCase = true) }
            if (st != null) return st.nameTc
        }
        return code
    }

    /**
     * Get Station English Name from station code across all lines
     */
    fun getStationNameEn(code: String): String {
        for (line in LINES) {
            val st = line.stations.find { it.code.equals(code, ignoreCase = true) }
            if (st != null) return st.nameEn
        }
        return code
    }
}
