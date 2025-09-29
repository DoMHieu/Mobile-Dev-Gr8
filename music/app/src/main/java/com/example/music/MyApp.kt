package com.example.music

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner

//For service foreground
class MyApp : Application(), DefaultLifecycleObserver {
    override fun onCreate() {
        super<Application>.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

}
