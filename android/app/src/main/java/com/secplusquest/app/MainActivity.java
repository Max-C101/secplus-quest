package com.secplusquest.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/** Hosts the Sec+ Quest web app (bundled in assets) full screen. */
public class MainActivity extends Activity {

    public static final String EXTRA_REVIEW = "open_review";
    private static final String APP_URL = "file:///android_asset/index.html";
    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window w = getWindow();
        w.setStatusBarColor(Color.WHITE);
        w.setNavigationBarColor(Color.WHITE);
        w.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        web = new WebView(this);
        web.setBackgroundColor(Color.WHITE);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);          // keeps progress (localStorage) between launches
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setTextZoom(100);
        web.addJavascriptInterface(new Bridge(getApplicationContext()), "AndroidBridge");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                if ("file".equals(u.getScheme())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) { }
                return true;
            }
        });
        setContentView(web);

        web.loadUrl(wantsReview(getIntent()) ? APP_URL + "#review" : APP_URL);
    }

    private static boolean wantsReview(Intent i) {
        return i != null && i.getBooleanExtra(EXTRA_REVIEW, false);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (wantsReview(intent) && web != null) {
            web.evaluateJavascript("window.__openReview && window.__openReview()", null);
        }
    }

    @Override
    public void onBackPressed() {
        if (web == null) { finish(); return; }
        web.evaluateJavascript("(window.__back && window.__back()) ? 'y' : 'n'", value -> {
            if (value == null || !value.contains("y")) finish();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        StatsWidget.updateAll(getApplicationContext());
    }

    /** Called from the web app whenever progress is saved. */
    static class Bridge {
        private final Context ctx;
        Bridge(Context c) { ctx = c; }

        @JavascriptInterface
        public void saveState(String json) {
            ctx.getSharedPreferences(StatsWidget.PREFS, Context.MODE_PRIVATE)
                    .edit().putString(StatsWidget.KEY, json).apply();
            StatsWidget.updateAll(ctx);
        }
    }
}
