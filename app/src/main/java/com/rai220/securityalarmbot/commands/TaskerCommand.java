package com.rai220.securityalarmbot.commands;

import com.google.common.base.Strings;
import com.pengrad.telegrambot.model.Message;
import com.rai220.securityalarmbot.BotService;
import com.rai220.securityalarmbot.prefs.Prefs;
import com.rai220.securityalarmbot.utils.tasker.TaskerIntent;

/**
 * Created by rai220 on 11/1/16.
 */

public class TaskerCommand extends AbstractCommand {
    public TaskerCommand(BotService service) {
        super(service);
    }

    private final static String TASKER_PACKAGE = "net.dinglisch.android.tasker.ACTION_TASK";

    @Override
    public String getCommand() {
        return "/tasker";
    }

    @Override
    public String getName() {
        return "Tasker command";
    }

    @Override
    public String getDescription() {
        return "Executes tasker command with name";
    }

    @Override
    public boolean isHide() {
        return true;
    }

    @Override
    public boolean execute(Message message, Prefs prefs) {
        final long chatId = message.chat().id();

        String cmd = message.text();
        if (cmd.toLowerCase().startsWith("/tasker")) {
            cmd = cmd.substring(7).trim();
        }
        if (cmd.toLowerCase().startsWith("tasker")) {
            cmd = cmd.substring(6).trim();
        }

        if (!Strings.isNullOrEmpty(cmd)) {
            if ( TaskerIntent.testStatus(botService).equals(TaskerIntent.Status.OK) ) {
                TaskerIntent i = new TaskerIntent(cmd);
                botService.sendBroadcast(i);

                telegramService.sendMessage(chatId, "Command '" + cmd + "' was sent to Tasker.");
                // TODO: 09.04.2017 mainK
//                telegramService.sendMessage(chatId, "Command '" + cmd + "' was sent to Tasker.", mainKeyBoard);
                telegramService.notifyToOthers(message.from().id(), " sent command '" + cmd + "' to tasker.");
            } else {
                telegramService.sendMessage(chatId, "Tasker not ready. Please install it and enable external control.");
                // TODO: 09.04.2017 mainK
//                telegramService.sendMessage(chatId, "Tasker not ready. Please install it and enable external control.", mainKeyBoard);
            }
            return false;
        } else {
            telegramService.sendMessage(chatId, "Type command name:");
            return true;
        }
    }
}
