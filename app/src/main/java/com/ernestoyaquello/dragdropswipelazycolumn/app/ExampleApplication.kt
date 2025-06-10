package com.ernestoyaquello.dragdropswipelazycolumn.app

import android.app.Application
import com.ernestoyaquello.dragdropswipelazycolumn.app.data.ExampleTasksRepository
import com.ernestoyaquello.dragdropswipelazycolumn.app.data.ExampleTasksRepositoryImpl

class ExampleApplication : Application() {

    lateinit var tasksRepository: ExampleTasksRepository
        private set

    override fun onCreate() {
        super.onCreate()
        tasksRepository = ExampleTasksRepositoryImpl()
    }
}