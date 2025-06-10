package com.ernestoyaquello.dragdropswipelazycolumn.app.data.models

import androidx.compose.runtime.Immutable

@Immutable
data class ExampleTask(
    val id: Int = -1,
    val index: Int = -1,
    val title: String = "",
    val isLocked: Boolean = false,
    val isCompleted: Boolean = false,
)