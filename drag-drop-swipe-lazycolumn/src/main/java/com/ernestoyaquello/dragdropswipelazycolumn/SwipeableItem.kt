package com.ernestoyaquello.dragdropswipelazycolumn

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalDragOrCancellation
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.ernestoyaquello.dragdropswipelazycolumn.AllowedSwipeDirections.All
import com.ernestoyaquello.dragdropswipelazycolumn.AllowedSwipeDirections.None
import com.ernestoyaquello.dragdropswipelazycolumn.AllowedSwipeDirections.OnlyLeftToRight
import com.ernestoyaquello.dragdropswipelazycolumn.AllowedSwipeDirections.OnlyRightToLeft
import com.ernestoyaquello.dragdropswipelazycolumn.DismissSwipeDirection.LeftToRight
import com.ernestoyaquello.dragdropswipelazycolumn.DismissSwipeDirection.RightToLeft
import com.ernestoyaquello.dragdropswipelazycolumn.DismissSwipeDirectionLayoutAdjusted.EndToStart
import com.ernestoyaquello.dragdropswipelazycolumn.DismissSwipeDirectionLayoutAdjusted.StartToEnd
import com.ernestoyaquello.dragdropswipelazycolumn.OngoingSwipeDirection.NotSwiping
import com.ernestoyaquello.dragdropswipelazycolumn.OngoingSwipeDirection.SwipingLeftToRight
import com.ernestoyaquello.dragdropswipelazycolumn.OngoingSwipeDirection.SwipingRightToLeft
import com.ernestoyaquello.dragdropswipelazycolumn.config.SwipeableItemColors
import com.ernestoyaquello.dragdropswipelazycolumn.config.SwipeableItemDefaults
import com.ernestoyaquello.dragdropswipelazycolumn.config.SwipeableItemIcons
import com.ernestoyaquello.dragdropswipelazycolumn.config.SwipeableItemShapes
import com.ernestoyaquello.dragdropswipelazycolumn.preview.MultiPreview
import com.ernestoyaquello.dragdropswipelazycolumn.preview.ThemedPreview
import com.ernestoyaquello.dragdropswipelazycolumn.state.SwipeableItemState
import com.ernestoyaquello.dragdropswipelazycolumn.state.rememberSwipeableItemState
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * A simple Composable that can be used to create a horizontally swipeable item.
 * When the item is swiped successfully, [onSwipeDismiss] will be invoked.
 *
 * @param modifier The [Modifier] to be applied to the item.
 * @param state The [SwipeableItemState] that will be used to control the item.
 * @param colors The [SwipeableItemColors] that will be used to style the item.
 * @param shapes The [SwipeableItemShapes] that will be used to style the item.
 * @param icons The [SwipeableItemIcons] that will be displayed behind the item during swiping.
 * @param minHeight The minimum height of the item.
 * @param contentStartPadding The padding to be applied at the start of the item content.
 * @param contentEndPadding The padding to be applied at the end of the item content.
 * @param minSwipeHorizontality The minimum horizontal delta to vertical delta ratio required for a
 *   horizontal swipe gesture to be considered a valid swipe start. The higher this value, the more
 *   "horizontal" the swipe gesture must be for it to be actually handled as a swipe. A `null` means
 *   no minimum horizontality for a horizontal swipe to be considered as such.
 * @param clickIndication The click indication to be applied to the item when it is clicked. It will
 *   only be applied if either [onClick] or [onLongClick] is not `null` and the item is not being
 *   swiped.
 * @param onClick The callback to be invoked when the item is clicked. It will only be invoked if
 *   the item is not being swiped.
 * @param onLongClick The callback to be invoked when the item is long-clicked. It will only be
 *   invoked if the item is not being swiped.
 * @param onSwipeGestureStart The callback to be invoked when the user starts swiping the item.
 * @param onSwipeGestureUpdate The callback to be invoked when the user is swiping the item and a
 *   swipe delta in pixels is detected, meaning that the user has swiped the item by some amount.
 * @param onSwipeGestureFinish The callback to be invoked when the user finishes swiping the item.
 * @param onSwipeDismiss The callback to be invoked when the user swipes the item far enough and/or
 *   fast enough to trigger the dismissal of the item. The direction in which the item was dismissed
 *   will be provided as a parameter.
 * @param content The content of the item.
 */
@Composable
fun SwipeableItem(
    modifier: Modifier = Modifier,
    state: SwipeableItemState = rememberSwipeableItemState(),
    colors: SwipeableItemColors = SwipeableItemDefaults.colors(),
    shapes: SwipeableItemShapes = SwipeableItemDefaults.shapes(),
    icons: SwipeableItemIcons = SwipeableItemDefaults.icons(),
    minHeight: Dp = SwipeableItemDefaults.minHeight,
    contentStartPadding: Dp = SwipeableItemDefaults.contentStartPadding,
    contentEndPadding: Dp = SwipeableItemDefaults.contentEndPadding,
    minSwipeHorizontality: Float? = SwipeableItemDefaults.minSwipeHorizontality,
    clickIndication: Indication? = ripple(color = colors.clickIndicationColor ?: Color.Unspecified),
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onSwipeGestureStart: (swipeDeltaInPx: Float) -> Unit = remember { {} },
    onSwipeGestureUpdate: (swipeDeltaInPx: Float, pressed: Boolean) -> Unit = remember { { _, _ -> } },
    onSwipeGestureFinish: () -> Unit = remember { {} },
    onSwipeDismiss: (DismissSwipeDirection) -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    var itemWidthInPx by remember { mutableFloatStateOf(0f) }
    var itemHeightInPx by remember { mutableFloatStateOf(0f) }
    var isItemBouncingBackToItsOriginalPosition by remember { mutableStateOf(false) }
    val animatedSwipeOffsetInPx = remember {
        Animatable(initialValue = state.offsetTargetInPx, typeConverter = Float.VectorConverter)
    }

    // Ensure swipe actions that are canceled don't result in the item being dismissed
    if (!state.isSwipeAllowed) {
        state.update {
            copy(
                ongoingSwipeDirection = NotSwiping,
                offsetTargetInPx = 0f,
                lastVelocity = 0f,
            )
        }
    }

    ApplySwipeOffsetIfNeeded(
        animatedSwipeOffsetInPx = animatedSwipeOffsetInPx,
        isUserSwiping = state.isBeingSwiped,
        swipeOffsetTargetInPx = state.offsetTargetInPx,
        lastSwipeVelocity = state.lastVelocity,
        isItemDismissedOrBeingDismissed = state.isItemDismissedOrBeingDismissed,
        onItemIsBouncingUpdated = { isItemBouncingBackToItsOriginalPosition = it },
        onDismissedViaSwiping = onSwipeDismiss,
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = contentStartPadding,
                end = contentEndPadding,
            )
            .onSizeChanged {
                itemWidthInPx = it.width.toFloat()
                itemHeightInPx = it.height.toFloat()
            },
    ) {
        val localDensity = LocalDensity.current
        val clickInteractionSource = remember(onClick, onLongClick) {
            MutableInteractionSource()
        }
        val adjustedMinSwipeHorizontality = minSwipeHorizontality?.takeUnless { it == 0f }
        val layoutDirection = LocalLayoutDirection.current

        // Only draw the "behind" content when the item isn't on its original position – in that
        // case, there would be no point on drawing it, as it would be fully hidden behind the item.
        // Additionally, we need to consider that the item might not be on its original position
        // simply because it's bouncing as part of the animation to make it return automatically to
        // said position, in which case we will make sure to hide the behind content, as it would be
        // weird to see the behind content appearing behind the item as the item bounces into place.
        if (animatedSwipeOffsetInPx.value != 0f && (!isItemBouncingBackToItsOriginalPosition || state.isBeingSwiped)) {
            val containerBackgroundColor = when {
                animatedSwipeOffsetInPx.value > 0f -> colors.behindLeftToRightSwipeContainerBackgroundColor
                animatedSwipeOffsetInPx.value < 0f -> colors.behindRightToLeftSwipeContainerBackgroundColor
                else -> null
            }
            val containerBackgroundShape = when {
                animatedSwipeOffsetInPx.value > 0f -> shapes.behindLeftToRightSwipeContainerShape
                animatedSwipeOffsetInPx.value < 0f -> shapes.behindRightToLeftSwipeContainerShape
                else -> RectangleShape
            }
            val iconBeforeThreshold = when {
                animatedSwipeOffsetInPx.value > 0f -> icons.behindLeftToRightSwipeIconSwipeStarting
                animatedSwipeOffsetInPx.value < 0f -> icons.behindRightToLeftSwipeIconSwipeStarting
                else -> null
            }
            val iconAfterThreshold = when {
                animatedSwipeOffsetInPx.value > 0f -> icons.behindLeftToRightSwipeIconSwipeOngoing
                animatedSwipeOffsetInPx.value < 0f -> icons.behindRightToLeftSwipeIconSwipeOngoing
                else -> null
            }
            val iconDuringDismissal = when {
                animatedSwipeOffsetInPx.value > 0f -> icons.behindLeftToRightSwipeIconSwipeFinishing
                animatedSwipeOffsetInPx.value < 0f -> icons.behindRightToLeftSwipeIconSwipeFinishing
                else -> null
            }
            val iconColor = when {
                animatedSwipeOffsetInPx.value > 0f -> colors.behindLeftToRightSwipeIconColor
                animatedSwipeOffsetInPx.value < 0f -> colors.behindRightToLeftSwipeIconColor
                else -> null
            }

            SwipeableItemBehindContent(
                itemWidth = with(localDensity) { itemWidthInPx.toDp() },
                currentSwipeOffset = with(localDensity) { animatedSwipeOffsetInPx.value.toDp() },
                minHeight = max(
                    a = minHeight.takeUnless { it == Dp.Unspecified } ?: 0.dp,
                    b = with(localDensity) { itemHeightInPx.toDp() },
                ),
                containerBackgroundColor = containerBackgroundColor,
                containerBackgroundShape = containerBackgroundShape,
                iconBeforeThreshold = iconBeforeThreshold,
                iconAfterThreshold = iconAfterThreshold,
                iconDuringDismissal = iconDuringDismissal,
                isItemBeingDismissed = state.isItemDismissedOrBeingDismissed,
                iconColor = iconColor,
            )
        }

        Box(
            modifier = Modifier
                .defaultMinSize(minHeight = minHeight)
                .fillMaxWidth()
                .pointerInput(
                    clickInteractionSource,
                    onClick,
                    onLongClick,
                ) {
                    handleTapGestures(
                        clickInteractionSource = clickInteractionSource,
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
                }
                .pointerInput(
                    state,
                    adjustedMinSwipeHorizontality,
                    itemWidthInPx,
                    localDensity,
                    contentStartPadding,
                    contentEndPadding,
                    layoutDirection,
                    onSwipeGestureStart,
                    onSwipeGestureUpdate,
                    onSwipeGestureFinish,
                ) {
                    awaitEachGesture {
                        handleSwipeGestures(
                            state = state,
                            minSwipeHorizontality = adjustedMinSwipeHorizontality,
                            itemWidthInPx = itemWidthInPx,
                            contentStartPaddingInPx = with(localDensity) { contentStartPadding.toPx() },
                            contentEndPaddingInPx = with(localDensity) { contentEndPadding.toPx() },
                            layoutDirection = layoutDirection,
                            onSwipeStart = onSwipeGestureStart,
                            onSwipeUpdate = onSwipeGestureUpdate,
                            onSwipeFinish = onSwipeGestureFinish,
                        )
                    }
                }
                .offset {
                    IntOffset(
                        x = if (state.isBeingSwiped) {
                            // The user is currently swiping the item, so here we apply the offset
                            // immediately to ensure the user's pointer input is followed as quickly
                            // as possible and without potential animation delays (even though we
                            // use "snap to" on the offset animatable when the item is being swiped,
                            // there might be a tiny delay before the "snap to" action is invoked at
                            // all, so here we just reference the target offset directly, as it is
                            // guaranteed to be up to date).
                            state.offsetTargetInPx.roundToInt()
                        } else {
                            // Otherwise, just apply the animated offset normally
                            animatedSwipeOffsetInPx.value.roundToInt()
                        },
                        y = 0,
                    )
                }
                .then(
                    other = if (colors.containerBackgroundColor != null) {
                        Modifier.background(
                            color = colors.containerBackgroundColor,
                            shape = shapes.containerBackgroundShape,
                        )
                    } else {
                        Modifier
                    },
                )
                .then(
                    other = if (clickIndication != null && !state.isBeingSwiped && (onClick != null || onLongClick != null)) {
                        // The clipped click indication needs to be added here at the end so that
                        // it's shifted appropriately by the offset applied above.
                        Modifier
                            .clip(
                                shape = shapes.containerBackgroundShape,
                            )
                            .indication(
                                interactionSource = clickInteractionSource,
                                indication = clickIndication,
                            )
                    } else {
                        Modifier
                    },
                ),
        ) {
            content()
        }
    }
}

@Composable
private fun ApplySwipeOffsetIfNeeded(
    animatedSwipeOffsetInPx: Animatable<Float, AnimationVector1D>,
    isUserSwiping: Boolean,
    swipeOffsetTargetInPx: Float,
    lastSwipeVelocity: Float,
    isItemDismissedOrBeingDismissed: Boolean,
    onItemIsBouncingUpdated: (Boolean) -> Unit,
    onDismissedViaSwiping: (DismissSwipeDirection) -> Unit,
) {
    LaunchedEffect(
        isUserSwiping,
        swipeOffsetTargetInPx,
        lastSwipeVelocity,
        isItemDismissedOrBeingDismissed,
        onItemIsBouncingUpdated,
        onDismissedViaSwiping,
    ) {
        // Do this immediately just in case the value was left as true in a canceled invocation
        onItemIsBouncingUpdated(false)

        // Either move immediately to wherever the user is swiping to, or animate to the target
        if (isUserSwiping) {
            animatedSwipeOffsetInPx.snapTo(swipeOffsetTargetInPx)
        } else {
            // Normally, we will just keep the current velocity of the animatable, but when the
            // item is going back to its original position, we'll make sure the animation takes
            // into account the last known swipe velocity (at least a little).
            val initialVelocity = if (isItemDismissedOrBeingDismissed) {
                animatedSwipeOffsetInPx.velocity
            } else {
                (0.5f * animatedSwipeOffsetInPx.velocity) + (0.5f * lastSwipeVelocity)
            }

            // If the item is being dismissed, we will want it to get to its destination (the edge
            // of the screen) fast and directly; but if the item is returning to its original
            // position, then the animation will look a little bit nicer with a bounce effect.
            val animationSpec = SpringSpec<Float>(
                dampingRatio = if (isItemDismissedOrBeingDismissed) {
                    Spring.DampingRatioNoBouncy
                } else {
                    Spring.DampingRatioMediumBouncy
                },
                stiffness = Spring.StiffnessMedium,
            )

            // Get the direction of the animation so we can check if the item is bouncing below
            val animDirection = (swipeOffsetTargetInPx - animatedSwipeOffsetInPx.value).sign

            // Finally, actually animate the item to its target position
            var isItemBouncing = false
            animatedSwipeOffsetInPx.animateTo(
                targetValue = swipeOffsetTargetInPx,
                initialVelocity = initialVelocity,
                animationSpec = animationSpec,
            ) {
                if (!isItemDismissedOrBeingDismissed && !isItemBouncing) {
                    // Now that we know the item is being moved back to its original position, we
                    // need to check if the currently ongoing movement isn't happening to reach said
                    // original position, but rather because of the bouncing that can happen after
                    // the item has already reached it.
                    isItemBouncing = if (animDirection > 0) {
                        value >= swipeOffsetTargetInPx
                    } else {
                        value <= swipeOffsetTargetInPx
                    }
                    if (isItemBouncing) {
                        onItemIsBouncingUpdated(true)
                    }
                }
            }

            // With this animation over, we know for a fact the item can no longer be bouncing
            if (isItemBouncing) {
                onItemIsBouncingUpdated(false)
            }

            // Lastly, we might need to invoke the callback to notify that the item was dismissed
            if (isItemDismissedOrBeingDismissed) {
                val dismissDirection = when {
                    swipeOffsetTargetInPx > 0f -> LeftToRight
                    swipeOffsetTargetInPx < 0f -> RightToLeft
                    else -> null // This should never happen, but just in case?
                }
                dismissDirection?.let { onDismissedViaSwiping(it) }
            }
        }
    }
}

@Composable
private fun SwipeableItemBehindContent(
    itemWidth: Dp,
    currentSwipeOffset: Dp,
    minHeight: Dp,
    containerBackgroundColor: Color?,
    containerBackgroundShape: Shape,
    iconBeforeThreshold: ImageVector?,
    iconAfterThreshold: ImageVector?,
    iconDuringDismissal: ImageVector?,
    isItemBeingDismissed: Boolean,
    iconColor: Color?,
) {
    val defaultIconSize = 24.dp
    val animatedIconSize = remember { Animatable(24.dp, Dp.VectorConverter) }
    val minIconPadding = 24.dp
    val maxIconPadding = (itemWidth - animatedIconSize.value) / 2f
    val extraIconSize = animatedIconSize.value - defaultIconSize
    val absSwipeOffset = currentSwipeOffset * currentSwipeOffset.value.sign
    val iconPadding = ((absSwipeOffset - minIconPadding) / 2f).coerceAtLeast(0.dp)
    val correctedIconPadding = (iconPadding - (extraIconSize / 2f)).coerceIn(
        minimumValue = minIconPadding,
        maximumValue = maxIconPadding,
    )
    val iconRevealPercentage = (iconPadding / (minIconPadding * 1.25f)).absoluteValue
    val correctedIconRevealPercentage = iconRevealPercentage.coerceAtMost(1f)
    val containerBackgroundAlpha = (0.5f + (0.5f * (absSwipeOffset / minIconPadding))).coerceAtMost(
        maximumValue = 1f,
    )
    val iconThresholdReached = correctedIconRevealPercentage == 1f

    LaunchedEffect(iconThresholdReached) {
        if (iconThresholdReached) {
            animatedIconSize.animateTo(
                targetValue = defaultIconSize * 1.2f,
                animationSpec = tween(durationMillis = 75),
            )
        } else {
            animatedIconSize.animateTo(
                targetValue = defaultIconSize,
                animationSpec = tween(durationMillis = 150),
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight)
            .then(
                other = if (containerBackgroundColor != null) {
                    Modifier.background(
                        color = containerBackgroundColor.copy(alpha = containerBackgroundAlpha),
                        shape = containerBackgroundShape,
                    )
                } else {
                    Modifier
                },
            )
            .padding(
                start = if (currentSwipeOffset.value > 0f) correctedIconPadding else 0.dp,
                end = if (currentSwipeOffset.value < 0f) correctedIconPadding else 0.dp,
            ),
        contentAlignment = if (currentSwipeOffset.value > 0f) {
            Alignment.CenterStart
        } else {
            Alignment.CenterEnd
        },
    ) {
        if (iconBeforeThreshold != null || iconAfterThreshold != null || iconDuringDismissal != null) {
            val iconIdentifier = when {
                isItemBeingDismissed -> 2
                iconThresholdReached -> 1
                else -> 0
            }

            Crossfade(
                modifier = Modifier
                    .size(animatedIconSize.value)
                    .alpha(correctedIconRevealPercentage),
                targetState = iconIdentifier,
                animationSpec = tween(durationMillis = 150),
            ) { iconToShowId ->
                when (iconToShowId) {
                    0 -> iconBeforeThreshold?.let {
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            imageVector = iconBeforeThreshold,
                            tint = iconColor ?: LocalContentColor.current,
                            contentDescription = null,
                        )
                    }

                    1 -> iconAfterThreshold?.let {
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            imageVector = iconAfterThreshold,
                            tint = iconColor ?: LocalContentColor.current,
                            contentDescription = null,
                        )
                    }

                    2 -> iconDuringDismissal?.let {
                        Icon(
                            modifier = Modifier.fillMaxSize(),
                            imageVector = iconDuringDismissal,
                            tint = iconColor ?: LocalContentColor.current,
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }
}

private suspend fun PointerInputScope.handleTapGestures(
    clickInteractionSource: MutableInteractionSource,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
) {
    detectTapGestures(
        onPress = { offset ->
            if (onClick != null || onLongClick != null) {
                // Delay to avoid the ripple effect being shown when the item is being swiped.
                // This isn't perfect, and it makes the tap gesture feel a bit delayed, but I
                // am not sure how to do it better.
                // FIXME Do this better
                delay(50)

                val press = PressInteraction.Press(offset)
                clickInteractionSource.emit(press)
                if (tryAwaitRelease()) {
                    clickInteractionSource.emit(PressInteraction.Release(press))
                } else {
                    clickInteractionSource.emit(PressInteraction.Cancel(press))
                }
            }
        },
        onTap = onClick?.let { { it() } },
        onLongPress = onLongClick?.let { { it() } },
    )
}

private suspend fun AwaitPointerEventScope.handleSwipeGestures(
    state: SwipeableItemState,
    minSwipeHorizontality: Float?,
    itemWidthInPx: Float,
    contentStartPaddingInPx: Float,
    contentEndPaddingInPx: Float,
    layoutDirection: LayoutDirection,
    onSwipeStart: (swipeDeltaInPx: Float) -> Unit,
    onSwipeUpdate: (swipeDeltaInPx: Float, pressed: Boolean) -> Unit,
    onSwipeFinish: () -> Unit,
) {
    var lastSwipe: PointerInputChange? = null
    val handleSwipe: (PointerInputChange?, PointerInputChange) -> Unit = { down, swipe ->
        val swipeDelta = swipe.position.x - swipe.previousPosition.x
        if (state.isSwipeAllowed && (
                    state.allowedSwipeDirections == All
                            || (state.allowedSwipeDirections != None && down == null)
                            || (state.allowedSwipeDirections == OnlyLeftToRight && swipeDelta > 0f)
                            || (state.allowedSwipeDirections == OnlyRightToLeft && swipeDelta < 0f)
                    )
        ) {
            down?.consume()
            swipe.consume()
            lastSwipe = swipe

            state.update {
                val ongoingSwipeDirection = when {
                    down != null && swipeDelta > 0f -> SwipingLeftToRight
                    down != null && swipeDelta < 0f -> SwipingRightToLeft
                    else -> ongoingSwipeDirection
                }
                val offsetTargetInPx = when (ongoingSwipeDirection) {
                    SwipingLeftToRight -> (offsetTargetInPx + swipeDelta).coerceAtLeast(
                        minimumValue = if (allowedSwipeDirections == All) Float.NEGATIVE_INFINITY else 0f,
                    )

                    SwipingRightToLeft -> (offsetTargetInPx + swipeDelta).coerceAtMost(
                        maximumValue = if (allowedSwipeDirections == All) Float.POSITIVE_INFINITY else 0f,
                    )

                    NotSwiping -> 0f
                }
                val timeDelta = swipe.uptimeMillis - swipe.previousUptimeMillis
                val lastVelocity = if (timeDelta > 0f) {
                    1000f * (swipeDelta / timeDelta.toFloat())
                } else {
                    0f
                }

                copy(
                    ongoingSwipeDirection = ongoingSwipeDirection,
                    offsetTargetInPx = offsetTargetInPx,
                    lastVelocity = lastVelocity,
                )
            }
        }
    }

    // Detect the swipe gesture by listening for the first touch event that goes over the slop
    val down = awaitFirstDown()
    var swipeStarted = false
    var swipe = awaitHorizontalTouchSlopOrCancellation(pointerId = down.id) { potentialSwipe, _ ->
        // Only handle the swipe if the horizontal delta is greater than the vertical delta,
        // as we only care about horizontal swipes.
        val potentialSwipeDelta = potentialSwipe.position.x - potentialSwipe.previousPosition.x
        val verticalDelta = potentialSwipe.position.y - potentialSwipe.previousPosition.y
        val horizontalSlope = if (verticalDelta != 0f) {
            abs(potentialSwipeDelta / verticalDelta)
        } else {
            Float.POSITIVE_INFINITY
        }
        if (minSwipeHorizontality == null || horizontalSlope >= minSwipeHorizontality) {
            handleSwipe(down, potentialSwipe)
            onSwipeStart(potentialSwipeDelta)
            swipeStarted = true
        }
    }

    // If we detect it, we need to keep listening for the rest of the swipe gesture
    while (swipe != null && swipe.pressed && state.isSwipeAllowed) {
        swipe = awaitHorizontalDragOrCancellation(pointerId = swipe.id)
        if (swipe != null) {
            val swipeToHandle = if (swipe.pressed) {
                swipe
            } else {
                // Let's take into account the time when the user stopped pressing. This way, if the
                // user swiped, then stopped for a while, and then released, we would consider the
                // extra time that it took them to release as part of the last swipe change, which
                // in turn will ensure that we won't launch the item to its dismissal in cases when
                // the user purposely stopped the swipe movement before releasing.
                requireNotNull(lastSwipe).copy(
                    uptimeMillis = swipe.uptimeMillis,
                    position = swipe.position,
                )
            }
            handleSwipe(null, swipeToHandle)

            val swipeDeltaInPx = swipeToHandle.position.x - swipeToHandle.previousPosition.x
            onSwipeUpdate(swipeDeltaInPx, swipe.pressed)
        }
    }

    if (state.isBeingSwiped) {
        // Finally, now that the swiping has ended, let's see if we should dismiss the item or not
        val speedRatioThreshold = 0.12f
        val horizontalPadding = contentStartPaddingInPx + contentEndPaddingInPx
        val speedRatio = abs(state.lastVelocity / (itemWidthInPx + horizontalPadding))
        val absDistanceRatio = abs(state.offsetTargetInPx / itemWidthInPx)
        val distanceSign = state.offsetTargetInPx.sign
        val wasCancelled = swipe == null || !state.isSwipeAllowed
        val velocitySign = state.lastVelocity.sign
        val isValidDirection = velocitySign == distanceSign || speedRatio < speedRatioThreshold
        val dismissViaSpeed = isValidDirection && speedRatio >= speedRatioThreshold
        val dismissViaDistance = isValidDirection && absDistanceRatio >= 0.5f
        state.update {
            copy(
                ongoingSwipeDirection = NotSwiping,
                offsetTargetInPx = if (!wasCancelled && (dismissViaSpeed || dismissViaDistance)) {
                    // Push over the edge to dismiss, adding a +1 for good measure
                    val paddingToCoverInPx = if (distanceSign > 0f) {
                        if (layoutDirection == Ltr) contentEndPaddingInPx else contentStartPaddingInPx
                    } else {
                        if (layoutDirection == Ltr) contentStartPaddingInPx else contentEndPaddingInPx
                    }
                    distanceSign * (itemWidthInPx + paddingToCoverInPx + 1f)
                } else {
                    // Unsuccessful dismissal, back to the original position
                    0f
                },
            )
        }
    }

    if (swipeStarted) {
        onSwipeFinish()
    }
}

private fun PointerInputChange.copy(
    uptimeMillis: Long,
    position: Offset,
) = PointerInputChange(
    id = id,
    uptimeMillis = uptimeMillis,
    position = position,
    pressed = pressed,
    pressure = pressure,
    previousUptimeMillis = previousUptimeMillis,
    previousPosition = previousPosition,
    previousPressed = previousPressed,
    isInitiallyConsumed = isConsumed,
    type = type,
    scrollDelta = scrollDelta,
)

@Immutable
enum class AllowedSwipeDirections {
    All,
    OnlyLeftToRight,
    OnlyRightToLeft,
    None,
}

@Immutable
enum class OngoingSwipeDirection {
    SwipingLeftToRight,
    SwipingRightToLeft,
    NotSwiping,
}

@Immutable
enum class DismissSwipeDirection {
    LeftToRight,
    RightToLeft,
}

@Immutable
enum class DismissSwipeDirectionLayoutAdjusted {
    StartToEnd,
    EndToStart,
}

@Composable
fun DismissSwipeDirection.toLayoutAdjustedDirection() = toLayoutAdjustedDirection(
    layoutDirection = LocalLayoutDirection.current,
)

fun DismissSwipeDirection.toLayoutAdjustedDirection(
    layoutDirection: LayoutDirection,
): DismissSwipeDirectionLayoutAdjusted {
    return when (this) {
        LeftToRight -> if (layoutDirection == Ltr) StartToEnd else EndToStart
        RightToLeft -> if (layoutDirection == Ltr) EndToStart else StartToEnd
    }
}

@Composable
@MultiPreview
private fun SwipeableItem_InteractivePreview_Basic() {
    val swipeState = rememberSwipeableItemState()
    var numberOfClicks by remember { mutableIntStateOf(0) }
    var numberOfLongClicks by remember { mutableIntStateOf(0) }
    var numberOfSwipes by remember { mutableIntStateOf(0) }

    ThemedPreview {
        SwipeableItem(
            state = swipeState,
            onClick = { numberOfClicks++ },
            onLongClick = { numberOfLongClicks++ },
            onSwipeDismiss = { numberOfSwipes++ },
        ) {
            PreviewItem(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                textData = "(full swipes: $numberOfSwipes, clicks: $numberOfClicks, long clicks: $numberOfLongClicks)",
            )
        }

        if (numberOfSwipes > 0) {
            LaunchedEffect(numberOfSwipes) {
                // Reset after a while so we can keep clicking, swiping, etc
                delay(300)
                swipeState.reset()
            }
        }
    }
}

@Composable
@MultiPreview
private fun SwipeableItem_InteractivePreview_Customized() {
    val swipeState = rememberSwipeableItemState()
    var numberOfClicks by remember { mutableIntStateOf(0) }
    var numberOfLongClicks by remember { mutableIntStateOf(0) }
    var numberOfDeleted by remember { mutableIntStateOf(0) }
    var numberOfFavorited by remember { mutableIntStateOf(0) }

    ThemedPreview {
        val layoutDirection = LocalLayoutDirection.current
        SwipeableItem(
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.background)
                .padding(vertical = 16.dp),
            state = swipeState,
            minHeight = 60.dp,
            contentStartPadding = 16.dp,
            contentEndPadding = 16.dp,
            colors = SwipeableItemColors.createRememberedWithLayoutDirection(
                containerBackgroundColor = MaterialTheme.colorScheme.primaryContainer,
                behindStartToEndSwipeContainerBackgroundColor = MaterialTheme.colorScheme.secondary,
                behindStartToEndSwipeIconColor = MaterialTheme.colorScheme.onSecondary,
                behindEndToStartSwipeContainerBackgroundColor = MaterialTheme.colorScheme.error,
                behindEndToStartSwipeIconColor = MaterialTheme.colorScheme.onError,
            ),
            shapes = SwipeableItemShapes.createRememberedWithLayoutDirection(
                containerBackgroundShape = MaterialTheme.shapes.extraSmall,
                behindStartToEndSwipeContainerShape = CircleShape,
                behindEndToStartSwipeContainerShape = MaterialTheme.shapes.medium,
            ),
            icons = SwipeableItemIcons.createRememberedWithLayoutDirection(
                behindStartToEndSwipeIconSwipeStarting = Icons.Outlined.FavoriteBorder,
                behindStartToEndSwipeIconSwipeOngoing = Icons.Filled.Favorite,
                behindStartToEndSwipeIconSwipeFinishing = Icons.Filled.Done,
                behindEndToStartSwipeIconSwipeStarting = Icons.Outlined.Delete,
                behindEndToStartSwipeIconSwipeOngoing = Icons.Filled.Delete,
                behindEndToStartSwipeIconSwipeFinishing = Icons.Filled.Clear,
            ),
            onClick = { numberOfClicks++ },
            onLongClick = { numberOfLongClicks++ },
            onSwipeDismiss = { dismissDirection ->
                when (dismissDirection.toLayoutAdjustedDirection((layoutDirection))) {
                    StartToEnd -> numberOfFavorited++
                    EndToStart -> numberOfDeleted++
                }
            },
        ) {
            PreviewItem(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                textData = "(deletions: $numberOfDeleted, favorited: $numberOfFavorited, clicks: $numberOfClicks, long clicks: $numberOfLongClicks)",
            )
        }

        if (numberOfDeleted > 0 || numberOfFavorited > 0) {
            LaunchedEffect(numberOfDeleted + numberOfFavorited) {
                // Reset after a while so we can keep clicking, swiping, etc
                delay(300)
                swipeState.reset()
            }
        }
    }
}

@Composable
private fun PreviewItem(
    modifier: Modifier = Modifier,
    textData: String,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Swipe or click",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Text(
            text = textData,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}