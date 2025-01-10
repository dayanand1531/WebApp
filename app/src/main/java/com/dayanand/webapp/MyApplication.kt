package com.dayanand.webapp

import android.app.Application
import androidx.webkit.WebViewCompat

class MyApplication: Application() {

    override fun onCreate() {
        super.onCreate()
       // WebViewCompat.setDataDirectorySuffix(this.packageName)
    }
}