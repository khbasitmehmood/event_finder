# Event Finder

Event Finder is a native Android application for discovering, creating, and
managing events. Attendees can browse events, purchase tickets, manage
favourites, and present QR tickets. Organizers can publish events, manage
bookings, view attendee statistics, and scan tickets at check-in.

## Features

- Email/password authentication and user profiles
- Event discovery, categories, search, filters, and favourites
- Event creation with location, schedule, visibility, and ticket details
- Draft, publish, postpone, reschedule, and cancel event workflows
- Ticket purchase, QR generation, validation, and attendee check-in
- Organizer dashboard, bookings, attendees, and event insights
- Firebase-backed data, storage, authentication, and notifications
- Offline-aware event operations and background work
- In-app AI chatbot

## Technology

- Kotlin
- Android Views, Material Design, View Binding, and Navigation Component
- MVVM with domain, data, and presentation layers
- Hilt dependency injection
- Kotlin Coroutines and StateFlow
- Firebase Authentication, Firestore, Storage, Cloud Messaging, and AI
- Google Maps and location services
- CameraX, ML Kit, and ZXing for QR scanning
- Gradle 8.9 and Android Gradle Plugin 8.7

## Requirements

- Android Studio with JDK 17
- Android SDK 35
- Android 7.0 (API 24) or newer device/emulator
- Internet connection for Firebase, maps, chatbot, and payment features

The Firebase configuration required by this submission is located at
`app/google-services.json`.

## Build

From the project root:

```bash
./gradlew assembleDebug
```

The generated APK is:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Prebuilt APK

The submission includes a ready-to-install APK:

**[Download EventFinder v1.0 (debug APK)](artifacts/EventFinder-v1.0-debug.apk)**

## Install

Enable USB debugging on an Android device or start an emulator, then run:

```bash
adb install -r artifacts/EventFinder-v1.0-debug.apk
```

## Tests

```bash
./gradlew test
./gradlew connectedAndroidTest
```

The second command requires a connected Android device or running emulator.

## Project Structure

```text
artifacts/
└── EventFinder-v1.0-debug.apk   # Ready-to-install APK
app/
└── src/
    ├── main/
    │   ├── java/com/eventfinder/app/
    │   │   ├── admin/          # Administrative screens
    │   │   ├── client/         # Attendee and organizer UI
    │   │   ├── data/           # Firebase, payments, DTOs, and repositories
    │   │   ├── di/             # Hilt modules
    │   │   ├── domain/         # Models, repository contracts, and use cases
    │   │   ├── fcm/            # Push notification handling
    │   │   ├── utils/          # Shared utilities
    │   │   └── worker/         # Background tasks
    │   └── res/                # Layouts, navigation, drawables, and values
    ├── test/                    # Local unit tests
    └── androidTest/             # Instrumented tests
```

## Configuration

The payment backend URL is read from `PAYMENT_API_BASE_URL` in
`gradle.properties`. Change that value before building if a different backend
is required.

## Build Configuration

- Application ID: `com.eventfinder.app`
- Minimum SDK: 24
- Target SDK: 34
- Compile SDK: 35
- Version: 1.0
