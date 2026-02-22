package com.whtis.memosly.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Centralized shape tokens for consistent design language across the app.
 */
object MemosShapes {
    /** Pill-shaped search bars */
    val SearchBar = RoundedCornerShape(20.dp)

    /** Memo cards, settings sections, containers */
    val Card = RoundedCornerShape(12.dp)

    /** Text fields, comment inputs */
    val Input = RoundedCornerShape(12.dp)

    /** Tags, small badges, thumbnails */
    val Chip = RoundedCornerShape(8.dp)
}
