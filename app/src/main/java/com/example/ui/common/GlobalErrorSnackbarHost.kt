package com.example.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VelorixAccent
import com.example.ui.theme.VelorixBg
import com.example.ui.theme.VelorixTextPrimary
import com.example.ui.theme.VelorixTextSecondary
import kotlinx.coroutines.delay

@Composable
fun GlobalErrorSnackbarHost(
    modifier: Modifier = Modifier
) {
    val errorState by GlobalErrorManager.errorState.collectAsState()

    // Auto dismiss after 6 seconds if visible
    LaunchedEffect(errorState?.timestamp) {
        if (errorState != null) {
            delay(6000L)
            GlobalErrorManager.dismissError()
        }
    }

    AnimatedVisibility(
        visible = errorState != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        val currentError = errorState ?: return@AnimatedVisibility

        val isError = currentError.isError
        val containerBg = if (isError) Color(0xFF2C151B) else Color(0xFF1B2B1E)
        val borderColor = if (isError) Color(0xFFF2B8B5) else Color(0xFFB1D18A)
        val iconColor = if (isError) Color(0xFFF2B8B5) else Color(0xFFB1D18A)
        val title = if (isError) "Notice" else "Success"

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("global_error_snackbar_surface"),
            shape = RoundedCornerShape(12.dp),
            color = containerBg,
            shadowElevation = 8.dp,
            tonalElevation = 6.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = if (isError) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = if (isError) "Error" else "Success",
                            tint = iconColor,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(22.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = title,
                                color = borderColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = currentError.message,
                                color = VelorixTextPrimary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )

                            val details = currentError.details
                            if (!details.isNullOrBlank() && details != currentError.message && !currentError.message.contains(details)) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = details,
                                    color = VelorixTextSecondary.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            val actionLabel = currentError.actionLabel
                            val onAction = currentError.onAction
                            if (actionLabel != null && onAction != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = {
                                        GlobalErrorManager.dismissError()
                                        onAction.invoke()
                                    },
                                    modifier = Modifier.testTag("snackbar_action_button"),
                                    colors = ButtonDefaults.textButtonColors(contentColor = borderColor)
                                ) {
                                    Text(
                                        text = actionLabel,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = { GlobalErrorManager.dismissError() },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("dismiss_snackbar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = VelorixTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
