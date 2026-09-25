package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun BusinessModeBadge(
    isEnabled: Boolean,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.testTag("business_mode_badge"),
        shape = RoundedCornerShape(12.dp),
        color = if (isEnabled) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            if (isEnabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant
        ),
        onClick = onClick ?: {}
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.BusinessCenter,
                contentDescription = "Business Mode",
                tint = if (isEnabled) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isEnabled) "Business Mode: Active" else "Business Mode: Off",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isEnabled) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isEnabled) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Active",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
