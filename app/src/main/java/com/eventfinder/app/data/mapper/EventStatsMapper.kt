package com.eventfinder.app.data.mapper

import com.eventfinder.app.data.model.EventStatsDto
import com.eventfinder.app.domain.model.EventStats
import com.google.firebase.Timestamp

/**
 * Maps between Firestore EventStats DTOs and Domain EventStats models
 */
object EventStatsMapper {

    /**
     * Maps EventStatsDto (from Firestore) to Domain EventStats model
     */
    fun toDomain(dto: EventStatsDto): EventStats {
        return EventStats(
            eventId = dto.eventId,
            totalTickets = dto.totalTickets,
            checkedInCount = dto.checkedInCount,
            reservedCount = dto.reservedCount,
            cancelledCount = dto.cancelledCount,
            totalRevenue = dto.totalRevenue,
            currency = dto.currency,
            lastUpdated = dto.lastUpdated?.toDate()?.time ?: System.currentTimeMillis()
        )
    }

    /**
     * Maps Domain EventStats model to EventStatsDto (for Firestore)
     */
    fun toDto(stats: EventStats): EventStatsDto {
        return EventStatsDto(
            eventId = stats.eventId,
            totalTickets = stats.totalTickets,
            checkedInCount = stats.checkedInCount,
            reservedCount = stats.reservedCount,
            cancelledCount = stats.cancelledCount,
            totalRevenue = stats.totalRevenue,
            currency = stats.currency,
            lastUpdated = Timestamp(
                stats.lastUpdated / 1000,
                ((stats.lastUpdated % 1000) * 1000000).toInt()
            )
        )
    }
}
