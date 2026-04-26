# Event Finder - Developer Quick Reference

Quick guide for developers working on the Event Finder Android app.

---

## Project Setup

### Prerequisites
- Android Studio Hedgehog or later
- JDK 11+
- Android SDK 24+ (minimum), 34 (target)
- Firebase project configured

### Clone & Build
```bash
git clone <repository-url>
cd event_finder-develop_pingo
./gradlew build
```

### Firebase Configuration
1. Download `google-services.json` from Firebase Console
2. Place in `app/` directory
3. Ensure Firebase Auth and Firestore are enabled

---

## Project Structure

```
app/src/main/java/com/eventfinder/app/
│
├── client/                     # User-facing features
│   ├── auth/                  # Login, Signup, Splash
│   ├── home/                  # Home screen, Calendar
│   ├── createevent/           # Event creation
│   ├── explore/               # Event discovery
│   ├── profile/               # User profile, Logout
│   ├── favourites/            # Saved events
│   ├── watchlist/             # Watchlist
│   └── chatbot/               # AI assistant
│
├── domain/                     # Business logic (framework-agnostic)
│   ├── model/                 # Domain entities
│   │   ├── User.kt
│   │   ├── Event.kt
│   │   └── ...
│   ├── repository/            # Repository interfaces
│   │   ├── AuthRepository.kt
│   │   └── EventRepository.kt
│   └── usecase/               # Single-responsibility use cases
│       ├── auth/
│       │   ├── LoginUseCase.kt
│       │   ├── SignupUseCase.kt
│       │   ├── LogoutUseCase.kt
│       │   └── GetCurrentUserUseCase.kt
│       ├── CreateEventUseCase.kt
│       ├── GetUserEventsUseCase.kt
│       └── GetExploreEventsUseCase.kt
│
├── data/                       # Data layer
│   ├── model/                 # DTOs (Firestore-compatible)
│   │   ├── UserDto.kt
│   │   └── EventDto.kt
│   ├── mapper/                # DTO ↔ Domain conversion
│   │   ├── UserMapper.kt
│   │   └── EventMapper.kt
│   ├── repository/            # Repository implementations
│   │   ├── AuthRepositoryImpl.kt
│   │   └── EventRepositoryImpl.kt
│   └── source/                # Data sources
│       └── FirestoreEventDataSource.kt
│
├── di/                         # Dependency Injection (Hilt)
│   ├── FirebaseModule.kt
│   ├── AuthModule.kt
│   └── EventModule.kt
│
└── utils/                      # Utilities
    ├── UserPreferences.kt
    ├── DateFormatter.kt
    └── LocationUtils.kt
```

---

## Key Design Patterns

### 1. Clean Architecture

**Rule:** Dependencies point inward (Presentation → Domain ← Data)

```
Fragment → ViewModel → UseCase → Repository Interface → Repository Impl → DataSource
```

### 2. MVVM (Model-View-ViewModel)

**Fragment (View):**
- Observes StateFlow from ViewModel
- Handles UI updates only

**ViewModel:**
- Exposes StateFlow for reactive updates
- Calls use cases
- No Android framework dependencies (except Hilt)

**Model:**
- Domain models (User, Event, etc.)
- UI state classes (sealed classes)

### 3. Repository Pattern

**Interface (Domain Layer):**
```kotlin
interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
}
```

**Implementation (Data Layer):**
```kotlin
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {
    override suspend fun login(email: String, password: String): Result<User> {
        // Implementation using Firebase
    }
}
```

### 4. Use Case Pattern

Each use case = single business operation

```kotlin
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<User> {
        return authRepository.login(email, password)
    }
}
```

---

## Common Tasks

### Adding a New Feature

1. **Create Domain Model** (if needed)
   ```kotlin
   // domain/model/MyModel.kt
   data class MyModel(
       val id: String,
       val name: String
   )
   ```

2. **Add Repository Interface**
   ```kotlin
   // domain/repository/MyRepository.kt
   interface MyRepository {
       suspend fun getMyData(): Result<List<MyModel>>
   }
   ```

3. **Create Use Case**
   ```kotlin
   // domain/usecase/GetMyDataUseCase.kt
   class GetMyDataUseCase @Inject constructor(
       private val repository: MyRepository
   ) {
       suspend operator fun invoke(): Result<List<MyModel>> {
           return repository.getMyData()
       }
   }
   ```

4. **Implement Repository**
   ```kotlin
   // data/repository/MyRepositoryImpl.kt
   class MyRepositoryImpl @Inject constructor(
       private val firestore: FirebaseFirestore
   ) : MyRepository {
       override suspend fun getMyData(): Result<List<MyModel>> {
           // Firestore implementation
       }
   }
   ```

5. **Create ViewModel**
   ```kotlin
   // client/myfeature/MyViewModel.kt
   @HiltViewModel
   class MyViewModel @Inject constructor(
       private val getMyDataUseCase: GetMyDataUseCase
   ) : ViewModel() {

       private val _data = MutableStateFlow<List<MyModel>>(emptyList())
       val data: StateFlow<List<MyModel>> = _data.asStateFlow()

       fun loadData() {
           viewModelScope.launch {
               getMyDataUseCase().fold(
                   onSuccess = { _data.value = it },
                   onFailure = { /* handle error */ }
               )
           }
       }
   }
   ```

6. **Create Fragment**
   ```kotlin
   // client/myfeature/MyFragment.kt
   @AndroidEntryPoint
   class MyFragment : Fragment(R.layout.fragment_my) {

       private val viewModel: MyViewModel by viewModels()

       override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
           super.onViewCreated(view, savedInstanceState)
           observeViewModel()
           viewModel.loadData()
       }

       private fun observeViewModel() {
           viewLifecycleOwner.lifecycleScope.launch {
               viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                   viewModel.data.collect { data ->
                       // Update UI
                   }
               }
           }
       }
   }
   ```

7. **Add to DI Module**
   ```kotlin
   // di/MyModule.kt
   @Module
   @InstallIn(SingletonComponent::class)
   object MyModule {

       @Provides
       @Singleton
       fun provideMyRepository(
           firestore: FirebaseFirestore
       ): MyRepository {
           return MyRepositoryImpl(firestore)
       }
   }
   ```

---

## Working with Firebase

### Firestore Queries

**Read documents:**
```kotlin
val snapshot = firestore.collection("events")
    .whereEqualTo("visibility", "PUBLIC")
    .orderBy("startTime", Query.Direction.ASCENDING)
    .limit(50)
    .get()
    .await()

val events = snapshot.documents.mapNotNull { doc ->
    doc.toObject(EventDto::class.java)?.let { dto ->
        EventMapper.toDomain(dto.copy(id = doc.id))
    }
}
```

**Create document:**
```kotlin
val eventDto = EventMapper.toDto(event)
val docRef = firestore.collection("events")
    .add(eventDto)
    .await()

val eventId = docRef.id
```

**Update document:**
```kotlin
firestore.collection("events")
    .document(eventId)
    .update(mapOf(
        "title" to newTitle,
        "updatedAt" to System.currentTimeMillis()
    ))
    .await()
```

**Delete document:**
```kotlin
firestore.collection("events")
    .document(eventId)
    .delete()
    .await()
```

### Firebase Auth

**Sign up:**
```kotlin
val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
val uid = authResult.user?.uid
```

**Sign in:**
```kotlin
val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
val uid = authResult.user?.uid
```

**Sign out:**
```kotlin
firebaseAuth.signOut()
```

**Get current user:**
```kotlin
val currentUser = firebaseAuth.currentUser
val uid = currentUser?.uid
```

---

## StateFlow Best Practices

### In ViewModel

**Private mutable, public immutable:**
```kotlin
private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
val uiState: StateFlow<UiState> = _uiState.asStateFlow()
```

**Update state:**
```kotlin
_uiState.value = UiState.Loading
// or
_uiState.update { currentState ->
    currentState.copy(isLoading = true)
}
```

### In Fragment

**Lifecycle-aware collection:**
```kotlin
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
            when (state) {
                is UiState.Loading -> showLoading()
                is UiState.Success -> showData(state.data)
                is UiState.Error -> showError(state.message)
            }
        }
    }
}
```

**Why `repeatOnLifecycle`?**
- Stops collection when lifecycle is below STARTED
- Restarts when lifecycle returns to STARTED
- Prevents memory leaks and crashes

---

## UI State Pattern

### Define sealed class:
```kotlin
sealed class MyUiState {
    object Idle : MyUiState()
    object Loading : MyUiState()
    data class Success(val data: List<MyModel>) : MyUiState()
    data class Error(val message: String) : MyUiState()
}
```

### In ViewModel:
```kotlin
private val _uiState = MutableStateFlow<MyUiState>(MyUiState.Idle)
val uiState: StateFlow<MyUiState> = _uiState.asStateFlow()

fun loadData() {
    viewModelScope.launch {
        _uiState.value = MyUiState.Loading

        myUseCase().fold(
            onSuccess = { data ->
                _uiState.value = MyUiState.Success(data)
            },
            onFailure = { exception ->
                _uiState.value = MyUiState.Error(exception.message ?: "Unknown error")
            }
        )
    }
}
```

### In Fragment:
```kotlin
viewModel.uiState.collect { state ->
    when (state) {
        is MyUiState.Idle -> {
            // Initial state, do nothing or show empty view
        }
        is MyUiState.Loading -> {
            binding.progressBar.isVisible = true
            binding.recyclerView.isVisible = false
        }
        is MyUiState.Success -> {
            binding.progressBar.isVisible = false
            binding.recyclerView.isVisible = true
            adapter.submitList(state.data)
        }
        is MyUiState.Error -> {
            binding.progressBar.isVisible = false
            Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
        }
    }
}
```

---

## Navigation

### Using Navigation Component

**Navigate with action:**
```kotlin
findNavController().navigate(R.id.action_home_to_detail)
```

**Navigate with arguments:**
```kotlin
val bundle = Bundle().apply {
    putString("EVENT_ID", eventId)
}
findNavController().navigate(R.id.eventDetailFragment, bundle)
```

**Pop back stack:**
```kotlin
findNavController().popBackStack()
```

**Clear back stack and navigate:**
```kotlin
// Defined in nav_graph.xml:
<action
    android:id="@+id/action_logout"
    app:destination="@id/loginFragment"
    app:popUpTo="@id/main_nav_graph"
    app:popUpToInclusive="true" />

// In code:
findNavController().navigate(R.id.action_logout)
```

---

## Testing

### Unit Testing ViewModels

```kotlin
@ExperimentalCoroutinesApi
class MyViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: MyViewModel
    private lateinit var mockUseCase: GetMyDataUseCase

    @Before
    fun setup() {
        mockUseCase = mock()
        viewModel = MyViewModel(mockUseCase)
    }

    @Test
    fun `loadData emits success state when use case succeeds`() = runTest {
        // Given
        val expectedData = listOf(MyModel("1", "Test"))
        whenever(mockUseCase()).thenReturn(Result.success(expectedData))

        // When
        viewModel.loadData()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state is MyUiState.Success)
        assertEquals(expectedData, (state as MyUiState.Success).data)
    }
}
```

### Repository Testing

```kotlin
@Test
fun `getUserEvents returns events for given userId`() = runTest {
    // Given
    val userId = "test_user_123"
    val mockSnapshot = mockFirestoreSnapshot(...)
    whenever(firestore.collection("events")
        .whereEqualTo("organizerId", userId)
        .get()
    ).thenReturn(Tasks.forResult(mockSnapshot))

    // When
    val result = repository.getUserEvents(userId)

    // Then
    assertTrue(result.isSuccess)
    val events = result.getOrNull()
    assertNotNull(events)
    assertEquals(2, events.size)
}
```

---

## Debugging Tips

### Enable Firestore Debug Logging

```kotlin
// In onCreate of Application class
FirebaseFirestore.setLoggingEnabled(true)
```

### View StateFlow Values in Logcat

```kotlin
viewModel.uiState.collect { state ->
    Log.d("MyFragment", "UI State: $state")
    // Update UI
}
```

### Check Firebase Auth State

```kotlin
Log.d("Auth", "Current user: ${firebaseAuth.currentUser?.uid}")
Log.d("Auth", "Is logged in: ${firebaseAuth.currentUser != null}")
```

### Debug Navigation Issues

```kotlin
findNavController().addOnDestinationChangedListener { _, destination, _ ->
    Log.d("Navigation", "Navigated to: ${destination.label}")
}
```

---

## Code Style Guidelines

### Naming Conventions

**Classes:**
- PascalCase: `UserRepository`, `LoginViewModel`

**Functions:**
- camelCase: `getUserEvents()`, `updateProfile()`

**Variables:**
- camelCase: `currentUser`, `isLoading`

**Constants:**
- UPPER_SNAKE_CASE: `MAX_PARTICIPANTS`, `DEFAULT_RADIUS_KM`

**Private properties:**
- Prefix with underscore for mutable state: `_uiState`

### File Organization

**Order in class:**
1. Companion object / Constants
2. Properties (public → private)
3. Init block
4. Public functions
5. Private functions
6. Inner classes

**Example:**
```kotlin
class MyViewModel @Inject constructor(
    private val useCase: MyUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "MyViewModel"
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        // Public function
    }

    private fun processData(data: List<Item>) {
        // Private function
    }

    sealed class State {
        // Inner class
    }
}
```

### Documentation

**Class-level documentation:**
```kotlin
/**
 * ViewModel for the Home screen.
 * Manages user events and featured events display.
 */
class HomeViewModel @Inject constructor(...)
```

**Function documentation (for complex logic):**
```kotlin
/**
 * Calculates distance between two geographic points using Haversine formula.
 *
 * @param lat1 Latitude of first point
 * @param lon1 Longitude of first point
 * @param lat2 Latitude of second point
 * @param lon2 Longitude of second point
 * @return Distance in kilometers
 */
fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double
```

---

## Performance Tips

### RecyclerView Optimization

```kotlin
// Set fixed size if item height doesn't change
recyclerView.setHasFixedSize(true)

// Use DiffUtil for efficient updates
class MyAdapter : ListAdapter<Item, ViewHolder>(ItemDiffCallback())

private class ItemDiffCallback : DiffUtil.ItemCallback<Item>() {
    override fun areItemsTheSame(oldItem: Item, newItem: Item) =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Item, newItem: Item) =
        oldItem == newItem
}
```

### Coroutine Best Practices

```kotlin
// Use appropriate dispatchers
viewModelScope.launch(Dispatchers.IO) {
    // Network/database operations
}

// Switch to Main for UI updates
withContext(Dispatchers.Main) {
    // Update UI
}

// Cancel when not needed
val job = viewModelScope.launch {
    // Long-running task
}
job.cancel() // When needed
```

### Avoid Memory Leaks

**In Fragment:**
```kotlin
private var _binding: FragmentBinding? = null
private val binding get() = _binding!!

override fun onDestroyView() {
    super.onDestroyView()
    _binding = null  // Important!
}
```

**Use viewLifecycleOwner:**
```kotlin
// Good
viewLifecycleOwner.lifecycleScope.launch { }

// Bad (can leak)
lifecycleScope.launch { }
```

---

## Common Issues & Solutions

### Issue: Build fails with "Unresolved reference"

**Solution:**
1. Clean and rebuild: `./gradlew clean build`
2. Invalidate caches: Android Studio → File → Invalidate Caches / Restart
3. Check Hilt setup: Ensure `@AndroidEntryPoint` and `@HiltViewModel` annotations

### Issue: Firestore query returns empty list

**Solution:**
1. Check Firestore rules (allow read)
2. Verify collection name matches
3. Check field names (case-sensitive)
4. Enable Firestore logging to see queries

### Issue: StateFlow not updating UI

**Solution:**
1. Use `viewLifecycleOwner.lifecycleScope` not `lifecycleScope`
2. Ensure `repeatOnLifecycle(Lifecycle.State.STARTED)`
3. Check if StateFlow is being collected

### Issue: Navigation action not found

**Solution:**
1. Rebuild project to regenerate navigation code
2. Verify action exists in `nav_graph.xml`
3. Check fragment IDs match

---

## Git Workflow

### Branch Naming

- Feature: `feature/user-authentication`
- Bug fix: `bugfix/event-display-issue`
- Hotfix: `hotfix/crash-on-login`

### Commit Messages

Follow conventional commits:

```
feat: add user profile completion flow
fix: resolve event list not updating after creation
refactor: extract event mapper to separate class
docs: update API documentation
test: add unit tests for LoginViewModel
```

### Before Committing

1. Run tests: `./gradlew test`
2. Check build: `./gradlew assembleDebug`
3. Format code: Android Studio → Code → Reformat Code
4. Review changes

---

## Useful Commands

### Gradle

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Check dependencies
./gradlew dependencies
```

### ADB

```bash
# Install APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Clear app data
adb shell pm clear com.eventfinder.app

# View logs
adb logcat | grep "MyTag"

# Get device list
adb devices
```

---

## Resources

### Official Documentation
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Android Architecture Components](https://developer.android.com/topic/libraries/architecture)
- [Firebase Android](https://firebase.google.com/docs/android/setup)
- [Hilt Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)

### Internal Documentation
- `IMPLEMENTATION_REPORT.md` - Complete feature overview
- `FLOW_DIAGRAMS.md` - Visual flow diagrams
- Code comments in domain models and repositories

---

## Getting Help

1. Check this guide first
2. Review `IMPLEMENTATION_REPORT.md` for architecture details
3. Look at existing similar implementations in the codebase
4. Search Firebase documentation
5. Ask the team in Slack/Discord

---

**Last Updated:** April 26, 2026
**Maintainer:** Development Team
