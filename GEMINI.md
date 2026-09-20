# Velorix Free Fire Tournaments - Admin Panel Firebase Architecture & Agent Guidelines

## 1. Firebase Backend Architecture
- **Firebase Project ID**: `velorix-tournaments`
- **Project Number**: `27931798964`
- **Firebase App ID**: `1:27931798964:android:459a432f51666b94be56e1` (`com.admin.velorix`)
- **Primary Backend**: Dual-database design using:
  1. **Firebase Realtime Database (RTDB)** (`https://velorix-tournaments-default-rtdb.asia-southeast1.firebasedatabase.app`) for real-time live sync, real-time match slots, active players, and broadcast messaging.
  2. **Cloud Firestore** (`velorix-tournaments`) for tournament records, user profiles, transactional audit history, and administrative authorization.

## 2. Authentication Methods
The application supports:
- **Email/Password Authentication** with input sanitization.
- **Google Sign-In** via Credential Manager (`27931798964-h6fiau2df3i6e3049fontnctpsh06763.apps.googleusercontent.com`).
- **Phone Authentication** via Firebase `PhoneAuthProvider` (SMS OTP verification).
- **Magic Link Authentication** (passwordless email link).

## 3. Strict Development Rules
- Use Firebase Realtime Database and Cloud Firestore as the real backend—no fake/mock persistence.
- Store authentic Firebase UIDs (`auth.currentUser.uid`) for all accounts and profiles.
- Keep package and client IDs aligned with `google-services.json`.
