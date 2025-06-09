package com.ernestoyaquello.dragdropswipelazycolumn.app.data.models

import androidx.compose.runtime.Immutable

@Immutable
data class ExampleItem(
    val id: Int = -1,
    val index: Int = -1,
    val title: String = "",
    val locked: Boolean = false,
)