package com.rai220.securityalarmbot.commands;

import com.pengrad.telegrambot.model.Message;
import com.rai220.securityalarmbot.BotService;
import com.rai220.securityalarmbot.R;
import com.rai220.securityalarmbot.photo.CameraTask;
import com.rai220.securityalarmbot.photo.ImageShot;
import com.rai220.securityalarmbot.prefs.Prefs;
import com.rai220.securityalarmbot.telegram.TelegramService;
import com.rai220.securityalarmbot.utils.FabricUtils;
import com.rai220.securityalarmbot.utils.L;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 */
public class PhotoCommand extends AbstractCommand {
    private static final ExecutorService es = Executors.newFixedThreadPool(1);

    public PhotoCommand(BotService service) {
        super(service);
    }

    @Override
    public String getCommand() {
        return "/photo";
    }

    @Override
    public String getName() {
        return "Photo";
    }

    @Override
    public String getDescription() {
        return "Take a photo";
    }

    @Override
    public boolean execute(final Message message, Prefs prefs) {
        getPhoto(botService, message.chat().id(), message.from().id());
        return false;
    }

    public static void getPhoto(final BotService botService, final Long chatId, final Integer fromId) {
        es.submit(new Runnable() {
            @Override
            public void run() {
                final TelegramService telegramService = botService.getTelegramService();
                final int[] cameraIds = FabricUtils.getSelectedCameras();
                for (int cameraId : cameraIds) {
                    boolean addTaskResult = botService.getCamera().addTask(new CameraTask(cameraId) {
                        @Override
                        public void processResult(ImageShot shot) {
                            if (shot != null) {
                                byte[] jpeg = shot.toYuvByteArray();
                                if (chatId != null && fromId != null) {
                                    telegramService.sendPhoto(chatId, jpeg);
                                    telegramService.notifyToOthers(fromId, botService.getString(R.string.get_photo));
                                } else {
                                    telegramService.sendPhotoToAll(jpeg);
                                }
                            } else {
                                if (chatId != null && fromId != null) {
                                    telegramService.sendMessage(chatId, botService.getString(R.string.camera_init_error));
                                } else {
                                    telegramService.sendMessageToAll(botService.getString(R.string.camera_init_error));
                                }
                            }
                        }
                    }, false);

                    if (!addTaskResult) {
                        telegramService.sendMessage(chatId, botService.getString(R.string.camera_busy));
                        L.e("Camera busy!");
                    }
                }
            }
        });
    }

}
