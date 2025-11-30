# 📱 SecretPass – Multi-Condition Login App

A modern Android application demonstrating access to device sensors, permissions, and contextual APIs.  
The login screen becomes available **only when 5 real-world device conditions are met simultaneously**.

This project uses:

- **Kotlin**
- **ViewBinding**
- **Modern runtime permissions (Activity Result API)**
- **AudioRecord (non-deprecated) for microphone access**
- **Android Sensor APIs** (Accelerometer + Rotation Vector)
- **Content Providers** (ContactsContract)
- **BatteryManager** for system battery percentage

---

## 🎯 Project Goal

This app was developed as part of an Android assignment requiring a login screen that depends on multiple real-world properties from the device.  
The login should only succeed when **all** conditions are satisfied at the same time.

This project demonstrates:

- Modern permission handling
- Real-time sensor monitoring
- Contact provider queries
- Battery percentage access
- Clean lifecycle-aware design
- State-driven UI updates

---

## ✅ Conditions Required for Login

The login button is enabled only when **all 5 conditions** are true:

### 1. 🧭 Phone Points North
Uses the **rotation vector sensor** to compute the azimuth (0° = north).  
Green when the device is pointing roughly north (350°–10°).

---

### 2. 🔊 Environment Is Noisy
Uses modern **AudioRecord** to read PCM samples and calculate RMS amplitude.  
If RMS > threshold → condition turns green.  
Permission required: `RECORD_AUDIO`.

---

### 3. 📳 Device Was Shaken
Uses the **accelerometer** to detect strong movement (acceleration > threshold).  
Once shaken, the condition stays green.

---

### 4. 📇 Specific Contact Exists
Checks if a contact with an **exact, case-sensitive name** exists in the device’s contacts list.  
Rechecked every time the user returns to the app.  
Permission required: `READ_CONTACTS`.

---

### 5. 🔋 Correct Password: `secret + battery%`
The password must match:
secret<current battery percent>

Example:  
Battery at 73% → Password must be `secret73`.

No permission required.

---

## 🖥️ App Flow

1. App launches.
2. Requests contacts permission immediately.
3. Checks contact existence.
4. Starts sensors:
    - Rotation vector → north detection
    - Accelerometer → shake detection
5. Requests microphone permission when starting noise monitoring.
6. UI updates live based on all 5 conditions.
7. When all are green → login button becomes enabled.
8. User navigates to `SuccessActivity`.


