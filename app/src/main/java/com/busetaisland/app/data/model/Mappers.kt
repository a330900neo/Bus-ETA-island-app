package com.busetaisland.app.data.model

fun CtbRouteData.toKmbRouteData(direction: String): KmbRouteData {
    // If direction is "I", we swap origin and destination
    return if (direction == "I") {
        KmbRouteData(
            co = co,
            route = route,
            bound = "I",
            serviceType = "1",
            origEn = destEn,
            origTc = destTc,
            origSc = destSc,
            destEn = origEn,
            destTc = origTc,
            destSc = origSc,
            dataTimestamp = dataTimestamp
        )
    } else {
        KmbRouteData(
            co = co,
            route = route,
            bound = "O",
            serviceType = "1",
            origEn = origEn,
            origTc = origTc,
            origSc = origSc,
            destEn = destEn,
            destTc = destTc,
            destSc = destSc,
            dataTimestamp = dataTimestamp
        )
    }
}

fun CtbRouteStopData.toKmbRouteStopData(): KmbRouteStopData {
    return KmbRouteStopData(
        co = co,
        route = route,
        bound = dir,
        serviceType = "1",
        seq = seq,
        stop = stop,
        dataTimestamp = dataTimestamp
    )
}

fun CtbStopDetail.toKmbStopDetail(): KmbStopDetail {
    return KmbStopDetail(
        stop = stop,
        nameEn = nameEn,
        nameTc = nameTc,
        nameSc = nameSc,
        lat = lat.toString(),
        long = long.toString(),
        dataTimestamp = dataTimestamp
    )
}

fun CtbEtaData.toKmbEtaData(): KmbEtaData {
    return KmbEtaData(
        co = co,
        route = route,
        dir = dir,
        serviceType = "1",
        seq = seq,
        stop = stop,
        destEn = destEn,
        destTc = destTc,
        destSc = destSc,
        etaSeq = etaSeq,
        eta = eta,
        rmkEn = rmkEn,
        rmkTc = rmkTc,
        rmkSc = rmkSc,
        dataTimestamp = dataTimestamp
    )
}

fun GmbEtaEntry.toKmbEtaData(
    route: String,
    bound: String,
    serviceType: String,
    seq: Int,
    stopId: String
): KmbEtaData {
    return KmbEtaData(
        co = "GMB",
        route = route,
        dir = bound,
        serviceType = serviceType,
        seq = seq,
        stop = stopId,
        destEn = "",
        destTc = "",
        destSc = "",
        etaSeq = etaSeq,
        eta = timestamp,
        rmkEn = remarksEn ?: "",
        rmkTc = remarksTc ?: "",
        rmkSc = remarksTc ?: "",
        dataTimestamp = null
    )
}

