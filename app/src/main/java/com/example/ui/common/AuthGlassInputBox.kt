package com.example.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.VelorixFontFamily

/**
 * Liquid Glass Input Box matching the reference design and floating navigation bar:
 * - 100% translucent, see-through frosted glass body without opaque black tint.
 * - Genuine optical glass specular reflections and bevel catch-lights via drawLiquidGlassOpticReflections.
 * - Glowing cyber neon icon on the left.
 */
@Composable
fun AuthGlassInputBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector? = null,
    leadingIconPainter: Int? = null,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cornerRadius = 20.dp
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .border(
                BorderStroke(
                    width = 0.75.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            if (isError) Color(0xFFFF5252).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.32f),
                            if (isError) Color(0xFFFF5252).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f),
                            if (isError) Color(0xFFFF5252).copy(alpha = 0.60f) else Color(0xFF818CF8).copy(alpha = 0.14f),
                            if (isError) Color(0xFFFF5252).copy(alpha = 0.40f) else Color.White.copy(alpha = 0.16f)
                        )
                    )
                ),
                shape = shape
            )
    ) {
        // Translucent Clear Glass Substrate with Gaussian blur (exact same as bottom nav bar)
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(radius = 5.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.14f),
                            Color.White.copy(alpha = 0.06f)
                        )
                    )
                )
        )

        // Glass Layer: Optical Specular Reflections & Bevels (exact same as bottom nav bar)
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    drawLiquidGlassOpticReflections(
                        cornerRadius = cornerRadius.toPx(),
                        intensity = 0.85f
                    )
                }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Glowing Cyber Icon on the left
                if (leadingIcon != null || leadingIconPainter != null) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .drawBehind {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0x356366F1),
                                            Color.Transparent
                                        ),
                                        center = Offset(size.width / 2f, size.height / 2f),
                                        radius = size.width * 0.85f
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (leadingIconPainter != null) {
                            Icon(
                                painter = painterResource(id = leadingIconPainter),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        } else if (leadingIcon != null) {
                            Icon(
                                imageVector = leadingIcon,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.95f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                // Input field with transparent background
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = Color.White.copy(alpha = 0.42f),
                            fontSize = 15.sp,
                            fontFamily = VelorixFontFamily,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 15.sp,
                            fontFamily = VelorixFontFamily,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(Color.White),
                        singleLine = true,
                        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions
                    )
                }

                // Trailing eye icon for password
                if (isPassword && onTogglePassword != null) {
                    IconButton(
                        onClick = onTogglePassword,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Password",
                            tint = Color.White.copy(alpha = 0.65f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Luminous Frosted Cyber-Glass Primary Action Button (non-whitish, sleek crystal glass)
 */
@Composable
fun PrimaryGlassAuthButton(
    text: String,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val cornerRadius = 22.dp
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .border(
                BorderStroke(
                    width = 0.85.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (enabled) 0.45f else 0.20f),
                            Color.White.copy(alpha = if (enabled) 0.10f else 0.04f),
                            Color(0xFF818CF8).copy(alpha = if (enabled) 0.28f else 0.10f),
                            Color.White.copy(alpha = if (enabled) 0.20f else 0.08f)
                        )
                    )
                ),
                shape = shape
            )
            .clickable(
                enabled = enabled && !isLoading,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        // Translucent Clear Glass Substrate with Gaussian blur (same as bottom nav bar)
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(radius = 5.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF818CF8).copy(alpha = if (enabled) 0.26f else 0.10f),
                            Color(0xFF6366F1).copy(alpha = if (enabled) 0.18f else 0.06f),
                            Color.White.copy(alpha = if (enabled) 0.06f else 0.02f)
                        )
                    )
                )
        )

        // Glass Layer: Optical Specular Reflections & Bevels (same as bottom nav bar)
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    drawLiquidGlassOpticReflections(
                        cornerRadius = cornerRadius.toPx(),
                        intensity = 0.90f
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = text,
                    color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
                    fontSize = 15.sp,
                    fontFamily = VelorixFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}

/**
 * Liquid Glass Google Sign-In Button matching bottom nav bar glass
 */
@Composable
fun GoogleGlassAuthButton(
    onClick: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cornerRadius = 22.dp
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .border(
                BorderStroke(
                    width = 0.75.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.06f),
                            Color(0xFF818CF8).copy(alpha = 0.14f),
                            Color.White.copy(alpha = 0.16f)
                        )
                    )
                ),
                shape = shape
            )
            .clickable(
                enabled = !isLoading,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        // Translucent Clear Glass Substrate with Gaussian blur (same as bottom nav bar)
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(radius = 5.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.12f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    )
                )
        )

        // Glass Layer: Optical Specular Reflections & Bevels (same as bottom nav bar)
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    drawLiquidGlassOpticReflections(
                        cornerRadius = cornerRadius.toPx(),
                        intensity = 0.80f
                    )
                }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f))
                            .border(0.75.dp, Color.White.copy(alpha = 0.20f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.google_favicon),
                            contentDescription = "Google Logo",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Sign in with Google",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontFamily = VelorixFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    }
}
