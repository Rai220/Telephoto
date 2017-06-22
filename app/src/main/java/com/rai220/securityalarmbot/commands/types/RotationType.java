package com.rai220.securityalarmbot.commands.types;

import com.pengrad.telegrambot.model.Message;
import com.rai220.securityalarmbot.commands.ICommand;
import com.rai220.securityalarmbot.prefs.Prefs;

import java.util.Collection;

/**
 *
 */

public enum RotationType implements ICommand {
    ANGLE_0("Angle 0", 0),
    ANGLE_90("Angle 90", 90),
    ANGLE_180("Angle 180", 180),
    ANGLE_270("Angle 270", 270);

    private String name;
    private float angle;

    RotationType(String name, float angle) {
        this.name = name;
        this.angle = angle;
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

    public float getAngle() {
        return angle;
    }

    public static RotationType getByName(String name) {
        for (RotationType rt : values()) {
            if (name.equals(rt.getName()) ||
                    name.toLowerCase().equals(rt.getCommand())) {
                return rt;
            }
        }
        return null;
    }
}
