package com.ernestoyaquello.dragdropswipelazycolumn.config

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

    /**
     * The background color of the item container while it is being dragged.
     */
    val containerBackgroundColorWhileDragged: Color?,
) {
    /**
     * The background color of the item container.
     */
    val containerBackgroundColor =
        swipeableItemColors.containerBackgroundColor

    /**
     * The color of the click indication when the item is clicked.
     */
    val clickIndicationColor =
        swipeableItemColors.clickIndicationColor

    /**
     * The background color of the container shown behind the item while the item is being swiped
     * from left to right.
     */
    val behindLeftToRightSwipeContainerBackgroundColor =
        swipeableItemColors.behindLeftToRightSwipeContainerBackgroundColor

    /**
     * The color of the icon shown behind the item while the item is swiped from left to right.
     */
    val behindLeftToRightSwipeIconColor =
        swipeableItemColors.behindLeftToRightSwipeIconColor

    /**
     * The background color of the container shown behind the item while the item is being swiped
     * from right to left.
     */
    val behindRightToLeftSwipeContainerBackgroundColor =
        swipeableItemColors.behindRightToLeftSwipeContainerBackgroundColor

    /**
     * The color of the icon shown behind the item while the item is swiped from right to left.
     */
    val behindRightToLeftSwipeIconColor =
        swipeableItemColors.behindRightToLeftSwipeIconColor

    companion object {

        /**
         * Creates a [DraggableSwipeableItemColors] instance with the specified colors.
         * 
         * @param containerBackgroundColor The background color of the item container.
         * @param containerBackgroundColorWhileDragged The background color of the item container
         *   while it is being dragged.
         * @param clickIndicationColor The color of the click indication when the item is clicked.
         * @param behindLeftToRightSwipeContainerBackgroundColor The background color of the
         *   container shown behind the item while the item is being swiped from left to right.
         * @param behindLeftToRightSwipeIconColor The color of the icon shown behind the item while
         *   the item is being swiped from left to right.
         * @param behindRightToLeftSwipeContainerBackgroundColor The background color of the
         *   container shown behind the item while the item is being swiped from right to left.
         * @param behindRightToLeftSwipeIconColor The color of the icon shown behind the item while
         *   the item is being swiped from right to left.
         */
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
        ): DraggableSwipeableItemColors {
            val swipeableItemColors = SwipeableItemColors.createRemembered(
                containerBackgroundColor = containerBackgroundColor,
                clickIndicationColor = clickIndicationColor,
                behindLeftToRightSwipeContainerBackgroundColor = behindLeftToRightSwipeContainerBackgroundColor,
                behindLeftToRightSwipeIconColor = behindLeftToRightSwipeIconColor,
                behindRightToLeftSwipeContainerBackgroundColor = behindRightToLeftSwipeContainerBackgroundColor,
                behindRightToLeftSwipeIconColor = behindRightToLeftSwipeIconColor,
            )
            return remember(containerBackgroundColorWhileDragged, swipeableItemColors) {
                DraggableSwipeableItemColors(
                    containerBackgroundColorWhileDragged = containerBackgroundColorWhileDragged,
                    swipeableItemColors = swipeableItemColors,
                )
            }
        }

        /**
         * Creates a [DraggableSwipeableItemColors] instance with the specified colors.
         *
         * @param containerBackgroundColor The background color of the item container.
         * @param containerBackgroundColorWhileDragged The background color of the item container
         *   while it is being dragged.
         * @param clickIndicationColor The color of the click indication when the item is clicked.
         * @param behindStartToEndSwipeContainerBackgroundColor The background color of the
         *   container shown behind the item while the item is being swiped from start to end.
         * @param behindStartToEndSwipeIconColor The color of the icon shown behind the item while
         *   the item is being swiped from start to end.
         * @param behindEndToStartSwipeContainerBackgroundColor The background color of the
         *   container shown behind the item while the item is being swiped from end to start.
         * @param behindEndToStartSwipeIconColor The color of the icon shown behind the item while
         *   the item is being swiped from end to start.
         */
        @Composable
        fun createRememberedWithLayoutDirection(
            containerBackgroundColor: Color? = MaterialTheme.colorScheme.secondaryContainer,
            containerBackgroundColorWhileDragged: Color? = containerBackgroundColor,
            clickIndicationColor: Color? = containerBackgroundColor?.let {
                contentColorFor(it)
            },
            behindStartToEndSwipeContainerBackgroundColor: Color? = MaterialTheme.colorScheme.errorContainer,
            behindStartToEndSwipeIconColor: Color? = behindStartToEndSwipeContainerBackgroundColor?.let {
                contentColorFor(it)
            },
            behindEndToStartSwipeContainerBackgroundColor: Color? = MaterialTheme.colorScheme.errorContainer,
            behindEndToStartSwipeIconColor: Color? = behindEndToStartSwipeContainerBackgroundColor?.let {
                contentColorFor(it)
            },
        ): DraggableSwipeableItemColors {
            val swipeableItemColors = SwipeableItemColors.createRememberedWithLayoutDirection(
                containerBackgroundColor = containerBackgroundColor,
                clickIndicationColor = clickIndicationColor,
                behindStartToEndSwipeContainerBackgroundColor = behindStartToEndSwipeContainerBackgroundColor,
                behindStartToEndSwipeIconColor = behindStartToEndSwipeIconColor,
                behindEndToStartSwipeContainerBackgroundColor = behindEndToStartSwipeContainerBackgroundColor,
                behindEndToStartSwipeIconColor = behindEndToStartSwipeIconColor,
            )
            return remember(containerBackgroundColorWhileDragged, swipeableItemColors) {
                DraggableSwipeableItemColors(
                    containerBackgroundColorWhileDragged = containerBackgroundColorWhileDragged,
                    swipeableItemColors = swipeableItemColors,
                )
            }
        }

        /**
         * Creates a [DraggableSwipeableItemColors] instance with the specified colors.
         *
         * @param containerBackgroundColor The background color of the item container.
         * @param containerBackgroundColorWhileDragged The background color of the item container
         *   while it is being dragged.
         * @param clickIndicationColor The color of the click indication when the item is clicked.
         * @param behindSwipeContainerBackgroundColor The background color of the
         *   container shown behind the item while the item is being swiped in any direction.
         * @param behindSwipeIconColor The color of the icon shown behind the item while
         *   the item is being swiped in any direction.
         */
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