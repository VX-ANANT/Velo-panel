# Velorix Free Fire Tournaments - Admin Panel Firebase Architecture & Agent Guidelines

## 1. Firebase Backend Architecture
- **Firebase Project ID**: `velorix-tournaments`
- **Project Number**: `27931798964`
- **Storage Bucket**: `velorix-tournaments.firebasestorage.app`
- **Firebase App IDs (`google-services.json` Synced)**:
  - `1:27931798964:android:459a432f51666b94be56e1` (`com.admin.velorix`)
  - `1:27931798964:android:8e63979e3ef30c1ebe56e1` (`com.tournaments.velorix`)
  - `1:27931798964:android:1e70339a88bafbbebe56e1` (`com.tournaments.velorix.mskqap`)
- **Primary Backend**: Dual-database design using:
  1. **Firebase Realtime Database (RTDB)** (`https://velorix-tournaments-default-rtdb.asia-southeast1.firebasedatabase.app`) for real-time live sync, real-time match slots, active players, and broadcast messaging.
  2. **Cloud Firestore** (`velorix-tournaments`) for tournament records, user profiles, transactional audit history, and administrative authorization.

## 2. Authentication Methods
The application supports:
- **Email/Password Authentication** with input sanitization.
- **Google Sign-In** via Credential Manager (`27931798964-h6fiau2df3i6e3049fontnctpsh06763.apps.googleusercontent.com`) and synced Android OAuth clients (`27931798964-fm76hd2plbv40kkqsv52bfi1ao24uh4d.apps.googleusercontent.com`, `27931798964-6maeova5d927082t82c9k33i5l5s3090.apps.googleusercontent.com`, `27931798964-m8j7b72jb7qe127ckdc5k5pvroifg8jd.apps.googleusercontent.com`).
- **Phone Authentication** via Firebase `PhoneAuthProvider` (SMS OTP verification).
- **Magic Link Authentication** (passwordless email link).

## 3. Strict Development Rules
- Use Firebase Realtime Database and Cloud Firestore as the real backend—no fake/mock persistence.
- Store authentic Firebase UIDs (`auth.currentUser.uid`) for all accounts and profiles.
- Keep package and client IDs aligned with `google-services.json`.
- Maintain pure `#000000` AMOLED layout divided by `0.5.dp` hairline dividers and underline mode selectors without nested box-in-box containers.
- Keep `/app-release.apk` in the repository root updated whenever `gradle assembleRelease` is built.
