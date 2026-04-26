# Event Finder - Android Application

An Android application that connects users with local events and enables organizers to create and manage events. Built with Clean Architecture, MVVM, and Firebase.

---

## 📱 Overview

Event Finder supports two user types:
- **Regular Users**: Browse, search, and join events
- **Organizers**: Create and manage professional events with verification

### Key Features

✅ **Authentication**
- Email/password registration and login
- Session persistence (auto-login)
- User type selection (USER or ORGANIZER)
- Profile management

✅ **Event Management**
- Create events with comprehensive details
- Category support (Music, Sports, Food, Technology, etc.)
- Location-based events with geohash
- Public/Private visibility
- Free and paid events

✅ **Dynamic Home Screen**
- Organizers: Calendar widget + "Your Events" section
- Regular Users: Featured Events + Quick Actions
- Both types can create events

✅ **Firebase Integration**
- Firebase Authentication
- Cloud Firestore database
- Real-time synchronization

---

## 🏗️ Architecture

The app follows **Clean Architecture** principles with three layers:

```
┌─────────────────────────────────────┐
│     Presentation Layer (UI)         │
│  Fragments, ViewModels, Adapters    │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│    Domain Layer (Business Logic)    │
│  Models, Use Cases, Repositories    │
│       (Framework-agnostic)           │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│       Data Layer (Backend)           │
│  Firebase, DTOs, Mappers, Repos     │
└─────────────────────────────────────┘
```

**Design Patterns:**
- MVVM (Model-View-ViewModel)
- Repository Pattern
- Use Case Pattern
- Dependency Injection (Hilt)
- StateFlow for reactive UI

---

## 🚀 Quick Start

### Prerequisites
- Android Studio Hedgehog or later
- JDK 11+
- Android SDK 24+ (minimum), 34 (target)
- Firebase project with Auth and Firestore enabled

### Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd event_finder-develop_pingo
   ```

2. **Add Firebase configuration**
   - Download `google-services.json` from Firebase Console
   - Place in `app/` directory

3. **Build and run**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Install on device/emulator**
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 📚 Documentation

- **[IMPLEMENTATION_REPORT.md](IMPLEMENTATION_REPORT.md)** - Comprehensive feature and architecture documentation
- **[FLOW_DIAGRAMS.md](FLOW_DIAGRAMS.md)** - Visual flow diagrams for all user journeys
- **[DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md)** - Quick reference for developers

---

## 📦 Project Structure

```
app/src/main/java/com/eventfinder/app/
├── client/              # User-facing features
│   ├── auth/           # Login, Signup, Splash
│   ├── home/           # Home screen with calendar
│   ├── createevent/    # Event creation
│   ├── explore/        # Event discovery
│   ├── profile/        # User profile
│   └── ...
│
├── domain/             # Business logic
│   ├── model/          # Domain entities
│   ├── repository/     # Repository interfaces
│   └── usecase/        # Use cases
│
├── data/               # Data layer
│   ├── model/          # DTOs
│   ├── mapper/         # Data mappers
│   ├── repository/     # Repository implementations
│   └── source/         # Data sources (Firestore)
│
├── di/                 # Dependency injection
└── utils/              # Utilities
```

---

## 🔑 Core Technologies

### Android
- **Language:** Kotlin
- **Minimum SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)
- **Architecture:** MVVM + Clean Architecture

### Libraries
- **UI:** Material Design 3, View Binding, RecyclerView
- **Async:** Kotlin Coroutines, StateFlow
- **DI:** Hilt (Dagger)
- **Navigation:** Android Navigation Component
- **Backend:** Firebase Auth, Cloud Firestore

### Backend
- **Authentication:** Firebase Authentication (Email/Password)
- **Database:** Cloud Firestore
- **Storage:** Firebase Storage (planned)
- **Notifications:** Firebase Cloud Messaging (planned)

---

## 🎨 Design

### Color Scheme
- **Primary:** `#1CAE81` (Green) - Main actions and highlights
- **Secondary:** `#FFA26B` (Orange) - Organizer-specific elements
- **Background:** `#FFFFFF` (White) - Clean, minimal design
- **Surface:** `#F8F9FD` (Light gray) - Cards and elevated surfaces

### UI Principles
- Modern minimalistic design
- Consistent 32dp icon sizing
- Clear visual hierarchy
- User type-specific interfaces

---

## 👥 User Types

### Regular User (USER)
**Can:**
- Browse and search events
- Save favorite events
- RSVP to events
- Create community events
- View featured and nearby events

**Home Screen:**
- Quick Actions section
- Featured Events list
- "Create Your Own Event" button

### Organizer (ORGANIZER)
**Can:**
- All USER capabilities, plus:
- Create professional events
- Manage created events
- View event analytics (planned)
- Get verification badge after approval

**Home Screen:**
- Calendar widget (week view)
- "Your Events" section with event list
- Featured Events list

**Verification (Pakistan-specific):**
- Organization details
- NTN/SECP registration number
- Verification documents
- Status: PENDING → VERIFIED or REJECTED

---

## 🔄 Key Flows

### App Launch Flow
```
App Start → Splash (checks session) → Home (if logged in) or Login (if not)
```

### Authentication Flow
```
Signup → Select User Type → Enter Credentials → Create Account → Home
Login → Enter Credentials → Verify → Home
```

### Event Creation Flow
```
Home → Create Event → Fill Form → Submit → Save to Firestore → Back to Home
```

### Session Management
```
Launch → Check Firebase Auth → If authenticated: fetch user → auto-login
Logout → Clear Firebase session → Clear preferences → Navigate to Login
```

---

## 🗄️ Firestore Structure

### Collections

**users/{userId}**
```json
{
  "uid": "string",
  "email": "string",
  "userType": "USER" | "ORGANIZER",
  "profile": {
    "fullName": "string",
    "phoneNumber": "string",
    "city": "string",
    "interests": ["string"]
  },
  "organizerProfile": {
    "organizationName": "string",
    "registrationNumber": "string",
    "verificationStatus": "PENDING" | "VERIFIED" | "REJECTED",
    "contactPerson": "string",
    "phoneNumber": "string"
  },
  "isProfileComplete": boolean,
  "createdAt": timestamp
}
```

**events/{eventId}**
```json
{
  "title": "string",
  "description": "string",
  "category": "MUSIC" | "SPORTS" | "FOOD" | ...,
  "organizerId": "string",
  "organizerName": "string",
  "startTime": timestamp,
  "endTime": timestamp,
  "location": {
    "latitude": number,
    "longitude": number,
    "geohash": "string",
    "name": "string",
    "city": "string"
  },
  "maxParticipants": number,
  "currentParticipantCount": number,
  "isFree": boolean,
  "price": number,
  "currency": "PKR",
  "imageUrls": ["string"],
  "visibility": "PUBLIC" | "PRIVATE",
  "createdAt": timestamp
}
```

---

## ✅ Current Status

**Build Status:** ✅ Successful

**Implemented Features:**
- ✅ Complete authentication flow (signup, login, logout)
- ✅ Session persistence with auto-login
- ✅ User type differentiation (USER vs ORGANIZER)
- ✅ Event creation with comprehensive form
- ✅ Event listing (user events, featured events)
- ✅ Dynamic home screen based on user type
- ✅ Calendar widget for organizers
- ✅ Firebase Auth + Firestore integration
- ✅ Clean architecture with MVVM

**In Progress / Planned:**
- ⏳ Profile completion flow
- ⏳ Event participation (RSVP/Join)
- ⏳ Nearby events with location
- ⏳ Event detail screen
- ⏳ Search and filters
- ⏳ Organizer verification workflow
- ⏳ Image upload to Firebase Storage
- ⏳ Notifications (Firebase Cloud Messaging)

---

## 🧪 Testing

### Run Tests
```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

### Test Coverage
- Unit tests for ViewModels
- Unit tests for Use Cases
- Unit tests for Repositories
- Integration tests (planned)
- UI tests (planned)

---

## 🐛 Known Issues

1. **Profile Completion:** Users can proceed without completing profile
2. **Image Upload:** Not integrated with Firebase Storage yet
3. **Search:** Limited to prefix matching (full-text search needed)
4. **Nearby Events:** Location permission and geohash queries not active
5. **Java Compiler:** Source/target 8 deprecation warnings

See [IMPLEMENTATION_REPORT.md](IMPLEMENTATION_REPORT.md) for details.

---

## 🤝 Contributing

### Branch Naming
- Feature: `feature/description`
- Bug fix: `bugfix/description`
- Hotfix: `hotfix/description`

### Commit Messages
Follow conventional commits:
```
feat: add new feature
fix: resolve bug
refactor: code improvement
docs: update documentation
test: add tests
```

### Before Committing
1. Run tests: `./gradlew test`
2. Build successfully: `./gradlew assembleDebug`
3. Format code
4. Review changes

---

## 📄 License

[Add license information here]

---

## 👨‍💻 Development Team

[Add team information here]

---

## 📞 Support

For issues, questions, or contributions:
1. Check documentation in `IMPLEMENTATION_REPORT.md` and `DEVELOPER_GUIDE.md`
2. Search existing issues
3. Create a new issue with detailed description

---

## 🔄 Version History

### v1.0.0 (Current - April 26, 2026)
- Initial release
- Complete authentication system
- Event creation and management
- Dynamic user type-based UI
- Firebase integration
- Clean architecture implementation

---

**Last Updated:** April 26, 2026
