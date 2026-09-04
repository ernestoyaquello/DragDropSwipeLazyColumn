package com.ernestoyaquello.dragdropswipelazycolumn

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class LazyColumnEnhancingWrapperTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun insertionBeforeLastItemRemainsFullyVisibleAfterExpanding() {
        verifyInsertionIsRevealed(insertionIndex = 5, initialFirstVisibleItemIndex = 2)
    }

    @Test
    fun insertionAboveViewportIsRevealed() {
        verifyInsertionIsRevealed(insertionIndex = 0, initialFirstVisibleItemIndex = 2)
    }

    @Test
    fun appendRemainsFullyVisibleAfterExpanding() {
        verifyInsertionIsRevealed(
            insertionIndex = 6,
            initialFirstVisibleItemIndex = 2,
            onlyRevealItemsAddedAtTheEnd = true,
        )
    }

    @Test
    fun defaultOptionDoesNotScrollToInsertionAboveViewport() {
        verifyInsertionIsRevealed(
            insertionIndex = 0,
            initialFirstVisibleItemIndex = 2,
            onlyRevealItemsAddedAtTheEnd = true,
            expectReveal = false,
        )
    }

    private fun verifyInsertionIsRevealed(
        insertionIndex: Int,
        initialFirstVisibleItemIndex: Int,
        onlyRevealItemsAddedAtTheEnd: Boolean = false,
        expectReveal: Boolean = true,
    ) {
        val items = mutableStateOf((0 until 6).toImmutableList())
        val listState = LazyListState(firstVisibleItemIndex = initialFirstVisibleItemIndex)
        composeRule.setContent {
            LazyColumnEnhancingWrapper(
                modifier = Modifier.width(240.dp).height(240.dp).testTag("list"),
                state = listState,
                items = items.value,
                key = remember { { it } },
                onlyRevealItemsAddedAtTheEnd = onlyRevealItemsAddedAtTheEnd,
            ) { listModifier, getItemModifier ->
                LazyColumn(modifier = listModifier, state = listState) {
                    itemsIndexed(items.value, key = { _, item -> item }) { index, item ->
                        Box(
                            modifier = getItemModifier(index, item)
                                .height(if (item == 99) 180.dp else 60.dp)
                                .width(240.dp)
                                .testTag("item-$item"),
                        )
                    }
                }
            }
        }

        composeRule.runOnIdle {
            items.value = items.value.toMutableList().apply {
                add(insertionIndex, 99)
            }.toImmutableList()
        }
        // The wrapper explicitly awaits a frame before starting the scroll animation.
        composeRule.mainClock.advanceTimeBy(2_000L)
        composeRule.waitForIdle()

        if (expectReveal) {
            val listBounds = composeRule.onNodeWithTag("list").getUnclippedBoundsInRoot()
            val itemBounds = composeRule.onNodeWithTag("item-99").getUnclippedBoundsInRoot()
            assertTrue(
                "Inserted item is clipped above: $itemBounds within $listBounds",
                itemBounds.top >= listBounds.top - 1.dp,
            )
            assertTrue(
                "Inserted item is clipped below: $itemBounds within $listBounds",
                itemBounds.bottom <= listBounds.bottom + 1.dp,
            )
            assertEquals(180f, (itemBounds.bottom - itemBounds.top).value, 1f)
        } else {
            composeRule.runOnIdle {
                assertEquals(initialFirstVisibleItemIndex + 1, listState.firstVisibleItemIndex)
                assertEquals(0, listState.firstVisibleItemScrollOffset)
            }
        }
    }
}
