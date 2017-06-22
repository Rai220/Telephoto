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

public class CameraModeCommand extends AbstractCommand {

    public CameraModeCommand(BotService service) {
        super(service);
    }

    @Override
    public String getCommand() {
        return "/camera_mode";
    }

    @Override
    public String getName() {
        return "Camera mode";
    }

    @Override
    public String getDescription() {
        return "Set camera mode (all/back/front)";
    }

    @Override
    public boolean execute(Message message, Prefs prefs) {
        final long chatId = message.chat().id();
        String text = message.text();
        final CameraMode mode = CameraMode.getByName(text);
        if (mode != null) {
            if (FabricUtils.isAvailable(mode)) {
                prefs.cameraMode = mode;
                PrefsController.instance.setPrefs(prefs);
                telegramService.sendMessage(chatId, botService.getString(R.string.camera_mode_changed) + mode);
                // TODO: 09.04.2017 mainK
//                telegramService.sendMessage(chatId, botService.getString(R.string.camera_mode_changed) + mode, mainKeyBoard);
                telegramService.notifyToOthers(message.from().id(), botService.getString(R.string.user_changed_camera_mode) + mode);
            } else {
                telegramService.sendMessage(chatId, botService.getString(R.string.camera_not_supported));
                // TODO: 09.04.2017 mainK
//                telegramService.sendMessage(chatId, botService.getString(R.string.camera_not_supported), mainKeyBoard);
            }
            return false;
        } else {
            telegramService.sendMessage(chatId, botService.getString(R.string.select_camera_mode), KeyboardUtils.getKeyboard(CameraMode.values()));
            return true;
        }
    }

}
