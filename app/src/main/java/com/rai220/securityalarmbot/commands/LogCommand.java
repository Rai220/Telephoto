package com.rai220.securityalarmbot.commands;

import com.pengrad.telegrambot.model.Message;
import com.rai220.securityalarmbot.BotService;
import com.rai220.securityalarmbot.prefs.Prefs;
import com.rai220.securityalarmbot.utils.L;

/**
 * Created by rai220 on 11/1/16.
 */

public class LogCommand extends AbstractCommand {
    public LogCommand(BotService service) {
        super(service);
    }

    @Override
    public String getCommand() {
        return "/log";
    }

    @Override
    public String getName() {
        return "Send logs file";
    }

    @Override
    public String getDescription() {
        return "Send log file to user";
    }

    @Override
    public boolean isHide() {
        return true;
    }

    @Override
    public boolean execute(Message message, Prefs prefs) {
        final long chatId = message.chat().id();
        telegramService.sendDocument(chatId, L.logsToString().getBytes(), "log.txt");
        return false;
    }
}
