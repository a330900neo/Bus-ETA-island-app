package com.busetaisland.app

import android.app.Application
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.busetaisland.app.data.repository.BusRepository

class BusApp : Application() {
    lateinit var repository: BusRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        com.busetaisland.app.service.OverlayStateHolder.init(this)
        repository = BusRepository(this)
    }

    companion object {
        lateinit var instance: BusApp
            private set
    }
}
