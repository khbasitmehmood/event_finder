package com.eventfinder.app.data.mapper

import com.eventfinder.app.data.model.EventDto
import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.EventCategory
import com.eventfinder.app.domain.model.EventCancellation
import com.eventfinder.app.domain.model.EventLocation
import com.eventfinder.app.domain.model.EventPostponement
import com.eventfinder.app.domain.model.EventReschedule
import com.eventfinder.app.domain.model.EventState
import com.eventfinder.app.domain.model.EventVisibility
import com.eventfinder.app.domain.model.OrganizerSocialLinks
import com.eventfinder.app.domain.model.RefundStatus
import com.eventfinder.app.domain.model.RescheduleField
import com.eventfinder.app.domain.model.StateChange
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.GeoPoint

/**
 * Maps between Firestore DTOs and Domain Models
 */
object EventMapper {

    /**
     * Maps EventDto (from Firestore) to Domain Event model
     */
    fun toDomain(dto: EventDto): Event {
        return Event(
            id = dto.id,
            eventId = dto.eventId,
            title = dto.title,
            description = dto.description,
            category = dto.category?.let { EventCategory(id = "", name = it) },

            organizerId = dto.organizerId,
            organizerName = dto.organizerName,
            organizerPhotoUrl = dto.organizerPhotoUrl,
            organizerSocialLinks = dto.organizerSocialLinks?.let {
                OrganizerSocialLinks(
                    website = it["website"],
                    facebook = it["facebook"],
                    instagram = it["instagram"],
                    twitter = it["twitter"],
                    youtube = it["youtube"],
                    tiktok = it["tiktok"]
                )
            },

            startTime = dto.startTime?.toDate()?.time ?: 0L,
            endTime = dto.endTime?.toDate()?.time,

            location = dto.location?.let {
                EventLocation(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    geohash = dto.geohash
                )
            } ?: EventLocation(0.0, 0.0),
            address = dto.address,

            maxParticipants = dto.maxParticipants,
            currentParticipantCount = dto.currentParticipantCount,

            isFree = dto.isFree,
            price = dto.price,
            currency = dto.currency,

            imageUrls = dto.imageUrls,
            mainImageUrl = dto.mainImageUrl ?: dto.imageUrls.firstOrNull(),

            tags = dto.tags,
            visibility = safeValueOfVisibility(dto.visibility),
            requiresTicket = dto.requiresTicket,

            createdAt = dto.createdAt?.toDate()?.time ?: 0L,
            updatedAt = dto.updatedAt?.toDate()?.time,

            // Phase 1: State Management
            state = safeValueOfState(dto.state),
            publishedAt = dto.publishedAt?.toDate()?.time,
            completedAt = dto.completedAt?.toDate()?.time,
            stateHistory = dto.stateHistory.mapNotNull { mapToStateChange(it) },

            // Phase 2: Postponement
            postponementHistory = dto.postponementHistory.mapNotNull { mapToPostponement(it) },
            currentPostponement = dto.currentPostponement?.let { mapToPostponement(it) },
            postponementCount = dto.postponementCount,
            maxPostponements = dto.maxPostponements,
            allowPostponement = dto.allowPostponement,

            // Phase 3: Rescheduling
            rescheduleHistory = dto.rescheduleHistory.mapNotNull { mapToReschedule(it) },
            currentReschedule = dto.currentReschedule?.let { mapToReschedule(it) },
            rescheduleCount = dto.rescheduleCount,
            maxReschedules = dto.maxReschedules,
            allowReschedule = dto.allowReschedule,

            // Phase 4: Cancellation
            cancellation = dto.cancellation?.let { mapToCancellation(it) },
            cancelledAt = dto.cancelledAt?.toDate()?.time
        )
    }

    /**
     * Maps Domain Event model to EventDto (for Firestore)
     */
    fun toDto(event: Event): EventDto {
        return EventDto(
            id = event.id,
            eventId = event.eventId,
            title = event.title,
            description = event.description,
            category = event.category?.name,

            organizerId = event.organizerId,
            organizerName = event.organizerName,
            organizerPhotoUrl = event.organizerPhotoUrl,
            organizerSocialLinks = event.organizerSocialLinks?.let {
                mapOf(
                    "website" to it.website,
                    "facebook" to it.facebook,
                    "instagram" to it.instagram,
                    "twitter" to it.twitter,
                    "youtube" to it.youtube,
                    "tiktok" to it.tiktok
                ).filterValues { value -> value != null } as Map<String, String>
            },

            startTime = Timestamp(event.startTime / 1000, ((event.startTime % 1000) * 1000000).toInt()),
            endTime = event.endTime?.let { Timestamp(it / 1000, ((it % 1000) * 1000000).toInt()) },

            location = GeoPoint(event.location.latitude, event.location.longitude),
            geohash = event.location.geohash,
            address = event.address,

            maxParticipants = event.maxParticipants,
            currentParticipantCount = event.currentParticipantCount,

            isFree = event.isFree,
            price = event.price,
            currency = event.currency,

            imageUrls = event.imageUrls,
            mainImageUrl = event.mainImageUrl,

            tags = event.tags,
            visibility = event.visibility.name,
            requiresTicket = event.requiresTicket,

            createdAt = Timestamp(event.createdAt / 1000, ((event.createdAt % 1000) * 1000000).toInt()),
            updatedAt = event.updatedAt?.let { Timestamp(it / 1000, ((it % 1000) * 1000000).toInt()) },

            // Phase 1: State Management
            state = event.state.name,
            publishedAt = event.publishedAt?.let { Timestamp(it / 1000, ((it % 1000) * 1000000).toInt()) },
            completedAt = event.completedAt?.let { Timestamp(it / 1000, ((it % 1000) * 1000000).toInt()) },
            stateHistory = event.stateHistory.map { stateChangeToMap(it) },

            // Phase 2: Postponement
            postponementHistory = event.postponementHistory.map { postponementToMap(it) },
            currentPostponement = event.currentPostponement?.let { postponementToMap(it) },
            postponementCount = event.postponementCount,
            maxPostponements = event.maxPostponements,
            allowPostponement = event.allowPostponement,

            // Phase 3: Rescheduling
            rescheduleHistory = event.rescheduleHistory.map { rescheduleToMap(it) },
            currentReschedule = event.currentReschedule?.let { rescheduleToMap(it) },
            rescheduleCount = event.rescheduleCount,
            maxReschedules = event.maxReschedules,
            allowReschedule = event.allowReschedule,

            // Phase 4: Cancellation
            cancellation = event.cancellation?.let { cancellationToMap(it) },
            cancelledAt = event.cancelledAt?.let { Timestamp(it / 1000, ((it % 1000) * 1000000).toInt()) }
        )
    }

    /**
     * Maps Firestore DocumentSnapshot to Domain Event model
     */
    fun fromFirestore(snapshot: DocumentSnapshot): Event? {
        return try {
            val dto = snapshot.toObject(EventDto::class.java) ?: return null
            toDomain(dto)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Safe valueOf for EventVisibility with fallback to PUBLIC
     */
    private fun safeValueOfVisibility(value: String?): EventVisibility {
        return try {
            value?.let { EventVisibility.valueOf(it) } ?: EventVisibility.PUBLIC
        } catch (e: IllegalArgumentException) {
            EventVisibility.PUBLIC
        }
    }

    /**
     * Safe valueOf for EventState with fallback to SCHEDULED
     * Default to SCHEDULED instead of DRAFT for backward compatibility
     * with existing Firestore events that don't have state field
     */
    private fun safeValueOfState(value: String?): EventState {
        return try {
            value?.let { EventState.valueOf(it) } ?: EventState.SCHEDULED
        } catch (e: IllegalArgumentException) {
            EventState.SCHEDULED
        }
    }

    /**
     * Map Firestore map to StateChange
     */
    private fun mapToStateChange(map: Map<String, Any>): StateChange? {
        return try {
            StateChange(
                fromState = EventState.valueOf(map["fromState"] as? String ?: return null),
                toState = EventState.valueOf(map["toState"] as? String ?: return null),
                changedAt = (map["changedAt"] as? Long) ?: System.currentTimeMillis(),
                changedBy = map["changedBy"] as? String,
                reason = map["reason"] as? String,
                automatic = (map["automatic"] as? Boolean) ?: false
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Convert StateChange to Firestore map
     */
    private fun stateChangeToMap(stateChange: StateChange): Map<String, Any> {
        return buildMap {
            put("fromState", stateChange.fromState.name)
            put("toState", stateChange.toState.name)
            put("changedAt", stateChange.changedAt)
            stateChange.changedBy?.let { put("changedBy", it) }
            stateChange.reason?.let { put("reason", it) }
            put("automatic", stateChange.automatic)
        }
    }

    /**
     * Map Firestore map to EventPostponement
     */
    private fun mapToPostponement(map: Map<String, Any>): EventPostponement? {
        return try {
            EventPostponement(
                originalStartTime = (map["originalStartTime"] as? Long) ?: return null,
                originalEndTime = map["originalEndTime"] as? Long,
                newStartTime = map["newStartTime"] as? Long,
                newEndTime = map["newEndTime"] as? Long,
                reason = (map["reason"] as? String) ?: return null,
                postponedAt = (map["postponedAt"] as? Long) ?: System.currentTimeMillis(),
                postponedBy = (map["postponedBy"] as? String) ?: return null,
                notificationSent = (map["notificationSent"] as? Boolean) ?: false
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Convert EventPostponement to Firestore map
     */
    private fun postponementToMap(postponement: EventPostponement): Map<String, Any> {
        return buildMap {
            put("originalStartTime", postponement.originalStartTime)
            postponement.originalEndTime?.let { put("originalEndTime", it) }
            postponement.newStartTime?.let { put("newStartTime", it) }
            postponement.newEndTime?.let { put("newEndTime", it) }
            put("reason", postponement.reason)
            put("postponedAt", postponement.postponedAt)
            put("postponedBy", postponement.postponedBy)
            put("notificationSent", postponement.notificationSent)
        }
    }

    /**
     * Map Firestore map to EventReschedule
     */
    private fun mapToReschedule(map: Map<String, Any>): EventReschedule? {
        return try {
            val originalLat = map["originalLatitude"] as? Double
            val originalLon = map["originalLongitude"] as? Double
            val newLat = map["newLatitude"] as? Double
            val newLon = map["newLongitude"] as? Double

            EventReschedule(
                originalStartTime = (map["originalStartTime"] as? Long) ?: return null,
                originalEndTime = map["originalEndTime"] as? Long,
                originalLocation = if (originalLat != null && originalLon != null) {
                    EventLocation(originalLat, originalLon)
                } else null,
                originalAddress = map["originalAddress"] as? String,
                newStartTime = (map["newStartTime"] as? Long) ?: return null,
                newEndTime = map["newEndTime"] as? Long,
                newLocation = if (newLat != null && newLon != null) {
                    EventLocation(newLat, newLon)
                } else null,
                newAddress = map["newAddress"] as? String,
                reason = (map["reason"] as? String) ?: return null,
                rescheduledAt = (map["rescheduledAt"] as? Long) ?: System.currentTimeMillis(),
                rescheduledBy = (map["rescheduledBy"] as? String) ?: return null,
                notificationSent = (map["notificationSent"] as? Boolean) ?: false
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Convert EventReschedule to Firestore map
     */
    private fun rescheduleToMap(reschedule: EventReschedule): Map<String, Any> {
        return buildMap {
            put("originalStartTime", reschedule.originalStartTime)
            reschedule.originalEndTime?.let { put("originalEndTime", it) }
            reschedule.originalLocation?.let {
                put("originalLatitude", it.latitude)
                put("originalLongitude", it.longitude)
            }
            reschedule.originalAddress?.let { put("originalAddress", it) }
            put("newStartTime", reschedule.newStartTime)
            reschedule.newEndTime?.let { put("newEndTime", it) }
            reschedule.newLocation?.let {
                put("newLatitude", it.latitude)
                put("newLongitude", it.longitude)
            }
            reschedule.newAddress?.let { put("newAddress", it) }
            put("reason", reschedule.reason)
            put("rescheduledAt", reschedule.rescheduledAt)
            put("rescheduledBy", reschedule.rescheduledBy)
            put("notificationSent", reschedule.notificationSent)
            put("changedFields", reschedule.getChangedFields().map { it.name })
        }
    }

    /**
     * Map Firestore map to EventCancellation
     */
    private fun mapToCancellation(map: Map<String, Any>): EventCancellation? {
        return try {
            EventCancellation(
                cancelledAt = (map["cancelledAt"] as? Long) ?: return null,
                cancelledBy = (map["cancelledBy"] as? String) ?: return null,
                reason = (map["reason"] as? String) ?: return null,
                refundStatus = try {
                    RefundStatus.valueOf((map["refundStatus"] as? String) ?: "NOT_APPLICABLE")
                } catch (e: Exception) {
                    RefundStatus.NOT_APPLICABLE
                },
                notificationSent = (map["notificationSent"] as? Boolean) ?: false,
                attendeeCount = ((map["attendeeCount"] as? Number)?.toInt()) ?: 0,
                refundAmount = (map["refundAmount"] as? Number)?.toDouble(),
                refundCurrency = map["refundCurrency"] as? String
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Convert EventCancellation to Firestore map
     */
    private fun cancellationToMap(cancellation: EventCancellation): Map<String, Any> {
        return buildMap {
            put("cancelledAt", cancellation.cancelledAt)
            put("cancelledBy", cancellation.cancelledBy)
            put("reason", cancellation.reason)
            put("refundStatus", cancellation.refundStatus.name)
            put("notificationSent", cancellation.notificationSent)
            put("attendeeCount", cancellation.attendeeCount)
            cancellation.refundAmount?.let { put("refundAmount", it) }
            cancellation.refundCurrency?.let { put("refundCurrency", it) }
        }
    }
}