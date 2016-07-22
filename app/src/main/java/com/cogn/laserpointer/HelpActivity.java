package com.cogn.laserpointer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class HelpActivity extends Activity implements View.OnClickListener {

    public static final String EXTRA_TEXT_ID = "com.cogn.EXTRA_TEXT_ID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        findViewById(R.id.tv_help_heading_quick).setOnClickListener(this);
        findViewById(R.id.tv_help_heading_setup).setOnClickListener(this);
        findViewById(R.id.tv_help_heading_usage).setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        String htmlFile = null;
        switch (id) {
            case R.id.tv_help_heading_quick:
                htmlFile = "file:///android_asset/help_content_quick.html";
                break;
            case R.id.tv_help_heading_setup:
                htmlFile = "file:///android_asset/help_content_setup.html";
                break;
            case R.id.tv_help_heading_usage:
                htmlFile = "file:///android_asset/help_content_usage.html";
                break;
            default:
                break;
        }
        if (htmlFile != null) {
            Intent intent = (new Intent(this, TopicActivity.class));
            intent.putExtra(EXTRA_TEXT_ID, htmlFile);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Detailed Help for that topic is not available.", Toast.LENGTH_SHORT).show();
        }
    }
}
