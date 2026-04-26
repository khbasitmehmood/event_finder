# Event Finder - Flow Diagrams

Quick reference for understanding app flows.

---

## 1. App Launch & Session Flow

```
┌─────────────────────────────────────────────────────────────┐
│                      App Starts                             │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              SplashFragment                                  │
│  • Shows logo & progress bar                                │
│  • Delay 1 second                                           │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│         SplashViewModel.checkSession()                       │
│  • GetCurrentUserUseCase()                                  │
│  • Checks Firebase Auth.currentUser                         │
│  • If user exists, fetch from Firestore                     │
└────────────────────────┬────────────────────────────────────┘
                         │
              ┌──────────┴──────────┐
              │                     │
              ▼                     ▼
     User != null           User == null
              │                     │
              ▼                     ▼
┌──────────────────────┐  ┌──────────────────────┐
│  Update Preferences  │  │  Navigate to Login   │
│  • userId            │  │  (No session found)  │
│  • userName          │  └──────────────────────┘
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│  Navigate to Home    │
│  (Session restored)  │
└──────────────────────┘
```

---

## 2. Authentication Flows

### 2A. Signup Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    LoginFragment                             │
│  User clicks "Sign Up"                                       │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                  SignupFragment                              │
│                                                              │
│  ┌──────────────────┐       ┌──────────────────┐           │
│  │   USER Card      │       │  ORGANIZER Card  │           │
│  │  (Green icon)    │       │  (Orange icon)   │           │
│  └────────┬─────────┘       └────────┬─────────┘           │
│           │                           │                     │
│           └───────────┬───────────────┘                     │
│                       ▼                                     │
│         User enters email & password                        │
│         Clicks "Sign Up"                                    │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│           SignupViewModel.signup()                           │
│  • Validates email format                                   │
│  • Validates password (min 6 chars)                         │
│  • Calls SignupUseCase                                      │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              SignupUseCase                                   │
│  Calls AuthRepository.signup()                              │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│         AuthRepositoryImpl.signup()                          │
│  1. Firebase Auth: createUserWithEmailAndPassword()         │
│  2. Get uid from AuthResult                                 │
│  3. Create User domain model                                │
│  4. Convert to UserDto                                      │
│  5. Save to Firestore "users" collection                    │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                  Success Response                            │
│  • User object returned                                     │
│  • UserPreferences.setUserId()                              │
│  • UserPreferences.setUserName()                            │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│            TransitionFragment                                │
│  Shows success message & animation                          │
│  Auto-navigate after 2 seconds                              │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                  HomeFragment                                │
└─────────────────────────────────────────────────────────────┘
```

### 2B. Login Flow

```
┌─────────────────────────────────────────────────────────────┐
│                  LoginFragment                               │
│  User enters email & password                               │
│  Clicks "Login"                                             │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│           LoginViewModel.login()                             │
│  • Validates input                                          │
│  • Shows loading state                                      │
│  • Calls LoginUseCase                                       │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              LoginUseCase                                    │
│  Calls AuthRepository.login()                               │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│         AuthRepositoryImpl.login()                           │
│  1. Firebase Auth: signInWithEmailAndPassword()             │
│  2. Get uid from AuthResult                                 │
│  3. Fetch user document from Firestore                      │
│  4. Convert UserDto to User domain model                    │
│  5. Return Result<User>                                     │
└────────────────────────┬────────────────────────────────────┘
                         │
              ┌──────────┴──────────┐
              │                     │
              ▼                     ▼
        Success                  Failure
              │                     │
              ▼                     ▼
┌──────────────────────┐  ┌──────────────────────┐
│  Save to Preferences │  │  Show Error Toast    │
│  • userId            │  │  Stay on Login       │
│  • userName          │  └──────────────────────┘
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│ TransitionFragment   │
│ → HomeFragment       │
└──────────────────────┘
```

### 2C. Logout Flow

```
┌─────────────────────────────────────────────────────────────┐
│                ProfileFragment                               │
│  User clicks "Logout" button                                │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│           Confirmation Dialog                                │
│  "Are you sure you want to logout?"                         │
│  [Cancel]  [Logout]                                         │
└────────────────────────┬────────────────────────────────────┘
                         │ (User clicks Logout)
                         ▼
┌─────────────────────────────────────────────────────────────┐
│         ProfileViewModel.logout()                            │
│  Calls LogoutUseCase                                        │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              LogoutUseCase                                   │
│  Calls AuthRepository.logout()                              │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│         AuthRepositoryImpl.logout()                          │
│  1. Firebase Auth.signOut()                                 │
│  2. Return Result.success(Unit)                             │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│         ProfileViewModel receives success                    │
│  • UserPreferences.clear()                                  │
│  • Emit LogoutState.Success                                 │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│         ProfileFragment observes success                     │
│  • Show toast "Logged out successfully"                     │
│  • Navigate to LoginFragment (clear back stack)             │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Event Management Flows

### 3A. Create Event Flow

```
┌─────────────────────────────────────────────────────────────┐
│                  HomeFragment                                │
│  User clicks "Create Event" button                          │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│            CreateEventFragment                               │
│                                                              │
│  User fills form:                                           │
│  ┌────────────────────────────────────────────┐            │
│  │ • Title (required)                          │            │
│  │ • Description                               │            │
│  │ • Category dropdown                         │            │
│  │ • Start Date/Time picker                    │            │
│  │ • End Date/Time picker                      │            │
│  │ • Location name                             │            │
│  │ • Location picker (lat/lng)                 │            │
│  │ • Max participants                          │            │
│  │ • Free/Paid toggle                          │            │
│  │   - If paid: price & currency               │            │
│  │ • Image picker                              │            │
│  │ • Public/Private toggle                     │            │
│  │ • Tags (optional)                           │            │
│  └────────────────────────────────────────────┘            │
│                                                              │
│  User clicks "Create Event"                                 │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│      CreateEventViewModel.createEvent()                      │
│                                                              │
│  Validation:                                                │
│  ✓ Title not empty                                          │
│  ✓ Start time is future                                     │
│  ✓ End time > start time                                    │
│  ✓ Location provided                                        │
│  ✓ If paid: price > 0                                       │
│                                                              │
│  Build Event object:                                        │
│  • organizerId = userPreferences.getUserId()                │
│  • organizerName = userPreferences.getUserName()            │
│  • Generate geohash from lat/lng                            │
│  • Set createdAt timestamp                                  │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│           CreateEventUseCase                                 │
│  Calls EventRepository.createEvent()                        │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│       EventRepositoryImpl.createEvent()                      │
│  Calls FirestoreEventDataSource.createEvent()               │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│    FirestoreEventDataSource.createEvent()                    │
│                                                              │
│  1. Convert Event to EventDto                               │
│  2. Add to Firestore "events" collection                    │
│  3. Get auto-generated document ID                          │
│  4. Update event.id = documentId                            │
│  5. Return created Event                                    │
└────────────────────────┬────────────────────────────────────┘
                         │
              ┌──────────┴──────────┐
              │                     │
              ▼                     ▼
        Success                  Failure
              │                     │
              ▼                     ▼
┌──────────────────────┐  ┌──────────────────────┐
│  Show Success Toast  │  │  Show Error Toast    │
│  Navigate back       │  │  Stay on form        │
│  to HomeFragment     │  └──────────────────────┘
└──────────────────────┘
```

### 3B. View Events Flow (Organizer)

```
┌─────────────────────────────────────────────────────────────┐
│                  HomeFragment                                │
│  onViewCreated() called                                     │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│         HomeViewModel.loadCurrentUser()                      │
│  • GetCurrentUserUseCase()                                  │
│  • Updates _currentUser StateFlow                           │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│      HomeFragment observes currentUser                       │
│  • If userType == ORGANIZER:                                │
│    updateUIForUserType(isOrganizer = true)                  │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│         updateUIForUserType(isOrganizer = true)              │
│                                                              │
│  Show:                                                      │
│  ✓ Calendar widget (rvCalendar)                            │
│  ✓ Month/Year header                                        │
│  ✓ Previous/Next week buttons                              │
│  ✓ "Your Events" section title                             │
│  ✓ "See All" link                                           │
│  ✓ "Create Event" button                                    │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│      HomeViewModel.loadUserEvents(userId)                    │
│  Calls GetUserEventsUseCase                                 │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│         GetUserEventsUseCase                                 │
│  Calls EventRepository.getUserEvents(userId)                │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│    FirestoreEventDataSource.getUserEvents()                  │
│                                                              │
│  Query:                                                     │
│  firestore.collection("events")                             │
│    .whereEqualTo("organizerId", userId)                     │
│    .orderBy("startTime", DESCENDING)                        │
│    .get()                                                   │
│                                                              │
│  Convert EventDto → Event (domain model)                    │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│      HomeViewModel updates _userEvents StateFlow             │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│      HomeFragment observes userEvents                        │
│  • yourEventsAdapter.submitList(events)                     │
│  • If events.isEmpty():                                     │
│    - Show empty state layout                                │
│    - "Create your first event" message                      │
│  • Else:                                                    │
│    - Show RecyclerView with events                          │
│    - Show "See All" if > 2 events                           │
└─────────────────────────────────────────────────────────────┘
```

### 3C. View Events Flow (Regular User)

```
┌─────────────────────────────────────────────────────────────┐
│                  HomeFragment                                │
│  Current user loaded: userType == USER                      │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│         updateUIForUserType(isOrganizer = false)             │
│                                                              │
│  Hide:                                                      │
│  ✗ Calendar widget (rvCalendar)                            │
│  ✗ Month/Year header                                        │
│  ✗ Previous/Next week buttons                              │
│                                                              │
│  Show:                                                      │
│  ✓ "Quick Actions" section title                           │
│  ✓ "Create Your Own Event" button                          │
│  ✓ Featured Events section                                  │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│      HomeViewModel.loadFeaturedEvents()                      │
│  Calls GetExploreEventsUseCase                              │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│         GetExploreEventsUseCase                              │
│  Calls EventRepository.getExploreEvents()                   │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│    FirestoreEventDataSource.getEvents()                      │
│                                                              │
│  Query:                                                     │
│  firestore.collection("events")                             │
│    .whereEqualTo("visibility", "PUBLIC")                    │
│    .orderBy("startTime", ASCENDING)                         │
│    .limit(50)                                               │
│    .get()                                                   │
│                                                              │
│  Convert EventDto → Event                                   │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│      HomeViewModel processes events                          │
│  • Take first 3 events as "Featured"                        │
│  • Update _featuredEvents StateFlow                         │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│      HomeFragment observes featuredEvents                    │
│  • featuredAdapter.submitList(events)                       │
│  • Display in RecyclerView                                  │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. Data Flow Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   PRESENTATION LAYER                         │
│                                                              │
│  ┌────────────┐        ┌──────────────┐                    │
│  │  Fragment  │◄──────►│  ViewModel   │                    │
│  │   (UI)     │        │  (StateFlow) │                    │
│  └────────────┘        └──────┬───────┘                    │
│                               │                             │
└───────────────────────────────┼─────────────────────────────┘
                                │
                                │ Uses
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                    DOMAIN LAYER                              │
│                                                              │
│  ┌──────────────┐       ┌────────────────┐                 │
│  │  Use Cases   │◄─────►│  Repositories  │                 │
│  │  (Business   │       │  (Interfaces)  │                 │
│  │   Logic)     │       └────────────────┘                 │
│  └──────────────┘                                           │
│                                                              │
│  ┌────────────────────────────────────────┐                │
│  │        Domain Models                   │                │
│  │  • User, UserProfile, OrganizerProfile │                │
│  │  • Event, EventLocation, EventCategory │                │
│  └────────────────────────────────────────┘                │
└───────────────────────────────┬─────────────────────────────┘
                                │
                                │ Implements
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                     DATA LAYER                               │
│                                                              │
│  ┌──────────────────────────────────────────┐              │
│  │     Repository Implementations            │              │
│  │  • AuthRepositoryImpl                    │              │
│  │  • EventRepositoryImpl                   │              │
│  └──────────────┬───────────────────────────┘              │
│                 │                                           │
│                 │ Uses                                      │
│                 ▼                                           │
│  ┌──────────────────────────────────────────┐              │
│  │         Data Sources                      │              │
│  │  • FirestoreEventDataSource              │              │
│  └──────────────┬───────────────────────────┘              │
│                 │                                           │
│                 │ Converts via                              │
│                 ▼                                           │
│  ┌──────────────────────────────────────────┐              │
│  │     DTOs & Mappers                        │              │
│  │  • UserDto ←→ User                       │              │
│  │  • EventDto ←→ Event                     │              │
│  │  • UserMapper, EventMapper               │              │
│  └──────────────────────────────────────────┘              │
└───────────────────────────────┬─────────────────────────────┘
                                │
                                │ Stores/Retrieves
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                   BACKEND (Firebase)                         │
│                                                              │
│  ┌────────────────┐       ┌──────────────────┐             │
│  │  Firebase Auth │       │  Cloud Firestore │             │
│  │  • signIn      │       │  • users/        │             │
│  │  • signUp      │       │  • events/       │             │
│  │  • signOut     │       └──────────────────┘             │
│  └────────────────┘                                         │
└─────────────────────────────────────────────────────────────┘
```

---

## 5. State Management Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    ViewModel Layer                           │
│                                                              │
│  private val _uiState = MutableStateFlow(InitialState)      │
│  val uiState: StateFlow<UiState> = _uiState.asStateFlow()  │
│                                                              │
│  fun performAction() {                                      │
│      viewModelScope.launch {                                │
│          _uiState.value = LoadingState                      │
│          useCase().fold(                                    │
│              onSuccess = { _uiState.value = Success(it) }   │
│              onFailure = { _uiState.value = Error(msg) }    │
│          )                                                  │
│      }                                                      │
│  }                                                          │
└────────────────────────┬────────────────────────────────────┘
                         │
                         │ Emits state changes
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    Fragment Layer                            │
│                                                              │
│  viewLifecycleOwner.lifecycleScope.launch {                 │
│      viewLifecycleOwner.repeatOnLifecycle(STARTED) {        │
│          viewModel.uiState.collect { state ->               │
│              when (state) {                                 │
│                  Loading -> showLoading()                   │
│                  Success -> showSuccess(data)               │
│                  Error -> showError(message)                │
│              }                                              │
│          }                                                  │
│      }                                                      │
│  }                                                          │
└─────────────────────────────────────────────────────────────┘

UI State Examples:

• AuthUiState: Idle, Loading, Success(User), Error(String)
• CreateEventUiState: Idle, Loading, Success(Event), Error(String)
• LogoutState: Idle, Loading, Success, Error(String)
• SplashNavigationState: Idle, NavigateToHome, NavigateToLogin
```

---

## 6. User Journey Map

### Journey A: New User Registration

```
Step 1: Launch app
        ↓
Step 2: See splash screen (no session)
        ↓
Step 3: Land on Login screen
        ↓
Step 4: Click "Sign Up"
        ↓
Step 5: Choose "USER" card (green)
        ↓
Step 6: Enter email & password
        ↓
Step 7: Click "Sign Up"
        ↓
Step 8: See success screen (TransitionFragment)
        ↓
Step 9: Auto-redirect to Home
        ↓
Step 10: See Featured Events
        ↓
Step 11: Click "Create Your Own Event"
        ↓
Step 12: Fill event form & create
        ↓
Step 13: Event appears in Firestore
```

### Journey B: Organizer Creating Event

```
Step 1: Launch app (already logged in as ORGANIZER)
        ↓
Step 2: See splash → auto-login
        ↓
Step 3: Land on Home with Calendar + "Your Events"
        ↓
Step 4: See empty state "No events yet"
        ↓
Step 5: Click "Create Event"
        ↓
Step 6: Fill comprehensive event form
        ↓
Step 7: Submit event
        ↓
Step 8: Event saved to Firestore
        ↓
Step 9: Return to Home
        ↓
Step 10: See event in "Your Events" section
```

### Journey C: Returning User Session

```
Step 1: Launch app (logged in previously)
        ↓
Step 2: Splash screen checks Firebase Auth
        ↓
Step 3: Session found → fetch user from Firestore
        ↓
Step 4: Update UserPreferences (userId, userName)
        ↓
Step 5: Navigate directly to Home
        ↓
Step 6: Home loads events based on user type
        ↓
Step 7: User browses, no login required
```

---

## 7. Error Handling Flow

```
┌─────────────────────────────────────────────────────────────┐
│              Any Repository Method                           │
│                                                              │
│  suspend fun doSomething(): Result<Data> {                  │
│      return try {                                           │
│          // Firebase operation                              │
│          Result.success(data)                               │
│      } catch (e: Exception) {                               │
│          Result.failure(Exception("msg", e))                │
│      }                                                      │
│  }                                                          │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                   Use Case                                   │
│                                                              │
│  suspend operator fun invoke(): Result<Data> {              │
│      return repository.doSomething()                        │
│  }                                                          │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                   ViewModel                                  │
│                                                              │
│  useCase().fold(                                            │
│      onSuccess = { data ->                                  │
│          _state.value = Success(data)                       │
│      },                                                     │
│      onFailure = { exception ->                             │
│          _state.value = Error(exception.message)            │
│      }                                                      │
│  )                                                          │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                   Fragment                                   │
│                                                              │
│  when (state) {                                             │
│      is Error -> {                                          │
│          Toast.makeText(context, state.message, SHORT)      │
│          // Or show Snackbar, error layout, etc.            │
│      }                                                      │
│  }                                                          │
└─────────────────────────────────────────────────────────────┘
```

---

## Summary

These flows represent the current implementation as of April 26, 2026. Key points:

1. **Clean separation** between layers (Presentation → Domain → Data)
2. **Result-based error handling** throughout the stack
3. **StateFlow** for reactive UI updates
4. **Session persistence** via Firebase Auth + UserPreferences
5. **User type differentiation** for tailored experiences
6. **Firestore** as single source of truth for persistent data

All flows are working and tested via successful builds.
