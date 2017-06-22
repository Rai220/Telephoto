package com.rai220.securityalarmbot.commands;

import com.pengrad.telegrambot.model.Message;
import com.rai220.securityalarmbot.BotService;
import com.rai220.securityalarmbot.R;
import com.rai220.securityalarmbot.commands.types.SensitivityType;
import com.rai220.securityalarmbot.prefs.Prefs;
import com.rai220.securityalarmbot.prefs.PrefsController;
import com.rai220.securityalarmbot.utils.KeyboardUtils;

/**
 *
 */

public class ShakeSensitivityCommand extends AbstractCommand {

    public ShakeSensitivityCommand(BotService service) {
        super(service);
    }

    @Override
    public String getCommand() {
        return "/shake_sensitivity";
    }

    @Override
    public String getName() {
        return "Shake sensitivity";
    }

    @Override
    public String getDescription() {
        return "Set sensitivity for shake sensor (high/medium/low)";
    }

    @Override
    public boolean isHide() {
        return true;
    }

    @Override
    public boolean execute(Message message, Prefs prefs) {
        final long chatId = message.chat().id();
        final SensitivityType sensType = SensitivityType.getByName(message.text());
        String result;
        if (sensType != null) {
            prefs.shakeSensitivity = sensType;
            PrefsController.instance.setPrefs(prefs);
            if (botService.isSensorStarted()) {
                botService.stopSensor();
                botService.startSensor();
            }
            result = botService.getString(R.string.shake_sensitivity_change) + sensType;
            telegramService.sendMessage(chatId, result);
            // TODO: 09.04.2017 mainK
//            telegramService.sendMessage(chatId, result, mainKeyBoard);
            telegramService.notifyToOthers(message.from().id(), botService.getString(R.string.user_switch_shake_sensitivity) + sensType);
            return false;
        } else {
            telegramService.sendMessage(chatId, botService.getString(R.string.select_sensitivity),
                    KeyboardUtils.getKeyboard(SensitivityType.values()));
            return true;
        }
    }
}
