# RecipXtractor Android App

Android application that transforms YouTube cooking videos into structured recipes using Google Gemini 3 AI.

## 🎥 Demo
```bash
https://youtube.com/shorts/koaSXtNql_I?feature=share
```

## ✨ Features

- Extract recipes from YouTube cooking videos
- AI-powered using Google Gemini 3
- Clean, structured ingredient lists
- Step-by-step cooking instructions


## 🛠️ Tech Stack

- **Language**: Kotlin
- **Min SDK**: Android 7.0 (API 24)
- **Networking**: Retrofit
- **Backend**: Flask server with Gemini 3 API

## 📋 Prerequisites

- Android Studio
- Android device or emulator (API 24+)
- Backend server running ([Backend Repo](https://github.com/sritaaa/recipe-backend))

## 🚀 Setup Instructions

### 1. Clone the Repository
```bash
git clone https://github.com/YOUR_USERNAME/recipe-extractor-android.git
cd recipXtractor-android
```

### 2. Open in Android Studio

- Open Android Studio
- Select "Open an existing project"
- Navigate to the cloned directory
- Wait for Gradle sync

### 3. Configure Backend URL

Find the file where you set your API base URL and update:
```java
// For Android Emulator
public static final String BASE_URL = "http://10.0.2.2:5000/";

// For Physical Device (use your computer's local IP)
public static final String BASE_URL = "http://192.168.X.X:5000/";
```

**To find your local IP:**
- Windows: Open CMD → type `ipconfig` → find IPv4 Address
- Mac: Open Terminal → type `ifconfig` → find inet address
- Linux: Open Terminal → type `ip addr` → find inet

### 4. Run the App

- Connect Android device (USB debugging enabled) OR start emulator
- Click **Run** button (green triangle)
- Select your device
- App will install and launch

## 📱 How to Use

1. Make sure backend server is running
2. Open the app
3. Paste a YouTube cooking video URL
4. Tap "Extract Recipe"
5. Wait 2-3 minutes for processing
6. View your structured recipe!

## 🔗 Related Repositories

- **Backend Server**: [https://github.com/sritaaa/recipe-backend]

## 📝 Notes for Setup

- Backend must be running for the app to work
- Processing takes 2-3 minutes depending on video length
- Works best with clear cooking tutorial videos


