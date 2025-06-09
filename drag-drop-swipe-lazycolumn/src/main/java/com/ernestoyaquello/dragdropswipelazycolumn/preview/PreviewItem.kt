package com.ernestoyaquello.dragdropswipelazycolumn.preview

import androidx.compose.runtime.Immutable

@Immutable
internal data class PreviewItem(
    val id: Int = -1,
    val index: Int = -1,
    val title: String = "",
    val locked: Boolean = false,
)