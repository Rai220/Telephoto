package com.rai220.securityalarmbot.photo;

import android.hardware.Camera;

import com.pengrad.telegrambot.model.Message;
import com.rai220.securityalarmbot.commands.ICommand;
import com.rai220.securityalarmbot.prefs.Prefs;

import java.util.Collection;

/**
 *
 */

public enum CameraMode implements ICommand {
    ALL(-1, "All"),
    BACK(Camera.CameraInfo.CAMERA_FACING_BACK, "Back"),
    FRONT(Camera.CameraInfo.CAMERA_FACING_FRONT, "Front");

    private int number;
    private String name;

    CameraMode(int number, String name) {
        this.number = number;
        this.name = name;
    }

    public int getNumber() {
        return number;
    }

    public static CameraMode getByName(String modeName) {
        for (CameraMode mode : values()) {
            if (modeName.equals(mode.getName())) {
                return mode;
            }
        }
        return null;
    }

    public static CameraMode getById(int id) {
        for (CameraMode mode : values()) {
            if (id == mode.getNumber()) {
                return mode;
            }
        }
        return null;
    }

    public static String getAvailables() {
        StringBuilder res = new StringBuilder();
        for (CameraMode mode : values()) {
            res.append(mode).append("/");
        }
        res.deleteCharAt(res.length() - 1);
        return res.toString();
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
}
