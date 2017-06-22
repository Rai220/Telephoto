package com.rai220.securityalarmbot.commands.types;

import com.pengrad.telegrambot.model.Message;
import com.rai220.securityalarmbot.commands.ICommand;
import com.rai220.securityalarmbot.prefs.Prefs;

import java.util.Collection;

/**
 * Switch type for motion detector
 */

public enum MdSwitchType implements ICommand {
    ON("On"),
    OFF("Off"),
    AUTO("Auto");

    private String name;

    MdSwitchType(String name) {
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


    public static MdSwitchType getByName(String name) {
        for (MdSwitchType st : values()) {
            if (name.equals(st.getName()) ||
                    name.toLowerCase().equals(st.getCommand())) {
                return st;
            }
        }
        return null;
    }
}
