package com.example.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * VelorixAnalytics: Integrates Firebase Analytics for event tracking and A/B Testing conversion.
 * Tracks user engagement, CTA button conversions (e.g. "Join Tournament" vs "Register Now"),
 * tournament registrations, room credential unlocks, and push notification engagements.
 */
object VelorixAnalytics {
    private const val TAG = "VelorixAnalytics"
    private var firebaseAnalytics: FirebaseAnalytics? = null

    fun initialize(context: Context) {
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context)
            Log.d(TAG, "Firebase Analytics initialized successfully.")
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Analytics initialization skipped or failed: ${e.message}")
        }
    }

    /**
     * A/B Testing Conversion: Logs CTA button clicks with variant text ("Join Tournament" vs "Register Now").
     */
    fun logCtaClick(ctaText: String, tournamentId: String, tournamentTitle: String) {
        val bundle = Bundle().apply {
            putString("cta_text", ctaText)
            putString("tournament_id", tournamentId)
            putString("tournament_title", tournamentTitle)
            putLong("timestamp", System.currentTimeMillis())
        }
        logEvent("cta_button_click", bundle)
        Log.d(TAG, "A/B Event: cta_button_click -> text: $ctaText, tourney: $tournamentId")
    }

    /**
     * Logs successful tournament registration.
     */
    fun logTournamentJoin(
        tournamentId: String,
        tournamentTitle: String,
        entryFee: Double,
        isVip: Boolean = false
    ) {
        val bundle = Bundle().apply {
            putString("tournament_id", tournamentId)
            putString("tournament_title", tournamentTitle)
            putDouble("entry_fee", entryFee)
            putBoolean("is_vip", isVip)
            putLong("timestamp", System.currentTimeMillis())
        }
        logEvent("tournament_joined", bundle)
    }

    /**
     * Logs when a user views/reveals Room ID and Room Password credentials.
     */
    fun logRoomCredentialsViewed(tournamentId: String, roomId: String) {
        val bundle = Bundle().apply {
            putString("tournament_id", tournamentId)
            putString("room_id", roomId)
            putLong("timestamp", System.currentTimeMillis())
        }
        logEvent("room_credentials_viewed", bundle)
    }

    /**
     * Logs incoming or tapped push notifications from FCM (Room Details, Payouts, Match Reminders).
     */
    fun logNotificationInteraction(type: String, action: String, tournamentId: String? = null) {
        val bundle = Bundle().apply {
            putString("notification_type", type)
            putString("action", action)
            tournamentId?.let { putString("tournament_id", it) }
            putLong("timestamp", System.currentTimeMillis())
        }
        logEvent("notification_interaction", bundle)
    }

    /**
     * Logs user payout request activity.
     */
    fun logPayoutRequest(amount: Double, method: String) {
        val bundle = Bundle().apply {
            putDouble("amount", amount)
            putString("payout_method", method)
            putLong("timestamp", System.currentTimeMillis())
        }
        logEvent("payout_requested", bundle)
    }

    /**
     * Set user property for A/B Testing segmentation (e.g. VIP loyalty tier).
     */
    fun setUserVipTier(isVip: Boolean) {
        try {
            firebaseAnalytics?.setUserProperty("vip_tier", if (isVip) "vip_player" else "standard_player")
        } catch (e: Exception) {
            Log.w(TAG, "Failed setting user property vip_tier: ${e.message}")
        }
    }

    fun setUserId(uid: String) {
        try {
            firebaseAnalytics?.setUserId(uid)
        } catch (e: Exception) {
            Log.w(TAG, "Failed setting user ID: ${e.message}")
        }
    }

    private fun logEvent(eventName: String, params: Bundle) {
        try {
            firebaseAnalytics?.logEvent(eventName, params)
        } catch (e: Exception) {
            Log.w(TAG, "Could not log event $eventName: ${e.message}")
        }
    }
}
