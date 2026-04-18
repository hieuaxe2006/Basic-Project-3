package com.socialapp

import android.app.Application
import com.google.firebase.FirebaseApp

class SocialApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
