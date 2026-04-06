# GPay Tracker — Android App

Automatically tracks your weekly expenses by reading Google Pay notifications.
No manual entry. No cloud. All data stays on your device.

---

## Project Structure

```
GPayTracker/
├── app/src/main/
│   ├── AndroidManifest.xml
│   └── java/com/gpaytracker/
│       ├── MainActivity.kt              # Entry point, permission check
│       ├── GPayTrackerApp.kt            # Application class, DI root
│       │
│       ├── data/
│       │   ├── Expense.kt               # Room @Entity + ExpenseCategory enum
│       │   ├── ExpenseDao.kt            # All DB queries (Flow-based)
│       │   ├── ExpenseDatabase.kt       # Room database singleton
│       │   └── ExpenseRepository.kt     # Single source of truth
│       │
│       ├── service/
│       │   ├── GPayListenerService.kt   # NotificationListenerService
│       │   └── GPayNotificationParser.kt # Regex parser + categorizer
│       │
│       ├── viewmodel/
│       │   └── ExpenseViewModel.kt      # StateFlows, budget logic
│       │
│       └── ui/
│           ├── MainScreen.kt            # Scaffold + BottomNav
│           └── screens/
│               ├── DashboardScreen.kt   # Budget ring, recent txns
│               └── OtherScreens.kt      # Transactions, Analytics, Settings
│
└── app/build.gradle.kts                 # Dependencies
```

---

## How It Works

### 1. Notification Listener (`GPayListenerService`)
Android's `NotificationListenerService` lets your app receive a callback
every time any notification is posted. The service filters for Google Pay's
package names and passes the title + body text to the parser.

### 2. Parser (`GPayNotificationParser`)
Uses regex to extract:
- **Amount** — matches ₹, Rs., INR patterns
- **Merchant** — extracts from "paid to X", "payment to X", or UPI ID
- **UPI ID** — regex for `handle@bank` format

### 3. Auto-categorization
Rule-based keyword matching maps merchants to categories:
- Swiggy/Zomato → FOOD
- Uber/Ola/Rapido → TRANSPORT
- Amazon/Flipkart → SHOPPING
- BigBasket/Zepto → GROCERIES
- Netflix/Spotify → ENTERTAINMENT
- PharmEasy/Apollo → HEALTH
- Jio/Airtel/BESCOM → UTILITIES

### 4. Room Database
All expenses stored locally in SQLite via Room.
Weekly queries use Monday–Sunday epoch ranges.

---

## Setup Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1) or newer
- Android device / emulator running Android 8.0+ (API 26+)
- Google Pay installed on the test device

### Steps

1. **Clone / open** the project in Android Studio.

2. **Sync Gradle** — Android Studio will download all dependencies automatically.

3. **Build and install** on your device (Run → Run 'app').

4. **Grant Notification Access**:
   The app will open the system settings screen on first launch.
   Navigate to: `Settings → Apps → Special App Access → Notification Access`
   Enable **GPay Tracker**.

5. **Make a GPay payment** — within seconds, the transaction will appear
   in the app and you'll see a quiet confirmation notification.

---

## Extending the App

| Feature | Where to add |
|---|---|
| New merchant → category rule | `GPayNotificationParser.categorize()` |
| Support other UPI apps (PhonePe, Paytm) | Add package to `GPAY_PACKAGES` set |
| CSV export | Add method in `ExpenseRepository`, call from `SettingsScreen` |
| Monthly view | Add new `@Query` in `ExpenseDao` with month range |
| Budget alerts | Add check in `GPayListenerService` after insert |
| ML categorization | Replace rule-map in parser with TFLite model |

---

## Permissions Used

| Permission | Why |
|---|---|
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Required to read notifications |
| `POST_NOTIFICATIONS` | Show confirmation notifications (Android 13+) |

> **Privacy note:** No data ever leaves the device. The app never makes
> network requests. Notification content is only processed locally.
