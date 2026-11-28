package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.common.UntitledIcons
import com.example.ui.theme.VelorixAccent
import com.example.ui.theme.VelorixAmber
import com.example.ui.theme.VelorixEmerald

data class TechAttribution(
    val title: String,
    val provider: String,
    val category: String,
    val description: String,
    val license: String,
    val icon: ImageVector,
    val accentColor: Color,
    val features: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttributionsScreen(
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val attributions = remember {
        listOf(
            TechAttribution(
                title = "Firebase Backend & Dual Realtime Sync",
                provider = "Google Cloud & Firebase",
                category = "Cloud Infrastructure",
                description = "Dual-database high-throughput backend architecture combining Firebase Realtime Database (RTDB) for zero-latency room syncing and Cloud Firestore for multi-document transactional persistence.",
                license = "Apache License 2.0 / Google Terms of Service",
                icon = Icons.Outlined.CloudSync,
                accentColor = VelorixAmber,
                features = listOf(
                    "Realtime WebSocket node replication at 60Hz",
                    "Dual-write ACID transactions for wallet debits/credits",
                    "Firebase Authentication with Google Credential Manager & Phone OTP",
                    "Continuous presence state and match slot allocation"
                )
            ),
            TechAttribution(
                title = "Untitled UI Design System",
                provider = "Untitled UI System",
                category = "Interface System",
                description = "Custom-engineered vector icon suite, hairline card borders, micro-tint elevation, and clean grid layouts inspired by modern product design standards.",
                license = "Commercial & Open Product License",
                icon = Icons.Outlined.DesignServices,
                accentColor = V14AccentBright,
                features = listOf(
                    "Geometric line-art vector icons (Trophy, Shield, Zap, Bracket, Sparkles)",
                    "Subtle neutral elevation with 0.5dp dark borders",
                    "High-density metric badges with state-driven color pills",
                    "Adaptive dual-pane support for foldables and tablets"
                )
            ),
            TechAttribution(
                title = "Generative AI Copilot Engine",
                provider = "Google Generative AI SDK",
                category = "Artificial Intelligence",
                description = "Real-time tournament rule analysis, bracket conflict resolution, fraud detection, and admin copilot assistance.",
                license = "Apache License 2.0",
                icon = Icons.Outlined.AutoAwesome,
                accentColor = V14AccentBright,
                features = listOf(
                    "Sub-second latency tournament dispute analysis",
                    "Real-time fraud and abnormal match proof verification",
                    "Intelligent automated support response generation",
                    "Low-latency streaming responses via Coroutine Channels"
                )
            ),
            TechAttribution(
                title = "Jetpack Compose & Material 3",
                provider = "Google Android Open Source Project",
                category = "UI Framework",
                description = "Declarative, reactive Android UI toolkit built with Kotlin, utilizing Material Design 3 dynamic color tokens, smooth spatial transitions, and hardware-accelerated rendering.",
                license = "Apache License 2.0",
                icon = Icons.Outlined.Layers,
                accentColor = VelorixEmerald,
                features = listOf(
                    "Zero-lag 120fps animated state transitions",
                    "Edge-to-edge system insets and immersive status bars",
                    "Type-safe navigation stack routing",
                    "Custom accessible touch targets exceeding 48dp"
                )
            ),
            TechAttribution(
                title = "Google Play Credential Manager",
                provider = "Google Play Services",
                category = "Identity & Security",
                description = "Modern federated authentication supporting Google Sign-In with ID tokens, passkeys, and biometric verification with zero legacy APIs.",
                license = "Google Play Services SDK Terms",
                icon = Icons.Outlined.Fingerprint,
                accentColor = V14AccentBright,
                features = listOf(
                    "One-tap account selection dialog",
                    "Automatic credential discovery and fallback",
                    "Cryptographic nonce verification",
                    "Encrypted device-account persistent binding"
                )
            ),
            TechAttribution(
                title = "Token-Bucket Sliding Rate Limiter",
                provider = "Velorix Core Architecture",
                category = "Security & Network Protection",
                description = "Server-synchronized sliding window log and fractional token-bucket engine regulating API throughput, prevent multi-request race conditions, and deter brute-force attempts.",
                license = "Proprietary Velorix Esports Engine",
                icon = Icons.Outlined.Speed,
                accentColor = VelorixEmerald,
                features = listOf(
                    "Continuous sub-second fractional token refills",
                    "Sliding window log preventing edge-of-interval abuse",
                    "Cloud-synchronized lockout state across user devices",
                    "Progressive exponential penalty multiplier on failure streaks"
                )
            ),
            TechAttribution(
                title = "Kotlin Coroutines & Reactive Flow",
                provider = "JetBrains",
                category = "Concurrency Runtime",
                description = "Lightweight structured concurrency and asynchronous data streaming powering real-time Firestore snapshots, timer countdowns, and network IO tasks.",
                license = "Apache License 2.0",
                icon = Icons.Outlined.Code,
                accentColor = VelorixAmber,
                features = listOf(
                    "Structured lifecycle-aware coroutine scopes",
                    "Cold and hot Flow state management",
                    "Non-blocking background thread offloading",
                    "Channel-based message queue coordination"
                )
            )
        )
    }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Technology Stack", "Open Source Licenses", "Architecture & Credits")

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(V14Bg)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(V14CardSurface)
                            .border(1.dp, V14Border, CircleShape)
                            .clickable(onClick = onNavigateBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Attribution & Credits",
                            color = V14TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "FRAMEWORKS, LIBRARIES & ARCHITECTURE",
                            color = V14TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = V14Border)
            }
        },
        containerColor = V14Bg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(V14Bg)
                .padding(innerPadding)
        ) {
            // Flat V14 Underline Mode Selector (no nested box-in-box)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                tabs.forEachIndexed { index, label ->
                    val isSelected = selectedTab == index
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = index }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) V14TextPrimary else V14TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .height(2.dp)
                                .background(if (isSelected) V14AccentPrimary else Color.Transparent)
                        )
                    }
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = V14Border)

            when (selectedTab) {
                0 -> {
                    // Tech Stack List
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(attributions) { item ->
                            AttributionCard(item = item)
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            PlatformSignatureFooter()
                        }
                    }
                }
                1 -> {
                    // Open Source Licenses
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                text = "OPEN SOURCE LICENSES & NOTICES",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        items(openSourceLicenses) { license ->
                            LicenseCard(license = license)
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            PlatformSignatureFooter()
                        }
                    }
                }
                2 -> {
                    // Architecture & Credits
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            ArchitectureOverviewCard()
                        }
                        item {
                            DualSyncSystemCard()
                        }
                        item {
                            SecurityComplianceCard()
                        }
                        item {
                            TeamCreditsCard()
                        }
                        item {
                            PlatformSignatureFooter()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttributionCard(item: TechAttribution) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = V14CardSurface,
        border = BorderStroke(1.dp, V14Border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = item.accentColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, item.accentColor.copy(alpha = 0.25f)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                item.icon,
                                contentDescription = null,
                                tint = item.accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = item.title,
                            color = V14TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = item.provider,
                            color = V14TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = V14CardElevated,
                    border = BorderStroke(0.5.dp, V14Border),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = item.category,
                        color = item.accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.description,
                color = V14TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item.features.forEach { feat ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = null,
                            tint = item.accentColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = feat,
                            color = V14TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "License",
                    color = V14TextMuted,
                    fontSize = 10.sp
                )
                Text(
                    text = item.license,
                    color = V14TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

data class OpenSourceLicense(
    val name: String,
    val author: String,
    val licenseType: String,
    val url: String,
    val summary: String
)

private val openSourceLicenses = listOf(
    OpenSourceLicense(
        name = "Firebase Android SDK (v33.8.0)",
        author = "Google LLC",
        licenseType = "Apache-2.0",
        url = "https://github.com/firebase/firebase-android-sdk",
        summary = "Provides real-time cloud data synchronization, authentication providers, Firestore document databases, and Cloud Storage integration."
    ),
    OpenSourceLicense(
        name = "AndroidX Jetpack Compose (v1.7.0)",
        author = "Google Android Open Source Project",
        licenseType = "Apache-2.0",
        url = "https://android.googlesource.com/platform/frameworks/support",
        summary = "Modern reactive toolkit for building native Android interfaces with hardware accelerated graphics and declarative UI paradigm."
    ),
    OpenSourceLicense(
        name = "Google Generative AI Client SDK (v0.9.0)",
        author = "Google LLC",
        licenseType = "Apache-2.0",
        url = "https://github.com/google/generative-ai-android",
        summary = "Official Kotlin SDK connecting Android clients directly to Gemini multimodal models."
    ),
    OpenSourceLicense(
        name = "AndroidX Credential Manager (v1.3.0)",
        author = "Google Android Open Source Project",
        licenseType = "Apache-2.0",
        url = "https://developer.android.com/training/sign-in/credential-manager",
        summary = "Unified authentication manager supporting Passkeys, Google Sign-In, and biometric password autofill."
    ),
    OpenSourceLicense(
        name = "Kotlinx Coroutines & Serialization",
        author = "JetBrains s.r.o.",
        licenseType = "Apache-2.0",
        url = "https://github.com/Kotlin/kotlinx.coroutines",
        summary = "Multiplatform coroutines and type-safe JSON serialization engine."
    ),
    OpenSourceLicense(
        name = "Coil Compose (v2.7.0)",
        author = "Coinbase, Inc.",
        licenseType = "Apache-2.0",
        url = "https://github.com/coil-kt/coil",
        summary = "Fast, lightweight image loading library for Android powered by Kotlin Coroutines."
    ),
    OpenSourceLicense(
        name = "Untitled UI Design Kit",
        author = "Untitled UI",
        licenseType = "Custom Product License",
        url = "https://www.untitledui.com",
        summary = "Precision vector UI system icons, typography hierarchy, and hairline container layouts."
    )
)

@Composable
private fun LicenseCard(license: OpenSourceLicense) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(V14Bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = license.name,
                    color = V14TextPrimary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = license.licenseType,
                    color = V14AccentBright,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = license.author,
                color = V14TextMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 1.dp, bottom = 6.dp)
            )

            Text(
                text = license.summary,
                color = V14TextSecondary,
                fontSize = 11.5.sp,
                lineHeight = 16.sp
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = V14Border)
    }
}

@Composable
private fun ArchitectureOverviewCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(V14Bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Hub, contentDescription = null, tint = V14AccentBright, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Velorix Dual-Engine Architecture", color = V14TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "The Velorix platform operates on an enterprise dual-backend architecture designed for zero data desynchronization between player client apps and administrative management consoles.",
                color = V14TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = V14Border)
    }
}

@Composable
private fun DualSyncSystemCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(V14Bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.SyncAlt, contentDescription = null, tint = V14AccentBright, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Live Bidirectional Synchronization", color = V14TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "1. Realtime Database (RTDB): Live slot capacity, player count updates, and instant room ID/Password broadcast channels.\n2. Cloud Firestore: Tournament rules, historical logs, payout adjudication, and admin permission enforcement.\n3. Automatic Provisioning: Every player is auto-activated upon Google, Phone, or Email authentication without manual approval barriers.",
                color = V14TextSecondary,
                fontSize = 11.5.sp,
                lineHeight = 17.sp
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = V14Border)
    }
}

@Composable
private fun SecurityComplianceCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(V14Bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Security, contentDescription = null, tint = VelorixEmerald, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Security, Cryptography & Rate Limiting", color = V14TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "All administrative commands and player transactions are protected by token-bucket rate limiting (60 queries/min, 15 mutations/min, 5 auth attempts/5min) with cross-device Firebase lockout synchronization.",
                color = V14TextSecondary,
                fontSize = 11.5.sp,
                lineHeight = 17.sp
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = V14Border)
    }
}

@Composable
private fun TeamCreditsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(V14Bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Group, contentDescription = null, tint = V14AccentBright, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Project Credits & Engineering", color = V14TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "Velorix Free Fire Esports Platform • Developed & Maintained by Velorix Esports Systems Team\nLead Administrator: anantisback47@gmail.com\nFirebase Project: velorix-tournaments (27931798964)",
                color = V14TextSecondary,
                fontSize = 11.5.sp,
                lineHeight = 17.sp
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = V14Border)
    }
}

@Composable
private fun PlatformSignatureFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "VELORIX ESPORTS TOURNAMENT ENGINE v3.4.2",
            color = V14TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = "Real-Time Esports Cloud Infrastructure",
            color = V14TextMuted.copy(alpha = 0.7f),
            fontSize = 9.5.sp
        )
    }
}
