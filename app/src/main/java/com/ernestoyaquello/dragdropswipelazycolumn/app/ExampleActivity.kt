package com.ernestoyaquello.dragdropswipelazycolumn.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ernestoyaquello.dragdropswipelazycolumn.app.ui.screens.ExampleTasksScreen
import com.ernestoyaquello.dragdropswipelazycolumn.app.ui.theme.DragDropSwipeLazyColumnTheme

class ExampleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DragDropSwipeLazyColumnTheme {
                ExampleTasksScreen()
            }
        }
    }
}