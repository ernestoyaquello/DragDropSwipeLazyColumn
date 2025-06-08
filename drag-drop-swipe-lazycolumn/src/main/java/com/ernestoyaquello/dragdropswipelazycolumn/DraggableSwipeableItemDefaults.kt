package com.ernestoyaquello.dragdropswipelazycolumn

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
object DraggableSwipeableItemDefaults {

    @Composable
    fun colors() = DraggableSwipeableItemColors.createRemembered()

    /**
     * The default minimum horizontal delta to vertical delta ratio required for a horizontal
     * swipe gesture to be considered a valid swipe start. The higher this value, the more
     * "horizontal" the swipe gesture must be for it to be actually handled as a swipe.
     *
     * This is relatively high by default to ensure that only intentional, very horizontal swipes
     * are considered valid swipes, thus avoiding accidental swipes caused by attempts at scrolling
     * vertically through the list.
     */
    const val minSwipeHorizontality = 2.5f

    /**
     * The default shadow elevation applied to the item while it is being dragged.
     */
    val shadowElevationWhenDragged = 4.dp
}

@Immutable
@ConsistentCopyVisibility
data class DraggableSwipeableItemColors private constructor(
    internal val swipeableItemColors: SwipeableItemColors,
    val containerBackgroundColorWhileDragged: Color?,
) {
    val containerBackgroundColor =
        swipeableItemColors.containerBackgroundColor

    val clickIndicationColor =
        swipeableItemColors.clickIndicationColor

    val behindLeftToRightSwipeContainerBackgroundColor =
        swipeableItemColors.behindLeftToRightSwipeContainerBackgroundColor

    val behindLeftToRightSwipeIconColor =
        swipeableItemColors.behindLeftToRightSwipeIconColor

    val behindRightToLeftSwipeContainerBackgroundColor =
        swipeableItemColors.behindRightToLeftSwipeContainerBackgroundColor

    val behindRightToLeftSwipeIconColor =
        swipeableItemColors.behindRightToLeftSwipeIconColor

    companion object {

        @Composable
        fun createRemembered(
            containerBackgroundColor: Color? = MaterialTheme.colorScheme.secondaryContainer,
            containerBackgroundColorWhileDragged: Color? = containerBackgroundColor,
            clickIndicationColor: Color? = containerBackgroundColor?.let {
                contentColorFor(it)
            },
            behindLeftToRightSwipeContainerBackgroundColor: Color? = MaterialTheme.colorScheme.errorContainer,
            behindLeftToRightSwipeIconColor: Color? = behindLeftToRightSwipeContainerBackgroundColor?.let {
                contentColorFor(it)
            },
            behindRightToLeftSwipeContainerBackgroundColor: Color? = MaterialTheme.colorScheme.errorContainer,
            behindRightToLeftSwipeIconColor: Color? = behindRightToLeftSwipeContainerBackgroundColor?.let {
                contentColorFor(it)
            },
        ) = remember(
            containerBackgroundColor,
            containerBackgroundColorWhileDragged,
            clickIndicationColor,
            behindLeftToRightSwipeContainerBackgroundColor,
            behindLeftToRightSwipeIconColor,
            behindRightToLeftSwipeContainerBackgroundColor,
            behindRightToLeftSwipeIconColor,
        ) {
            DraggableSwipeableItemColors(
                containerBackgroundColorWhileDragged = containerBackgroundColorWhileDragged,
                swipeableItemColors = SwipeableItemColors(
                    containerBackgroundColor = containerBackgroundColor,
                    clickIndicationColor = clickIndicationColor,
                    behindLeftToRightSwipeContainerBackgroundColor = behindLeftToRightSwipeContainerBackgroundColor,
                    behindLeftToRightSwipeIconColor = behindLeftToRightSwipeIconColor,
                    behindRightToLeftSwipeContainerBackgroundColor = behindRightToLeftSwipeContainerBackgroundColor,
                    behindRightToLeftSwipeIconColor = behindRightToLeftSwipeIconColor,
                ),
            )
        }

        @Composable
        fun createRemembered(
            containerBackgroundColor: Color? = MaterialTheme.colorScheme.secondaryContainer,
            containerBackgroundColorWhileDragged: Color? = containerBackgroundColor,
            clickIndicationColor: Color? = containerBackgroundColor?.let {
                contentColorFor(it)
            },
            behindSwipeContainerBackgroundColor: Color? = MaterialTheme.colorScheme.errorContainer,
            behindSwipeIconColor: Color? = behindSwipeContainerBackgroundColor?.let {
                contentColorFor(it)
            },
        ) = createRemembered(
            containerBackgroundColor = containerBackgroundColor,
            containerBackgroundColorWhileDragged = containerBackgroundColorWhileDragged,
            clickIndicationColor = clickIndicationColor,
            behindLeftToRightSwipeContainerBackgroundColor = behindSwipeContainerBackgroundColor,
            behindLeftToRightSwipeIconColor = behindSwipeIconColor,
            behindRightToLeftSwipeContainerBackgroundColor = behindSwipeContainerBackgroundColor,
            behindRightToLeftSwipeIconColor = behindSwipeIconColor,
        )
    }
}