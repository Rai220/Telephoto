package com.rai220.securityalarmbot.utils;

import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
import com.rai220.securityalarmbot.commands.ICommand;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 *
 */

public class KeyboardUtils {

    private KeyboardUtils() {
    }

    public static ReplyKeyboardMarkup getKeyboard(Collection<ICommand> values) {
        return getKeyboard(values.toArray(new ICommand[values.size()]));
    }

    public static ReplyKeyboardMarkup getKeyboard(ICommand[] values) {
        String[][] keyboard = getStrings(values, false);
        return new ReplyKeyboardMarkup(keyboard, true, false, false);
    }

    public static ReplyKeyboardMarkup getKeyboardIgnoreHide(ICommand[] values) {
        String[][] keyboard = getStrings(values, true);
        return new ReplyKeyboardMarkup(keyboard, true, false, false);
    }

    private static String[][] getStrings(ICommand[] values, boolean ignoreHide) {
        List<String> commandsName = new ArrayList<>();
        for (ICommand val : values) {
            if (val != null && val.isEnable() && (ignoreHide || !val.isHide())) {
                commandsName.add(val.getName());
            }
        }
        int mod = commandsName.size() % 3;
        int h = commandsName.size() / 3 + (mod != 0 ? 1 : 0);
        String[][] keyboard = new String[h][3];
        Iterator<String> iter = commandsName.iterator();
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < 3; j++) {
                keyboard[i][j] = iter.hasNext() ? iter.next() : "";
            }
        }
        return keyboard;
    }

}
