package com.jacobrozell.dartbuddy

import android.app.Application
import com.jacobrozell.dartbuddy.support.FirebaseBootstrap
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DartBuddyApp : Application() {
    @Inject lateinit var firebaseBootstrap: FirebaseBootstrap

    override fun onCreate() {
        super.onCreate()
        firebaseBootstrap.initialize(this)
    }
}
