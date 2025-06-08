package com.ernestoyaquello.dragdropswipelazycolumn

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
object SwipeableItemDefaults {

    @Composable
    fun colors() = SwipeableItemColors.createRemembered()

    @Composable
    fun shapes() = SwipeableItemShapes.createRemembered()

    @Composable
    fun icons() = SwipeableItemIcons.createRemembered()

    /**
     * The default minimum height of the swipeable item.
     */
    val minHeight: Dp = Dp.Companion.Unspecified

    /**
     * The default start padding applied to the sides of the swipeable item.
     */
    val contentStartPadding: Dp = 0.dp

    /**
     * The default start padding applied to the sides of the swipeable item.
     */
    val contentEndPadding: Dp = 0.dp

    /**
     * The default minimum horizontal delta to vertical delta ratio required for a horizontal
     * swipe gesture to be considered a valid swipe start. The higher this value, the more
     * "horizontal" the swipe gesture must be for it to be actually handled as a swipe.
     */
    val minSwipeHorizontality: Float? = null
}

@Immutable
@ConsistentCopyVisibility
data class SwipeableItemColors internal constructor(
    val containerBackgroundColor: Color?,
    val clickIndicationColor: Color?,
    val behindLeftToRightSwipeContainerBackgroundColor: Color?,
    val behindLeftToRightSwipeIconColor: Color?,
    val behindRightToLeftSwipeContainerBackgroundColor: Color?,
    val behindRightToLeftSwipeIconColor: Color?,
) {
    companion object {

        @Composable
        fun createRemembered(
            containerBackgroundColor: Color? = MaterialTheme.colorScheme.secondaryContainer,
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
            clickIndicationColor,
            behindLeftToRightSwipeContainerBackgroundColor,
            behindLeftToRightSwipeIconColor,
            behindRightToLeftSwipeContainerBackgroundColor,
            behindRightToLeftSwipeIconColor,
        ) {
            SwipeableItemColors(
                containerBackgroundColor = containerBackgroundColor,
                clickIndicationColor = clickIndicationColor,
                behindLeftToRightSwipeContainerBackgroundColor = behindLeftToRightSwipeContainerBackgroundColor,
                behindLeftToRightSwipeIconColor = behindLeftToRightSwipeIconColor,
                behindRightToLeftSwipeContainerBackgroundColor = behindRightToLeftSwipeContainerBackgroundColor,
                behindRightToLeftSwipeIconColor = behindRightToLeftSwipeIconColor,
            )
        }

        @Composable
        fun createRemembered(
            containerBackgroundColor: Color? = MaterialTheme.colorScheme.secondaryContainer,
            clickIndicationColor: Color? = containerBackgroundColor?.let {
                contentColorFor(it)
            },
            behindSwipeContainerBackgroundColor: Color? = MaterialTheme.colorScheme.errorContainer,
            behindSwipeIconColor: Color? = behindSwipeContainerBackgroundColor?.let {
                contentColorFor(it)
            },
        ) = createRemembered(
            containerBackgroundColor = containerBackgroundColor,
            clickIndicationColor = clickIndicationColor,
            behindLeftToRightSwipeContainerBackgroundColor = behindSwipeContainerBackgroundColor,
            behindLeftToRightSwipeIconColor = behindSwipeIconColor,
            behindRightToLeftSwipeContainerBackgroundColor = behindSwipeContainerBackgroundColor,
            behindRightToLeftSwipeIconColor = behindSwipeIconColor,
        )
    }
}

@Immutable
@ConsistentCopyVisibility
data class SwipeableItemShapes private constructor(
    val containerBackgroundShape: Shape,
    val behindLeftToRightSwipeContainerShape: Shape,
    val behindRightToLeftSwipeContainerShape: Shape,
) {
    companion object {

        @Composable
        fun createRemembered(
            containerBackgroundShape: Shape = RectangleShape,
            behindLeftToRightSwipeContainerShape: Shape = containerBackgroundShape,
            behindRightToLeftSwipeContainerShape: Shape = containerBackgroundShape,
        ) = remember(
            containerBackgroundShape,
            behindLeftToRightSwipeContainerShape,
            behindRightToLeftSwipeContainerShape,
        ) {
            SwipeableItemShapes(
                containerBackgroundShape = containerBackgroundShape,
                behindLeftToRightSwipeContainerShape = behindLeftToRightSwipeContainerShape,
                behindRightToLeftSwipeContainerShape = behindRightToLeftSwipeContainerShape,
            )
        }

        @Composable
        fun createRemembered(
            containerBackgroundShape: Shape = RectangleShape,
            behindSwipeContainerShape: Shape = containerBackgroundShape,
        ) = createRemembered(
            containerBackgroundShape = containerBackgroundShape,
            behindLeftToRightSwipeContainerShape = behindSwipeContainerShape,
            behindRightToLeftSwipeContainerShape = behindSwipeContainerShape,
        )

        @Composable
        fun createRemembered(
            containersBackgroundShape: Shape = RectangleShape,
        ) = createRemembered(
            containerBackgroundShape = containersBackgroundShape,
            behindLeftToRightSwipeContainerShape = containersBackgroundShape,
            behindRightToLeftSwipeContainerShape = containersBackgroundShape,
        )
    }
}

@Immutable
@ConsistentCopyVisibility
data class SwipeableItemIcons private constructor(
    val behindLeftToRightSwipeIconSwipeStarting: ImageVector?,
    val behindRightToLeftSwipeIconSwipeStarting: ImageVector?,
    val behindLeftToRightSwipeIconSwipeOngoing: ImageVector?,
    val behindRightToLeftSwipeIconSwipeOngoing: ImageVector?,
    val behindLeftToRightSwipeIconSwipeFinishing: ImageVector?,
    val behindRightToLeftSwipeIconSwipeFinishing: ImageVector?,
) {
    companion object {

        /**
         * Creates a remembered instance of [SwipeableItemIcons] with the specified icons.
         *
         * @param behindLeftToRightSwipeIconSwipeStarting The icon to show when the user starts
         *   swiping the item from left to right.
         * @param behindRightToLeftSwipeIconSwipeStarting The icon to show when the user starts
         *   swiping the item from right to left.
         * @param behindLeftToRightSwipeIconSwipeOngoing The icon to show when the user is swiping
         *   the item from left to right, which will only be shown if the user has swiped the item
         *   far enough for this icon to replace [behindLeftToRightSwipeIconSwipeStarting].
         * @param behindRightToLeftSwipeIconSwipeOngoing The icon to show when the user is swiping
         *   the item from right to left, which will only be shown if the user has swiped the item
         *   far enough for this icon to replace [behindRightToLeftSwipeIconSwipeStarting].
         * @param behindLeftToRightSwipeIconSwipeFinishing The icon to show when the user has
         *   finished the swipe from left to right and thus the item is getting dismissed.
         * @param behindRightToLeftSwipeIconSwipeFinishing The icon to show when the user has
         *   finished the swipe from right to left and thus the item is getting dismissed.
         */
        @Composable
        fun createRemembered(
            behindLeftToRightSwipeIconSwipeStarting: ImageVector? = Icons.Outlined.Delete,
            behindRightToLeftSwipeIconSwipeStarting: ImageVector? = Icons.Outlined.Delete,
            behindLeftToRightSwipeIconSwipeOngoing: ImageVector? = Icons.Filled.Delete,
            behindRightToLeftSwipeIconSwipeOngoing: ImageVector? = Icons.Filled.Delete,
            behindLeftToRightSwipeIconSwipeFinishing: ImageVector? = behindLeftToRightSwipeIconSwipeOngoing,
            behindRightToLeftSwipeIconSwipeFinishing: ImageVector? = behindRightToLeftSwipeIconSwipeOngoing,
        ) = remember(
            behindLeftToRightSwipeIconSwipeStarting,
            behindRightToLeftSwipeIconSwipeStarting,
            behindLeftToRightSwipeIconSwipeOngoing,
            behindRightToLeftSwipeIconSwipeOngoing,
            behindLeftToRightSwipeIconSwipeFinishing,
            behindRightToLeftSwipeIconSwipeFinishing,
        ) {
            SwipeableItemIcons(
                behindLeftToRightSwipeIconSwipeStarting = behindLeftToRightSwipeIconSwipeStarting,
                behindRightToLeftSwipeIconSwipeStarting = behindRightToLeftSwipeIconSwipeStarting,
                behindLeftToRightSwipeIconSwipeOngoing = behindLeftToRightSwipeIconSwipeOngoing,
                behindRightToLeftSwipeIconSwipeOngoing = behindRightToLeftSwipeIconSwipeOngoing,
                behindLeftToRightSwipeIconSwipeFinishing = behindLeftToRightSwipeIconSwipeFinishing,
                behindRightToLeftSwipeIconSwipeFinishing = behindRightToLeftSwipeIconSwipeFinishing,
            )
        }

        /**
         * Creates a remembered instance of [SwipeableItemIcons] with the specified icons.
         *
         * @param behindSwipeIconSwipeStarting The icon to show when the user starts swiping the
         *   item from left to right.
         * @param behindSwipeIconSwipeOngoing The icon to show when the user is swiping the item
         *   from left to right, which will only be shown if the user has swiped the item far enough
         *   for this icon to replace [behindSwipeIconSwipeStarting].
         * @param behindSwipeIconSwipeFinishing The icon to show when the user has finished the
         *   swipe from left to right and thus the item is getting dismissed.
         */
        @Composable
        fun createRemembered(
            behindSwipeIconSwipeStarting: ImageVector? = Icons.Outlined.Delete,
            behindSwipeIconSwipeOngoing: ImageVector? = Icons.Filled.Delete,
            behindSwipeIconSwipeFinishing: ImageVector? = behindSwipeIconSwipeOngoing,
        ) = createRemembered(
            behindLeftToRightSwipeIconSwipeStarting = behindSwipeIconSwipeStarting,
            behindRightToLeftSwipeIconSwipeStarting = behindSwipeIconSwipeStarting,
            behindLeftToRightSwipeIconSwipeOngoing = behindSwipeIconSwipeOngoing,
            behindRightToLeftSwipeIconSwipeOngoing = behindSwipeIconSwipeOngoing,
            behindLeftToRightSwipeIconSwipeFinishing = behindSwipeIconSwipeFinishing,
            behindRightToLeftSwipeIconSwipeFinishing = behindSwipeIconSwipeFinishing,
        )

        @Composable
        fun createRemembered(
            behindSwipeIcon: ImageVector? = Icons.Filled.Delete,
        ) = createRemembered(
            behindSwipeIconSwipeStarting = behindSwipeIcon,
            behindSwipeIconSwipeOngoing = behindSwipeIcon,
            behindSwipeIconSwipeFinishing = behindSwipeIcon,
        )
    }
}