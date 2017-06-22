package com.rai220.securityalarmbot.commands;

import android.hardware.Camera;

import com.google.common.base.Splitter;
import com.pengrad.telegrambot.model.Message;
import com.rai220.securityalarmbot.BotService;
import com.rai220.securityalarmbot.R;
import com.rai220.securityalarmbot.commands.types.RotationType;
import com.rai220.securityalarmbot.commands.types.SwitchType;
import com.rai220.securityalarmbot.photo.SelectCamera;
import com.rai220.securityalarmbot.photo.SettingsType;
import com.rai220.securityalarmbot.prefs.Prefs;
import com.rai220.securityalarmbot.prefs.PrefsController;
import com.rai220.securityalarmbot.utils.FabricUtils;
import com.rai220.securityalarmbot.utils.KeyboardUtils;
import com.rai220.securityalarmbot.utils.L;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CameraSettingsCommand extends AbstractCommand {
    public CameraSettingsCommand(BotService service) {
        super(service);
    }

    private volatile SelectCamera selectedCamera = null;
    private volatile SettingsType selectedSetting = null;

    @Override
    public String getCommand() {
        return "/camera_settings";
    }

    @Override
    public String getName() {
        return "Cameras settings";
    }

    @Override
    public String getDescription() {
        return "Configure cameras";
    }

    @Override
    public boolean execute(Message message, Prefs prefs) {
        final long chatId = message.chat().id();

        String text = message.text();

        SelectCamera cameraType = SelectCamera.getByName(text);
        SettingsType settingsType = SettingsType.getByName(text);

        if ((cameraType != null && cameraType.equals(SelectCamera.CANCEL)) ||
                (settingsType != null && settingsType.equals(SettingsType.CANCEL))) {
            telegramService.sendMessage(chatId, botService.getString(R.string.operation_cancel));
            // TODO: 09.04.2017 mainK
//            telegramService.sendMessage(chatId, botService.getString(R.string.operation_cancel), mainKeyBoard);
            selectedSetting = null;
            selectedCamera = null;
            return false;
        } else if (cameraType == null && settingsType == null && selectedSetting != null) {
            return processSelectedSetting(chatId, text);
        } else if (settingsType != null) {
            boolean isProc = processSettings(chatId, settingsType);
            if (!isProc) {
                selectedSetting = null;
                selectedCamera = null;
            }
            return isProc;
        } else {
            // Если у нас только одна камера - выбираем её
            if (cameraType == null && Camera.getNumberOfCameras() == 1) {
                cameraType = SelectCamera.BACK;
            }

            if (cameraType == null) {
                selectedSetting = null;
                StringBuilder settingsStr = new StringBuilder();
                settingsStr.append(botService.getString(R.string.settings_camera));
                int[] availCameras = FabricUtils.getAvailableCameras();
                for (int i = 0; i < availCameras.length; i++) {
                    SelectCamera camera = SelectCamera.getByNum(i);
                    if (camera != null) {
                        settingsStr.append("\n\n").append(camera.getName()).append(":");

                        Prefs.CameraPrefs cameraPrefs = prefs.getCameraPrefs(camera.getNumber());
                        settingsStr.append(String.format(botService.getString(R.string.resolution), cameraPrefs.width, cameraPrefs.height));
                        settingsStr.append(botService.getString(R.string.flash_mode)).append(cameraPrefs.flashMode);
                        settingsStr.append(botService.getString(R.string.white_balance)).append(cameraPrefs.wb).append("\n");
                        settingsStr.append("Rotation angle: ").append(cameraPrefs.angle);
                    }
                }
                settingsStr.append("\n");
                settingsStr.append(botService.getString(R.string.camera_settings_choose));
                telegramService.sendMessage(chatId, settingsStr.toString(),
                        KeyboardUtils.getKeyboard(SelectCamera.values()));
                return true;
            } else {
                selectedCamera = cameraType;
                selectedSetting = null;
                telegramService.sendMessage(chatId, botService.getString(R.string.camera_settings_what_command),
                        KeyboardUtils.getKeyboard(SettingsType.values()));
                return true;
            }
        }
    }

    private boolean processSelectedSetting(long chatId, String text) {
        Camera.Parameters params = botService.getCamera().getParameters(selectedCamera.getNumber());
        Prefs prefs = PrefsController.instance.getPrefs();
        switch (selectedSetting) {
            case IMAGE_SIZE:
                try {
                    List<String> parts = Splitter.on("x").trimResults().omitEmptyStrings().splitToList(text);
                    int w = Integer.valueOf(parts.get(0));
                    int h = Integer.valueOf(parts.get(1));
                    for (Camera.Size size : params.getSupportedPreviewSizes()) {
                        if (size.width == w && size.height == h) {
                            Prefs.CameraPrefs cameraPrefs = prefs.getCameraPrefs(selectedCamera.getNumber());
                            cameraPrefs.width = w;
                            cameraPrefs.height = h;

                            telegramService.sendMessage(chatId, botService.getString(R.string.camera_settings_selected_size) + w + " x " + h);
                            // TODO: 09.04.2017 mainK
//                            telegramService.sendMessage(chatId, botService.getString(R.string.camera_settings_selected_size) + w + " x " + h, mainKeyBoard);
                        }
                    }
                } catch (Throwable ex) {
                    telegramService.sendMessage(chatId, botService.getString(R.string.camera_settings_incorrect_size));
                    // TODO: 09.04.2017 mainK
//                    telegramService.sendMessage(chatId, botService.getString(R.string.camera_settings_incorrect_size), mainKeyBoard);
                    L.e(ex);
                }
                break;
            case WHITE_BALANCE:
                if (params.getSupportedWhiteBalance().contains(text)) {
                    prefs.getCameraPrefs(selectedCamera.getNumber()).wb = text;
                    telegramService.sendMessage(chatId, botService.getString(R.string.settings_selected_wb) + text);
                    // TODO: 09.04.2017 mainK
//                    telegramService.sendMessage(chatId, botService.getString(R.string.settings_selected_wb) + text, mainKeyBoard);
                } else {
                    telegramService.sendMessage(chatId, "Incorrect white balance!");
                    // TODO: 09.04.2017 mainK
//                    telegramService.sendMessage(chatId, "Incorrect white balance!", mainKeyBoard);
                }
                break;
            case ROTATION:
                RotationType rt = RotationType.getByName(text);
                if (rt != null) {
                    Prefs.CameraPrefs cameraPrefs = prefs.getCameraPrefs(selectedCamera.getNumber());
                    cameraPrefs.angle = rt.getAngle();
                    telegramService.sendMessage(chatId, "Camera rotation angle changed to " + rt.getAngle());
                    // TODO: 09.04.2017 mainK
//                    telegramService.sendMessage(chatId, "Camera rotation angle changed to " + rt.getAngle(), mainKeyBoard);
                } else {
                    telegramService.sendMessage(chatId, "Incorrect rotation type!");
                    // TODO: 09.04.2017 mainK
//                    telegramService.sendMessage(chatId, "Incorrect rotation type!", mainKeyBoard);
                }
                break;
            case FLASH:
                SwitchType type = SwitchType.getByName(text);
                if (type != null) {
                    if (type.equals(SwitchType.ON) && selectedCamera.equals(SelectCamera.FRONT)) {
                        telegramService.sendMessage(chatId, botService.getString(R.string.flash_on_front_camera));
                        // TODO: 09.04.2017 mainK
//                        telegramService.sendMessage(chatId, botService.getString(R.string.flash_on_front_camera), mainKeyBoard);
                    } else {
                        Prefs.CameraPrefs cameraPrefs = prefs.getCameraPrefs(selectedCamera.getNumber());
                        cameraPrefs.flashMode = text;
                        telegramService.sendMessage(chatId, botService.getString(R.string.camera_settings_selected_flash) + text);
                        // TODO: 09.04.2017 mainK
//                        telegramService.sendMessage(chatId, botService.getString(R.string.camera_settings_selected_flash) + text, mainKeyBoard);
                    }
                } else {
                    telegramService.sendMessage(chatId, "Incorrect flash mode!");
                    // TODO: 09.04.2017 mainK
//                    telegramService.sendMessage(chatId, "Incorrect flash mode!", mainKeyBoard);
                }
                break;
        }

        PrefsController.instance.setPrefs(prefs);
        selectedSetting = null;
        selectedCamera = null;
        return false;
    }

    private void postError(long chatId) {
        telegramService.sendMessage(chatId, botService.getString(R.string.camera_settings_error));
        // TODO: 09.04.2017 msinK
//        telegramService.sendMessage(chatId, botService.getString(R.string.camera_settings_error), mainKeyBoard);
    }

    private boolean processSettings(long chatId, SettingsType settingsType) {
        selectedSetting = settingsType;
        if (selectedCamera != null) {
            Camera.Parameters params = botService.getCamera().getParameters(selectedCamera.getNumber());
            if (params != null) {
                switch (settingsType) {
                    case IMAGE_SIZE:
                        List<ICommand> commands = new ArrayList<>();
                        for (Camera.Size size : params.getSupportedPreviewSizes()) {
                            commands.add(new CommandSize(size.width + " x " + size.height));
                        }
                        commands.add(new CommandSize("Cancel"));
                        telegramService.sendMessage(chatId,
                                botService.getString(R.string.camera_settings_choose_resolution), KeyboardUtils.getKeyboard(commands));
                        return true;
                    case FLASH:
                        telegramService.sendMessage(chatId,
                                "Choose flash type: ", KeyboardUtils.getKeyboard(SwitchType.values()));
                        return true;
                    case ROTATION:
                        telegramService.sendMessage(chatId,
                                "Choose rotation angle: ", KeyboardUtils.getKeyboard(RotationType.values()));
                        return true;
                    case WHITE_BALANCE:
                        List<ICommand> wbCommands = new ArrayList<>();
                        for (String wb : params.getSupportedWhiteBalance()) {
                            wbCommands.add(new CommandSize(wb));
                        }
                        wbCommands.add(new CommandSize("Cancel"));
                        telegramService.sendMessage(chatId, "Choose white balance type:", KeyboardUtils.getKeyboard(wbCommands));
                        return true;
                    default:
                        telegramService.sendMessage(chatId, botService.getString(R.string.camera_settings_not_supported));
                        // TODO: 09.04.2017 msinK
//                        telegramService.sendMessage(chatId, botService.getString(R.string.camera_settings_not_supported), mainKeyBoard);
                        return false;
                }
            }
        }
        postError(chatId);
        return false;
    }

    public static final class CommandSize implements ICommand {
        private final String sizeText;

        public CommandSize(String sizeText) {
            this.sizeText = sizeText;
        }

        @Override
        public String getCommand() {
            return "/" + sizeText.toLowerCase();
        }

        @Override
        public String getName() {
            return sizeText;
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public boolean isEnable() {
            return true;
        }

        @Override
        public boolean isHide() {
            return false;
        }

        @Override
        public boolean execute(Message message, Prefs prefs) {
            return false;
        }

        @Override
        public Collection<ICommand> execute(Message message) {
            return null;
        }
    }
}
