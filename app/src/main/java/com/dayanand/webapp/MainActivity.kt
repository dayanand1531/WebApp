package com.dayanand.webapp

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import com.dayanand.webapp.ui.theme.WebAppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    val viewModel: MainViewModel = MainViewModel()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WebAppTheme {

                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val coroutineScope = rememberCoroutineScope()
                val title by viewModel.title.collectAsState()
                ModalNavigationDrawer(drawerState = drawerState, drawerContent = {
                    DrawerContent(viewModel, drawerState, coroutineScope)
                }) {
                    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
                        TopAppBar(title = {
                            Text(
                                text = title ?: "",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                            navigationIcon = {
                                IconButton(onClick = {
                                    coroutineScope.launch {
                                        drawerState.open()
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.Menu,
                                        contentDescription = "menu"
                                    )
                                }
                            }
                        )
                    }, content = { innerPadding ->

                        WebApp(
                            viewModel,
                            innerPadding,
                        )
                    })
                }
            }
        }
    }

    override fun onBackPressed() {
        Log.d("TAG", "onBackPressed: Backpress")
        if (viewModel.webView?.canGoBack() == true) viewModel.webView?.goBack()
        else super.onBackPressed()
    }
}


@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebApp(
    viewModel: MainViewModel,
    innerPadding: PaddingValues
) {
    var isLoading by remember { mutableStateOf(false) }

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding()
            )
    ) {
        val (webLayout, loader, noInternetLayout) = createRefs()
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier
                .constrainAs(loader) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .fillMaxSize()
            )
        }

        AndroidView(modifier = Modifier
            .constrainAs(webLayout) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
            .fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                    }
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            //  isLoading = true
                            Log.d("TAG", "Loading Url: ${request?.url}")
                            return false
                        }

                        override fun onPageStarted(
                            view: WebView?,
                            url: String?,
                            favicon: Bitmap?
                        ) {
                            Log.d("TAG", "Loading page: start")
                            isLoading = true
                            super.onPageStarted(view, url, favicon)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            Log.d("TAG", "Page Finish: $url")
                            isLoading = false
                            // Inject JavaScript to change background color
                            view?.evaluateJavascript(
                                "document.body.style.backgroundColor = 'Tomato';"
                            ) {
                                Log.d("WebView", "JavaScript injected successfully")
                            }
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            view?.loadDataWithBaseURL(
                                null,
                                errorText(error?.description.toString()),
                                "text/html",
                                "UTF-8",
                                null
                            )
                            Log.d("TAG", "Error: ${error?.errorCode}")
                            isLoading = false
                            super.onReceivedError(view, request, error)

                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            super.onReceivedTitle(view, title)
                            viewModel.title.value = title
                            Log.d("TAG", "onReceivedTitle: $title")
                            Log.d("TAG", "URL: ${viewModel.webView?.url}")
                        }

                    }
                    viewModel.webView?.addJavascriptInterface(
                        WebAppInterface(this, viewModel),
                        "Android"
                    )

                    WebView.setWebContentsDebuggingEnabled(true)
                    loadUrl("https://incedoinc.com")
                    viewModel.webView = this

                }
            }, update = { webViewInstance ->
                viewModel.webView = webViewInstance
            })

    }
}

@Composable
fun DisconnectedView(modifier: Modifier) {
    ConstraintLayout(modifier = modifier.fillMaxWidth()) {
        val (icon, text, retryButton) = createRefs()
        Icon(
            painter = painterResource(R.drawable.icon_no_internet_4_24),
            contentDescription = "No internet connected.",
            modifier = Modifier.constrainAs(icon) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            })
        Text(text = "Internet not connected", modifier = Modifier.constrainAs(text) {
            top.linkTo(icon.bottom, 5.dp)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
        })

        Button(onClick = {}, modifier = Modifier.constrainAs(retryButton) {
            top.linkTo(text.bottom, 15.dp)
            start.linkTo(parent.start)
            end.linkTo(parent.end)
        }) { Text(text = "Retry") }

    }
}

@Composable
fun DrawerContent(
    viewModel: MainViewModel,
    drawerState: DrawerState,
    coroutineScope: CoroutineScope
) {
    ConstraintLayout(
        modifier = Modifier
            .background(Color.Cyan)
            .fillMaxHeight()
            .fillMaxWidth(0.5f)
    ) {
        val (homeText,
            companyText,
            careersText, contactText) = createRefs()

        Text(text = "Home", modifier = Modifier
            .constrainAs(homeText) {
                top.linkTo(parent.top, 75.dp)
                start.linkTo(parent.start, 16.dp)
            }
            .clickable {
                viewModel.webView?.loadUrl("https://incedoinc.com/")
                CloseDrawer(coroutineScope, drawerState)
            })
        Text(text = "Company Overview", modifier = Modifier
            .constrainAs(companyText) {
                top.linkTo(homeText.bottom, 20.dp)
                start.linkTo(parent.start, 16.dp)
            }
            .clickable {
                viewModel.webView?.loadUrl("https://www.incedoinc.com/company-overview/")
                CloseDrawer(coroutineScope, drawerState)
            })
        Text(text = "Careers", modifier = Modifier
            .constrainAs(careersText) {
                top.linkTo(companyText.bottom, 20.dp)
                start.linkTo(parent.start, 16.dp)
            }
            .clickable {
                viewModel.webView?.loadUrl("https://www.incedoinc.com/careers/")
                CloseDrawer(coroutineScope, drawerState)
            })
        Text(text = "Contact", modifier = Modifier
            .constrainAs(contactText) {
                top.linkTo(careersText.bottom, 20.dp)
                start.linkTo(parent.start, 16.dp)
            }
            .clickable {
                viewModel.webView?.loadUrl("https://www.incedoinc.com/contact/")
                CloseDrawer(coroutineScope, drawerState)
            })
    }
}

fun errorText(errorMsg: String): String {
    return """
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
            <title>Error</title>
            <style>
                body { margin: 0; height: 100vh; background-color: #f0f0f0; display: flex; justify-content: center; align-items: center; font-family: Arial, sans-serif; }
                p { text-align: center; margin-top: 20%; }
                p.retry { color: blue; cursor: pointer; }
            </style>
            <script type="text/javascript">
                function onTextClick() { Android.onTextClick(); }
            </script>
        </head>
        <body>
            <div>
                <p>${errorMsg}</p>
              
            </div>
        </body>
        </html>
    """.trimIndent()
}

class WebAppInterface(private val context: WebView, private val viewModel: MainViewModel) {

    @JavascriptInterface
    fun onTextClick() {
        Toast.makeText(context.context, "toast", Toast.LENGTH_SHORT).show()
        viewModel.webView?.post {
            viewModel.webView?.reload()
        }

    }
}

private fun CloseDrawer(
    coroutineScope: CoroutineScope,
    drawerState: DrawerState
) {
    coroutineScope.launch {
        drawerState.close()
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun GreetingPreview() {
    //   DisconnectedView()
}