package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.audio.VelorixSoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class SfxSampleInfo(
    val id: String,
    val title: String,
    val mjInspiration: String,
    val duration: String,
    val appTriggerSituation: String,
    val icon: ImageVector,
    val accentColor: Color,
    val playAction: (VelorixSoundManager) -> Unit
)

@Composable
fun VelorixAudioLabDialog(
    soundManager: VelorixSoundManager,
    onDismissRequest: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isMuted by remember { mutableStateOf(soundManager.isMuted) }
    var volume by remember { mutableFloatStateOf(soundManager.masterVolume) }
    var currentlyPlayingId by remember { mutableStateOf<String?>(null) }
    var isPlayingGroove by remember { mutableStateOf(false) }

    val samples = remember {
        listOf(
            SfxSampleInfo(
                id = "beat_tap",
                title = "Acoustic Beat Tap",
                mjInspiration = "Billie Jean punchy bass kick + tight rimshot",
                duration = "0.3s",
                appTriggerSituation = "Sending prompts, tapping category chips, button clicks",
                icon = Icons.Default.MusicNote,
                accentColor = Color(0xFFD49A3D),
                playAction = { sm -> sm.playBeatTap() }
            ),
            SfxSampleInfo(
                id = "snap_pop",
                title = "Funk Snap & Pop",
                mjInspiration = "Smooth Criminal / Bad wooden finger snap & funk pop",
                duration = "0.2s",
                appTriggerSituation = "Copying text to clipboard, quick toggles, inspector tabs",
                icon = Icons.Default.TouchApp,
                accentColor = Color(0xFF438A8A),
                playAction = { sm -> sm.playSnapPop() }
            ),
            SfxSampleInfo(
                id = "brass_hit",
                title = "Quincy Brass Stab",
                mjInspiration = "Thriller / Off the Wall analog synth brass stab (Em9)",
                duration = "0.5s",
                appTriggerSituation = "Deploying tournaments to Firebase, creating rooms, major actions",
                icon = Icons.Default.Campaign,
                accentColor = Color(0xFF7C72A0),
                playAction = { sm -> sm.playBrassHit() }
            ),
            SfxSampleInfo(
                id = "orchestra_hit",
                title = "Dramatic Orchestra Hit",
                mjInspiration = "Bad / Dangerous crisp staccato orchestral impact",
                duration = "0.4s",
                appTriggerSituation = "Anti-cheat player bans, security alerts, critical disputes",
                icon = Icons.Default.Gavel,
                accentColor = Color(0xFFB85D6B),
                playAction = { sm -> sm.playOrchestraHit() }
            ),
            SfxSampleInfo(
                id = "subtle_chime",
                title = "Subtle Crystal Chime",
                mjInspiration = "Human Nature delicate resonant chord shimmer",
                duration = "0.6s",
                appTriggerSituation = "Action verified, ticket resolved, database sync confirmed",
                icon = Icons.Default.CheckCircle,
                accentColor = Color(0xFF3B8A6E),
                playAction = { sm -> sm.playSubtleChime() }
            )
        )
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Color(0xFF262C3D), RoundedCornerShape(20.dp)),
            color = Color(0xFF10121A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFD49A3D).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Headphones,
                                contentDescription = null,
                                tint = Color(0xFFD49A3D),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "SFX Audio Lab",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Subtle Michael Jackson Inspired UI SFX",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E2230))
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Master Controls Bar
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF171B26),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262C3D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Master SFX Output",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isMuted) "Muted" else "Active",
                                    color = if (isMuted) Color(0xFFB85D6B) else Color(0xFF3B8A6E),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = !isMuted,
                                    onCheckedChange = { active ->
                                        isMuted = !active
                                        soundManager.isMuted = isMuted
                                        if (active) soundManager.playSnapPop()
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFD49A3D),
                                        checkedTrackColor = Color(0xFFD49A3D).copy(alpha = 0.3f),
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = Color(0xFF1E2230)
                                    ),
                                    modifier = Modifier.scale(0.85f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Volume Slider
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                if (isMuted || volume == 0f) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Slider(
                                value = volume,
                                onValueChange = { newVol ->
                                    volume = newVol
                                    soundManager.masterVolume = newVol
                                },
                                onValueChangeFinished = {
                                    soundManager.playBeatTap()
                                },
                                valueRange = 0f..1f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFD49A3D),
                                    activeTrackColor = Color(0xFFD49A3D),
                                    inactiveTrackColor = Color(0xFF262C3D)
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "${(volume * 100).toInt()}%",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(36.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Sequence Test Button
                Button(
                    onClick = {
                        if (!isPlayingGroove && !isMuted) {
                            isPlayingGroove = true
                            coroutineScope.launch {
                                // Play sequence groove
                                currentlyPlayingId = "beat_tap"
                                soundManager.playBeatTap()
                                delay(220)
                                currentlyPlayingId = "snap_pop"
                                soundManager.playSnapPop()
                                delay(220)
                                currentlyPlayingId = "beat_tap"
                                soundManager.playBeatTap()
                                delay(220)
                                currentlyPlayingId = "brass_hit"
                                soundManager.playBrassHit()
                                delay(320)
                                currentlyPlayingId = "subtle_chime"
                                soundManager.playSubtleChime()
                                delay(400)
                                currentlyPlayingId = null
                                isPlayingGroove = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlayingGroove) Color(0xFF262C3D) else Color(0xFF1E2230)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333B4F))
                ) {
                    Icon(
                        if (isPlayingGroove) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFFD49A3D),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPlayingGroove) "Playing Groove Sequence..." else "Play All In Sequence (MJ Beat Groove)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sound Effect Items List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(samples, key = { it.id }) { sample ->
                        val isPlaying = currentlyPlayingId == sample.id

                        val pulseScale by animateFloatAsState(
                            targetValue = if (isPlaying) 1.08f else 1.0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "pulse"
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF141722),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isPlaying) 1.5.dp else 1.dp,
                                color = if (isPlaying) sample.accentColor else Color(0xFF262C3D)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(pulseScale)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(sample.accentColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            sample.icon,
                                            contentDescription = null,
                                            tint = sample.accentColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = sample.title,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFF1E2230))
                                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = sample.duration,
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Sample: ${sample.mjInspiration}",
                                            color = sample.accentColor.copy(alpha = 0.9f),
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Trigger: ${sample.appTriggerSituation}",
                                            color = Color(0xFF64748B),
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = {
                                        currentlyPlayingId = sample.id
                                        sample.playAction(soundManager)
                                        coroutineScope.launch {
                                            delay(350)
                                            if (currentlyPlayingId == sample.id) {
                                                currentlyPlayingId = null
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = sample.accentColor.copy(alpha = 0.2f)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, sample.accentColor.copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = sample.accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "TEST",
                                        color = sample.accentColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
