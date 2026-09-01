package com.ernestoyaquello.dragdropswipelazycolumn

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isNotFocusable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ernestoyaquello.dragdropswipelazycolumn.AllowedSwipeDirections.All
import com.ernestoyaquello.dragdropswipelazycolumn.AllowedSwipeDirections.None
import com.ernestoyaquello.dragdropswipelazycolumn.AllowedSwipeDirections.OnlyLeftToRight
import com.ernestoyaquello.dragdropswipelazycolumn.DismissSwipeDirection.LeftToRight
import com.ernestoyaquello.dragdropswipelazycolumn.DismissSwipeDirection.RightToLeft
import com.ernestoyaquello.dragdropswipelazycolumn.state.DragDropSwipeLazyColumnState
import com.ernestoyaquello.dragdropswipelazycolumn.state.rememberDragDropSwipeLazyColumnState
import com.ernestoyaquello.dragdropswipelazycolumn.state.rememberSwipeableItemState
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class DragDropSwipeLazyColumnTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun clickAndLongClickExposeLabelsAndInvokeTheirCallbacks() {
        val clickCount = AtomicInteger()
        val longClickCount = AtomicInteger()

        composeRule.setContent {
            SwipeableItem(
                modifier = Modifier
                    .width(240.dp)
                    .height(ItemHeight)
                    .testTag(StandaloneItemTag),
                state = rememberSwipeableItemState(initialAllowedSwipeDirections = None),
                onClickLabel = "Mark item",
                onLongClickLabel = "Delete item",
                onClick = { clickCount.incrementAndGet() },
                onLongClick = { longClickCount.incrementAndGet() },
                onSwipeDismiss = {},
            ) {
                Text(
                    modifier = Modifier.testTag(StandaloneItemContentTag),
                    text = "Milk",
                )
            }
        }

        val item = composeRule.onNodeWithTag(StandaloneItemTag)
        item.assert(hasClickActionLabel("Mark item"))
        item.assert(hasLongClickActionLabel("Delete item"))
        item.assertTextContains("Milk")
        composeRule.onNodeWithTag(StandaloneItemContentTag).assertDoesNotExist()

        item.performSemanticsAction(SemanticsActions.OnClick)
        item.performSemanticsAction(SemanticsActions.OnLongClick)

        assertEquals(1, clickCount.get())
        assertEquals(1, longClickCount.get())
    }

    @Test
    fun dismissActionsOnlyExposeAllowedDirectionsAndInvokeTheMatchingDirection() {
        val dismissedDirection = AtomicReference<DismissSwipeDirection?>()

        composeRule.setContent {
            Column {
                SwipeableItem(
                    modifier = Modifier
                        .width(240.dp)
                        .height(ItemHeight)
                        .testTag(EnabledDismissItemTag),
                    state = rememberSwipeableItemState(
                        initialAllowedSwipeDirections = OnlyLeftToRight,
                    ),
                    dismissLeftToRightActionLabel = "Archive item",
                    dismissRightToLeftActionLabel = "Delete item",
                    onSwipeDismiss = dismissedDirection::set,
                ) {
                    Text(
                        modifier = Modifier.testTag(EnabledDismissItemContentTag),
                        text = "Enabled",
                    )
                }

                SwipeableItem(
                    modifier = Modifier
                        .width(240.dp)
                        .height(ItemHeight)
                        .testTag(DisabledDismissItemTag),
                    state = rememberSwipeableItemState(initialAllowedSwipeDirections = None),
                    dismissLeftToRightActionLabel = "Archive item",
                    dismissRightToLeftActionLabel = "Delete item",
                    onSwipeDismiss = dismissedDirection::set,
                ) {
                    Text("Disabled")
                }
            }
        }

        val enabledItem = composeRule.onNodeWithTag(EnabledDismissItemTag)
        enabledItem.assert(hasCustomAction("Archive item"))
        enabledItem.assert(doesNotHaveCustomAction("Delete item"))
        enabledItem.assertTextContains("Enabled")
        composeRule.onNodeWithTag(EnabledDismissItemContentTag).assertDoesNotExist()
        composeRule.onNodeWithTag(DisabledDismissItemTag)
            .assert(doesNotHaveCustomAction("Archive item"))
            .assert(doesNotHaveCustomAction("Delete item"))

        enabledItem.performCustomAccessibilityActionWithLabel("Archive item")

        assertEquals(LeftToRight, dismissedDirection.get())
    }

    @Test
    fun itemWithoutItemLevelActionsKeepsDescendantSemanticsSeparate() {
        composeRule.setContent {
            SwipeableItem(
                modifier = Modifier
                    .width(240.dp)
                    .height(ItemHeight),
                state = rememberSwipeableItemState(initialAllowedSwipeDirections = None),
                onSwipeDismiss = {},
            ) {
                Column {
                    Text(
                        modifier = Modifier.testTag(FirstChildTag),
                        text = "Title",
                    )
                    Text(
                        modifier = Modifier.testTag(SecondChildTag),
                        text = "Details",
                    )
                }
            }
        }

        composeRule.onNodeWithTag(FirstChildTag)
            .assertExists()
            .assertTextContains("Title")
        composeRule.onNodeWithTag(SecondChildTag)
            .assertExists()
            .assertTextContains("Details")
    }

    @Test
    fun nestedSwipeableItemDoesNotInheritItsParentMoveActions() {
        composeRule.setContent {
            DragDropSwipeLazyColumn(
                modifier = Modifier
                    .width(240.dp)
                    .height(240.dp),
                items = TestItems,
                key = TestItem::id,
                onItemsReordered = {},
            ) { _, item ->
                DraggableSwipeableItem(
                    modifier = Modifier
                        .height(ItemHeight)
                        .testTag(itemTag(item.id)),
                    allowedSwipeDirections = None,
                    applyShadowElevationWhenDragged = false,
                    moveUpActionLabel = "Move ${item.label} up",
                    moveDownActionLabel = "Move ${item.label} down",
                ) {
                    SwipeableItem(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(nestedItemTag(item.id)),
                        state = rememberSwipeableItemState(initialAllowedSwipeDirections = None),
                        onSwipeDismiss = {},
                    ) {
                        Text(item.label)
                    }
                }
            }
        }

        composeRule.onNodeWithTag(itemTag(2), useUnmergedTree = true)
            .assert(hasCustomAction("Move B up"))
            .assert(hasCustomAction("Move B down"))
        composeRule.onNodeWithTag(nestedItemTag(2), useUnmergedTree = true)
            .assert(doesNotHaveCustomAction("Move B up"))
            .assert(doesNotHaveCustomAction("Move B down"))
    }

    @Test
    fun semanticMoveReportsTheCompleteOrderAndOmitsUnavailableBoundaryAction() {
        val reportedOrder = AtomicReference<List<Int>?>()

        composeRule.setContent {
            var items: ImmutableList<TestItem> by remember { mutableStateOf(TestItems) }
            ReorderableTestList(
                items = items,
                onItemsReordered = { reorderedItems ->
                    reportedOrder.set(reorderedItems.map(TestItem::id))
                    items = reorderedItems
                },
            )
        }

        composeRule.onNodeWithTag(itemTag(2))
            .performCustomAccessibilityActionWithLabel("Move B up")

        assertEquals(listOf(2, 1, 3), reportedOrder.get())
        composeRule.onNodeWithTag(itemTag(2))
            .assert(doesNotHaveCustomAction("Move B up"))
            .assert(hasCustomAction("Move B down"))
    }

    @Test
    fun semanticMoveReportsChangedIndicesThroughTheIndicesCallback() {
        val reportedChanges = AtomicReference<List<OrderedItem<TestItem>>?>()

        composeRule.setContent {
            IndicesReorderableTestList(
                items = TestItems,
                onIndicesChangedViaDragAndDrop = reportedChanges::set,
            )
        }

        composeRule.onNodeWithTag(itemTag(2))
            .performCustomAccessibilityActionWithLabel("Move B up")

        val changes = requireNotNull(reportedChanges.get())
        assertEquals(listOf(2, 1), changes.map { it.value.id })
        assertEquals(listOf(1, 0), changes.map { it.initialIndex })
        assertEquals(listOf(0, 1), changes.map { it.newIndex })
    }

    @Test
    fun altArrowUsesTheSameDiscreteMovePath() {
        val reportedOrder = AtomicReference<List<Int>?>()

        composeRule.setContent {
            var items: ImmutableList<TestItem> by remember { mutableStateOf(TestItems) }
            ReorderableTestList(
                items = items,
                keyboardReorderEnabled = true,
                onItemsReordered = { reorderedItems ->
                    reportedOrder.set(reorderedItems.map(TestItem::id))
                    items = reorderedItems
                },
            )
        }

        composeRule.onNodeWithTag(itemTag(1))
            .requestFocus()
            .performKeyInput {
                withKeyDown(Key.AltLeft) {
                    pressKey(Key.DirectionDown)
                }
            }

        assertEquals(listOf(2, 1, 3), reportedOrder.get())
    }

    @Test
    fun altArrowWorksWithoutAccessibilityActionLabels() {
        val reportedOrder = AtomicReference<List<Int>?>()

        composeRule.setContent {
            var items: ImmutableList<TestItem> by remember { mutableStateOf(TestItems) }
            ReorderableTestList(
                items = items,
                keyboardReorderEnabled = true,
                moveActionLabelsEnabled = false,
                onItemsReordered = { reorderedItems ->
                    reportedOrder.set(reorderedItems.map(TestItem::id))
                    items = reorderedItems
                },
            )
        }

        composeRule.onNodeWithTag(itemTag(1))
            .assert(doesNotHaveCustomAction("Move A down"))
            .requestFocus()
            .performKeyInput {
                withKeyDown(Key.AltLeft) {
                    pressKey(Key.DirectionDown)
                }
            }

        assertEquals(listOf(2, 1, 3), reportedOrder.get())
    }

    @Test
    fun keyboardFocusIsOmittedWhenNoMoveIsAvailable() {
        composeRule.setContent {
            ReorderableTestList(
                items = persistentListOf(TestItem(id = 1, label = "Only")),
                keyboardReorderEnabled = true,
                onItemsReordered = {},
            )
        }

        composeRule.onNodeWithTag(itemTag(1)).assert(isNotFocusable())
    }

    @Test
    fun disabledDragOmitsMoveActionsAndKeyboardFocus() {
        composeRule.setContent {
            ReorderableTestList(
                items = TestItems,
                keyboardReorderEnabled = true,
                dragDropEnabled = false,
                onItemsReordered = {},
            )
        }

        composeRule.onNodeWithTag(itemTag(2))
            .assert(doesNotHaveCustomAction("Move B up"))
            .assert(doesNotHaveCustomAction("Move B down"))
            .assert(isNotFocusable())
    }

    @Test
    fun visualMoveDirectionAccountsForReverseLayout() {
        val reportedOrder = AtomicReference<List<Int>?>()

        composeRule.setContent {
            var items: ImmutableList<TestItem> by remember { mutableStateOf(TestItems) }
            ReorderableTestList(
                items = items,
                reverseLayout = true,
                onItemsReordered = { reorderedItems ->
                    reportedOrder.set(reorderedItems.map(TestItem::id))
                    items = reorderedItems
                },
            )
        }

        composeRule.onNodeWithTag(itemTag(2))
            .performCustomAccessibilityActionWithLabel("Move B up")

        assertEquals(listOf(1, 3, 2), reportedOrder.get())
    }

    @Test
    fun stableKeyStillTargetsTheItemAfterItsValueChanges() {
        val reportedOrder = AtomicReference<List<Int>?>()
        val refreshItems = AtomicReference<(() -> Unit)?>(null)

        composeRule.setContent {
            var items: ImmutableList<TestItem> by remember { mutableStateOf(TestItems) }
            refreshItems.set {
                items = items
                    .map { item -> item.copy(label = "${item.label} refreshed") }
                    .toImmutableList()
            }
            ReorderableTestList(
                items = items,
                onItemsReordered = { reorderedItems ->
                    reportedOrder.set(reorderedItems.map(TestItem::id))
                    items = reorderedItems
                },
            )
        }

        composeRule.runOnIdle {
            requireNotNull(refreshItems.get()).invoke()
        }
        composeRule.onNodeWithTag(itemTag(2)).assertTextContains("B refreshed")
        composeRule.onNodeWithTag(itemTag(2))
            .performCustomAccessibilityActionWithLabel("Move B refreshed up")

        assertEquals(listOf(2, 1, 3), reportedOrder.get())
        composeRule.onNodeWithTag(itemTag(2)).assertTextContains("B refreshed")
    }

    @Test
    fun semanticMoveUsesTheLatestListAfterAnItemIsAppended() {
        val reportedOrder = AtomicReference<List<Int>?>()
        val appendItem = AtomicReference<(() -> Unit)?>(null)

        composeRule.setContent {
            var items: ImmutableList<TestItem> by remember { mutableStateOf(TestItems) }
            appendItem.set {
                items = (items + TestItem(id = 4, label = "D")).toImmutableList()
            }
            ReorderableTestList(
                items = items,
                onItemsReordered = { reorderedItems ->
                    reportedOrder.set(reorderedItems.map(TestItem::id))
                    items = reorderedItems
                },
            )
        }

        composeRule.runOnIdle {
            requireNotNull(appendItem.get()).invoke()
        }
        composeRule.onNodeWithTag(itemTag(3))
            .performCustomAccessibilityActionWithLabel("Move C down")

        assertEquals(listOf(1, 2, 4, 3), reportedOrder.get())
    }

    @Test
    fun semanticMoveKeepsTheMovedItemVisibleAtTheViewportEdge() {
        val reportedOrder = AtomicReference<List<Int>?>()

        composeRule.setContent {
            var items: ImmutableList<TestItem> by remember { mutableStateOf(TestItems) }
            ReorderableTestList(
                items = items,
                listHeight = ItemHeight * 2,
                onItemsReordered = { reorderedItems ->
                    reportedOrder.set(reorderedItems.map(TestItem::id))
                    items = reorderedItems
                },
            )
        }

        composeRule.onNodeWithTag(itemTag(2))
            .performCustomAccessibilityActionWithLabel("Move B down")

        assertEquals(listOf(1, 3, 2), reportedOrder.get())
        val listBounds = composeRule.onNodeWithTag(ReorderableListTag).getUnclippedBoundsInRoot()
        val itemBounds = composeRule.onNodeWithTag(itemTag(2)).getUnclippedBoundsInRoot()
        assertTrue(
            "The moved item should be completely inside the list viewport",
            itemBounds.left >= listBounds.left &&
                    itemBounds.top >= listBounds.top &&
                    itemBounds.right <= listBounds.right &&
                    itemBounds.bottom <= listBounds.bottom,
        )
    }

    @Test
    fun semanticMoveKeepsTheMovedItemVisibleWithReverseLayout() {
        val reportedOrder = AtomicReference<List<Int>?>()

        composeRule.setContent {
            var items: ImmutableList<TestItem> by remember { mutableStateOf(TestItems) }
            ReorderableTestList(
                items = items,
                listHeight = ItemHeight * 2,
                reverseLayout = true,
                onItemsReordered = { reorderedItems ->
                    reportedOrder.set(reorderedItems.map(TestItem::id))
                    items = reorderedItems
                },
            )
        }

        composeRule.onNodeWithTag(itemTag(2))
            .performCustomAccessibilityActionWithLabel("Move B up")

        assertEquals(listOf(1, 3, 2), reportedOrder.get())
        val listBounds = composeRule.onNodeWithTag(ReorderableListTag).getUnclippedBoundsInRoot()
        val itemBounds = composeRule.onNodeWithTag(itemTag(2)).getUnclippedBoundsInRoot()
        assertTrue(
            "The moved item should be completely inside the reversed list viewport",
            itemBounds.left >= listBounds.left &&
                    itemBounds.top >= listBounds.top &&
                    itemBounds.right <= listBounds.right &&
                    itemBounds.bottom <= listBounds.bottom,
        )
    }

    @Test
    fun touchDragCanCrossMoreThanOneItemAndReportsTheCompleteOrder() {
        val reportedOrder = AtomicReference<List<Int>?>()
        val callbackCount = AtomicInteger()

        composeRule.setContent {
            var items: ImmutableList<TestItem> by remember { mutableStateOf(TestItems) }
            ReorderableTestList(
                items = items,
                onItemsReordered = { reorderedItems ->
                    callbackCount.incrementAndGet()
                    reportedOrder.set(reorderedItems.map(TestItem::id))
                    items = reorderedItems
                },
            )
        }

        var itemHeightInPx = 0f
        composeRule.onNodeWithTag(dragHandleTag(1), useUnmergedTree = true)
            .performTouchInput {
                itemHeightInPx = height.toFloat()
                down(center)
                moveBy(Offset(x = 0f, y = itemHeightInPx * 1.1f))
            }

        // Send the second move as a separate batch. That gives the lazy list time to lay out the
        // first swap before the dragged item crosses the next item. The fixed list node is used for
        // the remaining events because the dragged item's own node moves during each swap.
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ReorderableListTag).performTouchInput {
            moveBy(Offset(x = 0f, y = itemHeightInPx * 1.1f))
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ReorderableListTag).performTouchInput {
            up()
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            reportedOrder.get() != null
        }
        assertEquals(listOf(2, 3, 1), reportedOrder.get())
        assertEquals(1, callbackCount.get())
    }

    @Test
    fun dragHandleModifierCanBePreparedBeforeDraggableItemContent() {
        val reportedOrder = AtomicReference<List<Int>?>()

        composeRule.setContent {
            var items: ImmutableList<TestItem> by remember { mutableStateOf(TestItems) }
            ReorderableTestList(
                items = items,
                prepareDragHandleModifierEarly = true,
                onItemsReordered = { reorderedItems ->
                    reportedOrder.set(reorderedItems.map(TestItem::id))
                    items = reorderedItems
                },
            )
        }

        startDraggingItemDown(itemId = 1)
        releaseDrag()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            reportedOrder.get() != null
        }
        assertEquals(listOf(2, 1, 3), reportedOrder.get())
    }

    @Test
    fun sourceValueRefreshDuringDragPreservesTheCompletedMove() {
        val itemsState = mutableStateOf<ImmutableList<TestItem>>(TestItems)
        val listStateReference = AtomicReference<DragDropSwipeLazyColumnState?>()
        val reportedItems = AtomicReference<List<TestItem>?>()
        val draggedItemKeySeenByCallback = AtomicReference<Any?>()
        val callbackCount = AtomicInteger()

        composeRule.setContent {
            val listState = rememberDragDropSwipeLazyColumnState()
            listStateReference.set(listState)
            ReorderableTestList(
                state = listState,
                items = itemsState.value,
                onItemsReordered = { reorderedItems ->
                    callbackCount.incrementAndGet()
                    draggedItemKeySeenByCallback.set(listState.draggedItemKey)
                    reportedItems.set(reorderedItems)
                    itemsState.value = reorderedItems
                },
            )
        }
        val listState = requireNotNull(listStateReference.get())

        startDraggingItemDown(itemId = 1)

        // Refresh the item values while the pointer is still held. Replacing the source list ends
        // the stale gesture, but the move already made by that gesture must still be reported.
        composeRule.runOnIdle {
            itemsState.value = itemsState.value.map { item ->
                item.copy(label = "${item.label} refreshed")
            }.toImmutableList()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            reportedItems.get() != null && listState.draggedItemKey == null
        }

        releaseDrag()
        composeRule.waitForIdle()

        val result = requireNotNull(reportedItems.get())
        assertEquals(listOf(2, 1, 3), result.map(TestItem::id))
        assertEquals(
            listOf("B refreshed", "A refreshed", "C refreshed"),
            result.map(TestItem::label),
        )
        assertNull(draggedItemKeySeenByCallback.get())
        assertEquals(1, callbackCount.get())
    }

    @Test
    fun sourceValueRefreshDuringSwipeKeepsSwipeOwnershipUntilTheGestureEnds() {
        val itemsState = mutableStateOf<ImmutableList<TestItem>>(TestItems)
        val listStateReference = AtomicReference<DragDropSwipeLazyColumnState?>()
        val swipeFinishCount = AtomicInteger()

        composeRule.setContent {
            val listState = rememberDragDropSwipeLazyColumnState()
            listStateReference.set(listState)
            ReorderableTestList(
                state = listState,
                items = itemsState.value,
                allowedSwipeDirections = All,
                onSwipeGestureFinish = { swipeFinishCount.incrementAndGet() },
                onItemsReordered = {},
            )
        }
        val listState = requireNotNull(listStateReference.get())

        composeRule.onNodeWithTag(itemTag(1)).performTouchInput {
            down(center)
            moveBy(Offset(x = width * 0.25f, y = 0f))
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            1 in listState.swipedItemKeys
        }

        composeRule.runOnIdle {
            itemsState.value = itemsState.value.map { item ->
                item.copy(label = "${item.label} refreshed")
            }.toImmutableList()
        }
        composeRule.onNodeWithText("A refreshed").assertExists()
        assertTrue(1 in listState.swipedItemKeys)

        composeRule.onNodeWithTag(ReorderableListTag).performTouchInput {
            up()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            listState.swipedItemKeys.isEmpty() && swipeFinishCount.get() == 1
        }

        assertEquals(1, swipeFinishCount.get())
    }

    @Test
    fun structuralSourceChangeClearsThePendingReorder() {
        val itemsState = mutableStateOf<ImmutableList<TestItem>>(TestItems)
        val listStateReference = AtomicReference<DragDropSwipeLazyColumnState?>()
        val callbackCount = AtomicInteger()

        composeRule.setContent {
            val listState = rememberDragDropSwipeLazyColumnState()
            listStateReference.set(listState)
            ReorderableTestList(
                state = listState,
                items = itemsState.value,
                onItemsReordered = { callbackCount.incrementAndGet() },
            )
        }
        val listState = requireNotNull(listStateReference.get())

        composeRule.onNodeWithTag(dragHandleTag(1), useUnmergedTree = true)
            .performTouchInput {
                down(center)
                moveBy(Offset(x = 0f, y = height * 1.1f))
            }
        composeRule.waitForIdle()

        // Adding an item makes the caller's list a structurally different source of truth. The
        // abandoned internal swap must not influence later updates containing these same keys.
        val extraItem = TestItem(id = 4, label = "D")
        composeRule.runOnIdle {
            itemsState.value = persistentListOf(
                TestItems[0],
                TestItems[1],
                TestItems[2],
                extraItem,
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ReorderableListTag).performTouchInput {
            up()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            listState.draggedItemKey == null
        }

        composeRule.runOnIdle {
            itemsState.value = persistentListOf(TestItems[2], TestItems[1], TestItems[0], extraItem)
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(itemTag(3))
            .assert(doesNotHaveCustomAction("Move C up"))
        assertEquals(0, callbackCount.get())
    }

    @Test
    fun draggingAnotherItemCancelsAnActiveSwipeWithoutLeavingTheListLocked() {
        val listStateReference = AtomicReference<DragDropSwipeLazyColumnState?>()
        val swipeFinishCount = AtomicInteger()

        composeRule.setContent {
            val listState = rememberDragDropSwipeLazyColumnState()
            listStateReference.set(listState)
            ReorderableTestList(
                state = listState,
                items = TestItems,
                listHeight = ItemHeight * 2,
                allowedSwipeDirections = All,
                onSwipeGestureFinish = { swipeFinishCount.incrementAndGet() },
                onItemsReordered = {},
            )
        }
        val listState = requireNotNull(listStateReference.get())

        // Keep the first pointer down after crossing horizontal touch slop so its swipe remains
        // active while a second pointer starts dragging another item.
        composeRule.onNodeWithTag(itemTag(1)).performTouchInput {
            down(pointerId = 0, position = center)
            moveBy(pointerId = 0, delta = Offset(x = width * 0.25f, y = 0f))
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            1 in listState.swipedItemKeys
        }

        composeRule.onNodeWithTag(dragHandleTag(2), useUnmergedTree = true).performTouchInput {
            down(pointerId = 1, position = center)
            moveBy(pointerId = 1, delta = Offset(x = 0f, y = height * 0.4f))
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            listState.draggedItemKey == 2 &&
                    listState.swipedItemKeys.isEmpty() &&
                    swipeFinishCount.get() == 1
        }

        // Release both pointers from the stable list node because either item can move or
        // recompose while its gesture is being cancelled.
        composeRule.onNodeWithTag(ReorderableListTag).performTouchInput {
            up(pointerId = 1)
            up(pointerId = 0)
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            listState.draggedItemKey == null && listState.swipedItemKeys.isEmpty()
        }

        assertNull(listState.draggedItemKey)
        assertTrue(listState.swipedItemKeys.isEmpty())
        assertEquals(1, swipeFinishCount.get())
    }

    @Test
    fun touchSwipeStillDismissesAndThenRemovesItsDismissAction() {
        val dismissedDirection = AtomicReference<DismissSwipeDirection?>()

        composeRule.setContent {
            SwipeableItem(
                modifier = Modifier
                    .width(240.dp)
                    .height(ItemHeight)
                    .testTag(StandaloneItemTag),
                onClickLabel = "Open item",
                onLongClickLabel = "Edit item",
                dismissLeftToRightActionLabel = "Archive item",
                dismissRightToLeftActionLabel = "Delete item",
                onClick = {},
                onLongClick = {},
                onSwipeDismiss = dismissedDirection::set,
            ) {
                Text("Swipe me")
            }
        }

        composeRule.onNodeWithTag(StandaloneItemTag).performTouchInput {
            swipeLeft()
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            dismissedDirection.get() != null
        }
        assertEquals(RightToLeft, dismissedDirection.get())
        composeRule.onNodeWithTag(StandaloneItemTag)
            .assert(doesNotHaveClickAction())
            .assert(doesNotHaveLongClickAction())
            .assert(doesNotHaveCustomAction("Archive item"))
            .assert(doesNotHaveCustomAction("Delete item"))
    }

    @Test
    fun narrowSwipeableItemCanRevealBehindContentWithoutCrashing() {
        composeRule.setContent {
            SwipeableItem(
                modifier = Modifier
                    .width(56.dp)
                    .height(ItemHeight)
                    .testTag(NarrowItemTag),
                onSwipeDismiss = {},
            ) {
                Text("Narrow item")
            }
        }

        composeRule.onNodeWithTag(NarrowItemTag).performTouchInput {
            swipeRight()
        }

        composeRule.onNodeWithTag(NarrowItemTag).assertExists()
    }

    @Composable
    private fun ReorderableTestList(
        items: ImmutableList<TestItem>,
        state: DragDropSwipeLazyColumnState = rememberDragDropSwipeLazyColumnState(),
        listHeight: Dp = 240.dp,
        reverseLayout: Boolean = false,
        keyboardReorderEnabled: Boolean = false,
        dragDropEnabled: Boolean = true,
        allowedSwipeDirections: AllowedSwipeDirections = None,
        onDragFinish: () -> Unit = {},
        onSwipeGestureFinish: () -> Unit = {},
        moveActionLabelsEnabled: Boolean = true,
        prepareDragHandleModifierEarly: Boolean = false,
        onItemsReordered: (ImmutableList<TestItem>) -> Unit,
    ) {
        DragDropSwipeLazyColumn(
            modifier = Modifier
                .width(240.dp)
                .height(listHeight)
                .testTag(ReorderableListTag),
            state = state,
            items = items,
            key = TestItem::id,
            reverseLayout = reverseLayout,
            onItemsReordered = onItemsReordered,
        ) { _, item ->
            ReorderableTestItem(
                item = item,
                dragDropEnabled = dragDropEnabled,
                allowedSwipeDirections = allowedSwipeDirections,
                onDragFinish = onDragFinish,
                onSwipeGestureFinish = onSwipeGestureFinish,
                keyboardReorderEnabled = keyboardReorderEnabled,
                moveActionLabelsEnabled = moveActionLabelsEnabled,
                prepareDragHandleModifierEarly = prepareDragHandleModifierEarly,
            )
        }
    }

    @Composable
    private fun IndicesReorderableTestList(
        items: ImmutableList<TestItem>,
        onDragFinish: () -> Unit = {},
        onIndicesChangedViaDragAndDrop: (List<OrderedItem<TestItem>>) -> Unit,
    ) {
        DragDropSwipeLazyColumn(
            modifier = Modifier
                .width(240.dp)
                .height(240.dp)
                .testTag(ReorderableListTag),
            items = items,
            key = TestItem::id,
            onIndicesChangedViaDragAndDrop = onIndicesChangedViaDragAndDrop,
        ) { _, item ->
            ReorderableTestItem(
                item = item,
                onDragFinish = onDragFinish,
            )
        }
    }

    @Composable
    private fun DraggableSwipeableItemScope<TestItem>.ReorderableTestItem(
        item: TestItem,
        dragDropEnabled: Boolean = true,
        allowedSwipeDirections: AllowedSwipeDirections = None,
        onDragFinish: () -> Unit = {},
        onSwipeGestureFinish: () -> Unit = {},
        keyboardReorderEnabled: Boolean = false,
        moveActionLabelsEnabled: Boolean = true,
        prepareDragHandleModifierEarly: Boolean = false,
    ) {
        val preparedDragHandleModifier = if (prepareDragHandleModifierEarly) {
            Modifier.preparedDragHandleModifier(this)
        } else {
            Modifier
        }

        DraggableSwipeableItem(
            modifier = Modifier
                .height(ItemHeight)
                .testTag(itemTag(item.id)),
            allowedSwipeDirections = allowedSwipeDirections,
            dragDropEnabled = dragDropEnabled,
            applyShadowElevationWhenDragged = false,
            onDragFinish = onDragFinish,
            onSwipeGestureFinish = onSwipeGestureFinish,
            moveUpActionLabel = "Move ${item.label} up".takeIf { moveActionLabelsEnabled },
            moveDownActionLabel = "Move ${item.label} down".takeIf { moveActionLabelsEnabled },
            keyboardReorderEnabled = keyboardReorderEnabled,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (prepareDragHandleModifierEarly) {
                            preparedDragHandleModifier
                        } else {
                            Modifier.dragDropModifier()
                        },
                    )
                    .testTag(dragHandleTag(item.id)),
            ) {
                Text(item.label)
            }
        }
    }

    // This helper intentionally isn't composable: drag-handle modifiers should be reusable from
    // ordinary modifier-building functions, as they were before this release.
    private fun Modifier.preparedDragHandleModifier(
        scope: DraggableSwipeableItemScope<TestItem>,
    ): Modifier = with(scope) {
        this@preparedDragHandleModifier.dragDropModifier()
    }

    private fun startDraggingItemDown(itemId: Int) {
        composeRule.onNodeWithTag(dragHandleTag(itemId), useUnmergedTree = true)
            .performTouchInput {
                down(center)
                moveBy(Offset(x = 0f, y = height * 1.1f))
            }
        composeRule.waitForIdle()
    }

    private fun releaseDrag() {
        // Use the fixed list node because the dragged item moves when it crosses another item.
        composeRule.onNodeWithTag(ReorderableListTag).performTouchInput {
            up()
        }
    }

    private fun hasClickActionLabel(label: String) = SemanticsMatcher(
        description = "has click action label '$label'",
    ) { node ->
        node.config.getOrNull(SemanticsActions.OnClick)?.label == label
    }

    private fun hasLongClickActionLabel(label: String) = SemanticsMatcher(
        description = "has long-click action label '$label'",
    ) { node ->
        node.config.getOrNull(SemanticsActions.OnLongClick)?.label == label
    }

    private fun hasCustomAction(label: String) = SemanticsMatcher(
        description = "has custom action '$label'",
    ) { node ->
        node.config.getOrNull(SemanticsActions.CustomActions)?.any { it.label == label } == true
    }

    private fun doesNotHaveCustomAction(label: String) = SemanticsMatcher(
        description = "does not have custom action '$label'",
    ) { node ->
        node.config.getOrNull(SemanticsActions.CustomActions)?.none { it.label == label } != false
    }

    private fun doesNotHaveClickAction() = SemanticsMatcher(
        description = "does not have a click action",
    ) { node ->
        node.config.getOrNull(SemanticsActions.OnClick) == null
    }

    private fun doesNotHaveLongClickAction() = SemanticsMatcher(
        description = "does not have a long-click action",
    ) { node ->
        node.config.getOrNull(SemanticsActions.OnLongClick) == null
    }

    private data class TestItem(
        val id: Int,
        val label: String,
    )

    private companion object {

        val ItemHeight = 56.dp

        const val StandaloneItemTag = "standalone-item"
        const val StandaloneItemContentTag = "standalone-item-content"
        const val EnabledDismissItemTag = "enabled-dismiss-item"
        const val EnabledDismissItemContentTag = "enabled-dismiss-item-content"
        const val DisabledDismissItemTag = "disabled-dismiss-item"
        const val NarrowItemTag = "narrow-item"
        const val FirstChildTag = "first-child"
        const val SecondChildTag = "second-child"
        const val ReorderableListTag = "reorderable-list"

        val TestItems = persistentListOf(
            TestItem(id = 1, label = "A"),
            TestItem(id = 2, label = "B"),
            TestItem(id = 3, label = "C"),
        )

        fun itemTag(itemId: Int) = "item-$itemId"

        fun dragHandleTag(itemId: Int) = "drag-handle-$itemId"

        fun nestedItemTag(itemId: Int) = "nested-item-$itemId"
    }
}
