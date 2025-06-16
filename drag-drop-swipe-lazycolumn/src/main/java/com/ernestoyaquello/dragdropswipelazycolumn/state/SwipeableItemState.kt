package com.ernestoyaquello.dragdropswipelazycolumn.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.ernestoyaquello.dragdropswipelazycolumn.AllowedSwipeDirections
import com.ernestoyaquello.dragdropswipelazycolumn.AllowedSwipeDirections.All
import com.ernestoyaquello.dragdropswipelazycolumn.AllowedSwipeDirections.None
import com.ernestoyaquello.dragdropswipelazycolumn.AllowedSwipeDirections.OnlyLeftToRight
import com.ernestoyaquello.dragdropswipelazycolumn.AllowedSwipeDirections.OnlyRightToLeft
import com.ernestoyaquello.dragdropswipelazycolumn.OngoingSwipeDirection
import com.ernestoyaquello.dragdropswipelazycolumn.OngoingSwipeDirection.NotSwiping
import com.ernestoyaquello.dragdropswipelazycolumn.OngoingSwipeDirection.SwipingLeftToRight
import com.ernestoyaquello.dragdropswipelazycolumn.OngoingSwipeDirection.SwipingRightToLeft

@Stable
class SwipeableItemState internal constructor(
    private val initialAllowedSwipeDirections: AllowedSwipeDirections,
) {

    @Immutable
    internal data class State(
        val allowedSwipeDirections: AllowedSwipeDirections,
        val forceDisableSwipe: Boolean = false,
        val ongoingSwipeDirection: OngoingSwipeDirection = NotSwiping,
        val offsetTargetInPx: Float = 0f,
        val lastVelocity: Float = 0f,
    ) {
        val dismissalTriggered = offsetTargetInPx != 0f && ongoingSwipeDirection == NotSwiping
    }

    private val internalState = mutableStateOf(
        value = State(initialAllowedSwipeDirections),
    )

    /**
     * Specifies which swipe directions are allowed for this item.
     */
    val allowedSwipeDirections
        get() = internalState.value.allowedSwipeDirections

    /**
     * Indicates the direction in which the user is currently swiping the item.
     */
    val ongoingSwipeDirection
        get() = internalState.value.ongoingSwipeDirection

    /**
     * Indicates whether the user is currently swiping the item.
     */
    val isBeingSwiped
        get() = ongoingSwipeDirection != NotSwiping

    /**
     * Indicates whether the item is currently dismissed or being being animated into its dismissal,
     * which will only happen if the user swiped it (and released it) far enough and/or fast enough.
     */
    val isItemDismissedOrBeingDismissed
        get() = internalState.value.dismissalTriggered

    internal val offsetTargetInPx
        get() = internalState.value.offsetTargetInPx

    internal val lastVelocity
        get() = internalState.value.lastVelocity

    internal val isSwipeAllowed
        get() = !internalState.value.forceDisableSwipe && (
                allowedSwipeDirections == All
                        || (ongoingSwipeDirection == NotSwiping && allowedSwipeDirections != None)
                        || (ongoingSwipeDirection == SwipingLeftToRight && allowedSwipeDirections == OnlyLeftToRight)
                        || (ongoingSwipeDirection == SwipingRightToLeft && allowedSwipeDirections == OnlyRightToLeft)
                )

    internal fun update(
        update: State.() -> State,
    ) {
        internalState.value = internalState.value.update()
    }

    /**
     * Resets the state of the swipeable item, bringing it back to its initial position if needed.
     * It can be useful to bring the item back after a swipe dismissal that couldn't be handled.
     */
    fun reset(
        allowedSwipeDirections: AllowedSwipeDirections? = null,
    ) {
        internalState.value = State(allowedSwipeDirections ?: initialAllowedSwipeDirections)
    }
}

@Composable
fun rememberSwipeableItemState(
    initialAllowedSwipeDirections: AllowedSwipeDirections = All,
) = remember {
    SwipeableItemState(
        initialAllowedSwipeDirections = initialAllowedSwipeDirections,
    )
}