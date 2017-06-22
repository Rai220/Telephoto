package com.rai220.securityalarmbot.photo;

import com.pengrad.telegrambot.model.Message;
import com.rai220.securityalarmbot.commands.ICommand;
import com.rai220.securityalarmbot.prefs.Prefs;

import java.util.Collection;

/**
 *
 */
public enum SettingsType implements ICommand {
    WHITE_BALANCE("White balance"),
    EXPOSURE("Exposure"),
    IMAGE_SIZE("Image size"),
    FLASH("Flashlight"),
    ROTATION("Rotation"),
    CANCEL("Cancel");

    private String name;

    SettingsType(String name) {
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

    public static SettingsType getByName(String modeName) {
        for (SettingsType mode : values()) {
            if (modeName.equals(mode.getName())) {
                return mode;
            }
        }
        return null;
    }
}
