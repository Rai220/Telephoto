package com.rai220.securityalarmbot.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.rai220.securityalarmbot.BotService;
import com.rai220.securityalarmbot.prefs.PrefsController;

/**
 * Created by rai220 on 10/17/16.
 */
public class BootUpReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        PrefsController.instance.init(context);
        if (PrefsController.instance.isAutorunEnabled()) {
            context.startService(new Intent(context, BotService.class));
        }
    }
}