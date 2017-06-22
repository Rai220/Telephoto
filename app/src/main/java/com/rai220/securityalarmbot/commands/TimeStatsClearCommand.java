package com.rai220.securityalarmbot.commands;

import com.pengrad.telegrambot.model.Message;
import com.rai220.securityalarmbot.BotService;
import com.rai220.securityalarmbot.prefs.Prefs;
import com.rai220.securityalarmbot.prefs.PrefsController;

/**
 *
 */
public class TimeStatsClearCommand extends AbstractCommand {

    public TimeStatsClearCommand(BotService service) {
        super(service);
    }

    @Override
    public String getCommand() {
        return "/clear_time_stats";
    }

    @Override
    public String getName() {
        return "Clear time stats";
    }

    @Override
    public String getDescription() {
        return "Clear stats of all time";
    }

    @Override
    public boolean isHide() {
        return true;
    }

    @Override
    public boolean execute(Message message, Prefs prefs) {
        prefs.getTimeStatsList().clear();
        PrefsController.instance.setPrefs(prefs);
        telegramService.sendMessage(message.chat().id(), "Time stats was cleared");
        return false;
    }
}
