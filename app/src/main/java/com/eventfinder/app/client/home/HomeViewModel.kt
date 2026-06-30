package com.eventfinder.app.client.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.EventCategory
import com.eventfinder.app.domain.model.EventState
import com.eventfinder.app.domain.model.EventVisibility
import com.eventfinder.app.domain.model.User
import com.eventfinder.app.domain.model.UserType
import com.eventfinder.app.domain.usecase.GetEventCategoriesUseCase
import com.eventfinder.app.domain.usecase.GetExploreEventsUseCase
import com.eventfinder.app.domain.usecase.GetUserEventsUseCase
import com.eventfinder.app.domain.usecase.auth.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import javax.inject.Inject

/**
 * Combined UI State for Home Screen
 */
data class HomeUiState(
    val user: User? = null,
    val userEvents: List<Event> = emptyList(),
    val featuredEvents: List<Event> = emptyList(),
    val nearbyEvents: List<Event> = emptyList(),
    val dateFilteredEvents: List<Event> = emptyList(),
    val userCategories: List<EventCategory> = emptyList(),
    val selectedDate: Long = System.currentTimeMillis(),
    val isLoadingUserEvents: Boolean = false,
    val isLoadingFeatured: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getUserEventsUseCase: GetUserEventsUseCase,
    private val getExploreEventsUseCase: GetExploreEventsUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getEventCategoriesUseCase: GetEventCategoriesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    /**
     * Loads the user and subsequently their events, featured events, and categories
     */
    fun loadData() {
        viewModelScope.launch {
            // Fetch User
            getCurrentUserUseCase().fold(
                onSuccess = { user ->
                    _uiState.update { it.copy(user = user) }
                    // Load events requiring user context
                    user?.uid?.let { fetchUserEvents(it) }
                    
                    // Fetch categories to filter by user's interests
                    fetchUserCategories(user)
                },
                onFailure = {
                    _uiState.update { it.copy(user = null, error = "Failed to load user session.") }
                }
            )
            
            // Fetch Featured Events in parallel
            fetchFeaturedEvents()
        }
    }

    private suspend fun fetchUserCategories(user: User?) {
        if (user == null) return
        
        getEventCategoriesUseCase().fold(
            onSuccess = { allCategories ->
                val userInterestIds = if (user.userType == UserType.ORGANIZER) {
                    user.organizerProfile?.offeredEvents ?: emptyList()
                } else {
                    user.profile?.interests ?: emptyList()
                }
                
                val filteredCategories = allCategories.filter { it.id in userInterestIds }
                
                _uiState.update { it.copy(userCategories = filteredCategories) }
            },
            onFailure = {
                // Ignore failure for categories for now
            }
        )
    }

    private suspend fun fetchUserEvents(userId: String) {
        _uiState.update { it.copy(isLoadingUserEvents = true, error = null) }
        
        getUserEventsUseCase(userId).fold(
            onSuccess = { events ->
                _uiState.update { it.copy(userEvents = events, isLoadingUserEvents = false) }
            },
            onFailure = { exception ->
                _uiState.update { 
                    it.copy(
                        error = exception.message ?: "Failed to load your events",
                        isLoadingUserEvents = false
                    ) 
                }
            }
        )
    }

    private suspend fun fetchFeaturedEvents() {
        _uiState.update { it.copy(isLoadingFeatured = true, error = null) }

        getExploreEventsUseCase().fold(
            onSuccess = { events ->
                val user = _uiState.value.user
                val publicUpcomingEvents = events
                    .filter { it.visibility == EventVisibility.PUBLIC }
                    .filter { it.state in listOf(EventState.SCHEDULED, EventState.POSTPONED) }
                    .filter { it.startTime > System.currentTimeMillis() }
                    .sortedBy { it.startTime }

                val interestMatches = publicUpcomingEvents.filter { event ->
                    matchesUserInterests(user, event)
                }

                val nearbyMatches = publicUpcomingEvents.mapNotNull { event ->
                    distanceFromUser(user, event)?.let { distance ->
                        if (distance <= NEARBY_EVENT_RADIUS_KM) {
                            event.copy(distanceKm = distance)
                        } else {
                            null
                        }
                    }
                }.sortedBy { it.distanceKm ?: Double.MAX_VALUE }

                val prioritizedFeatured = (interestMatches + publicUpcomingEvents)
                    .distinctBy { event -> event.eventId.ifEmpty { event.id } }
                    .take(4)

                _uiState.update { 
                    it.copy(
                        featuredEvents = prioritizedFeatured,
                        nearbyEvents = nearbyMatches.take(10),
                        isLoadingFeatured = false
                    ) 
                }
            },
            onFailure = { exception ->
                _uiState.update { 
                    it.copy(
                        error = exception.message ?: "Failed to load featured events",
                        isLoadingFeatured = false
                    ) 
                }
            }
        )
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Filter events by selected date
     */
    fun selectDate(dateMillis: Long) {
        _uiState.update { state ->
            val filtered = state.featuredEvents.filter { event ->
                isSameDay(event.startTime, dateMillis)
            }
            state.copy(
                selectedDate = dateMillis,
                dateFilteredEvents = filtered
            )
        }
    }

    /**
     * Check if two timestamps are on the same day
     */
    private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val cal1 = java.util.Calendar.getInstance().apply { timeInMillis = timestamp1 }
        val cal2 = java.util.Calendar.getInstance().apply { timeInMillis = timestamp2 }
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
    }

    private fun matchesUserInterests(user: User?, event: Event): Boolean {
        val interests = user?.profile?.interests.orEmpty().map { it.normalized() }.toSet()
        if (interests.isEmpty()) return false

        val eventTerms = buildSet {
            event.category?.id?.takeIf { it.isNotBlank() }?.let { add(it.normalized()) }
            event.category?.name?.takeIf { it.isNotBlank() }?.let { add(it.normalized()) }
            event.tags.forEach { tag ->
                tag.takeIf { it.isNotBlank() }?.let { add(it.normalized()) }
            }
        }
        return eventTerms.any { it in interests }
    }

    private fun distanceFromUser(user: User?, event: Event): Double? {
        val profile = user?.profile ?: return null
        val latitude = profile.latitude ?: return null
        val longitude = profile.longitude ?: return null
        return haversineKm(
            lat1 = latitude,
            lon1 = longitude,
            lat2 = event.location.latitude,
            lon2 = event.location.longitude
        )
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        return earthRadiusKm * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun String.normalized(): String = trim().lowercase(Locale.US)

    companion object {
        private const val NEARBY_EVENT_RADIUS_KM = 25.0
    }
}
