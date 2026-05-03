# Ticket Management System - Implementation Plan

## Project Overview
Implement a comprehensive QR-based ticket management system for the Event Finder app, enabling organizers to manage event registrations and attendees while providing users with digital tickets.

---

## Business Requirements

### Event Types
1. **Public Events**: Free entry, users click "I am going" to reserve their spot (no ticket required)
2. **Private Events**: Require tickets with QR codes
   - **Free Private**: Button shows "Get Free Ticket"
   - **Paid Private**: Button shows "Buy Ticket - PKR XXX"

### Ticket Management
- Digital tickets with unique QR codes
- QR code scanning for check-ins at event entrance
- Real-time attendee tracking
- Event statistics (total attendees, check-in count, revenue)
- User profile integration showing "My Events"

---

## Technical Architecture

### Current Architecture Patterns (Observed)
- **Clean Architecture**: Domain → Data → Presentation layers
- **Backend**: Firebase Firestore
- **DI**: Hilt with `@Singleton` and `@Inject`
- **UI**: MVVM with StateFlow
- **Navigation**: Navigation Component
- **Mappers**: Object mappers between DTO ↔ Domain models
- **Result**: Kotlin `Result<T>` for repository operations

### Existing Event Model Structure
```kotlin
// Domain Model: Event.kt
- Has: visibility (PUBLIC/PRIVATE/FRIENDS_ONLY)
- Has: isFree, price, currency
- Has: maxParticipants, currentParticipantCount
- Uses: EventLocation, EventCategory, OrganizerSocialLinks

// DTO: EventDto.kt
- Uses: Timestamp for dates
- Uses: GeoPoint for location
- Uses: String for category (not nested object)
- Uses: Map<String, String> for organizerSocialLinks

// Mapper: EventMapper.kt
- Converts Timestamp ↔ Long (milliseconds)
- Converts GeoPoint ↔ EventLocation
- Converts String ↔ EventVisibility enum
```

---

## Phase 1: Foundation (Data Models & Backend)

### 1.1 Domain Models

#### Create: `domain/model/Ticket.kt`
```kotlin
data class Ticket(
    val id: String = "",
    val ticketId: String = "",           // Unique ticket identifier
    val eventId: String,
    val eventTitle: String,
    val eventStartTime: Long,
    val userId: String,
    val userName: String,
    val userEmail: String,
    val ticketType: TicketType,
    val status: TicketStatus,
    val qrCodeData: String,              // Unique QR code payload
    val purchasePrice: Double = 0.0,
    val currency: String = "PKR",
    val purchasedAt: Long,
    val checkedInAt: Long? = null,
    val checkedInBy: String? = null,     // Organizer ID who checked in
    val eventLocation: String? = null,
    val organizerId: String,
    val organizerName: String
)

enum class TicketType {
    PUBLIC_RESERVATION,  // "I am going" for public events
    FREE_PRIVATE,        // Free ticket for private events
    PAID                 // Paid ticket for private events
}

enum class TicketStatus {
    RESERVED,    // User has reserved (public events)
    PURCHASED,   // Ticket purchased (free or paid private events)
    CHECKED_IN,  // User checked in at event
    CANCELLED,   // Ticket cancelled by user
    EXPIRED      // Event has passed
}
```

#### Create: `domain/model/EventStats.kt`
```kotlin
data class EventStats(
    val eventId: String,
    val totalTickets: Int = 0,           // Total tickets issued
    val checkedInCount: Int = 0,         // Number checked in
    val reservedCount: Int = 0,          // Active reservations
    val cancelledCount: Int = 0,         // Cancelled tickets
    val totalRevenue: Double = 0.0,      // Total revenue (paid events)
    val currency: String = "PKR",
    val lastUpdated: Long = System.currentTimeMillis()
)
```

#### Update: `domain/model/Event.kt`
**Add new field:**
```kotlin
data class Event(
    // ... existing fields ...
    val requiresTicket: Boolean = false,  // NEW: Determines if QR tickets are needed
    
    // Note: We already have:
    // - visibility: EventVisibility (PUBLIC/PRIVATE/FRIENDS_ONLY)
    // - isFree: Boolean
    // - price: Double?
    // - currentParticipantCount: Int
)
```

**Logic:**
- `visibility = PUBLIC` → No ticket, just "I am going" reservation
- `visibility = PRIVATE` && `requiresTicket = true` → QR ticket required (free or paid)

### 1.2 Data Transfer Objects (DTOs)

#### Create: `data/model/TicketDto.kt`
```kotlin
data class TicketDto(
    @DocumentId
    val id: String = "",
    val ticketId: String = "",
    val eventId: String = "",
    val eventTitle: String = "",
    val eventStartTime: Timestamp? = null,
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val ticketType: String = "PUBLIC_RESERVATION",  // Store enum as string
    val status: String = "RESERVED",                // Store enum as string
    val qrCodeData: String = "",
    val purchasePrice: Double = 0.0,
    val currency: String = "PKR",
    val purchasedAt: Timestamp? = null,
    val checkedInAt: Timestamp? = null,
    val checkedInBy: String? = null,
    val eventLocation: String? = null,
    val organizerId: String = "",
    val organizerName: String = ""
)
```

#### Create: `data/model/EventStatsDto.kt`
```kotlin
data class EventStatsDto(
    @DocumentId
    val eventId: String = "",
    val totalTickets: Int = 0,
    val checkedInCount: Int = 0,
    val reservedCount: Int = 0,
    val cancelledCount: Int = 0,
    val totalRevenue: Double = 0.0,
    val currency: String = "PKR",
    val lastUpdated: Timestamp? = null
)
```

#### Update: `data/model/EventDto.kt`
**Add new field:**
```kotlin
data class EventDto(
    // ... existing fields ...
    val requiresTicket: Boolean = false  // NEW
)
```

### 1.3 Mappers

#### Create: `data/mapper/TicketMapper.kt`
```kotlin
object TicketMapper {
    fun toDomain(dto: TicketDto): Ticket {
        return Ticket(
            id = dto.id,
            ticketId = dto.ticketId,
            eventId = dto.eventId,
            eventTitle = dto.eventTitle,
            eventStartTime = dto.eventStartTime?.toDate()?.time ?: 0L,
            userId = dto.userId,
            userName = dto.userName,
            userEmail = dto.userEmail,
            ticketType = safeValueOfTicketType(dto.ticketType),
            status = safeValueOfTicketStatus(dto.status),
            qrCodeData = dto.qrCodeData,
            purchasePrice = dto.purchasePrice,
            currency = dto.currency,
            purchasedAt = dto.purchasedAt?.toDate()?.time ?: 0L,
            checkedInAt = dto.checkedInAt?.toDate()?.time,
            checkedInBy = dto.checkedInBy,
            eventLocation = dto.eventLocation,
            organizerId = dto.organizerId,
            organizerName = dto.organizerName
        )
    }

    fun toDto(ticket: Ticket): TicketDto {
        return TicketDto(
            id = ticket.id,
            ticketId = ticket.ticketId,
            eventId = ticket.eventId,
            eventTitle = ticket.eventTitle,
            eventStartTime = Timestamp(ticket.eventStartTime / 1000, 
                ((ticket.eventStartTime % 1000) * 1000000).toInt()),
            userId = ticket.userId,
            userName = ticket.userName,
            userEmail = ticket.userEmail,
            ticketType = ticket.ticketType.name,
            status = ticket.status.name,
            qrCodeData = ticket.qrCodeData,
            purchasePrice = ticket.purchasePrice,
            currency = ticket.currency,
            purchasedAt = Timestamp(ticket.purchasedAt / 1000, 
                ((ticket.purchasedAt % 1000) * 1000000).toInt()),
            checkedInAt = ticket.checkedInAt?.let { 
                Timestamp(it / 1000, ((it % 1000) * 1000000).toInt()) 
            },
            checkedInBy = ticket.checkedInBy,
            eventLocation = ticket.eventLocation,
            organizerId = ticket.organizerId,
            organizerName = ticket.organizerName
        )
    }

    private fun safeValueOfTicketType(value: String?): TicketType {
        return try {
            value?.let { TicketType.valueOf(it) } ?: TicketType.PUBLIC_RESERVATION
        } catch (e: IllegalArgumentException) {
            TicketType.PUBLIC_RESERVATION
        }
    }

    private fun safeValueOfTicketStatus(value: String?): TicketStatus {
        return try {
            value?.let { TicketStatus.valueOf(it) } ?: TicketStatus.RESERVED
        } catch (e: IllegalArgumentException) {
            TicketStatus.RESERVED
        }
    }
}
```

#### Create: `data/mapper/EventStatsMapper.kt`
```kotlin
object EventStatsMapper {
    fun toDomain(dto: EventStatsDto): EventStats {
        return EventStats(
            eventId = dto.eventId,
            totalTickets = dto.totalTickets,
            checkedInCount = dto.checkedInCount,
            reservedCount = dto.reservedCount,
            cancelledCount = dto.cancelledCount,
            totalRevenue = dto.totalRevenue,
            currency = dto.currency,
            lastUpdated = dto.lastUpdated?.toDate()?.time ?: 0L
        )
    }

    fun toDto(stats: EventStats): EventStatsDto {
        return EventStatsDto(
            eventId = stats.eventId,
            totalTickets = stats.totalTickets,
            checkedInCount = stats.checkedInCount,
            reservedCount = stats.reservedCount,
            cancelledCount = stats.cancelledCount,
            totalRevenue = stats.totalRevenue,
            currency = stats.currency,
            lastUpdated = Timestamp(stats.lastUpdated / 1000, 
                ((stats.lastUpdated % 1000) * 1000000).toInt())
        )
    }
}
```

#### Update: `data/mapper/EventMapper.kt`
**Add requiresTicket field mapping in both `toDomain()` and `toDto()` methods**

### 1.4 Repository Interfaces

#### Create: `domain/repository/TicketRepository.kt`
```kotlin
interface TicketRepository {
    // Ticket Creation
    suspend fun createTicket(ticket: Ticket): Result<Ticket>
    
    // Ticket Retrieval
    suspend fun getTicketById(ticketId: String): Result<Ticket?>
    suspend fun getUserTickets(userId: String): Result<List<Ticket>>
    suspend fun getEventAttendees(eventId: String): Result<List<Ticket>>
    
    // QR Code Operations
    suspend fun validateTicketByQR(qrCodeData: String): Result<Ticket?>
    suspend fun checkInTicket(ticketId: String, organizerId: String): Result<Ticket>
    
    // Ticket Management
    suspend fun cancelTicket(ticketId: String, userId: String): Result<Unit>
    
    // Event Statistics
    suspend fun getEventStats(eventId: String): Result<EventStats>
    suspend fun incrementEventStats(eventId: String, ticketType: TicketType, amount: Double): Result<Unit>
}
```

### 1.5 Data Sources

#### Create: `data/source/TicketDataSource.kt`
```kotlin
interface TicketDataSource {
    suspend fun createTicket(ticket: Ticket): Ticket
    suspend fun getTicketById(ticketId: String): Ticket?
    suspend fun getUserTickets(userId: String): List<Ticket>
    suspend fun getEventAttendees(eventId: String): List<Ticket>
    suspend fun validateTicketByQR(qrCodeData: String): Ticket?
    suspend fun updateTicketStatus(
        ticketId: String, 
        status: TicketStatus, 
        checkedInBy: String? = null
    ): Ticket
    suspend fun cancelTicket(ticketId: String): Unit
    suspend fun getEventStats(eventId: String): EventStats?
    suspend fun updateEventStats(eventId: String, stats: EventStats): Unit
}
```

#### Create: `data/source/FirestoreTicketDataSource.kt`
```kotlin
@Singleton
class FirestoreTicketDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) : TicketDataSource {
    
    private val ticketsCollection = firestore.collection("tickets")
    private val statsCollection = firestore.collection("event_stats")
    private val eventsCollection = firestore.collection("events")
    
    // Implement all methods with proper error handling
    // Use Firestore transactions for atomic operations
    // Update event's currentParticipantCount when tickets are created/cancelled
}
```

### 1.6 Repository Implementation

#### Create: `data/repository/TicketRepositoryImpl.kt`
```kotlin
@Singleton
class TicketRepositoryImpl @Inject constructor(
    private val ticketDataSource: TicketDataSource
) : TicketRepository {
    
    override suspend fun createTicket(ticket: Ticket): Result<Ticket> {
        return try {
            val createdTicket = ticketDataSource.createTicket(ticket)
            Result.success(createdTicket)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Implement all interface methods following the same pattern
    // Wrap data source calls in try-catch and return Result<T>
}
```

### 1.7 Use Cases

#### Create: `domain/usecase/ticket/PurchaseTicketUseCase.kt`
```kotlin
@Singleton
class PurchaseTicketUseCase @Inject constructor(
    private val ticketRepository: TicketRepository,
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(
        event: Event,
        userId: String,
        userName: String,
        userEmail: String
    ): Result<Ticket> {
        // Generate unique QR code
        val qrCodeData = generateUniqueQRCode(event.eventId, userId)
        
        // Determine ticket type
        val ticketType = when {
            event.visibility == EventVisibility.PUBLIC -> TicketType.PUBLIC_RESERVATION
            event.isFree -> TicketType.FREE_PRIVATE
            else -> TicketType.PAID
        }
        
        // Determine status
        val status = when (ticketType) {
            TicketType.PUBLIC_RESERVATION -> TicketStatus.RESERVED
            else -> TicketStatus.PURCHASED
        }
        
        // Create ticket
        val ticket = Ticket(
            ticketId = UUID.randomUUID().toString(),
            eventId = event.eventId,
            eventTitle = event.title,
            eventStartTime = event.startTime,
            userId = userId,
            userName = userName,
            userEmail = userEmail,
            ticketType = ticketType,
            status = status,
            qrCodeData = qrCodeData,
            purchasePrice = event.price ?: 0.0,
            currency = event.currency ?: "PKR",
            purchasedAt = System.currentTimeMillis(),
            eventLocation = event.address,
            organizerId = event.organizerId,
            organizerName = event.organizerName
        )
        
        // Save ticket
        val result = ticketRepository.createTicket(ticket)
        
        // Update event stats
        if (result.isSuccess) {
            ticketRepository.incrementEventStats(
                eventId = event.eventId,
                ticketType = ticketType,
                amount = ticket.purchasePrice
            )
        }
        
        return result
    }
    
    private fun generateUniqueQRCode(eventId: String, userId: String): String {
        return "${eventId}_${userId}_${UUID.randomUUID()}_${System.currentTimeMillis()}"
    }
}
```

#### Create: `domain/usecase/ticket/ValidateTicketQRUseCase.kt`
```kotlin
@Singleton
class ValidateTicketQRUseCase @Inject constructor(
    private val ticketRepository: TicketRepository
) {
    suspend operator fun invoke(qrCodeData: String): Result<Ticket?> {
        return ticketRepository.validateTicketByQR(qrCodeData)
    }
}
```

#### Create: `domain/usecase/ticket/CheckInAttendeeUseCase.kt`
```kotlin
@Singleton
class CheckInAttendeeUseCase @Inject constructor(
    private val ticketRepository: TicketRepository
) {
    suspend operator fun invoke(ticketId: String, organizerId: String): Result<Ticket> {
        return ticketRepository.checkInTicket(ticketId, organizerId)
    }
}
```

#### Create: `domain/usecase/ticket/GetUserTicketsUseCase.kt`
```kotlin
@Singleton
class GetUserTicketsUseCase @Inject constructor(
    private val ticketRepository: TicketRepository
) {
    suspend operator fun invoke(userId: String): Result<List<Ticket>> {
        return ticketRepository.getUserTickets(userId)
    }
}
```

#### Create: `domain/usecase/ticket/GetEventAttendeesUseCase.kt`
```kotlin
@Singleton
class GetEventAttendeesUseCase @Inject constructor(
    private val ticketRepository: TicketRepository
) {
    suspend operator fun invoke(eventId: String): Result<List<Ticket>> {
        return ticketRepository.getEventAttendees(eventId)
    }
}
```

#### Create: `domain/usecase/ticket/GetEventStatsUseCase.kt`
```kotlin
@Singleton
class GetEventStatsUseCase @Inject constructor(
    private val ticketRepository: TicketRepository
) {
    suspend operator fun invoke(eventId: String): Result<EventStats> {
        return ticketRepository.getEventStats(eventId)
    }
}
```

#### Create: `domain/usecase/ticket/CancelTicketUseCase.kt`
```kotlin
@Singleton
class CancelTicketUseCase @Inject constructor(
    private val ticketRepository: TicketRepository
) {
    suspend operator fun invoke(ticketId: String, userId: String): Result<Unit> {
        return ticketRepository.cancelTicket(ticketId, userId)
    }
}
```

### 1.8 Dependency Injection

#### Create: `di/TicketModule.kt`
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object TicketModule {
    
    @Provides
    @Singleton
    fun provideTicketDataSource(
        firestore: FirebaseFirestore
    ): TicketDataSource {
        return FirestoreTicketDataSource(firestore)
    }
    
    @Provides
    @Singleton
    fun provideTicketRepository(
        ticketDataSource: TicketDataSource
    ): TicketRepository {
        return TicketRepositoryImpl(ticketDataSource)
    }
}
```

---

## Firestore Database Structure

### Collections

#### `tickets` Collection
```
tickets/{ticketId}
├── id: string
├── ticketId: string (indexed)
├── eventId: string (indexed)
├── userId: string (indexed)
├── status: string (indexed)
├── qrCodeData: string (indexed, unique)
├── purchasedAt: timestamp
├── checkedInAt: timestamp?
└── ... other fields

Composite Indexes:
- eventId + status
- userId + status
- eventId + checkedInAt
```

#### `event_stats` Collection
```
event_stats/{eventId}
├── eventId: string (document ID)
├── totalTickets: number
├── checkedInCount: number
├── reservedCount: number
├── cancelledCount: number
├── totalRevenue: number
├── currency: string
└── lastUpdated: timestamp
```

#### `events` Collection (Update)
```
events/{eventId}
├── ... existing fields ...
├── requiresTicket: boolean  // NEW
└── currentParticipantCount: number (updated on ticket creation/cancellation)
```

### Firestore Security Rules
```javascript
match /tickets/{ticketId} {
  allow create: if request.auth != null;
  allow read: if request.auth.uid == resource.data.userId || 
                 request.auth.uid == resource.data.organizerId;
  allow update: if request.auth.uid == resource.data.organizerId &&
                   request.resource.data.diff(resource.data).affectedKeys()
                     .hasOnly(['status', 'checkedInAt', 'checkedInBy']);
  allow delete: if false;  // Never allow deletion, only status changes
}

match /event_stats/{eventId} {
  allow read: if request.auth != null;
  allow write: if request.auth.uid == get(/databases/$(database)/documents/events/$(eventId)).data.organizerId;
}
```

---

## Implementation Ripple Effects

### Impact on Event Creation (CreateEventFragment)
1. Add toggle for "Requires Ticket" (only show for PRIVATE events)
2. Update `viewModel.createEvent()` call to include `requiresTicket` field
3. Initialize `event_stats` document when event is created with `requiresTicket = true`

### Impact on Event Display (EventDetailFragment)
1. Add dynamic button logic:
   - If `visibility == PUBLIC`: Show "I am going" button
   - If `visibility == PRIVATE && requiresTicket`:
     - If `isFree`: Show "Get Free Ticket"
     - If `!isFree`: Show "Buy Ticket - ${currency} ${price}"
2. Check if user already has a ticket (query tickets collection by userId + eventId)
3. If user has ticket: Show "View Ticket" button instead
4. Add ticket purchase flow (navigate to confirmation screen or show bottom sheet)

### Impact on Organizer Dashboard (ManageEventFragment)
1. Load and display event stats in Overview tab
2. Load attendee list in Attendees tab
3. Add "Scan QR" floating action button
4. Update Insights tab with charts and stats

### Impact on User Profile
1. Add "My Tickets" section
2. Query tickets where `userId == currentUserId`
3. Show upcoming events, past events, cancelled tickets

---

## Next Phases (After Phase 1)

### Phase 2: Event Creation & Detail Updates
- Update CreateEventFragment UI and logic
- Update EventDetailFragment with ticket purchase buttons
- Create ticket purchase confirmation flow

### Phase 3: Ticket Display
- Create TicketDetailFragment with QR code
- Update TicketsFragment with real data
- Implement ticket filtering and search

### Phase 4: QR Scanner
- Implement QRScannerFragment with CameraX
- Add ML Kit barcode scanning
- Create check-in flow and validation

### Phase 5: Organizer Features
- Update ManageEventAttendeesFragment
- Update ManageEventInsightsFragment
- Add real-time listeners for stats

### Phase 6: Polish & Testing
- Add offline support
- Implement real-time updates
- Add notifications
- Testing and bug fixes

---

## Dependencies Required

Add to `gradle/libs.versions.toml`:
```toml
[versions]
zxing = "3.5.3"
zxingEmbedded = "4.3.0"
camerax = "1.3.0"
mlkit-barcode = "17.2.0"

[libraries]
zxing-core = { group = "com.google.zxing", name = "core", version.ref = "zxing" }
zxing-android-embedded = { group = "com.journeyapps", name = "zxing-android-embedded", version.ref = "zxingEmbedded" }
androidx-camera-core = { group = "androidx.camera", name = "camera-core", version.ref = "camerax" }
androidx-camera-camera2 = { group = "androidx.camera", name = "camera-camera2", version.ref = "camerax" }
androidx-camera-lifecycle = { group = "androidx.camera", name = "camera-lifecycle", version.ref = "camerax" }
androidx-camera-view = { group = "androidx.camera", name = "camera-view", version.ref = "camerax" }
mlkit-barcode-scanning = { group = "com.google.mlkit", name = "barcode-scanning", version.ref = "mlkit-barcode" }
```

---

## Testing Strategy

### Unit Tests
- TicketMapper conversion accuracy
- Use case business logic
- QR code generation uniqueness
- Stats calculation accuracy

### Integration Tests
- Ticket creation → Stats update
- Check-in → Ticket status change
- Event deletion → Ticket cleanup

### UI Tests
- Ticket purchase flow
- QR code display
- Scanner functionality
- Attendee list filtering

---

## Progress Tracking

- [x] Phase 1: Foundation (Domain models, DTOs, Mappers, Repositories, Use Cases) ✅ **COMPLETED**
- [x] Phase 2: Event Creation & Detail Updates ✅ **COMPLETED**
- [x] Phase 3: Ticket Display ✅ **COMPLETED**
- [x] Phase 4: QR Scanner ✅ **COMPLETED**
- [x] Phase 5: Organizer Features ✅ **COMPLETED**
- [ ] Phase 6: Polish & Testing

---

**Last Updated**: 2026-05-03  
**Status**: Phase 5 - ✅ Complete | Phase 6 - Ready to Start

**Phase 1 Summary**: Created 20 new files, updated 3 existing files. All foundation code compiles successfully. See `PHASE_1_COMPLETION_SUMMARY.md` for details.

**Phase 2 Summary**: Updated 8 files (7 Kotlin + 1 XML). Event creation now supports Public/Private with ticket options. Event detail shows dynamic buttons and handles ticket purchase. See `PHASE_2_COMPLETION_SUMMARY.md` for details.

**Phase 3 Summary**: Created 16 files (12 new + 4 updated). Implemented QR code generation with ZXing, ticket detail screen with QR display, and My Tickets tab with categorization. Users can view and cancel tickets. See `PHASE_3_COMPLETION_SUMMARY.md` for details.

**Phase 4 Summary**: Created 10 files, updated 6 existing files. Implemented complete QR scanner with CameraX, ML Kit Barcode Scanning, ticket validation, and check-in functionality. Added 16KB page size compatibility fix. See `PHASE_4_COMPLETION_SUMMARY.md` for details.

**Phase 5 Summary**: Created 1 new file (ManageEventSharedViewModel), updated 4 files. Integrated real ticket data and statistics into organizer interface. Attendees tab now has search, filters, and real data. Insights tab shows calculated statistics, revenue, capacity, and check-in rates. See `PHASE_5_COMPLETION_SUMMARY.md` for details.
