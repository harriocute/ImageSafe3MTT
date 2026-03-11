# Safe Try 3MTT — Privacy Ghost 🛡️

**An Android privacy tool that automatically protects users from leaking personal information when sharing photos.**

---

## Features

### 🔍 Share Interception
- Appears in Android's native share menu as **"Privacy Ghost"**
- Intercepts images before they reach any app (WhatsApp, Telegram, social media, etc.)

### 🧹 EXIF Metadata Scrubber
Automatically strips all sensitive metadata including:
- GPS location (latitude, longitude, altitude)
- Device model and manufacturer
- Camera and lens information
- Timestamps (capture time, digitization time)
- Orientation and maker notes

### 🤖 On-Device AI Redaction
Detects and blurs sensitive content using TensorFlow Lite:
- 🚗 Car license plates
- 🪧 Street signs and traffic signs
- 🪪 ID badges and credential cards
- 📄 Text documents visible in background
- 👤 Faces (optional toggle)

**All AI runs 100% on-device. No images are uploaded anywhere.**

### 📊 Privacy Score
- Generates a privacy score (1-10) before and after cleaning
- Shows a detailed privacy report of all issues found and fixed

### 🔄 Share Forwarding
After cleaning, re-opens the share menu so you can send the sanitized file to your intended app.

---

## Setup Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- Android SDK 34
- Kotlin 1.9.0+

### Step 1: Open in Android Studio
```bash
git clone <repo>
cd SafeTry3MTT
```
Open the project in Android Studio.

### Step 2: Add TFLite Model (Required for AI detection)

Download the EfficientDet-Lite0 model for object detection:

**Option A: Use TensorFlow Hub**
1. Download from: https://tfhub.dev/tensorflow/lite-model/efficientdet/lite0/detection/metadata/1
2. Rename to `detect.tflite`
3. Place in: `app/src/main/assets/models/detect.tflite`

**Option B: Use MobileNet SSD**
1. Download from: https://storage.googleapis.com/download.tensorflow.org/models/tflite/coco_ssd_mobilenet_v1_1.0_quant_2018_06_29.zip
2. Extract `detect.tflite`
3. Place in: `app/src/main/assets/models/detect.tflite`

> **Note:** Without the model file, the app still works — it uses Android's built-in face detection as a fallback. For full license plate/sign detection, the TFLite model is required.

### Step 3: Build and Install
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or press **Run** in Android Studio.

---

## Architecture

```
com.safetry.privacy/
├── ui/
│   ├── MainActivity.kt          # Main screen with settings
│   ├── ShareReceiverActivity.kt # Intercepts Android share intents
│   ├── ProcessingActivity.kt    # Processing & preview screen
│   └── PrivacyOverlayView.kt   # Custom view for detection boxes
├── processor/
│   ├── ExifScrubber.kt         # EXIF metadata analysis & removal
│   ├── AIRedactor.kt           # TFLite detection & blur
│   └── PrivacyScorer.kt        # Privacy score calculation
├── model/
│   └── Models.kt               # Data models
├── service/
│   └── PrivacyService.kt       # Optional background service
└── utils/
    ├── PreferencesManager.kt   # DataStore preferences
    └── FileUtils.kt            # Temp file management
```

---

## Privacy & Security

- ✅ **No Internet permission** — app works fully offline
- ✅ **No data collection** — zero telemetry or analytics
- ✅ **Temp files deleted** after sharing
- ✅ **On-device AI** — images never leave the device
- ✅ **Open source** — inspect every line of code

---

## Permissions Required

| Permission | Reason |
|---|---|
| `READ_MEDIA_IMAGES` (Android 13+) | To read selected photos |
| `READ_EXTERNAL_STORAGE` (Android 12-) | To read selected photos |
| `FOREGROUND_SERVICE` | For optional background service |

**NOT requested:** `INTERNET`, `WRITE_EXTERNAL_STORAGE` (except Android 9-)

---

## Contributing

Built for the **3MTT Nigeria** program. Contributions welcome!

---

## License

MIT License — Free for personal and commercial use.
