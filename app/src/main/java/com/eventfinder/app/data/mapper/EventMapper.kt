package com.eventfinder.app.data.mapper

import com.eventfinder.app.data.model.EventDto
import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.EventCategory
import com.eventfinder.app.domain.model.EventLocation
import com.eventfinder.app.domain.model.EventVisibility
import com.eventfinder.app.domain.model.OrganizerSocialLinks
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

            createdAt = dto.createdAt?.toDate()?.time ?: 0L,
            updatedAt = dto.updatedAt?.toDate()?.time
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

            createdAt = Timestamp(event.createdAt / 1000, ((event.createdAt % 1000) * 1000000).toInt()),
            updatedAt = event.updatedAt?.let { Timestamp(it / 1000, ((it % 1000) * 1000000).toInt()) }
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
}