# AquaSync 💧

AquaSync is a lightweight, real-time Android utility application designed to monitor and control a household water pump system synchronously across multiple family devices. Built using **Jetpack Compose** and backed by **Firebase Realtime Database**, it features automated alarm limits to prevent resource overflow with zero login friction for family members.

---

## Features ✨

* **Real-time Status Sync:** Displays whether the pump is currently running or off instantly across all connected household devices.
* **Activity Logs:** Tracks and displays who started or last stopped the pump motor (e.g., "Started by: Daniel").
* **Continuous Run Counter:** Automatically displays an increasing live runtime counter (`MM:SS`) when the pump is active.
* **Auto-Shutoff Alarms:** Supports pre-set runtime constraints (10, 20, 30, or 60 minutes). The system automatically cuts power to the pump node when the milestone is achieved.
* **Audible System Ringing:** Triggers the native device alarm tone when a timed milestone completes—both in the foreground and via background `AlarmManager` broadcasts if the device is locked.
* **Zero-Friction Identity:** Utilizes **Firebase Anonymous Authentication** securely in the background combined with a localized text setup row so family members never have to manage passwords.

---

## Local Development & Setup 🛠️

Because this repository enforces strict security rules to protect private database resources, confidential backend signatures have been omitted from the codebase via `.gitignore`. 

To compile and run this project on your local machine, follow these steps:

### 1. Add your Firebase Configuration
1. Go to the [Firebase Console](https://console.firebase.google.com/) and create a new project.
2. Add an **Android App** to your project using the package name `dc.aquasync`.
3. Download the generated `google-services.json` file.
4. Drop the file directly into the local project directory structure at:
   ```text
   AquaSync/app/google-services.json
