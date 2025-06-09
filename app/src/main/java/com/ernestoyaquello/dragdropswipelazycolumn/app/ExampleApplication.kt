package com.ernestoyaquello.dragdropswipelazycolumn.app

import android.app.Application
import com.ernestoyaquello.dragdropswipelazycolumn.app.data.ExampleItemsRepository
import com.ernestoyaquello.dragdropswipelazycolumn.app.data.ExampleItemsRepositoryImpl

class ExampleApplication : Application() {

    lateinit var itemsRepository: ExampleItemsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        itemsRepository = ExampleItemsRepositoryImpl()
    }
}