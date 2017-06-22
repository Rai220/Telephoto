package com.rai220.securityalarmbot.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

import com.rai220.securityalarmbot.BotService;
import com.rai220.securityalarmbot.model.IncomingMessage;
import com.rai220.securityalarmbot.telegram.TelegramService;
import com.rai220.securityalarmbot.utils.FabricUtils;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */

public class SmsReceiver extends BroadcastReceiver {

    private final BotService botService;

    public SmsReceiver(BotService botService) {
        this.botService = botService;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        TelegramService telegramService = botService.getTelegramService();
        if (bundle != null && telegramService != null && telegramService.isRunning()) {
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus != null && pdus.length > 0) {
                List<IncomingMessage> incomingMessages = new ArrayList<>();
                for (Object pdu : pdus) {
                    SmsMessage msg = SmsMessage.createFromPdu((byte[]) pdu);
                    String phone = msg.getOriginatingAddress();
                    String name = FabricUtils.getNameByPhone(context, phone);
                    incomingMessages.add(new IncomingMessage(phone, name, msg.getMessageBody()));
                }
                if (!incomingMessages.isEmpty()) {
                    telegramService.sendMessageToAll(incomingMessages);
                }
                abortBroadcast();
            }
        }
    }
}
