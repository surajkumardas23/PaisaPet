package com.example.paisapet

import android.app.Application
import com.example.paisapet.notify.NotificationChannels

class PaisaPetApp : Application() {
    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.create(this)
    }
}
