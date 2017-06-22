package com.rai220.securityalarmbot.commands;

import com.pengrad.telegrambot.model.Message;
import com.rai220.securityalarmbot.BotService;
import com.rai220.securityalarmbot.R;
import com.rai220.securityalarmbot.photo.CameraTask;
import com.rai220.securityalarmbot.photo.ImageShot;
import com.rai220.securityalarmbot.prefs.Prefs;
import com.rai220.securityalarmbot.prefs.PrefsController;
import com.rai220.securityalarmbot.utils.FabricUtils;
import com.rai220.securityalarmbot.utils.L;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 */
public class HDPhotoCommand extends AbstractCommand {

    private final ExecutorService es = Executors.newCachedThreadPool();

    public HDPhotoCommand(BotService service) {
        super(service);
    }

    @Override
    public String getCommand() {
        return "/hd_photo";
    }

    @Override
    public String getName() {
        return "HD Photo";
    }

    @Override
    public String getDescription() {
        return "Take a HD photo";
    }

    @Override
    public boolean isHide() {
        return true;
    }

    @Override
    public boolean execute(final Message message, Prefs prefs) {
        es.submit(new Runnable() {
            @Override
            public void run() {
                final Long chatId = message.chat().id();
                if (PrefsController.instance.isPro()) {
                    final int[] cameraIds = FabricUtils.getSelectedCameras();
                    for (int cameraId : cameraIds) {
                        boolean addTaskResult = botService.getCamera().addTask(new CameraTask(cameraId, 10000, 10000) {
                            @Override
                            public void processResult(ImageShot shot) {
                                if (shot != null) {
                                    byte[] jpeg = shot.toGoodQuality();
                                    telegramService.sendDocument(chatId, jpeg, "image.jpg");
                                    telegramService.notifyToOthers(message.from().id(), "took HD photo");
                                } else {
                                    telegramService.sendMessage(chatId, botService.getString(R.string.camera_init_error));
                                }
                            }
                        }, false);

                        if (!addTaskResult) {
                            telegramService.sendMessage(chatId, botService.getString(R.string.camera_busy));
                            L.e("Camera busy!");
                        }
                    }
                } else {
                    telegramService.sendMessage(chatId, botService.getString(R.string.only_pro));
                }
            }
        });
        return false;
    }
}
