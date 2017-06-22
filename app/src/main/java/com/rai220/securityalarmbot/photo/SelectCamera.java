package com.rai220.securityalarmbot.photo;

import android.hardware.Camera;

import com.pengrad.telegrambot.model.Message;
import com.rai220.securityalarmbot.commands.ICommand;
import com.rai220.securityalarmbot.prefs.Prefs;

import java.util.Collection;

/**
 *
 */
public enum SelectCamera implements ICommand {
    BACK(Camera.CameraInfo.CAMERA_FACING_BACK, "Back"),
    FRONT(Camera.CameraInfo.CAMERA_FACING_FRONT, "Front"),
    CANCEL(-1, "Cancel");

    private int number;
    private String modeName;

    SelectCamera(int number, String modeName) {
        this.number = number;
        this.modeName = modeName;
    }

    @Override
    public String getCommand() {
        return "/" + getName().toLowerCase();
    }

    @Override
    public String getName() {
        return modeName;
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

    public int getNumber() {
        return number;
    }

    public static SelectCamera getByNum(int number) {
        for (SelectCamera mode : values()) {
            if (number == mode.getNumber()) {
                return mode;
            }
        }
        return null;
    }

    public static SelectCamera getByName(String modeName) {
        for (SelectCamera mode : values()) {
            if (modeName.equals(mode.getName())) {
                return mode;
            }
        }
        return null;
    }

}
