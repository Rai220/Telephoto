package com.rai220.securityalarmbot.commands;

import com.pengrad.telegrambot.model.Message;
import com.rai220.securityalarmbot.BotService;
import com.rai220.securityalarmbot.R;
import com.rai220.securityalarmbot.commands.types.SwitchType;
import com.rai220.securityalarmbot.prefs.Prefs;
import com.rai220.securityalarmbot.prefs.PrefsController;
import com.rai220.securityalarmbot.utils.KeyboardUtils;

import java.util.Set;

/**
 *
 */

public class NotificationCommand extends AbstractCommand {

    public NotificationCommand(BotService service) {
        super(service);
    }

    @Override
    public String getCommand() {
        return "/notifications";
    }

    @Override
    public String getName() {
        return "Notifications";
    }

    @Override
    public String getDescription() {
        return "Control notifications from bot";
    }

    @Override
    public boolean execute(Message message, Prefs prefs) {
        Long chatId = message.chat().id();
        Prefs.UserPrefs userPrefs = prefs.getUser(message.from().id());
        if (userPrefs != null) {
            SwitchType switchType = SwitchType.getByName(message.text());
            if (switchType != null) {
                String resultMessage = "";
                Set<Prefs.UserPrefs> subscribers = prefs.getEventListeners();
                switch (switchType) {
                    case ON:
                        resultMessage = !userPrefs.isEventListener ? botService.getString(R.string.notification_on) :
                                botService.getString(R.string.notification_already_on);
                        userPrefs.isEventListener = true;
                        PrefsController.instance.setPrefs(prefs);
                        if (subscribers.size() == 0) {
                            startAllServices(prefs);
                        }
                        break;
                    case OFF:
                        boolean isEventListener = userPrefs.isEventListener;
                        resultMessage = userPrefs.isEventListener ? botService.getString(R.string.notification_off) :
                                botService.getString(R.string.notification_already_off);
                        userPrefs.isEventListener = false;
                        PrefsController.instance.setPrefs(prefs);
                        if (isEventListener && (subscribers.size() == 1)) {
                            stopAllServices();
                        }
                        break;
                    case CANCEL:
                        resultMessage = botService.getString(R.string.operation_cancel);
                        break;
                }
                telegramService.sendMessage(chatId, resultMessage);
                // TODO: 09.04.2017 mainK
//                telegramService.sendMessage(chatId, resultMessage, mainKeyBoard);
                return false;
            } else {
                telegramService.sendMessage(chatId, botService.getString(R.string.switch_notification),
                        KeyboardUtils.getKeyboard(SwitchType.values()));
                return true;
            }
        }
        return false;
    }

    private void startAllServices(Prefs prefs) {
        //todo start all stopped services, if they was been started
        botService.getObserver().start(prefs.minutesPeriod);
    }

    private void stopAllServices() {
        //todo stop all services
        botService.getObserver().stop();
    }

}
