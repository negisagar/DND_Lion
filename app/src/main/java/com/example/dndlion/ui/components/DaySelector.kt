package com.example.dndlion.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun DaySelector(
    selectedDays: Set<String>,
    onDaySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val days = listOf(
        "Mon" to "M",
        "Tue" to "T",
        "Wed" to "W",
        "Thu" to "T",
        "Fri" to "F",
        "Sat" to "S",
        "Sun" to "S"
    )

    Column(modifier = modifier) {
        Text(
            text = "Repeat on",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEach { (dayFull, dayShort) ->
                val selected = selectedDays.contains(dayFull)
                
                val backgroundColor by animateColorAsState(
                    targetValue = when {
                        selected -> MaterialTheme.colorScheme.primary
                        else -> Color.Transparent
                    },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "bgColor"
                )
                
                val textColor by animateColorAsState(
                    targetValue = when {
                        selected -> MaterialTheme.colorScheme.onPrimary
                        enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "textColor"
                )
                
                val borderColor by animateColorAsState(
                    targetValue = when {
                        selected -> MaterialTheme.colorScheme.primary
                        enabled -> MaterialTheme.colorScheme.outlineVariant
                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)
                    },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "borderColor"
                )

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(backgroundColor, CircleShape)
                        .border(1.dp, borderColor, CircleShape)
                        .clickable(enabled = enabled) { onDaySelected(dayFull) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayShort,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                }
            }
        }
    }
}
