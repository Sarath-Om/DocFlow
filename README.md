# DocFlow 📄✨

[![Android CI](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Backend-Firebase%20Firestore-orange.svg)](https://firebase.google.com/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**DocFlow** is a modern, high-performance Android document scanner, organizer, and viewer designed for seamless document lifecycle management. Built with 100% Jetpack Compose and modern Android architecture, DocFlow features real-time image processing, PDF pipeline generation, local-first offline support via Room, end-to-end client-side encryption, and instant cloud sync with Google Cloud / Firebase Firestore.

---

## 🌟 Key Features

- **Document Scanner & Camera Pipeline**: Real-time perspective crop, enhancement filters (grayscale, high contrast, clean document), and multi-page batch scanning.
- **PDF Generation & Export**: On-device PDF composition and generation powered by `ImageProcessingPdfPipeline`.
- **Crystal Glass Morphic UI**: Elegant, modern translucent design system (`CrystalGlassComponents`) built natively in Jetpack Compose.
- **Local-First Architecture**: Offline-first caching with Room DB (`DocFlowDatabase`, `DocFlowDao`) synchronized seamlessly with Firebase Firestore.
- **Privacy & Security**: Built-in file encryption/decryption utilities (`CryptoUtils`) safeguarding sensitive files before synchronization.
- **Document Management**: Folder structure organization (`FolderEntity`), tagging, categorization, and text search.
- **Document Viewer & Editor**: Interactive in-app document viewing with annotation, renaming, and metadata management.
- **Cloud Sync & CI/CD**: Firebase backend with automated multi-cloud deployment workflows (`GitHub Actions` for Azure, Alibaba Cloud, Google Cloud).

---

## 🏗️ Architecture & Tech Stack

DocFlow is built following official Android architecture guidelines (MVVM / Clean Architecture):

```
DocFlow
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/com/example/
│       │   │   ├── data/
│       │   │   │   ├── local/          # Room DB, DAOs & Entities
│       │   │   │   ├── model/          # Domain Models (DocEntity, FolderEntity)
│       │   │   │   ├── repository/     # Single source of truth (DocFlowRepository)
│       │   │   │   └── util/           # Crypto, File parsers, PDF pipeline
│       │   │   ├── ui/
│       │   │   │   ├── auth/           # Authentication UI
│       │   │   │   ├── components/     # Crystal Glass UI & reusable design system
│       │   │   │   ├── screens/        # Dashboard, Scanner, DocumentViewerEditor
│       │   │   │   ├── theme/          # Compose Theme, Color palettes, Typography
│       │   │   │   └── viewmodel/      # DocFlowViewModel & State Holders
│       │   │   └── MainActivity.kt     # Single Activity Host
│       │   └── res/                    # Drawables, mipmaps, XML configs
│       └── test/                       # Unit tests & Firestore Emulator tests
├── firestore.rules                     # Cloud Firestore security rules
├── firestore.test.js                   # Firestore security tests
└── .github/workflows/                  # Automated CI/CD pipelines
```

### Core Technologies
- **Language**: Kotlin 2.0+
- **UI Toolkit**: Jetpack Compose, Material 3
- **Local Persistence**: Room SQLite DB
- **Cloud Backend**: Firebase Authentication, Cloud Firestore
- **Asynchronous Flow**: Kotlin Coroutines & Flow
- **Security**: Android Keystore & AES-256 GCM (`CryptoUtils`)
- **Testing**: JUnit, Robolectric, Google Cloud Firestore Emulator

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Ladybug (or newer)
- JDK 17 or higher
- Android SDK 34+
- A configured [Firebase Project](https://console.firebase.google.com/)

### Installation & Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-username/DocFlow.git
   cd DocFlow
   ```

2. **Configure Environment:**
   Copy the example environment file:
   ```bash
   cp .env.example .env
   ```

3. **Firebase Configuration:**
   - Download your `google-services.json` from the Firebase Console.
   - Place `google-services.json` into the `app/` directory.
   - Configure Firestore rules:
     ```bash
     firebase deploy --only firestore:rules
     ```

4. **Build and Run:**
   - Open the project in Android Studio.
   - Sync Gradle dependencies (`File > Sync Project with Gradle Files`).
   - Run on an emulator or physical device with Camera permissions enabled.

---

## 🧪 Testing

### Unit & Integration Tests
Run local unit and Robolectric tests via Gradle:
```bash
./gradlew test
```

### Firestore Security Rules Testing
Run local Firestore security tests using the Firebase Emulator Suite:
```bash
npm install -g firebase-tools
firebase emulators:exec --only firestore "npm test firestore.test.js"
```

---

## 🔐 Security & Privacy

- Documents stored locally can be encrypted at rest using Android Keystore-backed keys.
- Granular Firestore Security Rules ensure users can only query, read, and write their own documents and folder collections.

---

## 🤝 Contributing

Contributions are welcome! Please follow these steps:
1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

Distributed under the Apache License 2.0. See `LICENSE` for more information.
