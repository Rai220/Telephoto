package com.rai220.securityalarmbot;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.rai220.securityalarmbot.utils.L;

/**
 * Created by rai220 on 11/3/16.
 */

public class LogsActivity extends Activity {
    private EditText editText = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        editText = (EditText)findViewById(R.id.logsEditText);
        editText.setText(L.logsToString());
    }

    public void onCloseClick(View view) {
        finish();
    }

    public void onCopyClick(View view) {
    }
}
