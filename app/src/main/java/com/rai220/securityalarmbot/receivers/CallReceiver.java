package com.rai220.securityalarmbot.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.rai220.securityalarmbot.R;
import com.rai220.securityalarmbot.telegram.ISenderService;
import com.rai220.securityalarmbot.utils.FabricUtils;

/**
 *
 */

public class CallReceiver extends BroadcastReceiver {

    private ISenderService senderService;

    public CallReceiver(ISenderService senderService) {
        this.senderService = senderService;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            telephonyManager.listen(new PhoneStateListener(), PhoneStateListener.LISTEN_CALL_STATE);

            if (telephonyManager.getCallState() == TelephonyManager.CALL_STATE_RINGING) {
                String phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                String name = FabricUtils.getNameByPhone(context, phoneNumber);
                if (senderService != null) {
                    senderService.sendMessageToAll(String.format(context.getString(R.string.incoming_call), phoneNumber, name));
                }
            }
        }
    }
}
