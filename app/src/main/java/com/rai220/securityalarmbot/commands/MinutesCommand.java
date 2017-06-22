package com.rai220.securityalarmbot.commands;

import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.Keyboard;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.rai220.securityalarmbot.BotService;
import com.rai220.securityalarmbot.R;
import com.rai220.securityalarmbot.prefs.Prefs;
import com.rai220.securityalarmbot.prefs.PrefsController;

/**
 *
 */
public class MinutesCommand extends AbstractCommand {

    public MinutesCommand(BotService service) {
        super(service);
    }

    @Override
    public String getCommand() {
        return "/minutes";
    }

    @Override
    public String getName() {
        return "Minutes";
    }

    @Override
    public String getDescription() {
        return "Set time to receive regular photo every N minutes";
    }

    @Override
    public boolean execute(Message message, Prefs prefs) {
        final long chatId = message.chat().id();

        try {
            int minutes = Integer.valueOf(message.text());
            prefs.minutesPeriod = minutes;
            PrefsController.instance.setPrefs(prefs);
            botService.getObserver().start(minutes);
            String result = String.format(botService.getString(R.string.time_period_changed), minutes);
            telegramService.sendMessage(chatId, result);
            // TODO: 09.04.2017 mainK
//            telegramService.sendMessage(chatId, result, mainKeyBoard);
            telegramService.notifyToOthers(message.from().id(), String.format(botService.getString(R.string.user_change_time_period), minutes));
            return false;
        } catch (NumberFormatException ex) {
//            result = "Wrong number format: " + phrase[1];
            String[][] keyboardStr = {
                    new String[]{"0", "60", "120"},
                    new String[]{"240", "480", "720"}
            };
            Keyboard keyboard = new ReplyKeyboardMarkup(keyboardStr, true, true, false);
            telegramService.sendMessage(chatId, botService.getString(R.string.select_minutes), keyboard);
            return true;
        }
    }
}
