package com.example.aibookreader.presentation.screens.reader.components

import android.annotation.SuppressLint
import android.graphics.Rect
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.example.aibookreader.domain.model.ReaderSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

private const val AI_MENU_ID = 999

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EpubWebViewPage(
    chapterFilePath: String,
    basePath: String,
    backgroundColor: Color,
    textColor: Color,
    readerSettings: ReaderSettings = ReaderSettings(),
    currentPageInChapter: Int = 0,
    onTotalPagesCalculated: (Int) -> Unit = {},
    onPageChanged: (Int) -> Unit = {},
    onAiRequested: (String) -> Unit = {},
    onTap: () -> Unit = {},
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    val currentOnTotalPages by rememberUpdatedState(onTotalPagesCalculated)
    val currentOnPageChanged by rememberUpdatedState(onPageChanged)
    val currentOnAiRequested by rememberUpdatedState(onAiRequested)
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnSwipeLeft by rememberUpdatedState(onSwipeLeft)
    val currentOnSwipeRight by rememberUpdatedState(onSwipeRight)
    val currentSettings by rememberUpdatedState(readerSettings)
    val currentBgColor by rememberUpdatedState(backgroundColor)
    val currentTextColor by rememberUpdatedState(textColor)

    val tracker = remember { ChapterTracker() }
    var scriptReady by remember { mutableStateOf(false) }
    val webViewHolder = remember { WebViewHolder() }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            val bridge = EpubJsBridge(
                onPagesCalculated = { total ->
                    scope.launch(Dispatchers.Main) { currentOnTotalPages(total) }
                },
                onPageChanged = { page ->
                    scope.launch(Dispatchers.Main) {
                        currentOnPageChanged(page)
                        scriptReady = true
                    }
                },
                onTap = { scope.launch(Dispatchers.Main) { currentOnTap() } },
                onSwipeLeft = { scope.launch(Dispatchers.Main) { currentOnSwipeLeft() } },
                onSwipeRight = { scope.launch(Dispatchers.Main) { currentOnSwipeRight() } }
            )

            (object : WebView(context) {
                private fun wrapCallback(original: ActionMode.Callback?): ActionMode.Callback {
                    val wv = this
                    return object : ActionMode.Callback {
                        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                            val r = original?.onCreateActionMode(mode, menu) ?: true
                            menu.add(0, AI_MENU_ID, Menu.NONE, "✨ ИИ")
                                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                            return r
                        }
                        override fun onPrepareActionMode(mode: ActionMode, menu: Menu) =
                            original?.onPrepareActionMode(mode, menu) ?: false
                        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                            if (item.itemId == AI_MENU_ID) {
                                wv.evaluateJavascript("window.getSelection().toString()") { raw ->
                                    val t = raw?.trim()?.removeSurrounding("\"")
                                        ?.replace("\\n", "\n")?.replace("\\\"", "\"") ?: ""
                                    if (t.isNotBlank()) currentOnAiRequested(t)
                                }
                                mode.finish(); return true
                            }
                            return original?.onActionItemClicked(mode, item) ?: false
                        }
                        override fun onDestroyActionMode(mode: ActionMode) {
                            original?.onDestroyActionMode(mode)
                        }
                    }
                }

                private fun wrapCallback2(original: ActionMode.Callback2): ActionMode.Callback2 {
                    val wv = this
                    return object : ActionMode.Callback2() {
                        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                            val r = original.onCreateActionMode(mode, menu)
                            menu.add(0, AI_MENU_ID, Menu.NONE, "✨ ИИ")
                                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                            return r
                        }
                        override fun onPrepareActionMode(mode: ActionMode, menu: Menu) =
                            original.onPrepareActionMode(mode, menu)
                        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                            if (item.itemId == AI_MENU_ID) {
                                wv.evaluateJavascript("window.getSelection().toString()") { raw ->
                                    val t = raw?.trim()?.removeSurrounding("\"")
                                        ?.replace("\\n", "\n")?.replace("\\\"", "\"") ?: ""
                                    if (t.isNotBlank()) currentOnAiRequested(t)
                                }
                                mode.finish(); return true
                            }
                            return original.onActionItemClicked(mode, item)
                        }
                        override fun onDestroyActionMode(mode: ActionMode) =
                            original.onDestroyActionMode(mode)
                        override fun onGetContentRect(mode: ActionMode, view: View, outRect: Rect) =
                            original.onGetContentRect(mode, view, outRect)
                    }
                }

                override fun startActionMode(cb: ActionMode.Callback?): ActionMode? =
                    super.startActionMode(if (cb != null) wrapCallback(cb) else null)

                override fun startActionMode(cb: ActionMode.Callback?, type: Int): ActionMode? {
                    if (cb == null) return super.startActionMode(null, type)
                    val w = if (cb is ActionMode.Callback2) wrapCallback2(cb) else wrapCallback(cb)
                    return super.startActionMode(w, type)
                }
            }).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    allowFileAccess = true
                    allowFileAccessFromFileURLs = true
                    allowUniversalAccessFromFileURLs = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = false
                    setSupportZoom(false)
                    builtInZoomControls = false
                    displayZoomControls = false
                }
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                overScrollMode = View.OVER_SCROLL_NEVER
                setBackgroundColor(backgroundColor.toArgb())
                addJavascriptInterface(bridge, "AndroidBridge")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        if (url == null || url == "about:blank") return
                        val bg = colorToHex(currentBgColor)
                        val tx = colorToHex(currentTextColor)
                        val initPage = currentPageInChapter
                        injectReaderScript(view, currentSettings, bg, tx, initPage)
                    }
                    override fun shouldOverrideUrlLoading(view: WebView, req: WebResourceRequest) = true
                }
                webViewHolder.webView = this
            }
        },
        update = { webView ->
            if (chapterFilePath != tracker.loadedChapter) {
                scriptReady = false
                tracker.loadedChapter = chapterFilePath
                val file = File(chapterFilePath)
                if (file.exists()) {
                    webView.loadDataWithBaseURL(
                        "file://$basePath/", file.readText(),
                        "application/xhtml+xml", "UTF-8", null
                    )
                }
            }
        }
    )

    LaunchedEffect(currentPageInChapter, scriptReady) {
        if (scriptReady && currentPageInChapter >= 0) {
            webViewHolder.webView?.evaluateJavascript(
                "if(typeof goToPage==='function')goToPage($currentPageInChapter);", null
            )
        }
    }

    LaunchedEffect(readerSettings.fontSize, readerSettings.lineHeightMultiplier, backgroundColor, textColor) {
        if (scriptReady) {
            val bg = colorToHex(backgroundColor)
            val tx = colorToHex(textColor)
            webViewHolder.webView?.let { wv ->
                wv.setBackgroundColor(backgroundColor.toArgb())
                wv.evaluateJavascript(
                    "if(typeof updateStyles==='function')updateStyles(${readerSettings.fontSize},${readerSettings.lineHeightMultiplier},'$bg','$tx');",
                    null
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewHolder.webView?.destroy()
            webViewHolder.webView = null
        }
    }
}

private class ChapterTracker { var loadedChapter = "" }
private class WebViewHolder { var webView: WebView? = null }

private fun injectReaderScript(
    webView: WebView, settings: ReaderSettings,
    bgColor: String, textColor: String, initialPage: Int
) {
    val js = """
    (function() {
        document.documentElement.style.setProperty('--rf','${settings.fontSize}px');
        document.documentElement.style.setProperty('--rl','${settings.lineHeightMultiplier}');
        document.documentElement.style.setProperty('--rb','${bgColor}');
        document.documentElement.style.setProperty('--rt','${textColor}');

        document.querySelectorAll('link[rel="stylesheet"]').forEach(function(e){e.remove();});
        document.querySelectorAll('style').forEach(function(e){e.remove();});
        document.body.removeAttribute('style');

        var s = document.createElement('style');
        s.id = 'reader-style';
        s.textContent = [
            '* { box-sizing:border-box; }',
            'html { margin:0 !important; padding:0 !important; overflow:hidden !important; height:100vh !important; width:100vw !important; background:var(--rb) !important; }',
            'body { margin:0 !important; padding:20px !important; height:100vh !important; width:100vw !important; overflow:visible !important; background:var(--rb) !important; color:var(--rt) !important; font-size:var(--rf) !important; line-height:var(--rl) !important; column-width:calc(100vw - 40px) !important; column-gap:40px !important; column-fill:auto !important; word-wrap:break-word; overflow-wrap:break-word; }',
            'img { max-width:100% !important; height:auto !important; page-break-inside:avoid; break-inside:avoid; }',
            'h1,h2,h3,h4,h5,h6 { color:var(--rt) !important; page-break-after:avoid; break-after:avoid; }',
            'p { margin:0.5em 0 !important; color:var(--rt) !important; }',
            'div,span,li,blockquote,pre,code,td,th,caption,figcaption { color:var(--rt) !important; }',
            'a { color:var(--rt) !important; text-decoration:underline; }',
            'table { max-width:100% !important; }',
            '::selection { background:rgba(128,90,213,0.3); }'
        ].join(' ');
        document.head.appendChild(s);

        window._rp = -999;
        window._rt = 1;

        window.recalculate = function() {
            var pw = window.innerWidth;
            if (pw <= 0) return;
            var sw = document.body.scrollWidth;
            window._rt = Math.max(1, Math.round(sw / pw));
        };

        window.goToPage = function(p) {
            if (p < 0) p = window._rt - 1;
            p = Math.max(0, Math.min(p, window._rt - 1));
            window._rp = p;
            document.body.style.transform = 'translateX(' + (-window.innerWidth * p) + 'px)';
            AndroidBridge.onPageChanged(p);
        };

        window.updateStyles = function(fs, lh, bg, tc) {
            document.documentElement.style.setProperty('--rf', fs+'px');
            document.documentElement.style.setProperty('--rl', ''+lh);
            document.documentElement.style.setProperty('--rb', bg);
            document.documentElement.style.setProperty('--rt', tc);
            setTimeout(function(){
                recalculate();
                AndroidBridge.onPagesCalculated(window._rt);
                var target = Math.min(window._rp < 0 ? 0 : window._rp, window._rt - 1);
                goToPage(target);
            }, 150);
        };

        var tx0=0, ty0=0, tt0=0;
        document.addEventListener('touchstart', function(e){
            tx0=e.touches[0].clientX; ty0=e.touches[0].clientY; tt0=Date.now();
        }, {passive:true});

        document.addEventListener('touchend', function(e){
            var dx=e.changedTouches[0].clientX-tx0;
            var dy=e.changedTouches[0].clientY-ty0;
            var dt=Date.now()-tt0;
            if(Math.abs(dx)<15 && Math.abs(dy)<15 && dt<300){
                var sel=window.getSelection();
                if(!sel||sel.toString().length===0) AndroidBridge.onTap();
                return;
            }
            if(Math.abs(dx)>Math.abs(dy)*1.5 && Math.abs(dx)>60){
                if(dx<0){
                    if(window._rp < window._rt-1) goToPage(window._rp+1);
                    else AndroidBridge.onSwipeLeft();
                } else {
                    if(window._rp > 0) goToPage(window._rp-1);
                    else AndroidBridge.onSwipeRight();
                }
            }
        }, {passive:true});

        var initPage = ${initialPage};
        function doInit() {
            recalculate();
            AndroidBridge.onPagesCalculated(window._rt);
            var target = initPage < 0 ? window._rt - 1 : Math.min(initPage, window._rt - 1);
            goToPage(target);
        }

        requestAnimationFrame(function(){
            doInit();
            setTimeout(doInit, 300);
        });

        var imgs = document.querySelectorAll('img');
        if (imgs.length > 0) {
            var loaded = 0, done = false;
            imgs.forEach(function(img){
                function check() {
                    loaded++;
                    if (!done && loaded >= imgs.length) { done = true; requestAnimationFrame(doInit); }
                }
                if (img.complete) check();
                else { img.addEventListener('load', check); img.addEventListener('error', check); }
            });
        }

        window.addEventListener('resize', function(){
            recalculate();
            AndroidBridge.onPagesCalculated(window._rt);
            goToPage(Math.min(window._rp < 0 ? 0 : window._rp, window._rt - 1));
        });
    })();
    """.trimIndent()
    webView.evaluateJavascript(js, null)
}

private fun colorToHex(color: Color): String {
    return String.format("#%06X", 0xFFFFFF and color.toArgb())
}

class EpubJsBridge(
    private val onPagesCalculated: (Int) -> Unit,
    private val onPageChanged: (Int) -> Unit,
    private val onTap: () -> Unit,
    private val onSwipeLeft: () -> Unit,
    private val onSwipeRight: () -> Unit
) {
    @JavascriptInterface fun onPagesCalculated(t: Int) = onPagesCalculated.invoke(t)
    @JavascriptInterface fun onPageChanged(p: Int) = onPageChanged.invoke(p)
    @JavascriptInterface fun onTap() = onTap.invoke()
    @JavascriptInterface fun onSwipeLeft() = onSwipeLeft.invoke()
    @JavascriptInterface fun onSwipeRight() = onSwipeRight.invoke()
}
