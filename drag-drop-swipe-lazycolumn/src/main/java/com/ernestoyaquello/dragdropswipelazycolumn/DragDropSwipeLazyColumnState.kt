package com.ernestoyaquello.dragdropswipelazycolumn

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Stable
class DragDropSwipeLazyColumnState internal constructor(
    lazyListState: LazyListState,
) {

    @Stable
    internal data class State(
        val lazyListState: LazyListState,
        val draggedItemKey: Any? = null,
        val swipedItemKeys: ImmutableSet<Any> = persistentSetOf(),
    )

    private val internalState = mutableStateOf(
        State(lazyListState = lazyListState),
    )

    /**
     * The [LazyListState] underlying this enhanced lazy list implementation.
     */
    val lazyListState get() = internalState.value.lazyListState

    /**
     * The key of the item that is currently being dragged by the user, if any.
     */
    val draggedItemKey get() = internalState.value.draggedItemKey

    /**
     * The keys of the items that are currently being swiped by the user.
     */
    val swipedItemKeys get() = internalState.value.swipedItemKeys

    internal fun update(
        update: State.() -> State,
    ) {
        internalState.value = internalState.value.update()
    }
}

@Composable
fun rememberDragDropSwipeLazyColumnState(
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
): DragDropSwipeLazyColumnState {
    val lazyListState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialFirstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = initialFirstVisibleItemScrollOffset,
    )
    return remember(lazyListState) {
        DragDropSwipeLazyColumnState(lazyListState)
    }
}