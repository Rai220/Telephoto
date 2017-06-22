package com.rai220.securityalarmbot;

import android.app.Service;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.rai220.securityalarmbot.utils.L;

/**
 * Created by rai220 on 11/24/16.
 */
public class TelephotoFirebaseInstanceIDService extends FirebaseInstanceIdService {
    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();
        String fbToken = FirebaseInstanceId.getInstance().getToken();
        L.i("Firebase token: " + fbToken);
    }
}
