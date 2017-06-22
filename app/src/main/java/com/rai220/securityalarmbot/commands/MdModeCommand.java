package com.rai220.securityalarmbot.commands;

import com.pengrad.telegrambot.model.Message;
import com.rai220.securityalarmbot.BotService;
import com.rai220.securityalarmbot.R;
import com.rai220.securityalarmbot.photo.CameraMode;
import com.rai220.securityalarmbot.prefs.Prefs;
import com.rai220.securityalarmbot.prefs.PrefsController;
import com.rai220.securityalarmbot.utils.FabricUtils;
import com.rai220.securityalarmbot.utils.KeyboardUtils;

/**
 *
 */

public class MdModeCommand extends AbstractCommand {
    public MdModeCommand(BotService service) {
        super(service);
    }

    @Override
    public String getCommand() {
        return "/md_mode";
    }

    @Override
    public String getName() {
        return "Motion camera";
    }

    @Override
    public String getDescription() {
        return "Set camera mode for motion detector (all/back/front)";
    }

    @Override
    public boolean isHide() {
        return true;
    }

    @Override
    public boolean execute(Message message, Prefs prefs) {
        final long chatId = message.chat().id();
        if (!PrefsController.instance.isPro()) {
            telegramService.sendMessage(chatId, botService.getString(R.string.only_pro));
            return false;
        } else {
            String text = message.text();
            final CameraMode mode = CameraMode.getByName(text);
            if (mode != null) {
                if (FabricUtils.isAvailable(mode)) {
                    if (mode == CameraMode.ALL) {
                        telegramService.sendMessage(chatId, "Sorry, but ALL mode is not supported yet for motion detection!");
                        // TODO: 09.04.2017 mainK
//                        telegramService.sendMessage(chatId, "Sorry, but ALL mode is not supported yet for motion detection!", mainKeyboard);
                    } else {
                        prefs.mdMode = mode;
                        PrefsController.instance.setPrefs(prefs);
                        telegramService.sendMessage(chatId, "Camera motion detector mode was successful changed to " + mode);
                        telegramService.notifyToOthers(message.from().id(), "changed camera motion detector mode to " + mode);
                    }
                } else {
                    telegramService.sendMessage(chatId, botService.getString(R.string.camera_not_supported));
                }
                return false;
            } else {
                telegramService.sendMessage(chatId, botService.getString(R.string.select_camera_mode),
                        KeyboardUtils.getKeyboard(CameraMode.values()));
                return true;
            }
        }
    }
}
