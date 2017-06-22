package com.rai220.securityalarmbot.commands;

import android.os.Build;

import com.google.firebase.iid.FirebaseInstanceId;
import com.pengrad.telegrambot.model.Message;
import com.rai220.securityalarmbot.BotService;
import com.rai220.securityalarmbot.R;
import com.rai220.securityalarmbot.prefs.Prefs;
import com.rai220.securityalarmbot.utils.L;

/**
 *
 */
public class ProCommand extends AbstractCommand {

    public ProCommand(BotService service) {
        super(service);
    }

    @Override
    public String getCommand() {
        return "/pro";
    }

    @Override
    public String getName() {
        return "Upgrade to Pro version";
    }

    @Override
    public String getDescription() {
        return "Upgrade to Pro version";
    }

    @Override
    public boolean isHide() {
        return true;
    }

    @Override
    public boolean execute(Message message, Prefs prefs) {
        Long chatId = message.chat().id();
        Prefs.UserPrefs userPrefs = prefs.getUser(message.from().id());
        if (userPrefs != null) {
            StringBuilder result = new StringBuilder();
            result.append(botService.getString(R.string.pro_step_1));
            result.append(botService.getString(R.string.pro_step_2));
            result.append(botService.getString(R.string.pro_step_3));
            result.append(botService.getString(R.string.pro_step_4));


            try {
                String token = FirebaseInstanceId.getInstance().getToken();
                result.append("\nSerial 1: ").append(token).append("\n");
                result.append("\nSerial 2: ").append(Build.SERIAL);
            } catch (Throwable ex) {
                L.e(ex);
            }

            telegramService.sendMessage(chatId, result.toString());
        } else {
            telegramService.sendMessage(chatId, "Incorrect user!");
        }
        return false;
    }
}
