package com.rai220.securityalarmbot.prefs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pengrad.telegrambot.model.User;
import com.rai220.securityalarmbot.commands.types.SensitivityType;
import com.rai220.securityalarmbot.model.TimeStats;
import com.rai220.securityalarmbot.photo.AlarmType;
import com.rai220.securityalarmbot.photo.CameraMode;
import com.rai220.securityalarmbot.utils.Converters;

import org.joda.time.DateTime;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.fatboyindustrial.gsonjodatime.Converters.registerDateTime;

/**
 * Created by rai220 on 10/14/16.
 */
public class Prefs {
    public static final Gson prefsGson = registerDateTime(new GsonBuilder()).create();

    /** Пары user-id, user-data */
    private Map<Integer, UserPrefs> usersDataById = new HashMap<>();
    private Map<Integer, CameraPrefs> camerasPrefs = new HashMap<>();
    private List<TimeStats> timeStatsList = new LinkedList<>();
    public int minutesPeriod = 0;
    public CameraMode cameraMode = CameraMode.ALL;
    public CameraMode mdMode = CameraMode.FRONT;
    public SensitivityType sensitivity = SensitivityType.MEDIUM;
    public SensitivityType shakeSensitivity = SensitivityType.LOW;
    public AlarmType alarmType = AlarmType.GIF;
    public String password = "";

    public boolean hasEventListeners() {
        for (UserPrefs userPref : usersDataById.values()) {
            if (userPref.isEventListener) {
                return true;
            }
        }
        return false;
    }

    public Set<UserPrefs> getEventListeners() {
        Set<UserPrefs> result = new HashSet<>();
        for (UserPrefs userPref : usersDataById.values()) {
            if (userPref.isEventListener) {
                result.add(userPref);
            }
        }
        return result;
    }

    public CameraPrefs getCameraPrefs(int cameraId) {
        CameraPrefs prefs = camerasPrefs.get(cameraId);
        if (prefs == null) {
            prefs = new CameraPrefs();
            camerasPrefs.put(cameraId, prefs);
        }
        return prefs;
    }

    public void setCamerasPrefs(int cameraId, CameraPrefs prefs) {
        this.camerasPrefs.put(cameraId, prefs);
    }

    public void addUser(UserPrefs user) {
        usersDataById.put(user.id, user);
    }

    public UserPrefs addUser(User user, long chatId) {
        Prefs.UserPrefs newUser = Converters.USER_TO_USERPREFS.apply(user);
        if (newUser != null) {
            newUser.lastChatId = chatId;
        }
        addUser(newUser);
        return newUser;
    }

    public UserPrefs getUser(Integer id) {
        return usersDataById.get(id);
    }

    public UserPrefs getUser(User user) {
        return usersDataById.get(user.id());
    }

    public boolean updateUser(User user) {
        UserPrefs savedUser = getUser(user.id());
        if (savedUser != null) {
            if ((savedUser.userName != null && !savedUser.userName.equals(user.username())) ||
                    (savedUser.userName == null && user.username() != null)) {
                long lastChat = savedUser.lastChatId;
                savedUser = Converters.USER_TO_USERPREFS.apply(user);
                if (savedUser != null) {
                    savedUser.lastChatId = lastChat;
                }
                addUser(savedUser);
                return true;
            }
        }
        return false;
    }

    public Set<UserPrefs> getUsers() {
        Set<UserPrefs> result = new HashSet<>();
        for (UserPrefs user : usersDataById.values()) {
            result.add(user);
        }
        return result;
    }

    public void removeRegisterUsers() {
        usersDataById.clear();
    }

    public void addTimeStats(TimeStats timeStats) {
        timeStatsList.add(timeStats);
    }

    public void removeOldTimeStats() {
        DateTime removeToDate = DateTime.now().withTimeAtStartOfDay().minusWeeks(1);
        Iterator<TimeStats> iter = timeStatsList.iterator();
        while (iter.hasNext()) {
            DateTime dateTime = iter.next().getDateTime();
            if (dateTime.isBefore(removeToDate)) {
                iter.remove();
            } else {
                break;
            }
        }
    }

    public List<TimeStats> getTimeStatsList() {
        sortTimeStatsList(timeStatsList);
        return timeStatsList;
    }

    public List<TimeStats> getTimeStatsList(DateTime from, DateTime to) {
        List<TimeStats> result = new LinkedList<>();
        if (!from.isAfter(to)) {
            int fromIndex = -1;
            int toIndex = -1;
            sortTimeStatsList(timeStatsList);
            for (TimeStats stat : timeStatsList) {
                if (fromIndex == -1 && !from.isAfter(stat.getDateTime())) {
                    fromIndex = timeStatsList.indexOf(stat);
                } else if (stat.getDateTime().isAfter(to)) {
                    toIndex = timeStatsList.indexOf(stat);
                    break;
                }
            }
            if (fromIndex != -1) {
                if (toIndex == -1 || toIndex > timeStatsList.size()) {
                    toIndex = timeStatsList.size();
                }
                result = timeStatsList.subList(fromIndex, toIndex);
            }
        }
        return result;
    }

    private void sortTimeStatsList(List<TimeStats> timeStatsList) {
        Collections.sort(timeStatsList, new Comparator<TimeStats>() {
            @Override
            public int compare(TimeStats ts1, TimeStats ts2) {
                return ts1.getDateTime().compareTo(ts2.getDateTime());
            }
        });
    }

    public static final class UserPrefs {
        public int id = 0;
        public String userName = null;
        public boolean isNick = false;
        public long lastChatId = 0;
        public boolean isEventListener = true;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            UserPrefs userPrefs = (UserPrefs) o;

            return id == userPrefs.id;
        }

        @Override
        public int hashCode() {
            return id;
        }
    }

    public static final class CameraPrefs {
        public int height = 0;
        public int width = 0;
        public float angle = 0;
        public String flashMode;
        public String wb;
    }
}
