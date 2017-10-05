package com.rai220.securityalarmbot.commands;

import com.google.gson.Gson;
import com.pengrad.telegrambot.model.Message;
import com.rai220.securityalarmbot.BotService;
import com.rai220.securityalarmbot.prefs.Prefs;

/**
 *
 */
public class SetMatrixCommand extends AbstractCommand {

    public SetMatrixCommand(BotService service) {
        super(service);
    }

    @Override
    public String getCommand() {
        return "/matrix";
    }

    @Override
    public String getName() {
        return "Motion detector matrix";
    }

    @Override
    public String getDescription() {
        return "Set activation matrix for motion detector";
    }

    @Override
    public boolean isHide() {
        return true;
    }

    @Override
    public boolean execute(Message message, Prefs prefs) {
        final long chatId = message.chat().id();

        try {
            Gson gson = new Gson();
            int[][] matrix = gson.fromJson(message.text().trim(), int[][].class);
            String result = botService.getDetector().setMatrix(matrix);
            telegramService.sendMessage(chatId, result);
            return false;
        } catch (Throwable ex) {
            telegramService.sendMessage(chatId, "Please configure matrix!");
            return true;
        }
    }
}
