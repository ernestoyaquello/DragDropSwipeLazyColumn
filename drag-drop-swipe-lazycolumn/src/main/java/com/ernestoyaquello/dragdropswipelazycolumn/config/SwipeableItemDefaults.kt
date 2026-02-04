package com.ernestoyaquello.dragdropswipelazycolumn.config

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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection.Ltr
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
    val minHeight: Dp = Dp.Unspecified

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
     * `null` for no minimum horizontality for a horizontal swipe to be considered as such.
     */
    val minSwipeHorizontality: Float? = null
}

@Immutable
@ConsistentCopyVisibility
data class SwipeableItemColors internal constructor(
    /**
     * The background color of the item container.
     */
    val containerBackgroundColor: Color?,

    /**
     * The color of the click indication when the item is clicked.
     */
    val clickIndicationColor: Color?,

    /**
     * The background color of the container shown behind the item while the item is being swiped
     * from left to right.
     */
    val behindLeftToRightSwipeContainerBackgroundColor: Color?,

    /**
     * The color of the icon shown behind the item while the item is swiped from left to right.
     */
    val behindLeftToRightSwipeIconColor: Color?,

    /**
     * The background color of the container shown behind the item while the item is being swiped
     * from right to left.
     */
    val behindRightToLeftSwipeContainerBackgroundColor: Color?,

    /**
     * The color of the icon shown behind the item while the item is swiped from right to left.
     */
    val behindRightToLeftSwipeIconColor: Color?,
) {
    companion object {

        /**
         * Creates a [SwipeableItemColors] instance with the specified colors.
         *
         * @param containerBackgroundColor The background color of the item container.
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

        /**
         * Creates a [SwipeableItemColors] instance with the specified colors.
         *
         * @param containerBackgroundColor The background color of the item container.
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
        ): SwipeableItemColors {
            val layoutDirection = LocalLayoutDirection.current
            return remember(
                layoutDirection,
                containerBackgroundColor,
                clickIndicationColor,
                behindStartToEndSwipeContainerBackgroundColor,
                behindStartToEndSwipeIconColor,
                behindEndToStartSwipeContainerBackgroundColor,
                behindEndToStartSwipeIconColor,
            ) {
                SwipeableItemColors(
                    containerBackgroundColor = containerBackgroundColor,
                    clickIndicationColor = clickIndicationColor,
                    behindLeftToRightSwipeContainerBackgroundColor = if (layoutDirection == Ltr) {
                        behindStartToEndSwipeContainerBackgroundColor
                    } else {
                        behindEndToStartSwipeContainerBackgroundColor
                    },
                    behindLeftToRightSwipeIconColor = if (layoutDirection == Ltr) {
                        behindStartToEndSwipeIconColor
                    } else {
                        behindEndToStartSwipeIconColor
                    },
                    behindRightToLeftSwipeContainerBackgroundColor = if (layoutDirection == Ltr) {
                        behindEndToStartSwipeContainerBackgroundColor
                    } else {
                        behindStartToEndSwipeContainerBackgroundColor
                    },
                    behindRightToLeftSwipeIconColor = if (layoutDirection == Ltr) {
                        behindEndToStartSwipeIconColor
                    } else {
                        behindStartToEndSwipeIconColor
                    },
                )
            }
        }

        /**
         * Creates a [SwipeableItemColors] instance with the specified colors.
         *
         * @param containerBackgroundColor The background color of the item container.
         * @param clickIndicationColor The color of the click indication when the item is clicked.
         * @param behindSwipeContainerBackgroundColor The background color of the
         *   container shown behind the item while the item is being swiped in any direction.
         * @param behindSwipeIconColor The color of the icon shown behind the item while
         *   the item is being swiped in any direction.
         */
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
    /**
     * The shape of the item container.
     */
    val containerBackgroundShape: Shape,

    /**
     * The shape of the container shown behind the item while the item is swiped from left to right.
     */
    val behindLeftToRightSwipeContainerShape: Shape,

    /**
     * The shape of the container shown behind the item while the item is swiped from right to left.
     */
    val behindRightToLeftSwipeContainerShape: Shape,
) {
    companion object {

        /**
         * Creates a [SwipeableItemShapes] instance with the specified shapes.
         *
         * @param containerBackgroundShape The shape of the item container.
         * @param behindLeftToRightSwipeContainerShape The shape of the container shown behind the
         *   item while the item is being swiped from left to right.
         * @param behindRightToLeftSwipeContainerShape The shape of the container shown behind the
         *   item while the item is being swiped from right to left.
         */
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

        /**
         * Creates a [SwipeableItemShapes] instance with the specified shapes.
         *
         * @param containerBackgroundShape The shape of the item container.
         * @param behindStartToEndSwipeContainerShape The shape of the container shown behind the
         *   item while the item is being swiped from start to end.
         * @param behindEndToStartSwipeContainerShape The shape of the container shown behind the
         *   item while the item is being swiped from end to start.
         */
        @Composable
        fun createRememberedWithLayoutDirection(
            containerBackgroundShape: Shape = RectangleShape,
            behindStartToEndSwipeContainerShape: Shape = containerBackgroundShape,
            behindEndToStartSwipeContainerShape: Shape = containerBackgroundShape,
        ): SwipeableItemShapes {
            val layoutDirection = LocalLayoutDirection.current
            return remember(
                layoutDirection,
                containerBackgroundShape,
                behindStartToEndSwipeContainerShape,
                behindEndToStartSwipeContainerShape,
            ) {
                SwipeableItemShapes(
                    containerBackgroundShape = containerBackgroundShape,
                    behindLeftToRightSwipeContainerShape = if (layoutDirection == Ltr) {
                        behindStartToEndSwipeContainerShape
                    } else {
                        behindEndToStartSwipeContainerShape
                    },
                    behindRightToLeftSwipeContainerShape = if (layoutDirection == Ltr) {
                        behindEndToStartSwipeContainerShape
                    } else {
                        behindStartToEndSwipeContainerShape
                    },
                )
            }
        }

        /**
         * Creates a [SwipeableItemShapes] instance with the specified shapes.
         *
         * @param containerBackgroundShape The shape of the item container.
         * @param behindSwipeContainerShape The shape of the container shown behind the
         *   item while the item is being swiped in any direction.
         */
        @Composable
        fun createRemembered(
            containerBackgroundShape: Shape = RectangleShape,
            behindSwipeContainerShape: Shape = containerBackgroundShape,
        ) = createRemembered(
            containerBackgroundShape = containerBackgroundShape,
            behindLeftToRightSwipeContainerShape = behindSwipeContainerShape,
            behindRightToLeftSwipeContainerShape = behindSwipeContainerShape,
        )

        /**
         * Creates a [SwipeableItemShapes] instance with the specified shapes.
         *
         * @param containerBackgroundShape The shape of the item container, which will also be used
         *   for the containers shown behind the item while the item is being swiped.
         */
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
    /**
     * The icon to show when the user starts swiping the item from left to right.
     */
    val behindLeftToRightSwipeIconSwipeStarting: ImageVector?,

    /**
     * The icon to show when the user starts swiping the item from right to left.
     */
    val behindRightToLeftSwipeIconSwipeStarting: ImageVector?,

    /**
     * The icon to show when the user is swiping the item from left to right, which will only be
     * shown if the user has swiped the item far enough for this icon to replace
     * [behindLeftToRightSwipeIconSwipeStarting].
     */
    val behindLeftToRightSwipeIconSwipeOngoing: ImageVector?,

    /**
     * The icon to show when the user is swiping the item from right to left, which will only be
     * shown if the user has swiped the item far enough for this icon to replace
     * [behindRightToLeftSwipeIconSwipeStarting].
     */
    val behindRightToLeftSwipeIconSwipeOngoing: ImageVector?,

    /**
     * The icon to show when the user has finished the swipe from left to right and thus the item is
     * getting dismissed.
     */
    val behindLeftToRightSwipeIconSwipeFinishing: ImageVector?,

    /**
     * The icon to show when the user has finished the swipe from right to left and thus the item is
     * getting dismissed.
     */
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
         * @param behindStartToEndSwipeIconSwipeStarting The icon to show when the user starts
         *   swiping the item from start to end.
         * @param behindEndToStartSwipeIconSwipeStarting The icon to show when the user starts
         *   swiping the item from end to start.
         * @param behindStartToEndSwipeIconSwipeOngoing The icon to show when the user is swiping
         *   the item from start to end, which will only be shown if the user has swiped the item
         *   far enough for this icon to replace [behindStartToEndSwipeIconSwipeStarting].
         * @param behindEndToStartSwipeIconSwipeOngoing The icon to show when the user is swiping
         *   the item from end to start, which will only be shown if the user has swiped the item
         *   far enough for this icon to replace [behindEndToStartSwipeIconSwipeStarting].
         * @param behindStartToEndSwipeIconSwipeFinishing The icon to show when the user has
         *   finished the swipe from start to end and thus the item is getting dismissed.
         * @param behindEndToStartSwipeIconSwipeFinishing The icon to show when the user has
         *   finished the swipe from end to start and thus the item is getting dismissed.
         */
        @Composable
        fun createRememberedWithLayoutDirection(
            behindStartToEndSwipeIconSwipeStarting: ImageVector? = Icons.Outlined.Delete,
            behindEndToStartSwipeIconSwipeStarting: ImageVector? = Icons.Outlined.Delete,
            behindStartToEndSwipeIconSwipeOngoing: ImageVector? = Icons.Filled.Delete,
            behindEndToStartSwipeIconSwipeOngoing: ImageVector? = Icons.Filled.Delete,
            behindStartToEndSwipeIconSwipeFinishing: ImageVector? = behindStartToEndSwipeIconSwipeOngoing,
            behindEndToStartSwipeIconSwipeFinishing: ImageVector? = behindEndToStartSwipeIconSwipeOngoing,
        ): SwipeableItemIcons {
            val layoutDirection = LocalLayoutDirection.current
            return remember(
                layoutDirection,
                behindStartToEndSwipeIconSwipeStarting,
                behindEndToStartSwipeIconSwipeStarting,
                behindStartToEndSwipeIconSwipeOngoing,
                behindEndToStartSwipeIconSwipeOngoing,
                behindStartToEndSwipeIconSwipeFinishing,
                behindEndToStartSwipeIconSwipeFinishing,
            ) {
                SwipeableItemIcons(
                    behindLeftToRightSwipeIconSwipeStarting = if (layoutDirection == Ltr) {
                        behindStartToEndSwipeIconSwipeStarting
                    } else {
                        behindEndToStartSwipeIconSwipeStarting
                    },
                    behindRightToLeftSwipeIconSwipeStarting = if (layoutDirection == Ltr) {
                        behindEndToStartSwipeIconSwipeStarting
                    } else {
                        behindStartToEndSwipeIconSwipeStarting
                    },
                    behindLeftToRightSwipeIconSwipeOngoing = if (layoutDirection == Ltr) {
                        behindStartToEndSwipeIconSwipeOngoing
                    } else {
                        behindEndToStartSwipeIconSwipeOngoing
                    },
                    behindRightToLeftSwipeIconSwipeOngoing = if (layoutDirection == Ltr) {
                        behindEndToStartSwipeIconSwipeOngoing
                    } else {
                        behindStartToEndSwipeIconSwipeOngoing
                    },
                    behindLeftToRightSwipeIconSwipeFinishing = if (layoutDirection == Ltr) {
                        behindStartToEndSwipeIconSwipeFinishing
                    } else {
                        behindEndToStartSwipeIconSwipeFinishing
                    },
                    behindRightToLeftSwipeIconSwipeFinishing = if (layoutDirection == Ltr) {
                        behindEndToStartSwipeIconSwipeFinishing
                    } else {
                        behindStartToEndSwipeIconSwipeFinishing
                    },
                )
            }
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
    }
}