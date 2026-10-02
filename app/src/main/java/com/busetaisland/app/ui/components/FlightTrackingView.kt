package com.busetaisland.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.busetaisland.app.ui.theme.BusDarkSurfaceVariant
import com.busetaisland.app.ui.theme.BusSubtleBorder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.busetaisland.app.data.model.TrackedFlightInfo

/**
 * Flight Tracking Module for Dynamic Island
 * Displays:
 * - Callsign & Registration (Reg)
 * - Origin & Destination IATA Codes & City
 * - Flight Progress Bar with dynamic plane indicator
 * - STD & Actual Departure (ATD/ETD)
 * - STA & Estimated/Actual Arrival (ETA/ATA)
 */
@Composable
fun FlightTrackingProgressRow(
    flight: TrackedFlightInfo,
    modifier: Modifier = Modifier
) {
    val progress = flight.progressPercent.coerceIn(0.0f, 1.0f)
    val progressPctInt = (progress * 100).toInt()

    // Smooth pulse for flight status indicator
    val infiniteTransition = rememberInfiniteTransition(label = "FlightPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    val statusBgColor = when {
        flight.statusText.contains("Landed", ignoreCase = true) -> Color(0xFF334155)
        flight.statusText.contains("Delayed", ignoreCase = true) -> Color(0x33FF5722)
        else -> Color(0x3300E5FF)
    }
    val statusTextColor = when {
        flight.statusText.contains("Landed", ignoreCase = true) -> Color(0xFF94A3B8)
        flight.statusText.contains("Delayed", ignoreCase = true) -> Color(0xFFFF7043)
        else -> Color(0xFF00E5FF)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BusDarkSurfaceVariant)
            .border(0.75.dp, BusSubtleBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .testTag("flight_tracking_module")
    ) {
        // Line 1: Flight Identification (Callsign, Flight No, Reg, Status Badge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AirplanemodeActive,
                    contentDescription = "Flight",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier
                        .size(13.dp)
                        .rotate(90f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                // Callsign / Flight Number
                Text(
                    text = flight.flightNumber,
                    color = Color(0xFFF1F5F9),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
                if (flight.callsign.isNotBlank() && flight.callsign != flight.flightNumber) {
                    Text(
                        text = " (${flight.callsign})",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                // Aircraft Registration Badge (e.g. B-LRA)
                Box(
                    modifier = Modifier
                        .background(Color(0x3338BDF8), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = flight.registration,
                        color = Color(0xFF38BDF8),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Status Badge (En Route / Landed / Scheduled)
            Row(
                modifier = Modifier
                    .background(statusBgColor, RoundedCornerShape(100.dp))
                    .padding(horizontal = 6.dp, vertical = 1.5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(statusTextColor.copy(alpha = pulseAlpha), CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = flight.statusText,
                    color = statusTextColor,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Line 2: Origin -> Progress Bar with Airplane -> Destination
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Origin Airport
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = flight.originIata,
                    color = Color(0xFFFFFFFF),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = flight.originCity,
                    color = Color(0xFF94A3B8),
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Flight Progress Bar & Plane Indicator
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.5.dp)
                        .background(Color(0xFF334155), RoundedCornerShape(2.dp))
                ) {
                    // Active Progress Track
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress)
                            .fillMaxHeight()
                            .background(Color(0xFF00E5FF), RoundedCornerShape(2.dp))
                    )
                }

                // Plane Icon at current progress position
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    if (progress > 0.05f) {
                        Spacer(modifier = Modifier.fillMaxWidth(fraction = (progress - 0.05f).coerceAtLeast(0f)))
                    }
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(Color(0xFF0F172A), CircleShape)
                            .border(1.dp, Color(0xFF00E5FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AirplanemodeActive,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier
                                .size(9.dp)
                                .rotate(90f)
                        )
                    }
                }
            }

            // Destination Airport
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = flight.destinationIata,
                    color = Color(0xFFFFFFFF),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = flight.destinationCity,
                    color = Color(0xFF94A3B8),
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Line 3: STD, ATD/ETD on Left | STA, ETA/ATA on Right + Progress %
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Departure Times
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "STD ",
                    color = Color(0xFF64748B),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = flight.stdFormatted,
                    color = Color(0xFFE2E8F0),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = flight.actualDepartureFormatted,
                    color = Color(0xFF38BDF8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Flight Progress Percentage
            Text(
                text = "${progressPctInt}%",
                color = Color(0xFF00E5FF),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            // Arrival Times (Always display ETA instead of Estimated)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "STA ",
                    color = Color(0xFF64748B),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = flight.staFormatted,
                    color = Color(0xFFE2E8F0),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(6.dp))
                val etaText = when {
                    flight.etaFormatted.startsWith("ATA ") -> flight.etaFormatted
                    flight.etaFormatted.startsWith("ETA ") -> flight.etaFormatted
                    flight.etaFormatted.contains("ATA") -> "ATA " + flight.etaFormatted.replace("ATA", "").trim()
                    else -> "ETA " + flight.etaFormatted.replace("Estimated", "").replace("預計", "").trim()
                }
                Text(
                    text = etaText,
                    color = Color(0xFF38BDF8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
