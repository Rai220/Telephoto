package com.rai220.securityalarmbot;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ToggleButton;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.rai220.securityalarmbot.prefs.PrefsController;
import com.rai220.securityalarmbot.utils.FabricUtils;
import com.rai220.securityalarmbot.utils.L;

import io.fabric.sdk.android.Fabric;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.rai220.securityalarmbot.BotService.TELEPHOTO_SERVICE_STOPPED;

public class SettingsActivity extends Activity {
    private static final int PERMISSION_REQUEST = 1;
    private static final int PHOTO_RESULT_CODE = 2;

    /** Способ запрашивать разрешения только один раз */
    private static volatile boolean permissionRequested = false;

    private static final String[] permissions = new String[]{
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CAMERA,
            Manifest.permission.SYSTEM_ALERT_WINDOW,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECEIVE_BOOT_COMPLETED,
            "net.dinglisch.android.tasker.PERMISSION_RUN_TASKS"
    };

    private final BroadcastReceiver serviceStopReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setStartStopButtonStatus(false);
        }
    };

    // -----
    // UI
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Fabric.with(this, new Crashlytics());
        FabricUtils.initFabric(this);

        setContentView(R.layout.activity_settings);
        PrefsController.instance.init(this);

//        final EditText tokenEditText = ((EditText) findViewById(R.id.botTokenEditText));
//        tokenEditText.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//            }
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) {
//                String text = editable.toString();
//                if (text.contains("API:")) {
//                    String[] lines = text.split("\n");
//                    for (int i = 0; i < lines.length; i++) {
//                        if (lines[i].contains("API:") && i < lines.length - 1) {
//                            text = lines[i + 1];
//                            tokenEditText.setText(text);
//                        }
//                    }
//                }
//                PrefsController.instance.setToken(text);
//            }
//        });

        ((EditText) findViewById(R.id.botPassword)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                PrefsController.instance.setPassword(editable.toString());
            }
        });

        if (PrefsController.instance.hasToken()) {
            startService();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (!permissionRequested) {
            permissionRequested = true;
            requestMultiplePermissions();
        }

        // ((EditText) findViewById(R.id.botTokenEditText)).setText(PrefsController.instance.getToken());
        ((EditText) findViewById(R.id.botPassword)).setText(PrefsController.instance.getPassword());
        ((CheckBox) findViewById(R.id.autorunCheckbox)).setChecked(PrefsController.instance.isAutorunEnabled());

        if (PrefsController.instance.isPro()) {
            findViewById(R.id.proVersionButton).setVisibility(View.GONE);
            ((ImageView) findViewById(R.id.mainIcon)).setImageResource(R.drawable.icon_pro);
        }

        if (!PrefsController.instance.isHelpShown()) {
            showHelp();
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(serviceStopReciever, new IntentFilter(TELEPHOTO_SERVICE_STOPPED));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(serviceStopReciever);
    }

    private void showHelp() {
        PrefsController.instance.setHelpShown(true);
        startActivity(new Intent(this, HelpActivity.class));
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    // UI
    // ------

    // ------
    // Permissions
    private boolean checkPermissions() {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission)
                    != PERMISSION_GRANTED) {
                L.i("No permission for: " + permission);
                return false;
            }
        }
        return true;
    }

    public void requestMultiplePermissions() {
        if (!checkPermissions()) {
            L.i("No permissions! Requesting...");
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST);
        } else {
            L.i("All permissions granted.");
        }
    }

    // Permissions
    // -----

    public void onBotStartStopClick(View view) {
        if (((ToggleButton) view).isChecked()) {
            if (PrefsController.instance.hasToken()) {
                startService();
            } else {
                setStartStopButtonStatus(false);
                new AlertDialog.Builder(this)
                        .setTitle(R.string.warning)
                        .setMessage(R.string.error_no_bot_token)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setNegativeButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                })
                        .setPositiveButton(R.string.how_help,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                        Answers.getInstance().logCustom(new CustomEvent("Help requested"));
                                        showHelp();
                                    }
                                })
                        .show();
            }
        } else {
            stopService();
        }
    }

    private void startService() {
        Intent intent = new Intent(this, BotService.class);
        startService(intent);
        setStartStopButtonStatus(true);
    }

    private void stopService() {
        Intent intent = new Intent(this, BotService.class);
        stopService(intent);
        setStartStopButtonStatus(false);
    }

    private void setStartStopButtonStatus(final boolean status) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                ((ToggleButton)findViewById(R.id.startStopServiceButton)).setChecked(status);
            }
        });
    }

    public void onAutostartCheckboxClick(View view) {
        PrefsController.instance.setAutorun(((CheckBox) view).isChecked());
    }

//    public void onTokenHintClick(View view) {
//        Answers.getInstance().logCustom(new CustomEvent("Help requested"));
//        showHelp();
//    }

    public void onLogsClick(View view) {
        Intent intent = new Intent(this, LogsActivity.class);
        startActivity(intent);
    }

    public void onGraphClick(View view) {
        Intent intent = new Intent(this, GraphActivity.class);
        startActivity(intent);
    }

    public void onProVersionClick(View view) {
        Intent intent = new Intent(this, BuyActivity.class);
        startActivity(intent);
    }

    public void createBotButton(View view) {
        Intent intent = new Intent(this, CreateBotActivity.class);
        startActivity(intent);
    }

    public void setTokenButton(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Please insert token here:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setSingleLine(false);
        input.setLines(5);
        input.setText(PrefsController.instance.getToken());
        builder.setView(input);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                stopService();
                PrefsController.instance.setToken(input.getText().toString().trim());
                dialog.cancel();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }
}
