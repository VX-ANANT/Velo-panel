# VELORIX ADMIN PANEL — MASTER INTEGRATION PROMPT (COMPACT & TOKEN-SAFE)

> **HOW TO USE THIS FILE:**
> Copy the prompt below (or copy one module at a time if your Admin Panel agent has a small context window) and paste it into your Admin Panel AI agent. It is specifically engineered to be **compact, exact, and token-efficient** so you NEVER get an `"Exceeds Agent Limit"` error and never waste prompt credits on broken UI.

---

## COPY-PASTE PROMPT FOR ADMIN PANEL AGENT

```markdown
You are building/updating the **Velorix Esports Admin Panel** connected to the same Firebase project (`Firebase Realtime Database` + `Cloud Firestore`) as the Velorix Android App.

### CRITICAL RULES FOR YOU (READ BEFORE WRITING ANY CODE)
1. **STRICT MINIMAL UI — NO COMPLICATED CLUTTER:**
   - Do NOT create cluttered dashboards, decorative charts, fake analytics graphs, or nested tabs.
   - Use a clean, minimal dark layout (`#0B0E14` background, `#11151F` cards, `1px solid rgba(255,255,255,0.08)` borders) with ONLY 6 clear sidebar pages:
     1. **Tournaments & Results**
     2. **Wallet Requests (Deposits & Withdrawals)**
     3. **Users & Moderation**
     4. **Account Deletions & Data Exports**
     5. **Support Tickets & Broadcasts**
     6. **App Config (Maintenance / Force Update / Flags)**
2. **ZERO BROKEN OR DEAD BUTTONS:**
   - Never render a button, toggle, or dropdown unless its real Firebase `RTDB + Firestore` write handler is 100% wired in the same file.
   - Do NOT use mock state or `console.log` placeholders. Every action must persist to Firebase immediately and show a clear toast.
3. **DUAL-SYNC RULE (RTDB + FIRESTORE + GLOBAL SYNC):**
   - The Android app listens to both Firebase Realtime Database (RTDB) and Cloud Firestore, plus `/global_app_state` in RTDB.
   - Whenever you update a tournament, room ID/password, standings/results, wallet request, user status, or config, write to **both RTDB and Firestore** and update `/global_app_state`:
     ```js
     await update(ref(rtdb, "global_app_state"), {
       eventType, // "TOURNAMENT_UPDATED" | "ROOM_CREDENTIALS_RELEASED" | "SCHEDULE_CHANGED" | "TOURNAMENT_CANCELLED" | "USER_STATUS_CHANGED"
       source: "ADMIN_PANEL",
       tournamentId: tournamentId || "",
       affectedUserId: userId || "",
       timestamp: Date.now(),
       payload: metadata || {}
     });
     ```

---

### MODULE 1: MULTI-POSITION TOURNAMENT STANDINGS & WINNER PAYOUT SYSTEM (HIGHEST PRIORITY)
**Problem to solve:** A tournament does NOT have only 1 winner. Every joined participant (`#1` to `#N`) must receive their final `rank` (position), `kills`, `rankPrize`, `killPrize` (`kills * perKillReward`), total `prizeWon` (`rankPrize + killPrize`), and `wonAnything` (`prizeWon > 0`), whether they won money or ₹0.

#### How the "Declare Standings & Distribute Prizes" Modal Must Work:
1. Inside the **Tournaments** table, add a **"Declare Results / Standings"** button on every tournament.
2. Clicking it loads all joined players for that tournament from:
   - RTDB `/tournaments/{tournamentId}/participants` (and fallback `/tournament_registrations` where `tournamentId == id`)
   - Firestore `tournaments/{tournamentId}/participants`
3. Display a clean, simple table of all joined participants (`Slot #`, `In-Game Name`, `Character ID`, `User UID`) with editable inputs for each player:
   - **`rank`** (Number: `1` to `maxSlots`, default sorted by slot)
   - **`kills`** (Number: `0`+, default `0`)
   - **`rankPrize`** (Number ₹: auto-suggested from prize pool distribution e.g. Rank 1 = 50%, Rank 2 = 30%, Rank 3 = 20%, Rank 4+ = ₹0, but editable by Admin for custom multi-winner distributions)
   - **`killPrize`** (Auto-calculated as `kills * tournament.perKillReward`, editable)
   - **`prizeWon`** (Auto-calculated as `rankPrize + killPrize`)
   - **Status Badge**: Shows `WINNER (₹{prizeWon})` if `prizeWon > 0`, or `PARTICIPATED (₹0)` if `prizeWon == 0`.
4. When Admin clicks **"Publish Final Standings & Credit Winners"**, execute this exact settlement logic for **ALL joined participants**:
   - Build the `finalLeaderboard` array (sorted by `rank` ascending):
     ```js
     const entry = {
       rank: Number(p.rank),
       userId: p.userId,
       username: p.username || p.inGameName,
       inGameName: p.inGameName || p.username,
       characterId: p.characterId || "",
       teamName: p.teamName || "",
       slotNumber: Number(p.slotNumber || 1),
       kills: Number(p.kills || 0),
       points: Number(p.kills || 0) * 2 + (p.rank === 1 ? 15 : p.rank === 2 ? 12 : p.rank === 3 ? 10 : 5),
       rankPrize: Number(p.rankPrize || 0),
       killPrize: Number(p.killPrize || 0),
       prizeWon: Number(p.prizeWon || 0),
       wonAnything: Number(p.prizeWon || 0) > 0,
       payoutStatus: Number(p.prizeWon || 0) > 0 ? "PAID" : "NO_PRIZE",
       updatedAt: Date.now()
     };
     ```
   - Save the completed tournament document (`status: "COMPLETED"`, `isCompleted: true`, `payoutStatus: "PAID"`, `completedAt: Date.now()`, `totalPrizeDisbursed`, `winnerName`, `winnerUid`, `winnerPrize`, `finalLeaderboard`, `standings: finalLeaderboard`, `results: finalLeaderboard`) to:
     - Firestore: `tournament_history/{tournamentId}`, `tournaments/{tournamentId}`, `matches/{tournamentId}`
     - RTDB: `/tournament_history/{tournamentId}`, `/tournaments/{tournamentId}`, `/matches/{tournamentId}`
     - Also write each entry to subcollection `tournaments/{tournamentId}/leaderboard/{userId}` and `tournament_history/{tournamentId}/leaderboard/{userId}`.
   - **For EVERY participant in `finalLeaderboard` (whether `prizeWon > 0` or `0`):**
     1. Write their match result record to `/users/{userId}/match_stats/{tournamentId}_{userId}` (RTDB) and `users/{userId}/match_stats/{tournamentId}_{userId}` (Firestore):
        ```js
        {
          id: `${tournamentId}_${entry.userId}`,
          tournamentId,
          tournamentTitle: tournament.title,
          game: tournament.game || "Free Fire",
          userId: entry.userId,
          matchNo: `#${tournament.displayId || tournamentId.slice(-4)}`,
          position: entry.rank,
          kills: entry.kills,
          winnings: entry.prizeWon,
          tokensEarned: entry.kills * 5,
          timestamp: Date.now(),
          status: entry.wonAnything ? "WON" : "PARTICIPATED"
        }
        ```
     2. **If `entry.prizeWon > 0` (for ALL winners — Rank 1, 2, 3, or any player with kill bounties):**
        - Increment the user's wallet `balance` by `+entry.prizeWon`, increment `matchesPlayed` by `+1`, `totalWins` by `+(entry.rank === 1 ? 1 : 0)`, and `totalKills` by `+entry.kills` in both RTDB `/users/{userId}` and Firestore `users/{userId}` (prevent double-crediting by checking if transaction `win_${tournamentId}_${entry.userId}` already exists).
        - Create transaction `win_${tournamentId}_${entry.userId}` in RTDB `/transactions/{txId}`, `/users/{userId}/transactions/{txId}`, and Firestore `transactions/{txId}`:
          ```js
          {
            id: `win_${tournamentId}_${entry.userId}`,
            userId: entry.userId,
            type: "WINNINGS",
            amount: entry.prizeWon,
            detail: `Won ${tournament.title} (Rank #${entry.rank} • ${entry.kills} Kills)`,
            isPositive: true,
            status: "SUCCESS",
            timestamp: Date.now()
          }
          ```
     3. **If `entry.prizeWon === 0`:**
        - Still increment `matchesPlayed` by `+1` and `totalKills` by `+entry.kills` on `/users/{userId}` and write a notification to `/users/{userId}/notifications/notif_result_${tournamentId}` so the player sees their exact Rank `#${entry.rank}` and `${entry.kills}` kills in the app!

---

### MODULE 2: TOURNAMENT CRUD, ROOM ID/PASSWORD & CANCELLATION REFUND
1. **Create / Edit Tournament**:
   - Fields: `id`, `title`, `game` (`"Free Fire"`), `format` (`"SOLO" | "DUO" | "SQUAD"`), `matchCategory` (`"BATTLE_ROYALE" | "CLASH_SQUAD" | "LONE_WOLF"`), `matchMode` (`"PER_KILL" | "SURVIVAL"`), `mapType` (`"Bermuda" | "Purgatory" | "Kalahari"`), `entryFee`, `prizePool`, `killBounty` (`perKillReward`), `maxSlots`, `filledSlots`, `dateTimeStr`, `startTimeMillis`, `status` (`"UPCOMING" | "LIVE" | "COMPLETED" | "CANCELLED"`), `roomId`, `roomPassword`, `imageUrl`.
   - Write to RTDB `/tournaments/{id}` + `/matches/{id}` AND Firestore `tournaments/{id}` + `matches/{id}`.
2. **Release Room ID & Password**:
   - Updating `roomId` and `roomPassword` must update both RTDB & Firestore and push `EVENT_ROOM_CREDENTIALS_RELEASED` to `/global_app_state` with `{ tournamentTitle, roomId, roomPass }`.
3. **Cancel Tournament & Auto-Refund**:
   - Setting status to `"CANCELLED"` must loop through all registered participants in `/tournaments/{id}/participants`, refund `+entryFee` to each user's `balance` in RTDB & Firestore, record transaction `refund_${id}_${userId}` (`type: "REFUND"`, `isPositive: true`, `status: "SUCCESS"`), and push `EVENT_TOURNAMENT_CANCELLED` to `/global_app_state`.

---

### MODULE 3: WALLET DEPOSITS & WITHDRAWALS
1. **Deposits (`/deposit_requests` in RTDB & `deposit_requests` in Firestore)**:
   - Fields: `id`, `userId`, `username`, `amount`, `utrNumber`, `paymentRef`, `transactionId`, `status` (`"PENDING" | "APPROVED" | "REJECTED"`), `timestamp`.
   - **Approve**: Set `status: "APPROVED"`, increment user `balance` by `+amount` in RTDB `/users/{userId}` & Firestore `users/{userId}`, update `/transactions/{transactionId}` to `status: "SUCCESS"`, and write a notification to `/users/{userId}/notifications`.
   - **Reject**: Set `status: "REJECTED"` and update `/transactions/{transactionId}` to `status: "REJECTED"`.
2. **Withdrawals (`/withdraw_requests` in RTDB & `withdraw_requests` in Firestore)**:
   - Fields: `id`, `userId`, `username`, `amount`, `upiId`, `transactionId`, `status` (`"PENDING" | "APPROVED" | "REJECTED"`), `timestamp`. (Note: User balance is already escrowed/deducted when requested in the app).
   - **Approve (Mark Paid)**: Set `status: "APPROVED"` and update `/transactions/{transactionId}` to `status: "SUCCESS"`.
   - **Reject & Refund**: Set `status: "REJECTED"`, update `/transactions/{transactionId}` to `status: "REJECTED"`, and refund `+amount` back to `/users/{userId}/balance` in RTDB & Firestore.

---

### MODULE 4: USERS, BAN / SUSPEND & WALLET ADJUSTMENT
- Read from RTDB `/users` & Firestore `users`.
- **Ban User**: Set `isBanned: true`, `banReason`, `banType: "PERMANENT"` on `/users/{uid}` (RTDB & Firestore), write to `/banned_users/{uid}`, and push `EVENT_USER_STATUS_CHANGED` (`newStatus: "BANNED"`) to `/global_app_state`.
- **Unban User**: Set `isBanned: false`, remove `/banned_users/{uid}`, and update `/users/{uid}`.
- **Suspend / Unsuspend User**: Set `isSuspended: true/false`, `suspendReason` on `/users/{uid}` and `/suspended_users/{uid}`.

---

### MODULE 5: ACCOUNT DELETION REQUESTS & DATA EXPORTS (DPDP ACT 2023)
1. **Account Deletion Queue**:
   - Listen to RTDB `/account_deletion_requests` and `/deletion_requests` + Firestore `account_deletion_requests`.
   - Fields: `requestId`, `userId`, `username`, `email`, `freeFireId`, `walletBalanceAtRequest`, `reason`, `status` (`"PENDING_ADMIN_APPROVAL" | "APPROVED_DELETED" | "REJECTED"`), `requestedAt`, `scheduledPurgeAt`.
   - **Approve & Purge User**: Delete user data from `/users/{userId}`, `users/{userId}`, mark request `status: "APPROVED_DELETED"`, and write to `/audit_logs`.
   - **Reject Request**: Set `status: "REJECTED"` and restore `/users/{userId}/deletionPending` to `false`.
2. **Audit Logs & Data Exports**:
   - Display `/data_export_logs` and `/audit_logs` in a simple read-only table.

---

### MODULE 6: SUPPORT TICKETS, BANNERS & GLOBAL APP CONFIG
1. **Support Tickets (`/reports` in RTDB & `reports` in Firestore)**:
   - Fields: `id`, `userId`, `username`, `category`, `subject`, `description`, `status` (`"OPEN" | "IN_PROGRESS" | "RESOLVED"`), `adminReply`, `timestamp`.
   - Admin can type an `adminReply` and change `status` to `"RESOLVED"`, updating both RTDB `/reports/{id}` and Firestore `reports/{id}`.
2. **Global App Config (`/app_config` & `/system_config` in RTDB + `app_config/global` in Firestore)**:
   - Simple toggles for:
     - `isMaintenance` (Boolean) + `maintenanceTitle`, `maintenanceMessage`, `maintenanceUntil`
     - `isForceUpdate` (Boolean) + `minRequiredVersion`, `updateUrl`, `changelog`
     - `showBanners` (Boolean) & `showDeveloperModal` (Boolean)
```
