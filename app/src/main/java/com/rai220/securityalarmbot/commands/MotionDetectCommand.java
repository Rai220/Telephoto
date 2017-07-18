package com.rai220.securityalarmbot.commands;

import com.pengrad.telegrambot.model.Message;
import com.rai220.securityalarmbot.BotService;
import com.rai220.securityalarmbot.R;
import com.rai220.securityalarmbot.commands.types.MdSwitchType;
import com.rai220.securityalarmbot.prefs.Prefs;
import com.rai220.securityalarmbot.prefs.PrefsController;
import com.rai220.securityalarmbot.utils.FabricUtils;
import com.rai220.securityalarmbot.utils.KeyboardUtils;

public class MotionDetectCommand extends AbstractCommand {
    private final BotService service;

    public MotionDetectCommand(BotService service) {
        super(service);
        this.service = service;
    }

    @Override
    public String getCommand() {
        return "/detect";
    }

    @Override
    public String getName() {
        return "Motion detection";
    }

    @Override
    public String getDescription() {
        return "Switch motion detection";
    }

    @Override
    public boolean execute(Message message, Prefs prefs) {
        final long chatId = message.chat().id();
        Prefs.UserPrefs userPrefs = prefs.getUser(message.from().id());
        if (userPrefs != null) {
            MdSwitchType switchType = MdSwitchType.getByName(message.text());
            prefs.mdSwitchType = switchType;
            PrefsController.instance.setPrefs(prefs);

            if (switchType != null) {
                String result = "";
                switch (switchType) {
                    case ON:
                    case AUTO:
                        if (userPrefs.isEventListener) {
                            int[] availCameras = FabricUtils.getAvailableCameras();
                            if (availCameras.length == 0) {
                                result = botService.getString(R.string.md_not_start);
                            } else {
                                botService.getDetector().start(switchType);
                                result = botService.getString(R.string.motion_detector_started);
                                telegramService.notifyToOthers(userPrefs.id, botService.getString(R.string.user_md_started));
                            }
                        } else {
                            result = botService.getString(R.string.turn_on_notifications);
                        }
                        break;
                    case OFF:
                        botService.getDetector().stop();
                        result = botService.getString(R.string.md_stopped);
                        telegramService.notifyToOthers(userPrefs.id, botService.getString(R.string.user_md_stopped));
                        break;
                }
                telegramService.sendMessage(chatId, result);
                // TODO: 09.04.2017 mainK
//                telegramService.sendMessage(chatId, result, mainKeyBoard);
                return false;
            } else {
                ICommand[] values = new ICommand[MdSwitchType.values().length + 2];
                int i = 0;
                while (i < MdSwitchType.values().length) {
                    values[i] = MdSwitchType.values()[i];
                    i++;
                }
//                values[i] = CommandType.SENSITIVITY;
//                values[i + 1] = CommandType.MD_MODE;
                telegramService.sendMessage(chatId, service.getString(R.string.cmd_motion_detector_comment),
                        KeyboardUtils.getKeyboardIgnoreHide(values));
                return true;
            }
        }
        return false;
    }
}
