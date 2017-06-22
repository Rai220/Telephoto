package com.rai220.securityalarmbot.commands;

import com.pengrad.telegrambot.model.Message;
import com.rai220.securityalarmbot.BotService;
import com.rai220.securityalarmbot.photo.AlarmType;
import com.rai220.securityalarmbot.prefs.Prefs;
import com.rai220.securityalarmbot.prefs.PrefsController;
import com.rai220.securityalarmbot.utils.KeyboardUtils;

public class AlarmSettingsCommand extends AbstractCommand {

    public AlarmSettingsCommand(BotService service) {
        super(service);
    }

    @Override
    public String getCommand() {
        return "/alarm_settings";
    }

    @Override
    public String getName() {
        return "Alarm settings";
    }

    @Override
    public String getDescription() {
        return "Configure alarms";
    }

    @Override
    public boolean execute(Message message, Prefs prefs) {
        final long chatId = message.chat().id();
        String text = message.text();
        AlarmType alarmType = AlarmType.getByName(text);

        if (alarmType == null) {
            telegramService.sendMessage(chatId, "Choose alarm type", KeyboardUtils.getKeyboard(AlarmType.values()));
            return true;
        } else {
            prefs.alarmType = alarmType;

            telegramService.notifyToOthers(message.from().id(), "set alarm mode to: " + alarmType);
            telegramService.sendMessage(chatId, "Alarm mode now: " + alarmType);
            // TODO: 09.04.2017 mainKeyb
//            telegramService.sendMessage(chatId, "Alarm mode now: " + alarmType, mainKeyBoard);

            PrefsController.instance.setPrefs(prefs);
            return false;
        }
    }
}
