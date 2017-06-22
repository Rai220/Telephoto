package com.rai220.securityalarmbot.commands;

import com.pengrad.telegrambot.model.Message;
import com.rai220.securityalarmbot.BotService;
import com.rai220.securityalarmbot.R;
import com.rai220.securityalarmbot.prefs.Prefs;

/**
 *
 */
public class HelpCommand extends AbstractCommand {

    public HelpCommand(BotService service) {
        super(service);
    }

    @Override
    public String getCommand() {
        return "/help";
    }

    @Override
    public String getName() {
        return "Help";
    }

    @Override
    public String getDescription() {
        return "Shows help information";
    }

    @Override
    public boolean isHide() {
        return true;
    }

    @Override
    public boolean execute(Message message, Prefs prefs) {
        final long chatId = message.chat().id();
        String result = botService.getString(R.string.help_support_group);
        telegramService.sendMessage(chatId, result);
        return false;
    }
}
