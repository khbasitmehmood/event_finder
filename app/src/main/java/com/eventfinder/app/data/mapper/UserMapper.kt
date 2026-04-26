package com.eventfinder.app.data.mapper

import com.eventfinder.app.data.model.OrganizerProfileDto
import com.eventfinder.app.data.model.OrganizerSocialLinksDto
import com.eventfinder.app.data.model.UserDto
import com.eventfinder.app.data.model.UserProfileDto
import com.eventfinder.app.domain.model.*

/**
 * Mapper for converting between User domain and DTO models
 */
object UserMapper {

    fun toDomain(dto: UserDto): User {
        return User(
            uid = dto.uid,
            email = dto.email,
            userType = safeValueOf(dto.userType, UserType.USER),
            profile = dto.profile?.let { profileToDomain(it) },
            organizerProfile = dto.organizerProfile?.let { organizerProfileToDomain(it) },
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt,
            isProfileComplete = dto.isProfileComplete
        )
    }

    fun toDto(user: User): UserDto {
        return UserDto(
            uid = user.uid,
            email = user.email,
            userType = user.userType.name,
            profile = user.profile?.let { profileToDto(it) },
            organizerProfile = user.organizerProfile?.let { organizerProfileToDto(it) },
            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
            isProfileComplete = user.isProfileComplete
        )
    }

    private fun profileToDomain(dto: UserProfileDto): UserProfile {
        return UserProfile(
            fullName = dto.fullName,
            phoneNumber = dto.phoneNumber,
            photoUrl = dto.photoUrl,
            bio = dto.bio,
            city = dto.city,
            interests = dto.interests
        )
    }

    private fun profileToDto(profile: UserProfile): UserProfileDto {
        return UserProfileDto(
            fullName = profile.fullName,
            phoneNumber = profile.phoneNumber,
            photoUrl = profile.photoUrl,
            bio = profile.bio,
            city = profile.city,
            interests = profile.interests
        )
    }

    private fun organizerProfileToDomain(dto: OrganizerProfileDto): OrganizerProfile {
        return OrganizerProfile(
            organizationName = dto.organizationName,
            organizationType = safeValueOf(dto.organizationType, OrganizationType.INDIVIDUAL),
            registrationNumber = dto.registrationNumber,
            verificationStatus = safeValueOf(dto.verificationStatus, VerificationStatus.PENDING),
            contactPerson = dto.contactPerson,
            phoneNumber = dto.phoneNumber,
            address = dto.address,
            city = dto.city,
            websiteUrl = dto.websiteUrl,
            socialLinks = dto.socialLinks?.let {
                OrganizerSocialLinks(
                    website = it.website,
                    facebook = it.facebook,
                    twitter = it.twitter,
                    instagram = it.instagram
                )
            },
            logoUrl = dto.logoUrl,
            description = dto.description,
            verificationDocumentUrls = dto.verificationDocumentUrls,
            offeredEvents = dto.offeredEvents
        )
    }

    private fun organizerProfileToDto(profile: OrganizerProfile): OrganizerProfileDto {
        return OrganizerProfileDto(
            organizationName = profile.organizationName,
            organizationType = profile.organizationType.name,
            registrationNumber = profile.registrationNumber,
            verificationStatus = profile.verificationStatus.name,
            contactPerson = profile.contactPerson,
            phoneNumber = profile.phoneNumber,
            address = profile.address,
            city = profile.city,
            websiteUrl = profile.websiteUrl,
            socialLinks = profile.socialLinks?.let {
                OrganizerSocialLinksDto(
                    website = it.website,
                    facebook = it.facebook,
                    twitter = it.twitter,
                    instagram = it.instagram,
                    linkedin = null // Not in domain model
                )
            },
            logoUrl = profile.logoUrl,
            description = profile.description,
            verificationDocumentUrls = profile.verificationDocumentUrls,
            offeredEvents = profile.offeredEvents
        )
    }

    private inline fun <reified T : Enum<T>> safeValueOf(value: String?, default: T): T {
        return try {
            value?.let { enumValueOf<T>(it) } ?: default
        } catch (e: IllegalArgumentException) {
            default
        }
    }
}