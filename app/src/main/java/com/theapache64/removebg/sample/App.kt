package com.theapache64.removebg.sample

import android.app.Application

class App : Application() {


    override fun onCreate() {
        super.onCreate()

        RemoveBg.init("FyHWo9AH9SAiQvzjFJ5E3Dyz")
    }
}