package com.whtis.memosly.core.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.whtis.memosly.core.model.Visibility

@Composable
fun VisibilityChip(
    visibility: Visibility,
    modifier: Modifier = Modifier,
) {
    val (label, color) = when (visibility) {
        Visibility.PUBLIC -> "Public" to MaterialTheme.colorScheme.primary
        Visibility.PROTECTED -> "Protected" to MaterialTheme.colorScheme.tertiary
        Visibility.PRIVATE -> "Private" to MaterialTheme.colorScheme.error
        Visibility.UNKNOWN -> "Unknown" to MaterialTheme.colorScheme.outline
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.1f),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
