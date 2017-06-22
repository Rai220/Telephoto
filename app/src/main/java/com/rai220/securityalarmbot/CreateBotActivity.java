package com.rai220.securityalarmbot;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.rai220.securityalarmbot.prefs.PrefsController;
import com.rai220.securityalarmbot.utils.L;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by rai220 on 10/29/16.
 */
public class CreateBotActivity extends Activity {
    private volatile ProgressDialog dialog = null;
    private ExecutorService es = Executors.newCachedThreadPool();
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_bot_activity);
    }

    public void createBotButton(View view) {
        String name = ((EditText)findViewById(R.id.nameEditText)).getText().toString().replaceAll("@", "");
        String phone = ((EditText)findViewById(R.id.phoneEditText)).getText().toString().replaceAll("\\+", "");
        if (Strings.isNullOrEmpty(name)) {
            showError("Please enter bot name! For example 'MyTestBot'.");
        } else if (!name.toLowerCase().endsWith("bot")) {
            showError("The name must end with 'bot', for example 'MyTestBot'");
        } else if (Strings.isNullOrEmpty(phone) || phone.length() < 10) {
            showError("Please enter your 10-digits phone number");
        } else {
            sendDataToServer(name, phone, null);
        }
    }

    private void sendDataToServer(final String name, final String phone, final String code) {
        showWaitDialog("Creating bot");
        es.submit(new Runnable() {
            @Override
            public void run() {
                final AtomicReference<String> resp = new AtomicReference<>(null);

                try {
                    Request.Builder request = new Request.Builder()
                            //.url("http://192.168.1.72:8080/create")
                            .url("http://botcreator.telephoto.me:8080/create")
                            .addHeader("Name", name)
                            .addHeader("Phone", phone);
                    if (!Strings.isNullOrEmpty(code)) {
                        request.addHeader("Code", code);
                    }

                    Response response = client.newCall(request.build()).execute();
                    if (response.isSuccessful()) {
                        final String ans = response.body().string();
                        L.i("Server response: " + ans);
                        resp.set(ans);
                    } else {
                        L.e("Server response error: " + response.code());
                    }
                } catch (Throwable ex) {
                    L.e(ex);
                }


                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        hideDialog();

                        if (resp.get() == null) {
                            showError("Sorry, server error! Please try again or create bot by yourself.");
                        } else {
                            try {
                                List<String> result = Splitter.on("\n").omitEmptyStrings().splitToList(resp.get());
                                switch (result.get(0)) {
                                    case "CREATED":
                                        String token = result.get(1);
                                        PrefsController.instance.setToken(token);
                                        showError("Bot succesfully created! Now you can start your bot!", new Runnable() {
                                            @Override
                                            public void run() {
                                                finish();
                                            }
                                        });
                                        break;
                                    case "NO_ANSER":
                                        showError(getString(R.string.bot_creation_server_error));
                                        break;
                                    case "INVALID_USERNAME":
                                        showError("Incorrect bot name! Please try different name!");
                                        break;
                                    case "TAKEN_USERNAME":
                                        showError("This bot name is already used!");
                                        break;
                                    case "CODE_SENT":
                                        getPin(name, phone);
                                        break;
                                    case "ERROR":
                                        showError(getString(R.string.bot_creation_server_error));
                                        break;
                                    case "TOO_MANY_ATTEMPTS":
                                        showError("Sorry, too many attempts to create bot! Try again later.");
                                        break;
                                    case "TOO_MANY_BOTS":
                                        showError("Sorry, you already have too many bots! You reach Telegram bots limit.");
                                        break;
                                    case "TRY_AGAIN":
                                        showError(getString(R.string.bot_creation_server_error));
                                        break;
                                    case "ERRORSENDINGCODE":
                                        showError("Pin-code sends limit reached. Please try again after some time!");
                                    case "INCORRECT_PIN":
                                        getPin(name, phone);
                                        break;
                                    default:
                                        showError("Sorry, unknown server error!");
                                        break;
                                }
                            } catch (Throwable ex) {
                                showError(getString(R.string.bot_creation_server_error));
                                L.e(ex);
                            }
                        }
                    }
                });
            }
        });
    }

    private void getPin(final String name, final String phone) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Please enter PIN from telegram or SMS:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String pin = input.getText().toString();
                sendDataToServer(name, phone, pin);
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

    private void showError(String text) {
        showError(text, null);
    }

    private void showError(String text, final Runnable okTask) {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMessage(text).
        setPositiveButton(
                "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        if (okTask != null) {
                            okTask.run();
                        }
                    }
                });
        AlertDialog alert11 = builder1.create();
        alert11.show();
    }

    private synchronized void hideDialog() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    private synchronized void showWaitDialog(String text) {
        hideDialog();
        dialog = ProgressDialog.show(this, "Creating", text, true);
    }
}