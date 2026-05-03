# Firebase Cloud Messaging (FCM) Setup Guide

This guide walks you through setting up Firebase Cloud Messaging for the Event Finder app.

---

## Overview

The FCM implementation is **ready to use** with the following features:
- ✅ Firebase Messaging Service configured
- ✅ Notification channels created (Default, High Priority, Urgent, Reminders)
- ✅ Firestore integration for notification storage
- ✅ Background workers for scheduled notifications
- ✅ Token management and topic subscriptions
- ✅ Deep linking support (ready for implementation)

---

## Step 1: Firebase Console Setup

### 1.1 Add google-services.json

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project (or create one)
3. Go to **Project Settings** → **General**
4. Under **Your apps**, click on your Android app
5. Download `google-services.json`
6. Place it in: `/app/google-services.json`

**Location:**
```
event_finder/
└── app/
    └── google-services.json  <-- Place here
```

### 1.2 Enable Cloud Messaging

1. In Firebase Console, go to **Build** → **Cloud Messaging**
2. Under **Firebase Cloud Messaging API**:
   - Click **Manage API in Google Cloud Console**
   - Enable **Firebase Cloud Messaging API** (if not already enabled)

---

## Step 2: Verify Dependencies

All dependencies are already added. Verify in `app/build.gradle.kts`:

```kotlin
// Firebase
implementation(platform(libs.firebase.bom))
implementation(libs.firebase.firestore)
implementation(libs.firebase.auth)
implementation(libs.firebase.storage)
implementation(libs.firebase.messaging)  // ✅ Added

// WorkManager
implementation(libs.androidx.work.runtime.ktx)
implementation(libs.hilt.work)
```

---

## Step 3: Firestore Setup

### 3.1 Create Collections

The app automatically creates documents, but you should set up indexes for performance:

**Collections:**
- `notifications` - Stores all notifications
- `user_fcm_tokens` - Stores FCM tokens per user (implement as needed)
- `user_preferences` - Notification preferences (implement as needed)

### 3.2 Create Firestore Indexes

Go to **Firestore** → **Indexes** → **Create Index**

**Index 1: User Notifications (Unread)**
- Collection: `notifications`
- Fields:
  - `recipientUserId` (Ascending)
  - `isRead` (Ascending)
  - `createdAt` (Descending)

**Index 2: User Notifications (All)**
- Collection: `notifications`
- Fields:
  - `recipientUserId` (Ascending)
  - `createdAt` (Descending)

**Index 3: Scheduled Notifications**
- Collection: `notifications`
- Fields:
  - `isDelivered` (Ascending)
  - `scheduledFor` (Ascending)

**Index 4: Event Notifications**
- Collection: `notifications`
- Fields:
  - `eventId` (Ascending)
  - `isDelivered` (Ascending)

### 3.3 Security Rules

Add these rules to `Firestore Rules`:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Notifications - users can only read their own
    match /notifications/{notificationId} {
      allow read: if request.auth != null && 
                     resource.data.recipientUserId == request.auth.uid;
      allow write: if false; // Only server can write
      allow update: if request.auth != null && 
                       resource.data.recipientUserId == request.auth.uid &&
                       request.resource.data.diff(resource.data).affectedKeys()
                         .hasOnly(['isRead', 'readAt']);
    }

    // User FCM Tokens
    match /user_fcm_tokens/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }

    // User Preferences
    match /user_preferences/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

---

## Step 4: Switch to Firebase Implementation

The app is currently configured to use Firebase. In `NotificationModule.kt`:

```kotlin
@Binds
@Singleton
abstract fun bindNotificationService(
    firebaseNotificationServiceImpl: FirebaseNotificationServiceImpl  // ✅ Active
): NotificationService
```

To switch back to in-memory (for testing):
```kotlin
@Binds
@Singleton
abstract fun bindNotificationService(
    notificationServiceImpl: NotificationServiceImpl  // In-memory
): NotificationService
```

---

## Step 5: Test FCM

### 5.1 Get FCM Token

Add this to your MainActivity or a test screen:

```kotlin
lifecycleScope.launch {
    val tokenManager = FcmTokenManager()
    tokenManager.getToken().onSuccess { token ->
        Log.d("FCM", "Token: $token")
        // Copy this token for testing
    }
}
```

### 5.2 Send Test Notification

Go to Firebase Console → **Cloud Messaging** → **Send your first message**

1. **Notification title**: "Test Notification"
2. **Notification text**: "This is a test"
3. **Target**: Select your app
4. Under **Additional options**:
   - Set **Custom data**:
     - Key: `type`, Value: `EVENT_CANCELLED`
     - Key: `eventId`, Value: `test123`
     - Key: `priority`, Value: `HIGH`

---

## Step 6: Subscribe to Topics

Topics allow broadcasting to groups of users:

```kotlin
val tokenManager = FcmTokenManager()

// Subscribe organizers
tokenManager.subscribeToTopic(FcmTokenManager.TOPIC_ORGANIZERS)

// Subscribe all users
tokenManager.subscribeToTopic(FcmTokenManager.TOPIC_ALL_USERS)
```

**Available Topics:**
- `all_users` - Everyone
- `organizers` - Only organizers
- `attendees` - Only attendees

---

## Step 7: Notification Permissions (Android 13+)

For Android 13+, you must request notification permission at runtime.

**Add to appropriate Activity/Fragment:**

```kotlin
private val notificationPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
) { isGranted ->
    if (isGranted) {
        Log.d("Permissions", "Notification permission granted")
    } else {
        Log.d("Permissions", "Notification permission denied")
    }
}

private fun requestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

// Call when user logs in or on first launch
requestNotificationPermission()
```

---

## Architecture Overview

### Components

1. **EventFinderMessagingService**
   - Receives FCM push notifications
   - Creates Android notifications with proper channels
   - Handles notification clicks and deep links

2. **FirebaseNotificationServiceImpl**
   - Stores notifications in Firestore
   - Sends notifications to users
   - Manages read/unread status

3. **NotificationDeliveryWorker**
   - Runs every 15 minutes
   - Checks for scheduled notifications (24h, 1h reminders)
   - Delivers notifications at the right time

4. **FcmTokenManager**
   - Retrieves FCM token
   - Manages topic subscriptions

### Notification Flow

```
Event Action (Cancel/Postpone/Reschedule)
    ↓
Use Case calls NotificationService
    ↓
FirebaseNotificationServiceImpl saves to Firestore
    ↓
[Server-side] Cloud Function sends FCM message
    ↓
EventFinderMessagingService receives message
    ↓
Android Notification displayed
    ↓
User taps → Opens app with deep link
```

---

## Cloud Functions (Optional - For Production)

For production, you should send FCM messages from a secure server.

**Example Cloud Function (Node.js):**

```javascript
const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.sendNotificationOnCreate = functions.firestore
  .document('notifications/{notificationId}')
  .onCreate(async (snap, context) => {
    const notification = snap.data();
    
    if (!notification.isDelivered && !notification.scheduledFor) {
      // Send immediately
      const message = {
        notification: {
          title: notification.title,
          body: notification.message
        },
        data: {
          type: notification.type,
          eventId: notification.eventId,
          notificationId: notification.notificationId,
          priority: notification.priority
        },
        token: await getUserFcmToken(notification.recipientUserId)
      };

      try {
        await admin.messaging().send(message);
        
        // Mark as delivered
        await snap.ref.update({
          isDelivered: true,
          deliveredAt: admin.firestore.FieldValue.serverTimestamp()
        });
        
        console.log('Notification sent:', notificationId);
      } catch (error) {
        console.error('Error sending notification:', error);
      }
    }
  });

async function getUserFcmToken(userId) {
  const doc = await admin.firestore()
    .collection('user_fcm_tokens')
    .doc(userId)
    .get();
  return doc.data()?.token;
}
```

**Deploy:**
```bash
firebase deploy --only functions
```

---

## Troubleshooting

### Issue: No notifications received

**Check:**
1. ✅ `google-services.json` is in `/app/` directory
2. ✅ Firebase Cloud Messaging API is enabled
3. ✅ App is in foreground/background (different handling)
4. ✅ Check Logcat for "FCMService" tag
5. ✅ Verify FCM token is valid

### Issue: Notifications not persisting

**Check:**
1. ✅ Firestore rules allow write access
2. ✅ Using FirebaseNotificationServiceImpl (not in-memory)
3. ✅ Network connectivity

### Issue: Scheduled notifications not delivered

**Check:**
1. ✅ NotificationDeliveryWorker is scheduled
2. ✅ Check WorkManager status: `adb shell dumpsys activity service WorkManagerService`
3. ✅ Firestore indexes created

---

## Testing Checklist

- [ ] FCM token retrieved successfully
- [ ] Test notification received from Firebase Console
- [ ] Notification appears in Android notification tray
- [ ] Tapping notification opens app
- [ ] Notification saved in Firestore
- [ ] Notification appears in NotificationsFragment
- [ ] Mark as read works
- [ ] Event postpone sends notification
- [ ] Event cancel sends notification
- [ ] Scheduled notifications work (24h/1h reminders)

---

## Production Checklist

Before going live:

- [ ] Add `google-services.json` to production
- [ ] Set up Firestore indexes
- [ ] Configure Firestore security rules
- [ ] Deploy Cloud Functions for FCM sending
- [ ] Store FCM tokens in `user_fcm_tokens` collection
- [ ] Request notification permission on user login
- [ ] Handle notification clicks with deep links
- [ ] Set up topic subscriptions based on user type
- [ ] Monitor FCM delivery reports in Firebase Console
- [ ] Set up error monitoring (Crashlytics)

---

## Next Steps

1. **Implement Token Storage**: Save FCM tokens to Firestore when user logs in
2. **Deep Linking**: Navigate to event detail when notification tapped
3. **Notification Preferences**: Let users control which notifications they receive
4. **Cloud Functions**: Move FCM sending to server-side for security
5. **Analytics**: Track notification open rates

---

## Resources

- [Firebase Cloud Messaging Documentation](https://firebase.google.com/docs/cloud-messaging)
- [Android Notification Channels](https://developer.android.com/develop/ui/views/notifications/channels)
- [WorkManager Guide](https://developer.android.com/topic/libraries/architecture/workmanager)
- Event Finder notification docs: `NOTIFICATION_SYSTEM_DOCUMENTATION.md`
