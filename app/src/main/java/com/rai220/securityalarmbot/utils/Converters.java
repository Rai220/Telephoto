package com.rai220.securityalarmbot.utils;

import android.support.annotation.NonNull;

import com.google.common.base.Function;
import com.pengrad.telegrambot.model.User;
import com.rai220.securityalarmbot.commands.ICommand;
import com.rai220.securityalarmbot.model.TimeStats;
import com.rai220.securityalarmbot.prefs.Prefs;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 *
 */

public class Converters {

    private Converters() {
    }

    public static final Function<Prefs.UserPrefs, String> USER_NAME_CONVERTER = new Function<Prefs.UserPrefs, String>() {
        @NonNull
        @Override
        public String apply(Prefs.UserPrefs input) {
            return input.isNick ? "@".concat(input.userName) :
                    "@".concat(String.valueOf(input.id)).concat(" (").concat(input.userName).concat(")");
        }
    };

    public static final Function<User, Prefs.UserPrefs> USER_TO_USERPREFS = new Function<User, Prefs.UserPrefs>() {
        @NonNull
        @Override
        public Prefs.UserPrefs apply(User user) {
            Prefs.UserPrefs newUser = new Prefs.UserPrefs();
            newUser.id = user.id();
            newUser.isNick = user.username() != null;
            if (newUser.isNick) {
                newUser.userName = user.username();
            } else {
                newUser.userName = FabricUtils.defaultIfBlank(user.firstName(), "") +
                        FabricUtils.defaultIfBlank(user.lastName(), "");
            }
            return newUser;
        }
    };

    public static final Function<List<TimeStats>, List<Float>> TIME_STATS_TO_TEMPERATURE_LIST = new Function<List<TimeStats>, List<Float>>() {
        @NonNull
        @Override
        public List<Float> apply(List<TimeStats> input) {
            List<Float> result = new LinkedList<>();
            for (TimeStats stats : input) {
                Float batteryTemperature = stats.getBatteryTemperature();
                if (batteryTemperature != null) {
                    result.add(batteryTemperature);
                }
            }
            return result;
        }
    };

    public static final Function<List<TimeStats>, List<Float>> TIME_STATS_TO_BATTERY_LEVEL = new Function<List<TimeStats>, List<Float>>() {
        @NonNull
        @Override
        public List<Float> apply(List<TimeStats> input) {
            List<Float> result = new LinkedList<>();
            for (TimeStats stats : input) {
                Float batteryLevel = stats.getBatteryLevel();
                if (batteryLevel != null) {
                    result.add(batteryLevel);
                }
            }
            return result;
        }
    };

    public static final Function<Collection<ICommand>, String> COMMANDS_TO_STRING = new Function<Collection<ICommand>, String>() {
        @NonNull
        @Override
        public String apply(Collection<ICommand> input) {
            StringBuilder result = new StringBuilder();
            for (ICommand command : input) {
                String name = FabricUtils.defaultIfBlank(command.getCommand(), "<empty>");
                String description = FabricUtils.defaultIfBlank(command.getDescription(), "<empty>");
                result.append(name).append(" - ").append(description).append(Constants.LS);
            }
            return result.toString();
        }
    };

}
