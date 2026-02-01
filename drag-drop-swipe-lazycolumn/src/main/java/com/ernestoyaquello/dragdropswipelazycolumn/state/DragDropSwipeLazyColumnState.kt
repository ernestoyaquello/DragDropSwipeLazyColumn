package com.ernestoyaquello.dragdropswipelazycolumn.state

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.ernestoyaquello.dragdropswipelazycolumn.DragDropSwipeLazyColumn
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
        State(
            lazyListState = lazyListState,
        ),
    )

    /**
     * The [LazyListState] used by the [LazyColumn] powering the [DragDropSwipeLazyColumn].
     */
    val lazyListState
        get() = internalState.value.lazyListState

    /**
     * The key of the item that is currently being dragged by the user, if any.
     * Only one item can be dragged at a time.
     */
    val draggedItemKey
        get() = internalState.value.draggedItemKey

    /**
     * The keys of the items that are currently being swiped by the user.
     * This set can contain multiple keys, as multiple items can be swiped at the same time.
     */
    val swipedItemKeys
        get() = internalState.value.swipedItemKeys

    internal fun update(
        update: State.() -> State,
    ) {
        internalState.value = internalState.value.update()
    }
}

/**
 * Creates a [DragDropSwipeLazyColumnState] that is remembered across compositions.
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * @param initialFirstVisibleItemIndex The initial first visible item index.
 * @param initialFirstVisibleItemScrollOffset The initial first visible item scroll offset.
 */
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