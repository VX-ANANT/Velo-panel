# VELORIX FREE FIRE TOURNAMENTS — SHARED BACKEND ARCHITECTURE & SYNC CONTRACT
> **Single Source of Truth** for both **Admin Panel** and **User Panel**
> Copy and share this file directly with the User Panel agent so both apps stay 100% in sync without manual back-and-forth.

---

## 1. Firebase Project Connection Details
- **Firebase Project ID**: `velorix-tournaments`
- **Project Number**: `27931798964`
- **Firestore Database**: `(default)` in project `velorix-tournaments`
- **Realtime Database (RTDB) URL**:
  `https://velorix-tournaments-default-rtdb.asia-southeast1.firebasedatabase.app`

---

## 2. Dual-Database Philosophy
| Storage Engine | Purpose | Data Stored |
| :--- | :--- | :--- |
| **Cloud Firestore** | Primary Record of Truth & History | Full tournament docs, user balances & profiles, payment audit trails, tickets. |
| **Realtime Database (RTDB)** | Instant Real-Time Sync & Live State | Live slot counter (`registeredPlayers`), Room ID & Password releases, player registrations per match, live announcements. |

---

## 3. Data Schemas (Exact Field Names & Types)

### A. Tournament Object (`tournaments` collection in Firestore & `/tournaments/{id}` in RTDB)
Both apps **must** use these exact field names:

```json
{
  "id": "FF-CS-1727092100",
  "title": "Clash Squad 4v4 Bermuda Pro Series",
  "game": "Free Fire",
  "category": "CS",             // Allowed: "BR" | "CS" | "LONE_WOLF"
  "format": "4v4",              // Allowed: "SOLO" | "DUO" | "SQUAD" | "4v4" | "1v1"
  "map": "Bermuda",             // Allowed: "Bermuda" | "Purgatory" | "Kalahari" | "Nexterra"
  "entryFee": 50,               // Integer (in INR / Velorix Tokens)
  "prizePool": 350,             // Integer
  "perKill": 0,                 // Integer (Used in BR Solo/Squad, 0 for CS)
  "maxPlayers": 8,              // Integer (8 for CS 4v4, 48 for BR, 2 for Lone Wolf)
  "registeredPlayers": 6,       // Current joined count (Must match players sub-node)
  "status": "UPCOMING",         // Allowed: "UPCOMING" | "LIVE" | "COMPLETED" | "CANCELLED"
  "scheduledTime": 1727092800000, // Unix timestamp in millis
  "rules": [
    "Gun Attributes: OFF",
    "Character Skill: ON",
    "No Grenades / Smoke only"
  ],
  "roomDetails": {
    "roomId": "8847291",        // Empty string until Admin publishes it
    "roomPassword": "7788",     // Empty string until Admin publishes it
    "updatedAt": 1727092500000
  },
  "createdAt": 1727090000000
}
```

### B. Registered Players Sub-Path (RTDB: `/tournaments/{id}/players/{uid}`)
When a user joins a tournament in the User Panel:
```json
{
  "uid": "USER_FIREBASE_UID",
  "ign": "VeloKiller99",
  "gameId": "194827401",
  "slotNumber": 3,
  "joinedAt": 1727091500000
}
```

### C. User Profile & Wallet (Firestore: `users/{uid}`)
```json
{
  "uid": "USER_FIREBASE_UID",
  "email": "player@gmail.com",
  "phoneNumber": "+919876543210",
  "ign": "VeloKiller99",
  "gameId": "194827401",
  "role": "USER",               // Allowed: "USER" | "ADMIN"
  "depositFunds": 150.0,        // Real money added via UPI
  "winningFunds": 420.0,        // Winnings eligible for cashout
  "bonusFunds": 25.0,           // Referral / promotional bonus
  "isBanned": false,
  "banReason": "",
  "createdAt": 1727000000000
}
```

### D. Support Ticket (Firestore: `support_tickets/{id}`)
```json
{
  "id": "TICKET-9921",
  "userId": "USER_FIREBASE_UID",
  "userEmail": "player@gmail.com",
  "subject": "Room password not working for CS 4v4",
  "status": "OPEN",             // Allowed: "OPEN" | "RESOLVED"
  "adminNote": "",
  "createdAt": 1727091800000,
  "resolvedAt": 0
}
```

### E. Announcements (RTDB: `/announcements/{id}`)
```json
{
  "id": "ANN-101",
  "title": "Room ID Released!",
  "content": "Room ID for CS Match #12 has been published in your joined tab.",
  "bannerType": "INFO",         // "INFO" | "WARNING" | "URGENT"
  "timestamp": 1727092000000
}
```

---

## 4. Golden Rules to Keep Admin & User Panels in Perfect Sync

1. **Category Enums**:
   - Both panels **must** use the 3 exact string keys:
     - `"BR"` (Battle Royale)
     - `"CS"` (Clash Squad)
     - `"LONE_WOLF"` (Lone Wolf 1v1 / 2v2)
   - Do NOT rename to `"CLASH_SQUAD"` or `"BATTLE_ROYALE"` in one app and `"CS"` in the other.

2. **Room Details Flow**:
   - Admin Panel writes `roomDetails.roomId` and `roomDetails.roomPassword` into both Firestore & RTDB.
   - User Panel listens to `/tournaments/{id}/roomDetails` in RTDB.
   - Security rule in User Panel: Room ID must only be visible to users whose `uid` exists in `/tournaments/{id}/players/{uid}`!

3. **Joining Tournament (User Panel)**:
   - User Panel checks wallet: `(depositFunds + winningFunds) >= entryFee`.
   - Runs a Firestore transaction to deduct fee and increment `registeredPlayers`.
   - Simultaneously puts the user entry in RTDB `/tournaments/{id}/players/{uid}`.
   - Because RTDB is synced, the Admin Panel immediately shows the updated slot count live without reloading!

4. **Tournament Creation (Admin Panel & AI Agent)**:
   - Admin creates tournament (via AI prompt or manual form).
   - Writes to both Firestore `tournaments/{id}` and RTDB `/tournaments/{id}`.
   - User Panel receives the new tournament instantly via RTDB `child_added` or Firestore snapshot listener.
