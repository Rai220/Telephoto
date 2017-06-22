package com.rai220.securityalarmbot.commands;

import com.pengrad.telegrambot.model.Message;
import com.rai220.securityalarmbot.BotService;
import com.rai220.securityalarmbot.telegram.TelegramService;

import java.util.Collection;

/**
 *
 */

public abstract class AbstractCommand implements ICommand {

    protected final BotService botService;
    protected final TelegramService telegramService;
    private boolean isEnable = true;
    private boolean isHide = false;

    public AbstractCommand(BotService service) {
        botService = service;
        telegramService = botService.getTelegramService();
    }

    @Override
    public boolean isEnable() {
        return isEnable;
    }

    @Override
    public boolean isHide() {
        return isHide;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ICommand other = (ICommand) o;
        return this.getName().equals(other.getName());

    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public Collection<ICommand> execute(Message message) {
        return null;
    }

    public void setEnable(boolean enable) {
        isEnable = enable;
    }

    public void setHide(boolean hide) {
        isHide = hide;
    }

}
