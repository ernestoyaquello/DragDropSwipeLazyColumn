# ↕️ DragDropSwipeLazyColumn

[![issues](https://img.shields.io/github/issues/ernestoyaquello/DragDropSwipeLazyColumn)](https://github.com/ernestoyaquello/DragDropSwipeLazyColumn/issues)
[![pull requests](https://img.shields.io/github/issues-pr/ernestoyaquello/DragDropSwipeLazyColumn)](https://github.com/ernestoyaquello/DragDropSwipeLazyColumn/pulls)
[![contributors](https://img.shields.io/github/contributors/ernestoyaquello/DragDropSwipeLazyColumn)](https://github.com/ernestoyaquello/DragDropSwipeLazyColumn/graphs/contributors)

Kotlin Android library for Jetpack Compose that implements a lazy column with drag-and-drop reordering and swipe-to-dismiss functionality. Spiritual successor to [DragDropSwipeRecyclerview](https://github.com/ernestoyaquello/DragDropSwipeRecyclerview). 🪦

## 🎥 Demo

With the [`DragDropSwipeLazyColumn`](https://github.com/ernestoyaquello/DragDropSwipeLazyColumn/blob/main/drag-drop-swipe-lazycolumn/src/main/java/com/ernestoyaquello/dragdropswipelazycolumn/DragDropSwipeLazyColumn.kt), you can drag an item beyond the last visible item, define different styles for different swipe directions, etc.

![Animated image as a demo for the `DragDropSwipeLazyColumn`](drag-drop-swipe-lazycolumn-demo.webp)

## ✏️ Show me the code!

You can make this work in a couple of simple steps. 👇

### 1. Reference the library

The library is available via `mavenCentral`, but you need to reference it from your project. There are two ways to do this.

#### If you are using a catalogue

If you have a `libs.versions.toml` catalog for your dependencies, add these two lines to it:

```toml
[versions]
dragDropSwipeLazyColumn = "0.9.0"

[libraries]
drag-drop-swipe-lazycolumn = { module = "com.ernestoyaquello.dragdropswipelazycolumn:drag-drop-swipe-lazycolumn", version.ref = "dragDropSwipeLazyColumn" }

```

Then, in your Gradle file, reference the library:

```kotlin
dependencies {
    implementation(libs.drag.drop.swipe.lazycolumn)
}
```

#### If you are *not* using a catalogue

Otherwise, if you are not using a catalogue to manage your dependencies, just add the library directly to the Gradle file of your application:

```kotlin
dependencies {
    implementation("com.ernestoyaquello.dragdropswipelazycolumn:drag-drop-swipe-lazycolumn:0.9.0")
}
```

### 2. Implement the list

Now, you can implement your list using `DragDropSwipeLazyColumn`:

```kotlin
DragDropSwipeLazyColumn(
    modifier = Modifier.fillMaxSize(),
    items = items, // must be an immutable list of items
    key = remember { { it.id } }, // each item must have a unique key
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    onIndicesChangedViaDragAndDrop = { viewModel.onReorderedItems(it) },
) { index, item ->

    // The draggable swipeable item must be the only composable here
    DraggableSwipeableItem(
        modifier = Modifier.animateDraggableSwipeableItem(),
        shapes = SwipeableItemShapes.createRemembered(MaterialTheme.shapes.medium),
        minHeight = 56.dp,
        onClick = { viewModel.onItemClick(item) },
        onLongClick = { viewModel.onItemLongClick(item) },
        onSwipeDismiss = { viewModel.onItemSwipeDismiss(item) },
    ) {

        // Down here you just need to specify the contents of your item
        Row {
            Text(
                modifier = Modifier.weight(1f),
                text = item.title,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )

            // Finally, apply the drag-drop modifier to the drag handle icon
            Icon(
                modifier = Modifier.dragDropModifier(),
                imageVector = Icons.Default.Menu,
                contentDescription = null,
            )
        }
    }
}
```

### 3. That's it!

If you've followed the steps above, everything should already be working.

## 🤔 Would you like to know more?

The code is **fully documented**, and there are some **usage examples**, such as an [example app](https://github.com/ernestoyaquello/DragDropSwipeLazyColumn/tree/main/app) and a few Compose previews, so I would recommend checking out this whole repository, as the code might answer most of your questions.

### 📝 Limitations & disclaimers

While this library is highly customizable, the functionality it offers is still somewhat limited, and many of the choices that were taken during its development were opinionated and not necessarily the most forward-thinking ones possible. Thus, this isn't a perfect, one-size-fits-all solution, but a tool that works in a certain way and is valid for many (but not all) scenarios.

For example, this library uses the `Material3` library, is only compatible with vertical lists (neither horizontal lists nor grids are supported), and doesn't support the use of partial swipes to reveal hidden, interactable content behind the item (this library only supports swipe gestures meant to dismiss the item). Not only that, but despite its extensive customization options, it also has a specific look and *feel* in some regards.

All in all, I still believe that the features this library currently provides are enough in most cases, but there might always be exceptions. And while I am willing to accommodate some of them (should they arise), I would rather keep things simple and maintainable if possible.

### 🤝 Contributions

I would be more than happy to accept help or suggestions, but I am probably going to be wary of massive and/or overly complex changes that could cause the code to become an untamable behemoth. 🙈

### ©️ License

You can do pretty much whatever you want with this code. In fact, copying it is highly encouraged!

```
MIT License

Copyright (c) 2025 Julio Ernesto Rodríguez Cabañas

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
