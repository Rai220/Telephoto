package com.rai220.securityalarmbot.commands;

import com.pengrad.telegrambot.model.Message;
import com.rai220.securityalarmbot.BotService;
import com.rai220.securityalarmbot.R;
import com.rai220.securityalarmbot.commands.types.SwitchType;
import com.rai220.securityalarmbot.prefs.Prefs;
import com.rai220.securityalarmbot.utils.KeyboardUtils;

/**
 *
 */

public class ShakeSensorCommand extends AbstractCommand {

    public ShakeSensorCommand(BotService service) {
        super(service);
    }

    @Override
    public String getCommand() {
        return "/shake_sensor";
    }

    @Override
    public String getName() {
        return "Shake sensor";
    }

    @Override
    public String getDescription() {
        return "Switch shake sensor";
    }

    @Override
    public boolean execute(Message message, Prefs prefs) {
        final long chatId = message.chat().id();
        Prefs.UserPrefs userPrefs = prefs.getUser(message.from().id());
        if (userPrefs != null) {
            SwitchType switchType = SwitchType.getByName(message.text());
            if (switchType != null) {
                String result = "";
                switch (switchType) {
                    case ON:
                        if (userPrefs.isEventListener) {
                            botService.startSensor();
                            result = botService.getString(R.string.shake_sensor_start);
                            telegramService.notifyToOthers(userPrefs.id, botService.getString(R.string.user_start_shake_sensor));
                        } else {
                            result = botService.getString(R.string.turn_on_notifications);
                        }
                        break;
                    case OFF:
                        botService.stopSensor();
                        result = botService.getString(R.string.shake_sensor_stop);
                        telegramService.notifyToOthers(userPrefs.id, botService.getString(R.string.user_shake_sensor_stop));
                        break;
                    case CANCEL:
                        result = botService.getString(R.string.operation_cancel);
                        break;
                }
                telegramService.sendMessage(chatId, result);
                // TODO: 09.04.2017 mainK
//                telegramService.sendMessage(chatId, result, mainKeyBoard);
                return false;
            } else {
                ICommand[] values = new ICommand[SwitchType.values().length + 1];
                int i = 0;
                while (i < SwitchType.values().length) {
                    values[i] = SwitchType.values()[i];
                    i++;
                }
//                values[i] = CommandType.SHAKE_SENSITIVITY;
                telegramService.sendMessage(chatId, botService.getString(R.string.switch_shake_sensor),
                        KeyboardUtils.getKeyboardIgnoreHide(values));
                return true;
            }
        }
        return false;
    }
}
