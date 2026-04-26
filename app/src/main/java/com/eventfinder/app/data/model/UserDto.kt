package com.eventfinder.app.data.model

import com.google.firebase.firestore.PropertyName

/**
 * Data Transfer Object for Firestore User collection
 */
data class UserDto(
    @PropertyName("uid") val uid: String = "",
    @PropertyName("email") val email: String = "",
    @PropertyName("userType") val userType: String = "USER",
    @PropertyName("profile") val profile: UserProfileDto? = null,
    @PropertyName("organizerProfile") val organizerProfile: OrganizerProfileDto? = null,
    @PropertyName("createdAt") val createdAt: Long = 0L,
    @PropertyName("updatedAt") val updatedAt: Long? = null,
    @PropertyName("isProfileComplete") val isProfileComplete: Boolean = false
)

data class UserProfileDto(
    @PropertyName("fullName") val fullName: String = "",
    @PropertyName("phoneNumber") val phoneNumber: String? = null,
    @PropertyName("photoUrl") val photoUrl: String? = null,
    @PropertyName("bio") val bio: String? = null,
    @PropertyName("city") val city: String? = null,
    @PropertyName("interests") val interests: List<String> = emptyList()
)

data class OrganizerProfileDto(
    @PropertyName("organizationName") val organizationName: String = "",
    @PropertyName("organizationType") val organizationType: String = "INDIVIDUAL",
    @PropertyName("registrationNumber") val registrationNumber: String? = null,
    @PropertyName("verificationStatus") val verificationStatus: String = "PENDING",
    @PropertyName("contactPerson") val contactPerson: String = "",
    @PropertyName("phoneNumber") val phoneNumber: String = "",
    @PropertyName("address") val address: String? = null,
    @PropertyName("city") val city: String? = null,
    @PropertyName("websiteUrl") val websiteUrl: String? = null,
    @PropertyName("socialLinks") val socialLinks: OrganizerSocialLinksDto? = null,
    @PropertyName("logoUrl") val logoUrl: String? = null,
    @PropertyName("description") val description: String? = null,
    @PropertyName("verificationDocumentUrls") val verificationDocumentUrls: List<String> = emptyList(),
    @PropertyName("offeredEvents") val offeredEvents: List<String> = emptyList()
)

data class OrganizerSocialLinksDto(
    @PropertyName("website") val website: String? = null,
    @PropertyName("facebook") val facebook: String? = null,
    @PropertyName("twitter") val twitter: String? = null,
    @PropertyName("instagram") val instagram: String? = null,
    @PropertyName("linkedin") val linkedin: String? = null
)
