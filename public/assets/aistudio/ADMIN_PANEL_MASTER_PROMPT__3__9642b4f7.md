# VELORIX ADMIN PANEL — MASTER INTEGRATION & CLEANUP PROMPT (CURRENT ARCHITECTURE)

> **HOW TO USE THIS FILE:**
> Copy the prompt below and paste it into your Admin Panel AI agent. It removes all obsolete legacy features and aligns your Admin Panel 100% with the current **VeloRix Android App** Firebase RTDB + Cloud Firestore schema (Unified 12-Channel Request Hub & `My Tickets`, Redeem Code Vault, Dual UPI + Voucher Payouts with 30% TDS, Lobby Chat Moderation, Multi-Position Standings, and Live Widget/App Sync).

---

## COPY-PASTE PROMPT FOR ADMIN PANEL AGENT

```markdown
You are updating the **VeloRix Esports Admin Panel** connected to the same Firebase project (`Firebase Realtime Database` + `Cloud Firestore`) as the VeloRix Android App.

### STEP 0: CLEANUP — REMOVE VERY OLD / OBSOLETE FEATURES FIRST
Before adding the new modules below, **DELETE** these deprecated legacy features from the Admin Panel UI, sidebar, state, and Firebase handlers so the panel stays fast, clean, and uncluttered:
1. **Remove Founder Pass Hub / Tiers** (`isFounder`, `founderTier`, `founder_pass_hub` flag, Founder Pass tabs/badges).
2. **Remove Legacy Calendar Festival Override & Notification Weight Sliders** (`activeFestivalOverride`, `customFestivalTitle`, `customFestivalMessage`, `customAttractingTitle`, `customAttractingMessage`, `attractingAlertWeight`, `timeContextualWeight`, `weeklyCalendarWeight`, `calendar_festival_sync`, `attracting_esports_alerts`).
3. **Remove Obsolete Legacy Feature Flags & Device Toggles** (`mj_signature_sfx`, `auto_upi_utr_listener`, `hyperos_super_island`, `ota_live_hot_patch`, `community_chat`).
4. **Remove the Old Fragmented 6-Channel Support Page** — replace it completely with the **Unified Request Hub & Ticket Desk (Module 4)** below.
5. **Remove Fake Analytics Charts / Decorative Widgets / Dead Buttons** — every single button, toggle, and form must perform a real Firebase RTDB + Firestore write and show a clear confirmation toast.

---

### CRITICAL ARCHITECTURE RULES
1. **STRICT MINIMAL DARK UI (`#0B0E14` bg, `#11151F` cards, `1px solid rgba(255,255,255,0.08)` borders):**
   Keep ONLY **7 clean sidebar pages**:
   1. **Tournaments, Room Vault & Standings**
   2. **Wallet, UPI & Voucher Payouts**
   3. **Redeem Code Vault**
   4. **Unified Requests & Ticket Desk (12 Channels)**
   5. **Users, Moderation & Lobby Chat**
   6. **Banners & Global Broadcasts**
   7. **Live System Config & DPDP Deletions**

2. **DUAL-SYNC RULE (RTDB + FIRESTORE + `/global_app_state`):**
   Whenever you mutate a tournament, room ID/password, standings, wallet request, redeem code, ticket reply, user ban/suspension, banner, or system config, write to **both RTDB and Firestore** and bump `/global_app_state` in RTDB:
   ```js
   await update(ref(rtdb, "global_app_state"), {
     eventId: `evt_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`,
     eventType, // "TOURNAMENT_UPDATED" | "ROOM_CREDENTIALS_RELEASED" | "SCHEDULE_CHANGED" | "TOURNAMENT_CANCELLED" | "USER_STATUS_CHANGED" | "SYSTEM_CONFIG_UPDATED" | "SYSTEM_FORCE_SYNC"
     source: "ADMIN_PANEL",
     tournamentId: tournamentId || "",
     affectedUserId: userId || "",
     lastUpdatedAt: Date.now(),
     timestamp: Date.now(),
     version: Date.now(),
     payload: metadata || {}
   });
   ```

---

### MODULE 1: TOURNAMENTS, ROOM CREDENTIALS, MULTI-POSITION STANDINGS & WIDGET SYNC
1. **Create / Edit Tournament (Syncs Android App + Home Screen Match Radar Widget + Live Mini-Player):**
   - Fields: `id` (sequential `#VLX` e.g. `VLX-01`), `title`, `game` (`"Free Fire" | "Free Fire Max" | "BGMI"`), `matchCategory` (`"BATTLE_ROYALE" | "CLASH_SQUAD" | "LONE_WOLF" | "SPECIAL_MODE"`), `format` (`"SOLO" | "DUO" | "SQUAD" | "1v1" | "2v2" | "3v3" | "4v4"`), `matchMode` (`"PER_KILL" | "SURVIVAL" | "HEADSHOT_ONLY" | "BODY_DAMAGE_ON" | "SNIPER_ONLY" | "LIMITED_AMMO" | "UNLIMITED_AMMO" | "PISTOL_ONLY"`), `customRuleBadge`, `mapType` (`"Bermuda" | "Bermuda CS" | "Purgatory" | "Kalahari" | "Erangel" | "Miramar"`), `perspective` (`"TPP" | "FPP"`), `entryFee` (VT), `prizePool` (VT), `rank1Prize`, `rank2Prize`, `rank3Prize`, `killBounty`, `maxSlots`, `filledSlots`, `dateTimeStr` (e.g. `"2026-11-21 19:30"`), `status` (`"UPCOMING" | "LIVE" | "COMPLETED" | "CANCELLED"`), `roomId`, `roomPassword`, `imageUrl`.
   - Write to RTDB `/tournaments/{id}` + `/matches/{id}` AND Firestore `tournaments/{id}` + `matches/{id}`.
   - Note: The Android **Home Screen Match Radar Widget** pulls directly from RTDB `/tournaments` on `↻` refresh and prioritizes Joined+Room Ready → Joined Live → Joined Upcoming → Open Live → Open Upcoming (hiding `COMPLETED`/`CANCELLED`).

2. **Release Room ID & Password (T-15m Vault):**
   - Updating `roomId` and `roomPassword` writes to RTDB `/tournaments/{id}` & `/matches/{id}` + Firestore `tournaments/{id}` & `matches/{id}`, and pushes `ROOM_CREDENTIALS_RELEASED` to `/global_app_state` with `{ tournamentTitle, roomId, roomPass }`.

3. **Declare Multi-Position Standings (`#1..#N`) & Distribute Prizes:**
   - Load joined players from RTDB `/tournaments/{id}/participants` (fallback Firestore `tournaments/{id}/participants`).
   - Editable table per participant: `rank` (`1..N`), `kills`, `rankPrize` (VT), `killPrize` (`kills * killBounty`), `prizeWon` (`rankPrize + killPrize`).
   - On **"Publish Final Standings & Credit Winners"**:
     - Save `finalLeaderboard` (sorted by `rank`), `standings`, `results`, `status: "COMPLETED"`, `isCompleted: true`, `payoutStatus: "PAID"`, `completedAt: Date.now()`, `totalPrizeDisbursed`, `winnerName`, `winnerUid`, `winnerPrize` to Firestore & RTDB (`tournaments/{id}`, `matches/{id}`, `tournament_history/{id}`) and leaderboard subcollections.
     - For **EVERY** participant (won VT or 0 VT):
       - Write match stat to RTDB `/users/{userId}/match_stats/{tournamentId}_{userId}`, `/match_stats/{userId}/{tournamentId}_{userId}` and Firestore `users/{userId}/match_stats/{tournamentId}_{userId}`.
       - Increment `matchesPlayed` (`+1`), `totalKills` (`+kills`), `totalWins` (`+(rank === 1 ? 1 : 0)`).
       - If `prizeWon > 0` and transaction `win_${tournamentId}_${userId}` does not exist: increment `balance` (`+prizeWon`), create `WINNINGS` transaction in `/transactions/{txId}`, `/users/{userId}/transactions/{txId}`, and Firestore `transactions/{txId}`, and send result notification to `/users/{userId}/notifications/notif_result_${tournamentId}`.
       - If `prizeWon === 0`: still write result notification with exact `#rank` and `kills`.

4. **Cancel Tournament & Auto-Refund:**
   - Set `status: "CANCELLED"`, refund `+entryFee` to every participant in `/tournaments/{id}/participants`, record `REFUND` transaction `refund_${id}_${userId}`, and push `TOURNAMENT_CANCELLED` to `/global_app_state`.

---

### MODULE 2: WALLET DEPOSITS, DYNAMIC UPI & DUAL PAYOUTS (UPI + REDEEM VOUCHERS)
1. **Deposits & Live UPI Orders (`/deposit_requests`, `/upi_payment_orders`, `/verified_bank_deposits`):**
   - Listen to RTDB `/deposit_requests`, `/upi_payment_orders` and Firestore `deposit_requests`.
   - Include a quick **"Pre-Verify UTR"** box writing to `/verified_bank_deposits/{utr}` (`{ utr, amount, claimed: false, timestamp: Date.now() }`).
   - **Approve:** Set `status: "APPROVED"` on `/deposit_requests/{id}` (and `status: "SUCCESS"` on linked `/upi_payment_orders/{orderId}`), increment user `balance` by `+amount` in RTDB & Firestore, mark `/transactions/{transactionId}` `"SUCCESS"`, update linked support ticket if present, and push user notification.
   - **Reject:** Set `status: "REJECTED"` on request + transaction, and update linked ticket `adminReply`.

2. **Dual Withdrawal Desk — UPI Bank + Redeem Code Vouchers (`/withdraw_requests`, `/wallet_requests`):**
   - Listen to RTDB `/withdraw_requests` (and `/wallet_requests`) + Firestore `withdraw_requests` (`payout_requests`).
   - Fields: `id` (`REQ-WDR-...` or `REQ-RDM-...`), `userId`, `username`, `inGameName`, `amount` (gross VT), `tdsAmount` (30% statutory TDS), `netAmount` (net ₹ payout), `paymentMethod` (`"UPI" | "REDEEM_CODE"`), `upiId`, `voucherSpec` (e.g. `"Google Play" | "Free Fire Diamonds" | "BGMI UC"`), `transactionId`, `status`, `timestamp`.
   - **Approve UPI Payout:** Set `status: "APPROVED"` on `/withdraw_requests/{id}` + `withdraw_requests/{id}` + `/wallet_requests/{id}`, mark `/transactions/{transactionId}` `"SUCCESS"`, and update linked ticket (`REQ-WDR-...`) to `status: "RESOLVED"` with payout UTR/note in `adminReply`.
   - **Approve Redeem Voucher Payout (NEW):**
     - Provide an input for Admin to paste the **16-digit Redeem Voucher Code**.
     - Save `status: "APPROVED"`, `deliveredRedeemCode: code` on `/withdraw_requests/{id}` & `withdraw_requests/{id}`, mark `/transactions/{transactionId}` `"SUCCESS"`, AND update the user's linked ticket (`id == withdrawReqId`) across RTDB `/admin_requests/all/{id}`, `/support_tickets/{id}`, `/reports/{id}`, `/users/{userId}/reports/{id}` + Firestore `admin_requests/{id}`, `support_tickets/{id}`, `reports/{id}`, `users/{userId}/reports/{id}` with:
       ```js
       {
         status: "RESOLVED",
         deliveredRedeemCode: code,
         adminReply: `Your ${voucherSpec} Redeem Code (Net ₹${netAmount}): ${code}`,
         updatedAt: Date.now()
       }
       ```
       (This immediately delivers the code inside the player's **My Tickets** tracker!).
   - **Reject & Refund:** Set `status: "REJECTED"`, mark transaction `"REJECTED"`, refund `+amount` VT to `/users/{userId}/balance` (RTDB & Firestore), and update linked ticket with rejection reason.

3. **Dynamic Merchant UPI Config:**
   - Edit `merchantUpiId` / `upiId` (default `veloxyra.anant@fam`) and `merchantName` on RTDB `/system_config` + `/app_config` and Firestore `system_config/app_settings`.

---

### MODULE 3: REDEEM CODE VAULT (PROMO & EVENT CODES) — NEW
The Android app validates and claims promo/event codes live from `/redeem_codes/{CODE}` (RTDB) and `redeem_codes/{CODE}` (Firestore).
1. **Create / Manage Redeem Codes:**
   - Key: Uppercase trimmed code (e.g. `VELORIX50`, `DIWALI100`).
   - Exact schema written to BOTH RTDB `/redeem_codes/{CODE}` and Firestore `redeem_codes/{CODE}`:
     ```js
     {
       code: cleanCode,
       title: "Welcome Bonus Voucher",
       active: true,
       status: "ACTIVE", // "ACTIVE" | "DISABLED"
       rewardVt: 50,     // Wallet VT credited
       rewardTokens: 100,// Combat Tokens credited
       maxUses: 100,     // 0 = unlimited
       usedCount: 0,
       expiresAt: 0,     // Epoch ms, 0 = never expires
       targetUserId: "", // Optional: lock code to one specific user UID
       createdAt: Date.now(),
       updatedAt: Date.now()
     }
     ```
   - Actions: Create Code, Toggle Active/Disabled, Edit `maxUses`/`expiresAt`, Delete Code.
2. **Claim Audit Log:**
   - Read-only table listening to RTDB `/redeem_claims` and Firestore `redeem_claims` showing who claimed each code, reward credited, and timestamp.

---

### MODULE 4: UNIFIED 12-CHANNEL REQUEST HUB & `MY TICKETS` DESK — NEW
**Every** request in the Android app (Support tickets, Ban appeals from `BannedScreen`, Suspension appeals from `SuspendedScreen`, Deposit issues, Withdrawal escalations, Redeem voucher requests, Room credential requests, Cheater reports, Match result disputes, Custom tournament hosting requests, Crash/bug reports, Account deletions, and Banner feedback) writes to ONE unified queue plus its dedicated channel queue.

1. **Primary Master Queue to Listen To:**
   - RTDB: `/admin_requests/all` (plus `/support_tickets` and `/reports`)
   - Firestore: `admin_requests` (plus `support_tickets` and `reports`)
2. **Filter Chips for All 12 Active Channels:**
   - `ALL` — Unified Queue (`admin_requests/all`)
   - `PAYMENT_DEPOSIT` (`#deposit-issues` → `deposit_issue_requests`)
   - `PAYMENT_WITHDRAW` (`#withdrawal-desk` → `withdrawal_issue_requests`)
   - `REDEEM_CODE_REQUEST` (`#redeem-codes` → `redeem_code_requests`)
   - `ROOM_CREDENTIALS` (`#room-credentials` → `room_credential_requests`)
   - `CHEATER_HACKER` (`#sentinel-anticheat` → `cheater_reports`)
   - `RESULT_DISPUTE` (`#match-disputes` → `result_dispute_requests`)
   - `TOURNAMENT_REQUEST` (`#tournament-requests` → `tournament_host_requests`)
   - `APP_BUG_CRASH` (`#crash-diagnostics` → `bug_reports`)
   - `BAN_APPEAL` / `ACCOUNT_ISSUE` (`#ban-appeals` → `ban_appeals`)
   - `SUSPENSION_APPEAL` / `VERIFICATION` (`#kyc-verification` → `verification_requests`)
   - `ACCOUNT_DELETION` (`#account-deletions` → `account_deletion_requests`)
   - `GENERAL_SUPPORT` / `BANNER_FEEDBACK` (`#general-support` → `general_support_requests`, `banner_feedback`)
3. **Ticket Actions (Real-Time Sync Back to User App's `My Tickets` Screen):**
   - Display `id` (`VRX-XXXXXX` / `REQ-...`), `category`, `channelLabel`, `userName`, `inGameName`, `userContact`, `title`, `description`, `relatedId`, `priority`, `source`, `status`, `createdAt`.
   - Admin can change `status` (`"PENDING" | "IN_REVIEW" | "INVESTIGATING" | "RESOLVED" | "REJECTED"`), type `adminReply`, and optionally attach `deliveredRedeemCode`.
   - Clicking **"Send Reply & Update Ticket"** MUST merge `{ status, adminReply, deliveredRedeemCode, updatedAt: Date.now() }` to ALL synced paths so the user sees it immediately in **My Tickets**:
     - RTDB: `/admin_requests/all/{id}`, `/support_tickets/{id}`, `/reports/{id}`, `/users/{userId}/reports/{id}`
     - Firestore: `admin_requests/{id}`, `support_tickets/{id}`, `reports/{id}`, `users/{userId}/reports/{id}`
     - Also write an in-app notification to `/users/{userId}/notifications/notif_ticket_${id}_${Date.now()}`.

---

### MODULE 5: USERS, SANCTIONS & TOURNAMENT LOBBY CHAT MODERATION — UPDATED
1. **Users & Sanctions (`/users` & `users`):**
   - Display `username`, `inGameName`, `freeFireId`, `phoneOrEmail`, `balance` (VT), `tokens`, `matchesPlayed`, `totalKills`, `totalWins`, `isBanned`, `isSuspended`, `isOnline`, `lastActiveAt`.
   - **Ban / Unban:** Update `isBanned`, `banReason`, `banType: "PERMANENT"` on `/users/{uid}` (RTDB & Firestore), sync `/banned_users/{uid}`, and push `USER_STATUS_CHANGED` to `/global_app_state`.
   - **Suspend / Unsuspend:** Update `isSuspended`, `suspendReason` on `/users/{uid}` and `/suspended_users/{uid}`, and push `USER_STATUS_CHANGED`.
   - **Manual Wallet / Token Adjustment:** Safe delta adjustment with mandatory reason that logs an official transaction in `/transactions` & `/users/{uid}/transactions`.
2. **Tournament Lobby Chat Moderation (NEW):**
   - Select any active tournament to view live messages from RTDB `/tournaments/{id}/chat_messages` & Firestore `tournaments/{id}/chat_messages`.
   - **Send Official Host/Admin Announcement:** Write message with `senderId: "admin"`, `senderName: "VeloRix Admin"`, `senderRole: "ADMIN"`, `isSystemMessage: true`, `messageType: "ANNOUNCEMENT"`, `channel: "ALL"`, `isPinned: true`.
   - **Pin / Unpin Message:** Toggle `isPinned` (`true/false`).
   - **Delete Message:** Soft-delete (`isDeleted: true`, `text: "Message removed by Admin"`).
   - **Chat Reports Queue:** Listen to `/chat_reports` & `chat_reports` with 1-click "Delete Message" or "Ban Sender".

---

### MODULE 6: BANNERS & GLOBAL BROADCASTS
1. **Banners CRUD (`/banners/{id}` in RTDB & `banners/{id}` in Firestore):**
   - Fields: `id`, `title`, `subtitle`, `badgeText`, `imageUrl`, `ctaText`, `actionType` (`"ANNOUNCEMENT" | "MATCH" | "WALLET" | "SUPPORT" | "LEADERBOARD"`), `targetId`, `gradientTheme`, `active` (`true/false`), `order`.
2. **Master Banner Toggle:** `showBanners` on `/app_config`, `/system_config` (RTDB) and `app_config/global` (Firestore).
3. **Push Global Notification:** Writes to `/notifications/{id}` (RTDB) and `notifications/{id}` (Firestore).

---

### MODULE 7: LIVE SYSTEM CONFIG & DPDP ACCOUNT DELETIONS (CLEANED UP)
1. **Live System Controls (writes to RTDB `/app_config`, `/system_config` & Firestore `app_config/global`, `system_config/config` + pushes `SYSTEM_CONFIG_UPDATED`):**
   - `isMaintenance` + `maintenanceTitle`, `maintenanceMessage`, `maintenanceUntil` (also mirror to RTDB `/maintenance`)
   - `isForceUpdate` + `minRequiredVersion`, `updateUrl`, `changelog`
   - `vpnRestrictionEnabled` (Boolean)
   - `showBanners` (Boolean)
   - `liveLobbyTicker` (String — live ticker banner text)
   - `otaPatchVersion` (String) & `otaReleaseNotes` (String)
   - **Active Core Feature Flags ONLY (`featureFlags` & `feature_flags`):**
     - `tournament_join` — Tournament Registrations
     - `room_credentials_delivery` — T-15m Room ID & Password Reveal
     - `instant_withdrawals` — UPI & Redeem Voucher Withdrawals
     - `token_conversion` — Combat Tokens to VT Conversion
     - `daily_login_rewards` — Daily Login Streak & Missions
     - `ai_sentinel_support` — 24x7 AI Support Assistant
2. **Account Deletion Requests & DPDP Audit Logs:**
   - Listen to RTDB `/account_deletion_requests` + `/deletion_requests` and Firestore `account_deletion_requests`.
   - **Approve & Purge:** Delete user profile from `/users/{uid}` & `users/{uid}`, mark request `status: "APPROVED_DELETED"`, update linked ticket to `RESOLVED`, and append entry to `/audit_logs`.
   - **Reject:** Set `status: "REJECTED"`, set `/users/{uid}/deletionPending = false`, and update linked ticket.
   - Read-only table for `/data_export_logs` and `/audit_logs`.
```
