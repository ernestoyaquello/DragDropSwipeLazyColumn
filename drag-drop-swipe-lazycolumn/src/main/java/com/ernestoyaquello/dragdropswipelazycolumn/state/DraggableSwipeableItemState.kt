package com.ernestoyaquello.dragdropswipelazycolumn.state

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.mutableStateOf
import com.ernestoyaquello.dragdropswipelazycolumn.AllowedSwipeDirections

@Immutable
class DraggableSwipeableItemState internal constructor(
    itemKey: Any,
    internal val swipeableItemState: SwipeableItemState,
) {

    @Immutable
    internal data class State(
        val itemKey: Any,
        val isBeingDragged: Boolean = false,
        val currentDragIndex: Int? = null,
        val offsetTargetInPx: Float = 0f,
        val pendingReorderCallbackInvocation: Boolean = false,
        val onDragFinishCallback: () -> Unit = {},
    )

    private val internalState = mutableStateOf(
        value = State(itemKey),
    )

    private val internalAnimatedOffsetInPx = Animatable(
        initialValue = internalState.value.offsetTargetInPx,
        typeConverter = Float.VectorConverter,
    )

    /**
     * The key of the item.
     */
    val itemKey
        get() = internalState.value.itemKey

    /**
     * Indicates whether the user is currently dragging the item.
     */
    val isBeingDragged
        get() = internalState.value.isBeingDragged

    /**
     * The index the item has while dragged (i.e., the index it would acquire if dropped).
     * `null` if the item is not currently being dragged.
     */
    val currentDragIndex
        get() = internalState.value.currentDragIndex

    /**
     * Specifies which swipe directions are allowed for this item.
     */
    val allowedSwipeDirections
        get() = swipeableItemState.allowedSwipeDirections

    /**
     * Indicates the direction in which the user is currently swiping the item.
     */
    val ongoingSwipeDirection
        get() = swipeableItemState.ongoingSwipeDirection

    /**
     * Indicates whether the user is currently swiping the item.
     */
    val isBeingSwiped
        get() = swipeableItemState.isBeingSwiped

    /**
     * Indicates whether the item is currently dismissed or being being animated into its dismissal,
     * which will only happen if the user swiped it (and released it) far enough and/or fast enough.
     */
    val isItemDismissedOrBeingDismissed
        get() = swipeableItemState.isItemDismissedOrBeingDismissed

    /**
     * The target offset in pixels to which the item will be animated. It will be non-zero if the
     * user is dragging the item, and zero if the item is moving back to its default position.
     */
    internal val offsetTargetInPx
        get() = internalState.value.offsetTargetInPx

    /**
     * Indicates whether the item being dragged has caused some items to be reordered at some point,
     * which would grant the need to invoke the reorder callback once the item is dropped.
     */
    internal val pendingReorderCallbackInvocation
        get() = internalState.value.pendingReorderCallbackInvocation

    /**
     * Animatable to push the item to its target position, which is defined in [offsetTargetInPx].
     */
    internal val animatedOffsetInPx
        @RememberInComposition
        get() = internalAnimatedOffsetInPx

    /**
     * The callback to be invoked once the drag operation finishes.
     */
    internal val onDragFinishCallback
        get() = internalState.value.onDragFinishCallback

    /**
     * Resets the state of the swipeable item, bringing it back to its initial position if needed.
     * It can be useful to bring the item back after a swipe dismissal that couldn't be handled.
     */
    fun resetSwipeableState(
        allowedSwipeDirections: AllowedSwipeDirections? = null,
    ) {
        swipeableItemState.reset(allowedSwipeDirections)
    }

    internal fun update(
        update: State.() -> State,
    ) {
        internalState.value = internalState.value.update()
    }
}
