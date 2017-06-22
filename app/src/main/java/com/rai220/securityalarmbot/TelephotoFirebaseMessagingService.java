package com.rai220.securityalarmbot;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.rai220.securityalarmbot.prefs.PrefsController;
import com.rai220.securityalarmbot.utils.L;

import java.util.Map;

/**
 * Created by rai220 on 11/24/16.
 */
public class TelephotoFirebaseMessagingService extends FirebaseMessagingService {
    public static final String SERIAL = "SERIAL";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        try {
            //super.onMessageReceived(remoteMessage);
            String from = remoteMessage.getFrom();
            if (from.equals("596470572574")) {
                final Map<String, String> data = remoteMessage.getData();
                PrefsController.instance.init(this);
                if (data.containsKey(SERIAL)) {
                    if (data.get(SERIAL).equals(Build.SERIAL)) {
                        if (data.containsKey("pro")) {
                            PrefsController.instance.makePro();
                        }
                        if (data.containsKey("not_pro")) {
                            PrefsController.instance.unmakePro();
                        }
                        if (data.containsKey("toast")) {
                            Handler handler = new Handler(Looper.getMainLooper());
                            final Context fContext = this;
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(fContext, data.get("toast"), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        L.i("Recieved message: " + remoteMessage);
                    }
                }
            }
        } catch (Throwable ex) {
            L.e(ex);
        }
    }
}
