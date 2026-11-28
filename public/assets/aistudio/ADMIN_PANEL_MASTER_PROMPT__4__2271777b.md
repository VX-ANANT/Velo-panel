# VELORIX ADMIN PANEL — CLOUD FIRESTORE & CLOUD FUNCTIONS MASTER PROMPT

> **HOW TO USE THIS FILE:**
> Copy the prompt below and paste it into your Admin Panel AI agent (Lovable / web admin).
> It configures **Cloud Firestore (`getFirestore(app)`) + Firebase Cloud Functions (`getFunctions(app)`)** as the unified Single Source of Truth — 100% synced in real time with the VeloRix Android App, removes very old/obsolete legacy modules & blocked collections, and adds the new **Manual Notification Studio (FCM Push + In-App Delivery)**, **Server-Authoritative Deposit/Withdrawal/Tournament Settlement Callables**, **Auto-Synced Joined Player Roster**, and **Backend Reconciliation Desk**.

---

## COPY-PASTE PROMPT FOR ADMIN PANEL AGENT

```markdown
You are updating the **VeloRix Esports Admin Panel** connected to the same Firebase project (`velorix-tournaments`) as the VeloRix Android App.

### DATABASE & BACKEND ARCHITECTURE (CLOUD FIRESTORE + CLOUD FUNCTIONS)
- **PRIMARY DATABASE:** Use **Cloud Firestore (`getFirestore(app)`)** for all reads, live `onSnapshot` listeners, and standard admin CRUD writes.
- **PRIMARY FINANCIAL & PUSH EXECUTION:** Use **Firebase Cloud Functions (`httpsCallable(getFunctions(app), "<name>")`)** for all financial settlements, tournament payouts, manual push notifications, and backend reconciliation so server-side transactions, idempotency, RTDB mirroring, and FCM push alerts happen atomically without double-crediting.
- **ADMIN ACCESS VERIFICATION:** Only verified admin accounts pass `firestore.rules` and Cloud Function auth guards:
  - Verified email `anantisback47@gmail.com` or `service.veloxyra@gmail.com` (`email_verified === true`), OR
  - Custom Auth claims (`admin === true`, `super_admin === true`, or `role === "admin"`), OR
  - Document exists at `admins/{uid}`.
- **ACTIVE FIRESTORE COLLECTIONS (100% aligned with `firestore.rules` & Android live listeners):**
  - `tournaments` (subcollections: `participants`, `slots`, `leaderboard`, `chat_messages`)
  - `admin_match_rosters` (subcollection: `players`)
  - `tournament_registrations`, `tournament_history` (subcollection: `leaderboard`)
  - `users` (subcollections: `match_stats`, `transactions`, `notifications`, `reports`, `deposit_requests`, `withdraw_requests`)
  - `deposit_requests`, `withdraw_requests`, `upi_payment_orders`, `transactions`, `verified_bank_deposits`
  - `redeem_codes` (subcollection: `claims`), `redeem_claims`
  - `admin_requests`, `support_tickets`, `reports`, `chat_reports`
  - `manual_notifications`, `admin_notifications`, `notifications`, `banners`
  - `system_config` (`config`, `app_settings`)
  - `global_app_state` (`latest`), `global_app_state_history`
  - `audit_logs`, `security_audit_logs`
- **No RTDB SDK is required in the Admin Panel.** Cloud Functions automatically mirror Firestore updates to Realtime Database and send FCM pushes to Android devices.

---

### STEP 0: CLEANUP — REMOVE VERY OLD / OBSOLETE FEATURES FIRST
Before implementing the updated modules below, **DELETE** all of these outdated/broken legacy items from the Admin Panel UI, sidebar, state, and Firebase handlers:
1. **Remove Founder Pass Hub / Tiers** (`isFounder`, `founderTier`, `founder_pass_hub`, Founder Pass tabs/badges/cards).
2. **Remove Legacy Calendar Festival Overrides & Notification Weight Sliders** (`activeFestivalOverride`, `customFestivalTitle`, `customFestivalMessage`, `attractingAlertWeight`, `timeContextualWeight`, `weeklyCalendarWeight`).
3. **Remove Very Old Device / OTA / SFX Feature Flags & Fields** (`mj_signature_sfx`, `auto_upi_utr_listener`, `hyperos_super_island`, `ota_live_hot_patch`, `otaPatchVersion`, `otaReleaseNotes`, `liveLobbyTicker`, `community_chat`).
4. **Remove Blocked / Deprecated Firestore Paths** — do NOT read or write `app_config/global`, `banned_users`, `suspended_users`, `account_deletion_requests`, or `data_export_logs` (these are denied by strict zero-trust `firestore.rules`; use `system_config/config`, `users/{uid}`, and `admin_requests` instead).
5. **Remove Direct Client-Side Wallet Balance Math for Deposits & Withdrawals** — NEVER do manual `increment(amount)` on `users/{uid}.balance` from browser code when approving deposits or rejecting withdrawals (doing so causes double-credits alongside server triggers). Always call `processDepositDecision` and `processWithdrawalDecision` Cloud Functions instead.
6. **Remove the Old Fragmented 6-Channel Support Page & Old Simple Broadcast Input** — replace them completely with **Module 4 (Unified 12-Channel Request Hub)** and **Module 6 (Manual Notification Studio & Banners)**.
7. **Remove Fake Analytics Charts / Decorative Widgets / Dead Buttons** — every single button, toggle, and form must perform a real Cloud Firestore write or Cloud Function call and display a clear toast with the server result.

---

### CRITICAL UI & REAL-TIME SYNC RULES
1. **STRICT MINIMAL DARK OLED UI (`#000000` bg, `#0E1014` cards, `0.5px solid rgba(255,255,255,0.08)` borders):**
   Keep ONLY **7 clean sidebar pages**:
   1. **Tournaments, Room Vault & Auto-Roster Standings**
   2. **Wallet, UPI & Dual Payouts**
   3. **Redeem Code Vault**
   4. **Unified Requests & Ticket Desk (12 Channels)**
   5. **Users, Sanctions & Lobby Chat**
   6. **Manual Notification Studio & Banners** *(NEW)*
   7. **Live System Config, Health & Audit Logs**

2. **CLOUD FIRESTORE GLOBAL SYNC PULSE (`global_app_state/latest`):**
   After editing a tournament, releasing room credentials, updating user bans/suspensions, changing banners, or updating system config, publish a pulse to `global_app_state/latest`:
   ```js
   import { doc, setDoc } from "firebase/firestore";

   await setDoc(doc(db, "global_app_state", "latest"), {
     eventId: `evt_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`,
     eventType, // "TOURNAMENT_UPDATED" | "ROOM_CREDENTIALS_RELEASED" | "SCHEDULE_CHANGED" | "TOURNAMENT_CANCELLED" | "USER_STATUS_CHANGED" | "SYSTEM_CONFIG_UPDATED" | "SYSTEM_FORCE_SYNC"
     source: "ADMIN_PANEL",
     tournamentId: tournamentId || "",
     affectedUserId: userId || "",
     lastUpdatedAt: Date.now(),
     timestamp: Date.now(),
     version: Date.now(),
     payload: metadata || {}
   }, { merge: true });
   ```
   *(The Android app listens live to `global_app_state/latest` and refreshes within milliseconds.)*

---

### MODULE 1: TOURNAMENTS, ROOM VAULT & AUTO-SYNCED ROSTER POSITION SELECTOR

1. **Create / Edit Tournament (`tournaments/{id}` + mirror `matches/{id}`):**
   - Fields: `id` (sequential e.g. `VLX-01`), `title`, `game` (`"Free Fire" | "Free Fire Max" | "BGMI"`), `matchCategory` (`"BATTLE_ROYALE" | "CLASH_SQUAD" | "LONE_WOLF" | "SPECIAL_MODE"`), `format` (`"SOLO" | "DUO" | "SQUAD" | "1v1" | "2v2" | "3v3" | "4v4"`), `matchMode` (`"PER_KILL" | "SURVIVAL" | "HEADSHOT_ONLY" | "BODY_DAMAGE_ON" | "SNIPER_ONLY" | "LIMITED_AMMO" | "UNLIMITED_AMMO" | "PISTOL_ONLY"`), `customRuleBadge`, `mapType`, `perspective` (`"TPP" | "FPP"`), `entryFee` (VT), `prizePool` (VT), `rank1Prize`, `rank2Prize`, `rank3Prize`, `killBounty`, `maxSlots`, `filledSlots`, `dateTimeStr` (e.g. `"2026-11-28 19:30"`), `status` (`"UPCOMING" | "LIVE" | "COMPLETED" | "CANCELLED"`), `roomId`, `roomPassword`, `imageUrl`, `updatedAt`.
   - Changing `status` on `tournaments/{id}` automatically fires the server trigger `onTournamentStatusUpdated` (which mirrors state & notifies joined players).

2. **Release Room ID & Password (T-15m Vault):**
   - Update `roomId`, `roomPassword`, and `updatedAt` on `tournaments/{id}` (and `matches/{id}`), then push `ROOM_CREDENTIALS_RELEASED` to `global_app_state/latest` with `{ tournamentTitle, roomId, roomPass }`.
   - Optionally trigger a **Room Credentials** push to joined players via `sendManualNotification` (`targetType: "TOURNAMENT"`, `type: "ROOM_CREDENTIALS"`, `tournamentId`, `roomId`, `roomPassword`) so every registered player gets an immediate high-priority alert.

3. **Auto-Synced Joined Player Roster & Zero-Typing Direct Position Selector (`1st`, `2nd`, `3rd`, `#N` + Tied Ranks):**
   - Subscribe live using `onSnapshot(collection(db, "admin_match_rosters", tournamentId, "players"))` (merging any entries from `tournaments/{tournamentId}/participants` or `tournaments/{tournamentId}.joinedPlayersMap`).
   - Automatically render each joined player row with: **Slot Badge (`SLOT #1..#N`)**, **In-Game Name (`inGameName`)**, **Game UID (`characterId` / `gameId`)**, **Username (`@username`)**, and **Ticket (`ticketCode`)**.
   - Next to every player row, render **1-Tap Position Chips**:
     `[ 1st ]` `[ 2nd ]` `[ 3rd ]` `[ 4th ]` `[ 5th ]` `[ #N Custom ]` `[ Part. ]` + a `Kills` stepper (`-` / `+`).
   - **Shared / Tied Positions:** Allow assigning the exact same rank to 1, 2, or more players (displaying a `TIED #R` pill) and include a **Tie Prize Mode Toggle (`tiePrizeMode`)**:
     - `"SPLIT_TIED_POOL"` *(Default)*: splits summed placement prizes evenly across tied players.
     - `"FULL_PRIZE_EACH"`: awards each tied player the full base prize for that rank.
   - **Publish & Distribute Winnings (CALL CLOUD FUNCTION `finalizeTournamentPayouts`):**
     ```js
     import { httpsCallable } from "firebase/functions";

     const finalize = httpsCallable(functions, "finalizeTournamentPayouts");
     const { data } = await finalize({
       tournamentId,
       payoutStatus: "PAID",
       tiePrizeMode, // "SPLIT_TIED_POOL" | "FULL_PRIZE_EACH"
       positionAssignments: rosterRows.map((p) => ({
         userId: p.userId,
         rank: p.selectedRank, // 1, 2, 3... or 0 for participant-only
         kills: Number(p.kills || 0)
       }))
     });
     ```
     *(The Cloud Function atomically calculates tied prizes + kill bounties, credits wallets idempotently, updates `tournament_history`, `tournaments`, `admin_match_rosters`, player `match_stats`, transactions, RTDB mirrors, and dispatches match-result notifications to every participant.)*

4. **Cancel Tournament & Auto-Refund:**
   - Update `status: "CANCELLED"` on `tournaments/{id}` (and `matches/{id}` / `admin_match_rosters/{id}`), refund entry fees via server ledger/cancellation flow, and push `TOURNAMENT_CANCELLED` to `global_app_state/latest`.

---

### MODULE 2: WALLET DEPOSITS, DYNAMIC UPI & DUAL PAYOUTS (SERVER-AUTHORITATIVE)

1. **Deposits Desk (`deposit_requests` + `upi_payment_orders`):**
   - Listen live to Firestore `deposit_requests` (ordered by `timestamp desc`) and `upi_payment_orders`.
   - **Pre-Verify Bank UTR (optional auto-match):** Write doc `verified_bank_deposits/{utr}` (`{ utr, amount, claimed: false, timestamp: Date.now() }`).
   - **Approve or Reject Deposit (CALL CLOUD FUNCTION `processDepositDecision`):**
     ```js
     const decideDeposit = httpsCallable(functions, "processDepositDecision");
     await decideDeposit({
       requestId: req.id,
       status: "APPROVED", // or "REJECTED"
       adminNote: note || "Verified via bank statement"
     });
     ```
     *(Server atomically credits `users/{uid}.balance`, locks UTR against replay, updates `transactions/{txId}`, mirrors to RTDB, and sends a wallet push notification. Never manually increment `balance` from client code.)*

2. **Dual Withdrawal Desk — UPI Bank & Redeem Vouchers (`withdraw_requests`):**
   - Listen live to Firestore `withdraw_requests` (ordered by `timestamp desc`).
   - Display `id`, `userId`, `username`, `inGameName`, `amount` (gross VT), `tdsAmount` (30% statutory TDS), `netAmount` (net ₹ payout), `paymentMethod` (`"UPI" | "REDEEM_CODE"`), `upiId`, `voucherSpec` (`"Google Play" | "Free Fire Diamonds" | "BGMI UC"`), `transactionId`, `status`, `timestamp`.
   - **Approve UPI Payout OR Reject & Auto-Refund (CALL CLOUD FUNCTION `processWithdrawalDecision`):**
     ```js
     const decideWithdrawal = httpsCallable(functions, "processWithdrawalDecision");
     await decideWithdrawal({
       requestId: req.id,
       status: "APPROVED", // or "REJECTED" (auto-refunds wallet balance on server!)
       utrReference: utrOrVoucherCode || "",
       adminNote: note || ""
     });
     ```
   - **Approve Redeem Code Voucher Payout:**
     - Provide an input for Admin to paste the **16-digit Redeem Voucher Code**.
     - Call `processWithdrawalDecision({ requestId: req.id, status: "APPROVED", utrReference: voucherCode, adminNote: \`Your \${req.voucherSpec} Redeem Code (Net ₹\${req.netAmount}): \${voucherCode}\` })`, AND update the linked ticket (`admin_requests/{id}`, `support_tickets/{id}`, `reports/{id}`, `users/{userId}/reports/{id}`) with `{ status: "RESOLVED", deliveredRedeemCode: voucherCode, adminReply, updatedAt: Date.now() }` so the code appears immediately inside the player's **My Tickets** screen.

3. **Dynamic Merchant UPI Config:**
   - Edit `merchantUpiId` / `upiId` (default `veloxyra.anant@fam`) and `merchantName` in `system_config/app_settings` and `system_config/config`.

---

### MODULE 3: REDEEM CODE VAULT (`redeem_codes` & `redeem_claims`)
1. **Create / Toggle Redeem Codes (`redeem_codes/{UPPERCASE_CODE}`):**
   ```js
   {
     code: cleanCode,
     title: "Weekend Bonus Voucher",
     active: true,
     status: "ACTIVE", // "ACTIVE" | "DISABLED"
     rewardVt: 50,
     rewardTokens: 100,
     maxUses: 100,     // 0 = unlimited
     usedCount: 0,
     expiresAt: 0,     // Epoch ms, 0 = never expires
     targetUserId: "", // Optional: lock code to one specific user UID
     createdAt: Date.now(),
     updatedAt: Date.now()
   }
   ```
   *(Players claim codes exclusively via the server-authoritative `redeemPromoCodeServer` Cloud Function.)*
2. **Claim Audit Log:** Live `onSnapshot` table on `redeem_claims`.

---

### MODULE 4: UNIFIED 12-CHANNEL REQUEST HUB & `MY TICKETS` DESK
1. **Master Queue:** Live `onSnapshot` on `admin_requests` (merging `support_tickets` & `reports` by ID so no ticket is missed).
2. **Filter Chips for All 12 Active Channels:**
   `ALL`, `PAYMENT_DEPOSIT`, `PAYMENT_WITHDRAW`, `REDEEM_CODE_REQUEST`, `ROOM_CREDENTIALS`, `CHEATER_HACKER`, `RESULT_DISPUTE`, `TOURNAMENT_REQUEST`, `APP_BUG_CRASH`, `BAN_APPEAL`, `SUSPENSION_APPEAL`, `ACCOUNT_DELETION`, `GENERAL_SUPPORT`.
3. **Reply & Status Sync (Real-Time Back to Android `My Tickets`):**
   - Clicking **"Send Reply & Update Ticket"** merges `{ status, adminReply, deliveredRedeemCode, updatedAt: Date.now() }` across:
     - `admin_requests/{id}`
     - `support_tickets/{id}`
     - `reports/{id}`
     - `users/{userId}/reports/{id}`
   - Also creates a notification doc at `users/{userId}/notifications/notif_ticket_{id}_{Date.now()}` (which automatically triggers `onUserNotificationCreated` and sends an FCM push to the player's phone!).
4. **Account Deletion Tickets (`ACCOUNT_DELETION` channel):**
   - When approving an account deletion ticket, delete `users/{uid}`, mark the ticket `RESOLVED` with confirmation reply, and log an entry to `audit_logs`.

---

### MODULE 5: USERS, SANCTIONS & TOURNAMENT LOBBY CHAT MODERATION
1. **Users Directory & Sanctions (`users/{uid}`):**
   - Show `username`, `inGameName`, `freeFireId`, `phoneOrEmail`, `balance` (VT), `tokens`, `matchesPlayed`, `totalKills`, `totalWins`, `isBanned`, `isSuspended`, `isOnline`, `lastActiveAt`, and whether `fcmToken` is registered (`Push Ready` indicator).
   - **Ban / Unban:** Update `{ isBanned, banned: isBanned, banReason, banType: "PERMANENT", updatedAt: Date.now() }` directly on `users/{uid}` and push `USER_STATUS_CHANGED` to `global_app_state/latest`.
   - **Suspend / Unsuspend:** Update `{ isSuspended, suspendReason, suspensionExpiresAt, updatedAt: Date.now() }` directly on `users/{uid}` and push `USER_STATUS_CHANGED`.
   - **Quick "Notify User" Button:** Opens **Module 6 Manual Notification Composer** pre-filled with `targetType: "USER"` and `targetUserId: uid`.
2. **Tournament Lobby Chat Moderation (`tournaments/{id}/chat_messages` & `chat_reports`):**
   - View live messages for any selected tournament.
   - **Send Official Admin Announcement:** Write with `{ senderId: "admin", senderName: "VeloRix Admin", senderRole: "ADMIN", isSystemMessage: true, messageType: "ANNOUNCEMENT", channel: "ALL", isPinned: true, timestamp: Date.now() }`.
   - **Pin / Unpin** (`isPinned`), **Soft-Delete** (`isDeleted: true`, `text: "Message removed by Admin"`), and resolve **Chat Reports** (`chat_reports`).

---

### MODULE 6: MANUAL NOTIFICATION STUDIO & BANNERS *(NEW — FULL PUSH + IN-APP DELIVERY)*

1. **Manual Notification Composer (Sends Real Android System Tray Push + Live In-App Banner + Notification Center Entry):**
   Build a dedicated composer panel with live preview and real delivery counters:
   - **Target Audience Selector (`targetType`):**
     - `ALL` — Broadcasts to every app user (`all_users` FCM topic + Firestore `notifications/{id}` + RTDB `notifications/broadcast/{id}`).
     - `USER` — Single user lookup by **UID**, **Email**, or **Username** (`targetUserId` / `targetEmail` / `targetUsername` — server automatically resolves the player and sends push + personal inbox entry).
     - `USERS` — Multi-select list of user UIDs (`targetUserIds: string[]`).
     - `TOURNAMENT` — Select any tournament (`tournamentId`) to notify all joined participants automatically (`tournament_{id}` + participant FCM tokens).
     - `TOPIC` — Custom FCM topic (`topic`, e.g. `all_users`, `announcements`, `manual_notifications`).
   - **Notification Category (`type`):**
     `MANUAL` (Official Alert), `ANNOUNCEMENT`, `ADMIN_BROADCAST`, `URGENT`, `PROMO`, `MAINTENANCE`, `PRIZE_ANNOUNCEMENT`, `TOURNAMENT_UPDATE`, `WALLET_UPDATE`, `ROOM_CREDENTIALS`.
   - **Fields:**
     - `title` *(required, max 140 chars)*
     - `message` *(required, max 1000 chars)*
     - `imageUrl` *(optional HTTPS banner URL — renders BigPicture image inside Android system notification)*
     - `tournamentId` *(optional — tapping notification opens that tournament in-app)*
     - `roomId` & `roomPassword` *(optional — shown when `type === "ROOM_CREDENTIALS"`)*
   - **Primary Dispatch (CALL CLOUD FUNCTION `sendManualNotification`):**
     ```js
     import { httpsCallable } from "firebase/functions";

     const sendManual = httpsCallable(functions, "sendManualNotification");
     const { data } = await sendManual({
       title,
       message,
       type,           // e.g. "MANUAL" | "ANNOUNCEMENT" | "URGENT" | "PROMO" | "TOURNAMENT_UPDATE"
       targetType,     // "ALL" | "USER" | "USERS" | "TOURNAMENT" | "TOPIC"
       targetUserId,   // when targetType === "USER"
       targetEmail,    // optional email lookup when targetType === "USER"
       targetUsername, // optional username lookup when targetType === "USER"
       targetUserIds,  // when targetType === "USERS"
       tournamentId,   // required when targetType === "TOURNAMENT", optional deep-link otherwise
       imageUrl,       // optional https://... banner URL
       roomId,         // optional
       roomPassword    // optional
     });
     // Response: { success, notificationId, targetType, recipientCount, fcmSuccessCount, fcmFailureCount }
     ```
   - **Automatic Fallback / Direct Firestore Mode:**
     Writing a document directly to Firestore `manual_notifications/{notifId}` with the same fields also auto-triggers `onManualNotificationCreated` on the server (and writing to `notifications/{id}` or `users/{uid}/notifications/{id}` auto-triggers `onGlobalNotificationCreated` / `onUserNotificationCreated`). The Android app automatically deduplicates FCM + Firestore/RTDB delivery so users never receive duplicate notifications.
   - **Sent Notification History & Delivery Stats Table:**
     Subscribe live to `manual_notifications` (ordered by `timestamp desc`, limit 50) showing: **Title**, **Type Badge**, **Audience (`targetType`)**, **Recipients (`recipientCount`)**, **FCM Delivered (`fcmSuccessCount` / `fcmFailureCount`)**, **Sent By (`sentBy`)**, and **Timestamp**. Include a **"Resend / Duplicate"** button that pre-fills the composer.

2. **Home Screen Banners CRUD (`banners/{id}`):**
   - Fields: `id`, `title`, `subtitle`, `badgeText`, `imageUrl`, `ctaText`, `actionType` (`"ANNOUNCEMENT" | "MATCH" | "WALLET" | "SUPPORT" | "LEADERBOARD"`), `targetId`, `gradientTheme`, `active`, `order`, `updatedAt`.
   - Master Banner Visibility Toggle: `showBanners` on `system_config/config`.

---

### MODULE 7: LIVE SYSTEM CONFIG, BACKEND HEALTH & AUDIT LOGS

1. **Live System Controls (writes to `system_config/config` + mirrors `system_config/app_settings` + pushes `SYSTEM_CONFIG_UPDATED` to `global_app_state/latest`):**
   - **Maintenance Mode:** `isMaintenance`, `maintenanceTitle`, `maintenanceMessage`, `maintenanceUntil`
   - **Force Update:** `isForceUpdate`, `minRequiredVersion`, `updateUrl`, `changelog`
   - **Security & UI:** `vpnRestrictionEnabled`, `showBanners`
   - **Active Core Feature Flags ONLY (`featureFlags`):**
     - `tournament_join`
     - `room_credentials_delivery`
     - `instant_withdrawals`
     - `token_conversion`
     - `daily_login_rewards`

2. **1-Click Backend Reconciliation Desk *(NEW)*:**
   - Add a **"Run Backend Reconciliation Now"** button that invokes Cloud Function `triggerBackendReconciliation()` and displays the returned JSON summary (reconciled deposits, withdrawals, tournament slots, and Firestore ↔ RTDB sync health).

3. **Audit & Security Logs Viewer:**
   - Read-only live `onSnapshot` tabs for `audit_logs` and `security_audit_logs` (ordered by `timestamp desc`).
```
