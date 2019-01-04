package com.ahren.google.registration;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import java.lang.ref.WeakReference;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RegActivity extends Activity {

    private static final boolean DEBUG = true;

    @BindView(R.id.progress)
    ProgressBar mProgressBar;

    @BindView(R.id.webView)
    WebView mWebView;

    private static final int MSG_PAGE_FINISHED = 1;
    private static final int MSG_CHECK_FINISHED = 2;
    private static final int MSG_CHECK_ERROR = 3;
    private static final int MSG_RELOAD = 4;
    private static final Uri sUri = Uri.parse("content://com.google.android.gsf.gservices");

    private final String TAG = getClass().getSimpleName();

    String mJs = "";

    private boolean mIsRedirected = false;
    private boolean mRegStarted = false;
    private boolean mIsReload = false;
    private int mPageProgress = 0;
    private Handler mHandler;
    private ProgressDialog mDialog;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reg);
        ButterKnife.bind(this);
        mHandler = new myHandler(this);

        mDialog = new ProgressDialog(this);
        mDialog.setMessage("Checking result!");

        mProgressBar.setMax(100);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new myWebClient());
//        mWebView.getSettings().setSupportZoom(true);
//        mWebView.setInitialScale(50);
        mWebView.setWebChromeClient(new WebChromeClient(){

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                String message = consoleMessage.message();
                if(DEBUG) Log.i(TAG, "onConsoleMessage: " + message);

                if(message.contains("check:") && message.contains(getGSFID(RegActivity.this))){
                    if(mHandler != null){
                        mHandler.sendEmptyMessage(MSG_CHECK_FINISHED);
                    }
                }

                return super.onConsoleMessage(consoleMessage);
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if(DEBUG) Log.i(TAG, "onProgressChanged: " + newProgress);
                super.onProgressChanged(view, newProgress);
                if(mIsRedirected){
                    mProgressBar.setProgress(newProgress);
                    mPageProgress = newProgress;
                }

                if(mRegStarted && newProgress == 100){
                    final String js = "javascript:document.querySelectorAll('[role=\"option\"][tabindex=\"-1\"]').forEach(function(element) {" +
                            "  console.log(\"check:\" + element.getAttribute(\"aria-label\"))" +
                            "})";

                    if(mWebView != null){
                        mWebView.evaluateJavascript(js, new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String value) {
                                if(DEBUG) Log.i(TAG, "onProgressChanged onReceiveValue: " + value);
                            }
                        });
                    }

                    mRegStarted = false;
                }
            }
        });
        mWebView.loadUrl("https://g.co/androiddeviceregistration");
    }

    @Override
    protected void onResume() {
        super.onResume();
        String id = getGSFID(RegActivity.this);
        Log.i(TAG, "onResume: " + id);
        mJs = "document.getElementsByTagName('input')[0].value = '" +  id + "';" +
              "document.querySelectorAll('[role=\"button\"][tabindex=\"0\"][aria-disabled=\"false\"]')[0].click();";
    }

    public static String getGSFID(Context context) {
        try {
            Cursor query = context.getContentResolver().query(sUri, null, null, new String[] { "android_id" }, null);
            if (query == null) {
                return "Not found";
            }
            if (!query.moveToFirst() || query.getColumnCount() < 2) {
                query.close();
                return "Not found";
            }
            String id = query.getString(1);
            query.close();
            return id;
        } catch (SecurityException e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mHandler != null){
            mHandler.removeMessages(MSG_CHECK_ERROR);
            mHandler.removeMessages(MSG_CHECK_FINISHED);
            mHandler.removeMessages(MSG_PAGE_FINISHED);
            mHandler = null;
        }
        if (mWebView != null) {
            // 要首先移除

            // 清理缓存
            mWebView.stopLoading();
            mWebView.onPause();
            mWebView.clearHistory();
            mWebView.clearCache(true);
            mWebView.clearFormData();
            mWebView.clearSslPreferences();
            mWebView.destroyDrawingCache();
            mWebView.removeAllViews();

            // 最后再去webView.destroy();
            mWebView.destroy();
            mWebView = null;
        }
    }

    private void onPageFinished(){
        if(mWebView == null) return;


        mDialog.show();

        if(mIsReload){
            mRegStarted = true;
            mHandler.sendEmptyMessageDelayed(MSG_CHECK_ERROR, 20000);
            mIsRedirected = false;
            mPageProgress = 0;
        }else{
            Log.i(TAG, "onPageFinished: Reload!!!!!!!");
            mWebView.evaluateJavascript(mJs,
                    new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            if(DEBUG) Log.i(TAG, "onPageFinished onReceiveValue: " + value);
//                            mWebView.clearCache(false);
//                            mWebView.clearFormData();
//                            mWebView.reload();
                            mHandler.sendEmptyMessageDelayed(MSG_RELOAD, 6000);
                        }
                    }
            );


            mIsReload = true;
        }


    }

    class myWebClient extends WebViewClient{
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if(DEBUG) Log.i(TAG, "onPageStarted: " + url);
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            if(DEBUG) Log.i(TAG, "shouldOverrideUrlLoading: " + request.getUrl().toString());
            if("https://www.google.com/android/uncertified/".equals(request.getUrl().toString())){
                mIsRedirected = true;
            }
            return false;
        }

        public void onPageFinished(WebView view, String url) {
            if(DEBUG) Log.i(TAG, "onPageFinished: " + url);
            super.onPageFinished(view, url);
            setTitle(url);
            if("https://www.google.com/android/uncertified/".equals(url) && mIsRedirected && mPageProgress == 100){
                mHandler.removeMessages(MSG_PAGE_FINISHED);
                mHandler.sendEmptyMessageDelayed(MSG_PAGE_FINISHED, 300);

            }
        }
    }

    private static class myHandler extends Handler{
        WeakReference<RegActivity> activity;

        myHandler(RegActivity activity){
            this.activity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            final RegActivity regActivity = activity.get();
            if(regActivity == null) return;
            switch (msg.what){
                case MSG_PAGE_FINISHED:
                    regActivity.onPageFinished();
                    break;
                case MSG_CHECK_FINISHED:
                    regActivity.mDialog.dismiss();
                    AlertDialog dialog = new AlertDialog.Builder(regActivity)
                            .setMessage("Congratulations, registration is successful, Please reboot device after 5 minutes!")
                            .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    regActivity.finish();
                                }
                            })
//                            .setNeutralButton("reboot", new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//                                    PowerManager powerManager = (PowerManager) regActivity.getSystemService(POWER_SERVICE);
//                                    powerManager.reboot("");
//                                }
//                            })
                            .create();
                    dialog.show();

                    removeMessages(MSG_CHECK_ERROR);
                    break;
                case MSG_CHECK_ERROR:
                    regActivity.mDialog.dismiss();
                    AlertDialog error = new AlertDialog.Builder(regActivity)
                            .setMessage("Congratulations, error!!")
                            .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    regActivity.mHandler.sendEmptyMessageDelayed(MSG_RELOAD, 500);
                                }
                            })
                            .create();
                    error.show();
                    break;
                case MSG_RELOAD:
                    regActivity.mWebView.stopLoading();
                    regActivity.mWebView.reload();
                    break;
            }
        }
    }

}
