# VeloRix Esports — Complete Admin Panel Integration Master Prompt

> **HOW TO USE THIS FILE:**
> Copy everything inside the prompt block below (or pass this entire `.md` file directly) to your **Admin Panel Coding Agent**. It contains the exact Firebase Realtime Database paths, Cloud Firestore collections, Firebase Storage paths, field names, data types, status enums, and bidirectional sync rules implemented in the **VeloRix Esports Android App** over the past month.

---

## COPY & PASTE THIS PROMPT TO YOUR ADMIN PANEL AGENT

```markdown
You are building/updating the **VeloRix Esports Admin Panel** so that it is 100% synchronized with the **VeloRix Esports Android Application**.

CRITICAL ARCHITECTURE RULE:
The Android app uses a **Dual-Database Authoritative Architecture** (both **Firebase Realtime Database (RTDB)** AND **Cloud Firestore**). Whenever the Admin Panel creates, updates, approves, rejects, or deletes any record, it **MUST write to BOTH Firebase Realtime Database AND Cloud Firestore simultaneously** using the exact paths and schemas documented below so the Android app reacts in <100ms in real time.

Below is the complete specification of every module, database path, schema, and workflow added to the VeloRix Esports Android App. Implement or update every single section in the Admin Panel UI and backend logic.

---

# 1. DEDICATED ADMIN CHANNELS & INBOX ROUTING SYSTEM

The Android app routes every user-initiated request, report, financial ticket, compliance action, and situational test to dedicated channel nodes in both RTDB and Firestore.

### Database Paths
- **RTDB Primary Channel Stream:** `/admin_channels/{channelType}/{requestId}`
- **RTDB Secondary Mirror:** `/admin_requests/{channelType}/{requestId}`
- **Firestore Primary Collection:** `admin_channels/{channelType}/items/{requestId}`
- **Firestore Secondary Collection:** `admin_requests_{channelType}/{requestId}`

### Active `{channelType}` Keys to Subscribe To in the Admin Panel Sidebar
1. `account_deletion_requests` (`#account-deletions`) — User permanent account deletion requests.
2. `data_export_requests` (`#data-exports`) — User DPDP Act 2023 JSON archive exports & resync logs.
3. `deposit_requests` (`#deposits`) — Manual UPI / UTR / SpacePay / Razorpay deposit verification requests.
4. `withdrawal_requests` (`#withdrawals`) — Winnings withdrawal payout requests (UPI / Bank).
5. `support_tickets` (`#support-tickets`) — Live customer support chat tickets & problem reports.
6. `player_reports` (`#player-reports`) — Anti-cheat / hacker / toxic player reports with screenshot proof.
7. `kyc_requests` (`#kyc-verifications`) — 18+ identity & PAN/Aadhaar/In-Game UID verification requests.
8. `tournament_disputes` (`#match-disputes`) — Match result disputes, custom room issues, and prize claims.
9. `token_conversions` (`#token-ledger`) — Token-to-VT (10 Tokens = 1 VT) conversion audit events.
10. `admin_situation_tests` (`#situation-tester`) — Live diagnostic broadcasts triggered from the app's Admin Situation Tester.

### Common Envelope Schema for All Channel Items
```json
{
  "id": "req_account_deletion_requests_1730548000000_a1b2c",
  "channelType": "account_deletion_requests",
  "adminChannel": "admin_channels/account_deletion_requests",
  "channelLabel": "#account-deletions",
  "title": "Account Deletion Request: @PlayerOne",
  "message": "User @PlayerOne requested permanent account deletion...",
  "senderUid": "uid_12345",
  "senderName": "PlayerOne",
  "status": "PENDING_DELETION | DISPATCHED | EXPORTED | PENDING | APPROVED | REJECTED | RESOLVED",
  "createdAt": 1730548000000,
  "timestamp": 1730548000000
}
```

---

# 2. ACCOUNT DELETION & DATA EXPORT (DPDP ACT 2023 COMPLIANCE)

## 2A. Account Deletion Requests (`#account-deletions`)
When a user clicks **"DELETE ACCOUNT"** in the Android app's Danger Zone, the app awaits writes to all of the following paths BEFORE signing the user out:
- **RTDB:**
  - `/account_deletion_requests/{requestId}`
  - `/admin_channels/account_deletion_requests/{requestId}`
  - `/admin_requests/account_deletion_requests/{requestId}`
  - `/users/{userId}` (patched with `accountStatus: "PENDING_DELETION"`, `deletionRequested: true`, `deletionRequestId`, `deletionReason`, `deletionDetails`, `deletionRequestedAt`)
- **Firestore:**
  - `account_deletion_requests/{requestId}`
  - `admin_channels/account_deletion_requests/items/{requestId}`
  - `admin_requests_account_deletion_requests/{requestId}`
  - `users/{userId}` (merged with the same `accountStatus: "PENDING_DELETION"` fields)

### Payload Schema (`account_deletion_requests/{requestId}`)
```json
{
  "id": "del_9f8e7d6c_1730548000000",
  "requestId": "del_9f8e7d6c_1730548000000",
  "userId": "9f8e7d6c5b4a3210",
  "username": "ShadowSniper",
  "phoneOrEmail": "player@example.com",
  "inGameName": "VLX_SHADOW",
  "freeFireId": "2849102938",
  "balance": 140.0,
  "depositBalance": 40.0,
  "winningsBalance": 90.0,
  "bonusBalance": 10.0,
  "tokens": 250,
  "reason": "Not using the app anymore | Too many bugs | Privacy concerns | Other",
  "details": "User's optional explanation",
  "channelNode": "account_deletion_requests",
  "adminChannel": "admin_channels/account_deletion_requests",
  "channelLabel": "#account-deletions",
  "status": "PENDING_DELETION",
  "requestedAt": 1730548000000,
  "timestamp": 1730548000000
}
```

### Required Admin Panel Actions for Account Deletion
1. **Approve & Permanently Delete User**:
   - Delete `/users/{userId}` in RTDB and `users/{userId}` in Firestore (or set `accountStatus: "DELETED"` and wipe PII).
   - Update `status: "COMPLETED"` and `processedAt: Date.now()` in `/account_deletion_requests/{requestId}`, `/admin_channels/account_deletion_requests/{requestId}`, and Firestore `account_deletion_requests/{requestId}`.
2. **Reject / Restore Account**:
   - Update `/users/{userId}` and Firestore `users/{userId}` with `accountStatus: "ACTIVE"`, `deletionRequested: false`.
   - Update `status: "RESTORED"` on the deletion request nodes.

---

## 2B. Account Data Export Archive (`#data-exports`)
When a user clicks **"EXPORT ACCOUNT DATA"** in the app's Danger Zone, the app compiles a full DPDP Act 2023 JSON archive (profile, wallet balances, transactions, joined tournaments, notification count), allows the user to Copy/Share/Save the `.json` file on-device, and uploads the full export record to:
- **RTDB:**
  - `/data_export_requests/{exportId}`
  - `/admin_channels/data_export_requests/{exportId}`
  - `/admin_requests/data_export_requests/{exportId}`
  - `/users/{userId}` (`dataExported: true`, `lastExportedAt: timestamp`, `lastExportId: exportId`)
- **Firestore:**
  - `data_export_requests/{exportId}`
  - `admin_channels/data_export_requests/items/{exportId}`
  - `users/{userId}` (`dataExported: true`, `lastExportedAt: timestamp`, `lastExportId: exportId`)

### Required Admin Panel Features for Data Exports
- Display a **"Data Export Audit Log"** table showing every user export (`exportId`, `username`, `userId`, `balance`, `transactionCount`, `joinedTournamentCount`, `requestedAt`).
- Include a **"View / Download JSON Archive"** modal in the Admin Panel that renders the `exportedJson` field and allows admins to download the exact `.json` snapshot the user exported.

---

# 3. AUTHORITATIVE USER PROFILE, AVATAR (CAMERA + FIREBASE STORAGE), BAN & SUSPENSION SYSTEM

## 3A. User Schema (`/users/{uid}` in RTDB & `users/{uid}` in Firestore)
```json
{
  "id": "uid_12345",
  "username": "ShadowSniper",
  "phoneOrEmail": "player@example.com",
  "inGameName": "VLX_SHADOW",
  "freeFireId": "2849102938",
  "bio": "Competitive BR & CS Rusher",
  "socialLink": "https://instagram.com/vlx_shadow",
  "avatarUrl": "https://firebasestorage.googleapis.com/v0/b/.../avatars%2Fuid_12345_1730548000.jpg?alt=media",
  "balance": 150.0,
  "depositBalance": 50.0,
  "winningsBalance": 85.0,
  "bonusBalance": 15.0,
  "lockedBalance": 0.0,
  "tokens": 320,
  "referralCode": "VLXSHADOW99",
  "referredBy": "VLXALPHA01",
  "referralCount": 4,
  "referralEarnings": 45.0,
  "kycVerified": true,
  "dataExported": false,
  "isBanned": false,
  "banReason": "",
  "isSuspended": false,
  "suspensionEndTimeMillis": 0,
  "suspensionReason": "",
  "accountStatus": "ACTIVE",
  "version": 12,
  "updatedAt": 1730548000000
}
```

## 3B. Camera Profile Picture & Firebase Storage Integration
Users can capture a photo with their device camera (or pick from gallery / presets) in the Android app.
- The app uploads the compressed JPEG (`512x512`) to **Firebase Storage** at path:
  - `avatars/{userId}_{timestamp}.jpg` (and/or `profile_pictures/{userId}.jpg`)
- If Firebase Storage rules/bucket fallback is triggered, the app stores a compact `data:image/jpeg;base64,...` URI in `avatarUrl`.
- **Admin Panel Requirement:**
  - The Admin Panel must render `avatarUrl` whether it is an `https://` URL, a `data:image/...;base64,...` URI, or a preset key (`avatar_1` to `avatar_12`).
  - Provide an **"Admin Reset Avatar"** button that resets inappropriate profile photos back to `"avatar_1"` in both RTDB `/users/{uid}/avatarUrl` and Firestore `users/{uid}.avatarUrl` and deletes the file in Firebase Storage if applicable.

## 3C. Authoritative Versioned Sync (`version` & `updatedAt`)
The Android app uses `FirestoreUserSyncManager` with monotonic versioning.
- **CRITICAL:** Whenever the Admin Panel modifies a user's wallet (`balance`, `depositBalance`, `winningsBalance`, `bonusBalance`), `tokens`, `isBanned`, `isSuspended`, or profile fields:
  1. Increment `version` by `+1` (or use `FieldValue.increment(1)` in Firestore) and set `updatedAt = Date.now()`.
  2. Always keep `balance = depositBalance + winningsBalance + bonusBalance`.
  3. Write the update to **both** Firestore `users/{uid}` AND RTDB `/users/{uid}`.

## 3D. Permanent Ban vs. Temporary Time-Bound Suspension
1. **Permanent Ban**:
   - Write to RTDB `/banned_users/{uid}`: `{ "banned": true, "reason": "Aimbot / Wallhack detected", "timestamp": Date.now() }`
   - Write to RTDB `/users/{uid}` & Firestore `users/{uid}`: `{ "isBanned": true, "banReason": "Aimbot / Wallhack detected", "accountStatus": "BANNED", "updatedAt": Date.now() }`
   - To **Unban**: Remove `/banned_users/{uid}` and set `isBanned: false, banReason: "", accountStatus: "ACTIVE"` in both RTDB & Firestore.
2. **Temporary Time-Bound Suspension (With Live Countdown Timer in App)**:
   - Set in RTDB `/users/{uid}` and Firestore `users/{uid}`:
     ```json
     {
       "isSuspended": true,
       "suspensionEndTimeMillis": 1730634400000,
       "suspensionReason": "Teaming in Solo BR Match #402",
       "accountStatus": "SUSPENDED",
       "updatedAt": 1730548000000
     }
     ```
   - The Android app automatically displays a full-screen **Account Suspended** overlay with a live ticking countdown timer (`HH:MM:SS`) until `suspensionEndTimeMillis`.

---

# 4. TOURNAMENTS, CUSTOM UPLOADED BANNERS, LIVE LOBBY CHAT & MINI-PLAYER

## 4A. Tournament Paths & Multi-Key Image Support
- **RTDB:** `/tournaments/{tournamentId}`
- **Firestore:** `tournaments/{tournamentId}`

### Tournament Schema
```json
{
  "id": "tour_1730548000",
  "title": "VeloRix Pro Scrims #108",
  "gameName": "Free Fire MAX",
  "gameMode": "Battle Royale | Clash Squad | Lone Wolf | Special Mode",
  "teamType": "SOLO | DUO | SQUAD",
  "map": "Bermuda | Purgatory | Kalahari | Alpine | Nexterra",
  "entryFee": 25.0,
  "prizePool": 1000.0,
  "perKillPrize": 10.0,
  "maxSlots": 48,
  "joinedCount": 18,
  "status": "UPCOMING | LIVE | COMPLETED | CANCELLED",
  "startTime": "2026-11-05 18:00",
  "startTimeMillis": 1730829600000,
  "roomId": "84920192",
  "roomPassword": "778",
  "streamUrl": "https://youtube.com/watch?v=...",
  "imageUrl": "https://... OR data:image/jpeg;base64,...",
  "bannerUrl": "https://... OR data:image/jpeg;base64,...",
  "coverImage": "https://... OR data:image/jpeg;base64,...",
  "rules": "No emulators. Level 40+ accounts only.",
  "prizeDistribution": [
    { "rank": "1st Place", "amount": 400 },
    { "rank": "2nd Place", "amount": 200 },
    { "rank": "3rd Place", "amount": 100 }
  ],
  "updatedAt": 1730548000000
}
```
> **IMPORTANT IMAGE RULE:** When an admin uploads a custom image for a tournament in the Admin Panel (either via Firebase Storage URL, external URL, or Base64 Data URL `data:image/jpeg;base64,...`), save the image value into `imageUrl`, `bannerUrl`, AND `coverImage` on the tournament object so all cards, hero headers, and detail screens in the Android app immediately render the admin's uploaded cover image instead of fallback category artwork.

## 4B. Tournament Participants & Slot Management
- **RTDB:**
  - `/tournaments/{tournamentId}/participants/{userId}`
  - `/tournament_participants/{tournamentId}/{userId}`
- **Firestore:**
  - `tournaments/{tournamentId}/participants/{userId}`
- Participant object:
  ```json
  {
    "userId": "uid_12345",
    "username": "ShadowSniper",
    "inGameName": "VLX_SHADOW",
    "freeFireId": "2849102938",
    "slotNumber": 14,
    "kills": 6,
    "rank": 1,
    "prizeWon": 460.0,
    "joinedAt": 1730548000000
  }
  ```

## 4C. Room ID & Password Dispatch (Triggers Live Mini-Player & Notification)
When the Admin Panel updates `roomId` and `roomPassword` on a tournament (10–15 minutes before match start):
1. Write `roomId`, `roomPassword`, and `status` (`"UPCOMING"` or `"LIVE"`) to both RTDB `/tournaments/{id}` and Firestore `tournaments/{id}`.
2. Broadcast a notification to all joined participants at `/notifications/{userId}/{notifId}` with `type: "ROOM_CREDENTIALS"` so the Android app's **Floating Tournament Mini-Player** and system notification immediately alert players that the Room ID & Password are live.

## 4D. Tournament Lobby Chat & Admin Moderation
- **RTDB:** `/tournament_chats/{tournamentId}/{messageId}` AND `/tournaments/{tournamentId}/chat/{messageId}`
- **Firestore:** `tournaments/{tournamentId}/chat/{messageId}`
- Message Schema:
  ```json
  {
    "id": "msg_1730548000",
    "tournamentId": "tour_1730548000",
    "senderId": "admin",
    "senderName": "VeloRix Official",
    "senderAvatar": "",
    "message": "Room ID & Password have been published! Join Slot 1-48 strictly.",
    "isAdmin": true,
    "isPinned": true,
    "timestamp": 1730548000000
  }
  ```
- **Admin Panel Requirement:** Allow admins to view live lobby chat for any tournament, post Official Admin Messages (`isAdmin: true`), pin announcements (`isPinned: true`), and delete abusive messages.

---

# 5. WALLET, DEPOSITS (UPI / SPACEPAY / RAZORPAY), WITHDRAWALS & TOKEN LEDGER

## 5A. Deposit Requests (`/deposit_requests/{reqId}` & `deposit_requests/{reqId}`)
When a user deposits via Manual UPI QR / UTR or Gateway:
- **RTDB:** `/deposit_requests/{reqId}`, `/admin_channels/deposit_requests/{reqId}`, `/transactions/{userId}/{txId}`
- **Firestore:** `deposit_requests/{reqId}`, `transactions/{txId}`
- Schema:
  ```json
  {
    "id": "dep_1730548000",
    "transactionId": "tx_1730548000",
    "userId": "uid_12345",
    "username": "ShadowSniper",
    "amount": 100.0,
    "utrNumber": "431092837465",
    "paymentMethod": "UPI_MANUAL | SPACEPAY | RAZORPAY",
    "status": "PENDING | APPROVED | REJECTED",
    "timestamp": 1730548000000
  }
  ```
- **When Admin Approves a Deposit:**
  1. Mark `/deposit_requests/{reqId}/status = "APPROVED"` and `/transactions/{userId}/{txId}/status = "COMPLETED"` (plus Firestore `transactions/{txId}`).
  2. Increment the user's `depositBalance` by `amount` and recalculate `balance = depositBalance + winningsBalance + bonusBalance`, incrementing `version` by `+1` in both RTDB `/users/{userId}` and Firestore `users/{userId}`.
  3. **Referral Commission Check:** If the user has a non-empty `referredBy` code, look up the referrer user, calculate their tier commission (10% to 15% of deposit `amount`), credit `bonusBalance` (or `referralEarnings`) on the referrer's account, and send a `"REFERRAL"` notification.

## 5B. Withdrawal Requests (`/withdrawal_requests/{reqId}` & `withdrawal_requests/{reqId}`)
- **RTDB:** `/withdrawal_requests/{reqId}`, `/admin_channels/withdrawal_requests/{reqId}`, `/transactions/{userId}/{txId}`
- **Firestore:** `withdrawal_requests/{reqId}`, `transactions/{txId}`
- Schema:
  ```json
  {
    "id": "wd_1730548000",
    "transactionId": "tx_1730548000",
    "userId": "uid_12345",
    "username": "ShadowSniper",
    "amount": 250.0,
    "upiId": "shadowsniper@okaxis",
    "bankAccount": "",
    "ifscCode": "",
    "status": "PENDING | APPROVED | REJECTED",
    "adminNote": "",
    "timestamp": 1730548000000
  }
  ```
- **Note:** When the user submits a withdrawal in the Android app, `amount` is already deducted from `winningsBalance` and placed into `lockedBalance` (or deducted immediately).
  - **If Admin Approves:** Set `status = "APPROVED"` on `withdrawal_requests/{reqId}` and `status = "COMPLETED"` on `transactions/{txId}`, clear `lockedBalance`, and send a notification to the user.
  - **If Admin Rejects:** Set `status = "REJECTED"`, refund `amount` back to the user's `winningsBalance` and `balance` (with `version + 1` in both RTDB & Firestore), and set `status = "REFUNDED"` or `"REJECTED"` on the transaction.

## 5C. Token-to-VT Conversion Ledger (10 Tokens = 1 VT)
- Users earn Tokens from Missions, Daily Spin/Check-ins, and Referrals, and convert them in multiples of 10 (`10 Tokens = 1 VT Bonus Balance`).
- **RTDB:** `/token_conversions/{conversionId}`, `/admin_channels/token_conversions/{conversionId}`
- **Firestore:** `token_conversions/{conversionId}`
- The Admin Panel should display the Token Conversion Ledger and allow admins to grant bonus Tokens or adjust mission rewards at `/missions/{missionId}` (RTDB) and `missions/{missionId}` (Firestore).

---

# 6. SYSTEM CONFIG, REMOTE CONTROLS & ADMIN SITUATION TESTER

The Android app listens in real time to `/system_config` (RTDB) and `system_config/global` (Firestore) for platform-wide switches, payment gateway configuration, and emergency overlays.

### Schema (`/system_config` in RTDB & `system_config/global` in Firestore)
```json
{
  "maintenanceMode": false,
  "maintenanceMessage": "Scheduled server upgrade in progress. Back in 30 minutes!",
  "maintenanceEndTimeMillis": 1730551600000,
  "forceUpdateRequired": false,
  "latestVersionCode": 2,
  "latestVersionName": "1.1.0",
  "updateDownloadUrl": "https://velorix.esports/download/velorix-latest.apk",
  "updateChangelog": "• Faster live match updates\n• Enhanced anti-cheat protection",
  "upiId": "velorix.pay@okaxis",
  "upiMerchantName": "VeloRix Esports",
  "minDepositVt": 10.0,
  "minWithdrawalVt": 50.0,
  "tokenConversionRate": 10,
  "whatsappSupportUrl": "https://wa.me/919999999999",
  "telegramChannelUrl": "https://t.me/velorixesports",
  "instagramUrl": "https://instagram.com/velorixesports",
  "discordUrl": "https://discord.gg/velorix",
  "vpnBlockEnabled": true,
  "updatedAt": 1730548000000
}
```

### Promotional Banners (`/banners/{bannerId}` in RTDB & `banners/{bannerId}` in Firestore)
```json
{
  "id": "ban_1",
  "title": "MEGA SUNDAY SHOWDOWN — 10,000 VT PRIZE POOL",
  "subtitle": "Register now before slots fill up",
  "imageUrl": "https://... OR data:image/jpeg;base64,...",
  "actionUrl": "tournament:tour_1730548000",
  "isActive": true,
  "priority": 1
}
```

---

# 7. SUPPORT TICKETS, PROBLEM REPORTS & BROADCAST NOTIFICATIONS

## 7A. Support Tickets & Problem Reports
- **RTDB:** `/support_tickets/{ticketId}`, `/problem_reports/{reportId}`, `/admin_channels/support_tickets/{ticketId}`
- **Firestore:** `support_tickets/{ticketId}`, `problem_reports/{reportId}`, `admin_channels/support_tickets/items/{ticketId}`
- Schema:
  ```json
  {
    "id": "rep_1730548000",
    "userId": "uid_12345",
    "username": "ShadowSniper",
    "category": "PAYMENT_ISSUE | TOURNAMENT_DISPUTE | BUG_REPORT | HACKER_REPORT | GENERAL_SUPPORT",
    "subject": "Deposit UTR 431092837465 not credited",
    "description": "Paid ₹100 via GPay 20 mins ago.",
    "screenshotUrl": "https://... OR data:image/jpeg;base64,...",
    "status": "OPEN | IN_PROGRESS | RESOLVED | CLOSED",
    "adminReply": "Verified UTR and credited 100 VT to your Deposit Balance!",
    "repliedAt": 1730548600000,
    "createdAt": 1730548000000,
    "timestamp": 1730548000000
  }
  ```
- **When Admin Replies to a Ticket/Report:**
  1. Update `status` (`"IN_PROGRESS"` or `"RESOLVED"`), `adminReply`, and `repliedAt` in both RTDB (`/support_tickets/{id}` and `/problem_reports/{id}`) and Firestore (`support_tickets/{id}` and `problem_reports/{id}`).
  2. Push a notification to `/notifications/{userId}/{notifId}` so the user sees the admin response immediately inside the app's **My Reports** dialog and Notification Center.

## 7B. Global & Targeted Push / In-App Notifications
- **Global Broadcasts:** Write to RTDB `/notifications/global/{notifId}` and `/notifications/{notifId}`, plus Firestore `notifications/{notifId}`.
- **Targeted User Notifications:** Write to RTDB `/notifications/{userId}/{notifId}` and Firestore `users/{userId}/notifications/{notifId}`.
- Notification Schema:
  ```json
  {
    "id": "notif_1730548000",
    "userId": "ALL | uid_12345",
    "title": "Room ID Released — Pro Scrims #108",
    "message": "Room ID: 84920192 | Pass: 778. Join within 5 minutes!",
    "type": "TOURNAMENT | WALLET | SYSTEM | REFERRAL | SUPPORT | ROOM_CREDENTIALS",
    "isRead": false,
    "timestamp": 1730548000000
  }
  ```

---

# 8. IMPLEMENTATION CHECKLIST FOR THE ADMIN PANEL AGENT

Please implement or verify the following views and controllers in the Admin Panel:
1. **Dual-Write Service Layer (`syncDualDatabase`)**: Create a helper utility in the Admin Panel that writes every mutation to **both** Firebase Realtime Database and Cloud Firestore.
2. **Admin Channels Live Feed Sidebar**: Real-time listeners on `/admin_channels/{channelType}` with unread badges for `#account-deletions`, `#data-exports`, `#deposits`, `#withdrawals`, `#support-tickets`, `#player-reports`, `#kyc-verifications`, and `#match-disputes`.
3. **Account Deletion & DPDP Data Export Manager**:
   - View pending account deletion requests from `/account_deletion_requests` & `/admin_channels/account_deletion_requests`, with **Approve (Wipe User)** and **Reject (Restore User)** buttons.
   - View exported user data archives from `/data_export_requests` & `/admin_channels/data_export_requests`, with a JSON viewer and `.json` file download button.
4. **User & Security Control Center**:
   - Edit wallet balances (`depositBalance`, `winningsBalance`, `bonusBalance`, `tokens`) with automatic `balance` sum and `version + 1` increment.
   - View/Reset camera-captured profile avatars (`avatarUrl` supporting Firebase Storage URLs, Base64 Data URLs, and preset IDs).
   - Toggle Permanent Ban (`/banned_users/{uid}` + `isBanned`) and Time-Bound Suspension (`isSuspended` + `suspensionEndTimeMillis` + `suspensionReason`).
5. **Tournament & Cover Image Studio**:
   - Upload tournament banners (saving to `imageUrl`, `bannerUrl`, and `coverImage`), publish `roomId` / `roomPassword` with 1-click participant notification, manage live lobby chat, and distribute match prizes/kill bounties directly to winners' `winningsBalance`.
6. **System Config & Situation Controller**:
   - Live toggles for Maintenance Mode (with countdown timer), Force App Update, UPI ID settings, Promotional Banners, and VPN Block configuration.
```
