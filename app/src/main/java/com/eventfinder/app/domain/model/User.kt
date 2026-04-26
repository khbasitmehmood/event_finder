package com.eventfinder.app.domain.model

/**
 * User domain model
 * Represents both regular users and organizers
 */
data class User(
    val uid: String = "",
    val email: String = "",
    val userType: UserType = UserType.USER,
    val profile: UserProfile? = null,
    val organizerProfile: OrganizerProfile? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null,
    val isProfileComplete: Boolean = false
)

data class UserProfile(
    val fullName: String = "",
    val phoneNumber: String? = null,
    val photoUrl: String? = null,
    val bio: String? = null,
    val city: String? = null,
    val interests: List<String> = emptyList()
)

data class OrganizerProfile(
    val organizationName: String = "",
    val organizationType: OrganizationType = OrganizationType.INDIVIDUAL,
    val registrationNumber: String? = null, // NTN or SECP registration number
    val verificationStatus: VerificationStatus = VerificationStatus.PENDING,
    val contactPerson: String = "",
    val phoneNumber: String = "",
    val address: String? = null,
    val city: String? = null,
    val websiteUrl: String? = null,
    val socialLinks: OrganizerSocialLinks? = null,
    val logoUrl: String? = null,
    val description: String? = null,
    val verificationDocumentUrls: List<String> = emptyList(),
    val offeredEvents: List<String> = emptyList()
)

enum class UserType {
    USER,
    ORGANIZER
}

enum class OrganizationType {
    INDIVIDUAL,
    COMPANY,
    NGO,
    GOVERNMENT,
    EDUCATIONAL_INSTITUTION,
    OTHER
}

enum class VerificationStatus {
    PENDING,
    VERIFIED,
    REJECTED
}
