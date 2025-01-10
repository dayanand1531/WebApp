package com.dayanand.webapp

import android.webkit.WebView
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class MainViewModel: ViewModel() {

    var webView: WebView? = null

    var title = MutableStateFlow<String?>("")

}