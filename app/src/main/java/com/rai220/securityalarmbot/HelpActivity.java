package com.rai220.securityalarmbot;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

/**
 * Created by rai220 on 10/29/16.
 */

public class HelpActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help_activity);
    }

    public void okButtonClick(View view) {
        finish();
    }

    public void onStep2HelpClick(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://telegram.me/botfather"));
        startActivity(browserIntent);
    }

    public void onHelpContactsClick(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://telegram.me/telephoto_me"));
        startActivity(browserIntent);
    }
}