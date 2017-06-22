package com.rai220.securityalarmbot.commands;

import android.content.Intent;

import com.pengrad.telegrambot.model.Message;
import com.rai220.securityalarmbot.BotService;
import com.rai220.securityalarmbot.prefs.Prefs;
import com.rai220.securityalarmbot.utils.L;

/**
 * Created by rai220 on 11/1/16.
 */
public class RestartCommand extends AbstractCommand {
    public RestartCommand(BotService service) {
        super(service);
    }

    @Override
    public String getCommand() {
        return "/restart";
    }

    @Override
    public String getName() {
        return "Restart the bot";
    }

    @Override
    public String getDescription() {
        return "Restart the bot for troubleshooting";
    }

    @Override
    public boolean isHide() {
        return true;
    }

    @Override
    public boolean execute(Message message, Prefs prefs) {
        final long chatId = message.chat().id();

        telegramService.sendMessage(chatId, "Bot will be restarted.");
        telegramService.notifyToOthers(message.from().id(), " restarted the bot.");

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                    botService.stopSelf();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(5000);
                                Intent intent = new Intent(botService, BotService.class);
                                botService.startService(intent);
                            } catch (Throwable ex) {
                                L.e(ex);
                            }
                        }
                    }).start();
                } catch (Throwable ex) {
                    L.e(ex);
                }
            }
        }).start();

        return false;
    }
}
