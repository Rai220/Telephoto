package com.rai220.securityalarmbot.commands;

import com.pengrad.telegrambot.model.Message;
import com.rai220.securityalarmbot.BotService;
import com.rai220.securityalarmbot.model.TimeStats;
import com.rai220.securityalarmbot.prefs.Prefs;

import java.util.List;

/**
 *
 */
public class TimeStatsCommand extends AbstractCommand {

    public TimeStatsCommand(BotService service) {
        super(service);
    }

    @Override
    public String getCommand() {
        return "/time_stats";
    }

    @Override
    public String getName() {
        return "Time stats";
    }

    @Override
    public String getDescription() {
        return "Show device statistics";
    }

    @Override
    public boolean isHide() {
        return true;
    }

    @Override
    public boolean execute(Message message, Prefs prefs) {
        List<TimeStats> timeStatsList = prefs.getTimeStatsList();
        StringBuilder result = new StringBuilder();
        result.append("Battery stats: ");
        if (!timeStatsList.isEmpty()) {
            int i = 0;
            for (TimeStats timeStats : timeStatsList) {
                i++;
                result.append("\n").append(timeStats.toString());
                if (i >= 15) {
                    telegramService.sendMessage(message.chat().id(), result.toString());
                    result.setLength(0);
                    i = 0;
                }
            }
            if (result.length() > 0) {
                telegramService.sendMessage(message.chat().id(), result.toString());
            }
        } else {
            result.append("empty");
            telegramService.sendMessage(message.chat().id(), result.toString());
        }
        return false;
    }
}
