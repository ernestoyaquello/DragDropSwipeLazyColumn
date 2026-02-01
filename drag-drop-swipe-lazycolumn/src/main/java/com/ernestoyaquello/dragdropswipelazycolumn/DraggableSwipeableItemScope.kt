package com.ernestoyaquello.dragdropswipelazycolumn

import androidx.annotation.FloatRange
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyScopeMarker
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import com.ernestoyaquello.dragdropswipelazycolumn.state.DragDropSwipeLazyColumnState
import com.ernestoyaquello.dragdropswipelazycolumn.state.DraggableSwipeableItemState

@LazyScopeMarker
class DraggableSwipeableItemScope<TItem> internal constructor(
    val itemState: DraggableSwipeableItemState,
    internal val currentIndex: Int,
    internal val listState: DragDropSwipeLazyColumnState,
    internal val contentStartPadding: Dp,
    internal val contentEndPadding: Dp,
    internal val lazyItemScope: LazyItemScope,
) : LazyItemScope {

    internal var dragDropModifier by mutableStateOf<Modifier>(Modifier)

    /**
     * This modifier should be applied to whatever element within the item will be used for the user
     * to drag the item. Usually, it'll be a drag handle icon, but it can be anything (you do you!).
     *
     * Note that you don't need to remove this modifier if you want to disable the drag-and-drop
     * functionality, as it will do nothing by default whenever "dragDropEnabled" is set to false
     * within [DraggableSwipeableItem].
     */
    fun Modifier.dragDropModifier() = then(dragDropModifier)

    /**
     * This modifier animates the item appearance (fade in), disappearance (fade out) and placement
     * changes (such as an item reordering). For it to work properly, it must be applied to the
     * top-most modifier of [DraggableSwipeableItem].
     *
     * @param fadeInSpec The animation specs to use for animating the item appearance. Null means no
     *   appearance animation.
     * @param placementSpecDefault The animation specs that will be used to animate the item
     *   placement when no dragging is taking place. Null means no default placement animation when
     *   no item is being dragged.
     * @param placementSpecWhenAnyItemIsBeingDragged The animation specs that will be used to
     *   animate the item placement when an item is being dragged. Null means no placement animation
     *   when an item is being dragged.
     * @param fadeOutSpec The animation specs to use for animating the item disappearance. Null
     *   means no disappearance animation.
     */
    fun Modifier.animateDraggableSwipeableItem(
        fadeInSpec: FiniteAnimationSpec<Float>? = spring(
            stiffness = Spring.StiffnessMediumLow,
        ),
        // When items are being reordered automatically because the data has changed (and not
        // because the user is dragging one of them), we default to the default placement animation.
        placementSpecDefault: FiniteAnimationSpec<IntOffset>? = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
            visibilityThreshold = IntOffset.VisibilityThreshold,
        ),
        // If an item is being dragged, placement animations will happen as a result of the dragged
        // item swapping positions with other items. In this case, we'll want the reordering
        // animation to be quick and only slightly bouncy by default.
        placementSpecWhenAnyItemIsBeingDragged: FiniteAnimationSpec<IntOffset>? = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium,
            visibilityThreshold = IntOffset.VisibilityThreshold,
        ),
        fadeOutSpec: FiniteAnimationSpec<Float>? = spring(
            stiffness = Spring.StiffnessMediumLow,
        ),
    ): Modifier = then(
        // We want to animate each item, except the one that is being dragged or that is currently
        // moving to its place after being dropped, as we don't want to interfere with the dragging
        // and dropping animations we are manually applying.
        other = if (!itemState.isBeingDragged && itemState.offsetTargetInPx == 0f) {
            val isUserDraggingAnotherItem = listState.draggedItemKey != null
            val placementSpec = if (isUserDraggingAnotherItem) {
                placementSpecWhenAnyItemIsBeingDragged
            } else {
                placementSpecDefault
            }
            @Suppress("DEPRECATION")
            Modifier.animateItem(
                fadeInSpec = fadeInSpec,
                placementSpec = placementSpec,
                fadeOutSpec = fadeOutSpec,
            )
        } else {
            Modifier
        },
    )

    @Deprecated(
        message = "This method is deprecated for draggable items. Use animateDraggableSwipeableItem() instead.",
        replaceWith = ReplaceWith("animateDraggableSwipeableItem()"),
    )
    override fun Modifier.animateItem(
        fadeInSpec: FiniteAnimationSpec<Float>?,
        placementSpec: FiniteAnimationSpec<IntOffset>?,
        fadeOutSpec: FiniteAnimationSpec<Float>?,
    ): Modifier = with(lazyItemScope) {
        animateItem(
            fadeInSpec = fadeInSpec,
            placementSpec = placementSpec,
            fadeOutSpec = fadeOutSpec,
        )
    }

    override fun Modifier.fillParentMaxHeight(
        @FloatRange(from = 0.0, to = 1.0) fraction: Float,
    ): Modifier = with(lazyItemScope) { fillParentMaxHeight(fraction) }

    override fun Modifier.fillParentMaxSize(
        @FloatRange(from = 0.0, to = 1.0) fraction: Float,
    ): Modifier = with(lazyItemScope) { fillParentMaxSize(fraction) }

    override fun Modifier.fillParentMaxWidth(
        @FloatRange(from = 0.0, to = 1.0) fraction: Float,
    ): Modifier = with(lazyItemScope) { fillParentMaxWidth(fraction) }
}