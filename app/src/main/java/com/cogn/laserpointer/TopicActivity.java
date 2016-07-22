package com.cogn.laserpointer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.webkit.WebView;
import android.widget.TextView;

public class TopicActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic);

        Intent in = getIntent ();
        String htmlFile = in.getStringExtra(HelpActivity.EXTRA_TEXT_ID);
        WebView webView = (WebView) findViewById (R.id.topic_text);
        webView.loadUrl(htmlFile);

    }
}
