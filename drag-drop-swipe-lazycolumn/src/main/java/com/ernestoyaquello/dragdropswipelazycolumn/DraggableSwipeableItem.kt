package com.ernestoyaquello.dragdropswipelazycolumn

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Indication
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalDragOrCancellation
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.zIndex
import com.ernestoyaquello.dragdropswipelazycolumn.AllowedSwipeDirections.All
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Composable that can be used to a create draggable, swipeable item.
 *
 * To make an element within the item (e.g., a drag icon) able to start the dragging,
 * you must assign it the modifier dragDropModifier().
 *
 * This item MUST be used as the root item content's Composable within a [DragDropSwipeLazyColumn].
 */
@Composable
fun <TItem> DraggableSwipeableItemScope<TItem>.DraggableSwipeableItem(
    modifier: Modifier = Modifier,
    colors: DraggableSwipeableItemColors = DraggableSwipeableItemDefaults.colors(),
    shapes: SwipeableItemShapes = SwipeableItemDefaults.shapes(),
    icons: SwipeableItemIcons = SwipeableItemDefaults.icons(),
    minHeight: Dp = SwipeableItemDefaults.minHeight,
    minSwipeHorizontality: Float? = DraggableSwipeableItemDefaults.minSwipeHorizontality,
    allowedSwipeDirections: AllowedSwipeDirections = All,
    dragDropEnabled: Boolean = true,
    applyShadowElevationWhenDragged: Boolean = true,
    shadowElevationWhenDragged: Dp = DraggableSwipeableItemDefaults.shadowElevationWhenDragged,
    clickIndication: Indication? = ripple(color = colors.clickIndicationColor ?: Color.Unspecified),
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onDragStart: (dragDeltaInPx: Float) -> Unit = remember { {} },
    onDragUpdate: (dragDeltaInPx: Float) -> Unit = remember { {} },
    onDragFinish: () -> Unit = remember { {} },
    onSwipeGestureStart: (swipeDeltaInPx: Float) -> Unit = remember { {} },
    onSwipeGestureUpdate: (swipeDeltaInPx: Float, pressed: Boolean) -> Unit = remember { { _, _ -> } },
    onSwipeGestureFinish: () -> Unit = remember { {} },
    onSwipeDismiss: (DismissSwipeDirection) -> Unit = remember { {} },
    content: @Composable BoxScope.() -> Unit,
) {
    // Set the allowed swiping directions and ensure that the swipe functionality gets temporarily
    // disabled while the user is dragging any item.
    itemState.swipeableItemState.update {
        copy(
            allowedSwipeDirections = allowedSwipeDirections,
            forceDisableSwipe = listState.draggedItemKey != null,
        )
    }

    // Apply a shadow when the item is being dragged, ensuring to account for the horizontal padding
    val localDensity = LocalDensity.current
    var widthInPx by remember { mutableFloatStateOf(0f) }
    var height by remember { mutableStateOf(0.dp) }

    val containerHorizontalPaddingInPx = remember(
        localDensity,
        contentStartPadding,
        contentEndPadding,
    ) {
        with(localDensity) { (contentStartPadding + contentEndPadding).toPx() }
    }
    val shadowScaleX = remember(widthInPx, containerHorizontalPaddingInPx) {
        if (widthInPx > 0f) {
            (widthInPx - (containerHorizontalPaddingInPx * 2f)) / widthInPx
        } else {
            1f
        }
    }
    val animatedShadowElevation by animateDpAsState(
        targetValue = when {
            itemState.isBeingDragged -> shadowElevationWhenDragged
            itemState.animatedOffsetInPx.value != 0f -> shadowElevationWhenDragged * 0.5f
            else -> 0.dp
        },
    )
    val animatedContainerBackgroundColor by animateColorAsState(
        targetValue = if (itemState.isBeingDragged) {
            colors.containerBackgroundColorWhileDragged ?: Color.Unspecified
        } else {
            colors.containerBackgroundColor ?: Color.Unspecified
        },
    )
    val isUserSwipingOrDragging = listState.draggedItemKey != null ||
            listState.swipedItemKeys.isNotEmpty()
    val isUserDraggingAnotherItem = listState.draggedItemKey != null &&
            listState.draggedItemKey != itemState.itemKey

    SwipeableItem(
        modifier = modifier
            .onSizeChanged {
                widthInPx = it.width.toFloat()
                height = with(localDensity) { it.height.toDp() }
            }
            .defaultMinSize(
                minHeight = minHeight,
            )
            .offset {
                IntOffset(
                    x = 0,
                    y = if (itemState.isBeingDragged) {
                        // The user is dragging the item, so we want to apply the offset immediately
                        // to ensure the user's pointer input is followed as quickly as possible and
                        // without potential animation delays (even though we use "snap to" on the
                        // offset animatable when the item is being dragged, there might be a tiny
                        // delay before the "snap to" action is invoked at all, so here we just
                        // reference the offset directly, as it is guaranteed to be up to date).
                        itemState.offsetTargetInPx.roundToInt()
                    } else {
                        // Otherwise, just apply the animated offset normally
                        itemState.animatedOffsetInPx.value.roundToInt()
                    },
                )
            }
            .zIndex(
                zIndex = when {
                    itemState.isBeingDragged -> 2f
                    itemState.animatedOffsetInPx.value != 0f -> 1f
                    else -> 0f
                },
            )
            .then(
                other = if (applyShadowElevationWhenDragged) {
                    // Scale down before drawing the shadow to ensure the horizontal padding is
                    // applied, then scale back up so that the final size of the item is unchanged.
                    Modifier
                        .scale(
                            scaleX = shadowScaleX,
                            scaleY = 1f,
                        )
                        .shadow(
                            elevation = animatedShadowElevation,
                            shape = shapes.containerBackgroundShape,
                            clip = false,
                        )
                        .scale(
                            scaleX = 1 / shadowScaleX,
                            scaleY = 1f,
                        )
                } else {
                    Modifier
                },
            ),
        state = itemState.swipeableItemState,
        colors = colors.swipeableItemColors.copy(
            containerBackgroundColor = animatedContainerBackgroundColor,
        ),
        shapes = shapes,
        icons = icons,
        minSwipeHorizontality = minSwipeHorizontality,
        minHeight = max(minHeight, height),
        contentStartPadding = contentStartPadding,
        contentEndPadding = contentEndPadding,
        clickIndication = clickIndication?.takeUnless { itemState.isBeingDragged },
        onClick = onClick?.takeUnless { isUserSwipingOrDragging },
        onLongClick = onLongClick?.takeUnless { isUserSwipingOrDragging },
        onSwipeGestureStart = onSwipeGestureStart,
        onSwipeGestureUpdate = onSwipeGestureUpdate,
        onSwipeGestureFinish = onSwipeGestureFinish,
        onSwipeDismiss = onSwipeDismiss,
    ) {
        // Ensure the drag-drop modifier is ready before the item is drawn, then draw the item
        dragDropModifier = if (dragDropEnabled && !isUserDraggingAnotherItem) {
            val adjustedMinSwipeHorizontality = minSwipeHorizontality?.takeUnless { it == 0f }
            Modifier.pointerInput(
                itemState,
                adjustedMinSwipeHorizontality,
                onDragStart,
                onDragUpdate,
                onDragFinish,
            ) {
                awaitEachGesture {
                    handleDragDropGestures(
                        itemState = itemState,
                        minSwipeHorizontality = adjustedMinSwipeHorizontality,
                        onDragStart = onDragStart,
                        onDragUpdate = onDragUpdate,
                        onDragFinish = onDragFinish,
                    )
                }
            }
        } else {
            Modifier
        }
        content()
    }
}

private suspend fun AwaitPointerEventScope.handleDragDropGestures(
    itemState: DraggableSwipeableItemState,
    minSwipeHorizontality: Float?,
    onDragStart: (dragDeltaInPx: Float) -> Unit,
    onDragUpdate: (dragDeltaInPx: Float) -> Unit,
    onDragFinish: () -> Unit,
) {
    val handleDrag: (PointerInputChange) -> Unit = { drag ->
        val dragDelta = drag.position.y - drag.previousPosition.y
        drag.consume()
        itemState.update {
            copy(
                isBeingDragged = true,
                offsetTargetInPx = offsetTargetInPx + dragDelta,
            )
        }
    }

    // Detect the drag gesture by listening for the first touch event that goes over the slop
    val down = awaitFirstDown()
    var drag = awaitVerticalTouchSlopOrCancellation(pointerId = down.id) { potentialDrag, _ ->
        val potentialDragDelta = potentialDrag.position.y - potentialDrag.previousPosition.y
        val horizontalDelta = potentialDrag.position.x - potentialDrag.previousPosition.x
        val verticalSlope = if (horizontalDelta != 0f) {
            abs(potentialDragDelta / horizontalDelta)
        } else {
            Float.POSITIVE_INFINITY
        }

        // Ensure only somewhat vertical drags are considered valid, that way we avoid interfering
        // with potential horizontal swipes and other gestures.
        if (minSwipeHorizontality == null || verticalSlope >= (1f / minSwipeHorizontality)) {
            down.consume()
            itemState.update { copy(isBeingDragged = true) }
            handleDrag(potentialDrag)
            onDragStart(potentialDragDelta)
        }
    }

    // If we detect it, we need to keep listening for the rest of the drag gesture
    while (drag?.pressed == true) {
        drag = awaitVerticalDragOrCancellation(pointerId = drag.id)
        drag?.let {
            if (drag.pressed) {
                handleDrag(drag)

                val dragDelta = drag.position.y - drag.previousPosition.y
                onDragUpdate(dragDelta)
            }
        }
    }

    // Finally, the dragging has ended
    if (itemState.isBeingDragged) {
        itemState.update {
            copy(
                isBeingDragged = false,
                offsetTargetInPx = 0f,
            )
        }
    }

    onDragFinish()
}