package com.eventfinder.app.utils

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Utility object for location-related operations
 */
object LocationUtils {

    /**
     * Calculate distance between two coordinates using Haversine formula
     * @return distance in kilometers
     */
    fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadiusKm = 6371.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadiusKm * c
    }

    /**
     * Format distance to readable string
     * Example: "2.5 km", "500 m"
     */
    fun formatDistance(distanceKm: Double): String {
        return when {
            distanceKm < 0.1 -> "${(distanceKm * 1000).toInt()} m"
            distanceKm < 1 -> "${"%.1f".format(distanceKm * 1000)} m"
            distanceKm < 10 -> "${"%.1f".format(distanceKm)} km"
            else -> "${distanceKm.toInt()} km"
        }
    }

    /**
     * Get short address from full address
     * Example: "Lahore Expo Center, Lahore, Pakistan" -> "Lahore Expo Center"
     */
    fun getShortAddress(address: String?): String {
        if (address.isNullOrBlank()) return "Location not specified"

        val parts = address.split(",")
        return parts.firstOrNull()?.trim() ?: address
    }
}
