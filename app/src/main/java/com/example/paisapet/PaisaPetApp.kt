package com.example.paisapet

import android.app.Application

class PaisaPetApp : Application() {
    val container by lazy { AppContainer(this) }
}
