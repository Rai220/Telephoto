package com.rai220.securityalarmbot.photo;

import com.pengrad.telegrambot.model.Message;
import com.rai220.securityalarmbot.commands.ICommand;
import com.rai220.securityalarmbot.prefs.Prefs;

import java.util.Collection;

/**
 *
 */

public enum AlarmType implements ICommand {
    GIF("GIF"),
    GIF_AND_PHOTO("GIF and photo"),
    GIF_AND_3_PHOTOS("GIF and 3 photos"),
    CANCEL("Cancel");

    private String name;

    AlarmType(String name) {
        this.name = name;
    }

    @Override
    public String getCommand() {
        return "/" + getName().toLowerCase();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public boolean isEnable() {
        return true;
    }

    @Override
    public boolean isHide() {
        return false;
    }

    @Override
    public boolean execute(Message message, Prefs prefs) {
        return false;
    }

    @Override
    public Collection<ICommand> execute(Message message) {
        return null;
    }

    public static AlarmType getByName(String modeName) {
        for (AlarmType mode : values()) {
            if (modeName.equals(mode.getName())) {
                return mode;
            }
        }
        return null;
    }
}
