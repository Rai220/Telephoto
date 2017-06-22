package com.rai220.securityalarmbot.commands.types;

import com.pengrad.telegrambot.model.Message;
import com.rai220.securityalarmbot.commands.ICommand;
import com.rai220.securityalarmbot.prefs.Prefs;

import java.util.Collection;

/**
 *
 */

public enum SensitivityType implements ICommand {
    HIGH("High", 30f, 0.5f),
    MEDIUM("Medium", 50f, 5f),
    LOW("Low", 70f, 10f);

    private String name;
    private float mdValue;
    private float shakeValue;

    SensitivityType(String name, float mdValue, float shakeValue) {
        this.name = name;
        this.mdValue = mdValue;
        this.shakeValue = shakeValue;
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

    public float getMdValue() {
        return mdValue;
    }

    public float getShakeValue() {
        return shakeValue;
    }

    public static SensitivityType getByName(String name) {
        for (SensitivityType type : values()) {
            if (name.equals(type.getName())) {
                return type;
            }
        }
        return null;
    }

}
