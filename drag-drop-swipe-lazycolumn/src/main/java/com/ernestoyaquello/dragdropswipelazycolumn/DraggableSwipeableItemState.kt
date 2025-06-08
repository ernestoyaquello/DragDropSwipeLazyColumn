package com.ernestoyaquello.dragdropswipelazycolumn

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf

class DraggableSwipeableItemState internal constructor(
    itemKey: Any,
    val swipeableItemState: SwipeableItemState,
) {

    @Immutable
    internal data class State(
        val itemKey: Any,
        val isBeingDragged: Boolean = false,
        val offsetTargetInPx: Float = 0f,
        val pendingReorderOffsetCorrection: Float = 0f,
        val pendingReorderCallbackInvocation: Boolean = false,
    )

    private val internalState = mutableStateOf(State(itemKey))

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
     * The target offset in pixels to which the item will be animated. It will be non-zero if the
     * user is dragging the item, and zero if the item is moving back to its default position.
     */
    internal val offsetTargetInPx
        get() = internalState.value.offsetTargetInPx

    /**
     * The offset in pixels that will be applied to the item when it is repositioned within the
     * list due to reordering. This will ensure that, despite the new index causing the default
     * position of the item to change, it will be displayed in the same screen position as before.
     */
    internal val pendingReorderOffsetCorrection
        get() = internalState.value.pendingReorderOffsetCorrection

    /**
     * Indicates whether the item being dragged has caused some items to be reordered at some point,
     * which would grant the need to invoke the reorder callback once the item is dropped.
     */
    internal val pendingReorderCallbackInvocation
        get() = internalState.value.pendingReorderCallbackInvocation

    /**
     * Animatable to push the item to its target position, which is defined in [offsetTargetInPx].
     */
    internal val animatedOffsetInPx = Animatable(
        initialValue = internalState.value.offsetTargetInPx,
        typeConverter = Float.Companion.VectorConverter,
    )

    internal fun update(
        update: State.() -> State,
    ) {
        internalState.value = internalState.value.update()
    }
}