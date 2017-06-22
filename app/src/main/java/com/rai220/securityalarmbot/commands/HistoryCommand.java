package com.rai220.securityalarmbot.commands;

import com.pengrad.telegrambot.model.Message;
import com.rai220.securityalarmbot.BotService;
import com.rai220.securityalarmbot.prefs.Prefs;

/**
 *
 */

public class HistoryCommand extends AbstractCommand {

    public HistoryCommand(BotService service) {
        super(service);
    }

    @Override
    public String getCommand() {
        return "/history";
    }

    @Override
    public String getName() {
        return "History";
    }

    @Override
    public String getDescription() {
        return "Get history as GIF";
    }

    @Override
    public boolean execute(final Message message, Prefs prefs) {
        byte[] gif = botService.getObserver().getGif();
        telegramService.sendDocument(message.chat().id(), gif, "gif.gif");
        return false;
    }
}
