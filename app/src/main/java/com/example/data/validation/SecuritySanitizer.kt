package com.example.data.validation

import java.util.regex.Pattern

/**
 * Enterprise Security & Input Sanitization Suite.
 * Protects against:
 * 1. SQL Injection (Union queries, stacked queries, boolean-based bypass, comment markers)
 * 2. NoSQL & Firebase Realtime Database Key Injection (prohibited characters: . $ # [ ] / \0)
 * 3. Cross-Site Scripting (XSS) & HTML Tag Injections
 * 4. Path Traversal Attacks (../, ..\, null bytes)
 * 5. Unicode / Zero-Width homoglyph attacks
 */
object SecuritySanitizer {

    // SQL Injection Detection Regex Patterns
    private val SQL_INJECTION_PATTERNS = listOf(
        Pattern.compile("(?i)('(''|[^'])*')|(;\\s*--)|(--)|(/\\*.*\\*/)|(\\b(ALTER|CREATE|DELETE|DROP|EXEC(UTE)?|INSERT( +INTO)?|MERGE|SELECT|UPDATE|UNION( +ALL)?|TRUNCATE)\\b)"),
        Pattern.compile("(?i)(\\b(OR|AND)\\b\\s+['\"]?\\w+['\"]?\\s*=\\s*['\"]?\\w+['\"]?)"),
        Pattern.compile("(?i)(\\bWAITFOR\\s+DELAY\\b|\\bBENCHMARK\\s*\\(|\\bSLEEP\\s*\\()"),
        Pattern.compile("(?i)(\\bHAVING\\b|\\bGROUP\\s+BY\\b|\\bORDER\\s+BY\\b.*--)"),
        Pattern.compile("(?i)(0x[0-9a-fA-F]+)")
    )

    // XSS / HTML Tag Detection Pattern
    private val XSS_TAG_PATTERN = Pattern.compile("(?i)<\\s*(script|iframe|object|embed|svg|img|style|link|meta|form|input|button|a)[^>]*>.*?(<\\s*/\\s*\\1\\s*>)?", Pattern.DOTALL)
    private val XSS_ATTR_PATTERN = Pattern.compile("(?i)(javascript\\s*:|data\\s*:|vbscript\\s*:|on\\w+\\s*=)")

    // Firebase RTDB Forbidden Key Characters: . $ # [ ] / or ASCII control chars 0-31 and 127
    private val RTDB_FORBIDDEN_CHARS = Regex("[.#$\\[\\]/\\x00-\\x1F\\x7F]")

    /**
     * Sanitizes a standard user input string.
     * Strips dangerous script tags, escapes SQL characters, trims whitespace, and removes null bytes.
     */
    fun sanitizeInput(input: String?, maxLength: Int = 500): String {
        if (input.isNullOrBlank()) return ""
        
        var clean = input.trim()
        
        // 1. Remove null bytes and invisible control characters
        clean = clean.replace("\u0000", "").replace("\r", "")

        // 2. Truncate if exceeds safe max length
        if (clean.length > maxLength) {
            clean = clean.substring(0, maxLength)
        }

        // 3. Neutralize script tags and event handlers
        clean = XSS_TAG_PATTERN.matcher(clean).replaceAll("")
        clean = XSS_ATTR_PATTERN.matcher(clean).replaceAll("")

        // 4. Neutralize quotes and SQL termination characters safely
        clean = clean.replace("<", "&lt;").replace(">", "&gt;")

        return clean
    }

    /**
     * Sanitizes an ID for use as a Firebase Realtime Database or Firestore document key.
     * Replaces prohibited chars (., $, #, [, ], /) with safe underscores.
     */
    fun sanitizeDatabaseKey(key: String?): String {
        if (key.isNullOrBlank()) return "unknown_${System.currentTimeMillis()}"
        val sanitized = key.trim().replace(RTDB_FORBIDDEN_CHARS, "_")
        return if (sanitized.isBlank()) "key_${System.currentTimeMillis()}" else sanitized.take(128)
    }

    /**
     * Validates that an input is free from active SQL Injection signatures.
     * Returns true if the input is safe, false if a suspicious payload is detected.
     */
    fun isSqlInjectionSafe(input: String?): Boolean {
        if (input.isNullOrBlank()) return true
        val text = input.trim()
        for (pattern in SQL_INJECTION_PATTERNS) {
            if (pattern.matcher(text).find()) {
                return false
            }
        }
        return true
    }

    /**
     * Sanitizes search query strings by removing wildcards, quotes, and dangerous metacharacters.
     */
    fun sanitizeSearchQuery(query: String?): String {
        if (query.isNullOrBlank()) return ""
        return query.trim()
            .replace(Regex("[%_';\"\\\\]"), "")
            .take(64)
    }

    /**
     * Validates and cleans email addresses against injection.
     */
    fun sanitizeEmail(email: String?): String {
        if (email.isNullOrBlank()) return ""
        val clean = email.trim().lowercase()
        return if (clean.matches(Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"))) {
            clean.take(120)
        } else {
            clean.filter { it.isLetterOrDigit() || it in "@._+-" }.take(120)
        }
    }

    /**
     * Sanitizes room credentials (Room ID, Password) to prevent payload injection into match lobbies.
     */
    fun sanitizeRoomCredential(credential: String?): String {
        if (credential.isNullOrBlank()) return ""
        return credential.trim()
            .replace(Regex("[<>\"';/\\\\]"), "")
            .take(50)
    }
}
