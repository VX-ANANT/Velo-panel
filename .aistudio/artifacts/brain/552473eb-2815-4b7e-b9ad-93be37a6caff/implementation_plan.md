# Velorix Esports Administration & Tournament Ecosystem Overhaul

A comprehensive overhaul of the Velorix Tournament Admin platform focusing on multi-tournament management, custom dynamic prize distribution with human-written breakdowns, complete removal of casual emojis in favor of a sleek esports interface, automated zero-friction authentication, in-depth player profiles with full referral tree tracking, and a unified consolidated architecture.

> [!IMPORTANT]
> **Key Decisions Confirmed by User:**
> 1. **Multi-Tournament Creation:** Support versatile multi-tournament creation including step-by-step modal, slot presets, quick duplication, and scheduled batch generation.
> 2. **Custom Prize Distribution:** Dynamic custom prize calculations editable on-the-fly from the tournament dashboard, presented in clean, professional human wording.
> 3. **Design Standard:** Complete removal of all emojis across all screens in favor of high-precision Material Symbols and esports aesthetics.
> 4. **Authentication & Verification:** Removal of manual KYC approval blockers (instant auto-verification) and automatic persistent session login from splash.
> 5. **Referral Tracking:** Full referral tracking inside player profiles detailing referral count, rewards, and an itemized list of referred users.

---

## 1. Overview & Core Concept

Velorix Admin is the central command center for competitive Free Fire tournaments. This overhaul resolves workflow bottlenecks for tournament organizers by:
- Allowing administrators to generate, clone, and schedule multiple tournaments in bulk rather than one-by-one.
- Presenting tournament prize structures in human-readable terms with customizable rank and per-kill distributions.
- Eradicating manual KYC verification bottlenecks so players can participate immediately.
- Enabling automatic authentication so admins never get stranded at redundant login screens.
- Providing deep visibility into player profiles and referral networks (tracking exact referred users, joined dates, and earned bonuses).
- Consolidating scattered independent views into a coherent, tabbed esports operations hub.

---

## 2. User Experience & Visual Design

### Aesthetic Direction: Precision Esports Command Center
- **Color Palette:** Dark luxury esports theme with deep obsidian backgrounds (`#0A0A0E`, `#12121A`), sharp electric accent highlights (Cyan `#00E5FF`, Amber `#FFB300`, Emerald `#00E676`), and muted technical borders (`#232336`).
- **Emoji Elimination:** Zero emojis anywhere in the app. Replaced with Material Symbols (`MilitaryTech`, `SportsEsports`, `AccountBalanceWallet`, `Group`, `Shield`, `Schedule`, `Analytics`, `CheckCircle`, etc.).
- **Typography & Hierarchy:** Clean technical hierarchy using condensed uppercase badges, clear tabular figures for prize pools, and human-crafted explanatory paragraphs.

### Key User Flows

```
┌────────────────────────────────────────────────────────────────────────┐
│                        App Launch (Splash Screen)                      │
│        Auto-Detects Bound Account or Cached Firebase Session           │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
       ┌────────────────────────────┴───────────────────────────┐
       │ Session Valid?                                         │
       ├────────────────────────────┬───────────────────────────┤
       │ YES (Automatic Pass)       │ NO                        │
       ▼                            ▼                           │
┌──────────────────────────────┐ ┌──────────────────────────────┐
│ Direct to Unified Dashboard  │ │ Fast Single-Tap Google /     │
│ (No KYC Blockers)            │ │ Passwordless Link / Email    │
└──────────────┬───────────────┘ └──────────────┬───────────────┘
               │                                │
               │◄───────────────────────────────┘
               ▼
┌────────────────────────────────────────────────────────────────────────┐
│                      Unified Esports Admin Hub                         │
├─────────────────┬──────────────────┬─────────────────┬─────────────────┤
│  Tournaments    │  Users & Referrals│  Finance & Payout│  Operations     │
│  - Multi-Create │  - User Profile  │  - UPI Approvals│  - Announce     │
│  - Batch Clone  │  - Referral List │  - Audit Logs   │  - Room ID      │
│  - Prize Engine │  - Ban / Unban   │  - Revenue Graph│  - Support      │
└─────────────────┴──────────────────┴─────────────────┴─────────────────┘
```

---

## 3. Detailed Component & Feature Specification

### 3.1 Multi-Tournament Creation & Batch Scheduler
- **Batch Generator Dialog:** Create recurring daily slots (e.g. 5 Battle Royale matches spaced by 60 minutes) in a single click.
- **Quick Clone / Duplicate:** Duplicate any existing tournament with custom title incrementing, preserving prize logic and weapon rules while resetting registrations.
- **Unified Tournament List Filter:** Filter tournaments by Category (BR, CS, Lone Wolf, Scrims), Status (Upcoming, Live, Completed), and Format (Solo, Duo, Squad).

### 3.2 Dynamic Custom Prize Distribution & Human Explanations
- **Dynamic Prize Calculations:**
  - First Place (Champion): Customizable amount & % of pool
  - Second Place (Runner-Up): Customizable amount & % of pool
  - Third Place (Podium): Customizable amount & % of pool
  - Top Fragger Bounty: Dedicated reward for highest eliminations
  - Per-Kill Bonus: Direct cash rewarded per confirmed kill
- **Human-Readable Breakdown Generator:**
  - Generates clear, professional match summaries:
    > "Champion receives ₹500 directly in their wallet upon room verification. Runner-up receives ₹250, and 3rd place receives ₹150. Every recorded elimination awards an instant ₹20 per-kill bounty. Total prize pool of ₹1,000 is distributed automatically following room screenshot audit."
- **Dashboard Quick-Edit:** Organizer can modify prize pool values, entry fee, and distribution directly from the tournament management screen without recreating the match.

### 3.3 Zero-Gate Authentication & KYC Removal
- **Auto-Login:** Splash screen checks for active Firebase Auth session or bound account; automatically enters dashboard within 300ms without manual re-login.
- **KYC Requirement Removal:** All user accounts have `eligibleForCashTournaments = true` and `ageConfirmed = true` by default. Manual KYC approval bottlenecks are eliminated so users can play and withdraw without waiting for manual verification.

### 3.4 Deep Profile & Full Referral Tracking Hub
- **Player Profile Modal:** Detailed view showing player avatar, username, Free Fire UID, IGN, contact, registration timestamp, wallet balance (deposit, winning, bonus), matches played, kills, and win rate.
- **Referral Network Drill-Down:**
  - Displays user's unique referral code and referral stats (Total Referred, Active Participants, Total Bonus Earned).
  - Itemized Referral List: Fetches and displays all users referred by this player:
    - Referred Player Username & Free Fire UID
    - Registration Date & Time
    - Match Activity (Matches played)
    - Bonus Credit Status (Credited / Pending)

### 3.5 Unified System Architecture & Consolidation
- Replaces disconnected scattered screens with a cohesive 4-tab central console:
  1. **Tournaments Hub:** Active, upcoming, and completed tournaments, batch creator, quick room ID publisher, prize calculator.
  2. **Players & Referrals Hub:** Searchable user directory, player profile inspect, referral tree view, wallet balances, balance adjustments.
  3. **Finance & Payouts Hub:** Instant UPI withdrawal requests approval/rejection, deposit approvals, audit log.
  4. **Operations Hub:** Custom room ID broadcast, match proofs verification, support tickets, global announcements.

---

## 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            Repository Layer                                 │
├──────────────────────────────────────┬──────────────────────────────────────┤
│      Firebase Realtime Database      │           Cloud Firestore            │
│  - Live Room IDs & Passwords         │  - tournaments/ (Rules, Prizes)      │
│  - Realtime Registered Slots         │  - users/ (Profiles, Referrals)      │
│  - Active Broadcasts                 │  - payoutRequests/ (Withdrawals)     │
│  - Presence & Live Sync              │  - supportTickets/ & auditLogs/      │
└──────────────────────────────────────┴──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                       TournamentDashboardViewModel                          │
├─────────────────────────────────────────────────────────────────────────────┤
│  - createTournament() & createTournamentsBatch()                            │
│  - duplicateTournament()                                                    │
│  - updateTournamentPrizes(id, prizePool, rank1, rank2, rank3, perKill)       │
│  - getUserReferrals(userId): Flow<List<UserProfile>>                        │
│  - autoVerifyPlayer(userId)                                                 │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            Compose UI Layer                                 │
├───────────────────┬──────────────────┬───────────────────┬──────────────────┤
│ TournamentsHub    │ PlayersHub       │ FinanceHub        │ OperationsHub    │
│ - BatchDialog     │ - ProfileSheet   │ - PayoutCard      │ - RoomIdSheet    │
│ - PrizeEngineCard │ - ReferralList   │ - TransactionList │ - SupportChat    │
└───────────────────┴──────────────────┴───────────────────┴──────────────────┘
```

---

## 5. Verification Plan

1. **Compilation Verification:** Run `compile_applet` to confirm error-free compilation of all Kotlin sources and Compose components.
2. **Auto-Login Check:** Verify splash screen transitions directly to dashboard when authenticated.
3. **Batch Tournament Creation:** Verify batch creation publishes multiple tournament records to Firestore simultaneously.
4. **Prize Distribution Calculation:** Verify custom prize distributions generate human-readable text and reflect immediately in tournament details.
5. **Referral Tree Verification:** Confirm player profile correctly displays total referrals and loads the itemized list of referred users.
6. **Emoji Cleanliness:** Audit screens to ensure all emojis are replaced with clean Material Symbols.
