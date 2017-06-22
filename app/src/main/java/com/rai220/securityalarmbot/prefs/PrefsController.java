package com.rai220.securityalarmbot.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.common.base.Strings;
import com.rai220.securityalarmbot.utils.FabricUtils;

import static com.rai220.securityalarmbot.prefs.Prefs.prefsGson;

/**
 * Created by rai220 on 10/14/16.
 */

public class PrefsController {
    public static final PrefsController instance = new PrefsController();

    private static final String PREFS_CODE = "PREFS_CODE";
    private static final String TELEGRAM_BOT_TOKEN = "TELEGRAM_BOT_TOKEN";
    private static final String AUTORUN = "AUTORUN";
    private static final String IS_HELP_SHOWN = "IS_HELP_SHOWN";
    private static final String LAST_MD_DETECTED = "LAST_MD_DETECTED";
    private static final String IS_PRO = "IS_PRO";

    private volatile SharedPreferences preferences;

    public synchronized void init(Context context) {
        if (preferences == null) {
            preferences = PreferenceManager.getDefaultSharedPreferences(context);
        }
    }

    private PrefsController() {
    }

    public boolean hasToken() {
        return !Strings.isNullOrEmpty(getToken());
    }

    public String getToken() {
        return preferences.getString(TELEGRAM_BOT_TOKEN, "");
    }

    public void setToken(String newToken) {
        newToken = newToken.replaceAll("\\s+", "");
        preferences.edit().putString(TELEGRAM_BOT_TOKEN, newToken).apply();
    }

    public boolean isHelpShown() {
        return preferences.getBoolean(IS_HELP_SHOWN, false);
    }

    public void setHelpShown(boolean isShown) {
        preferences.edit().putBoolean(IS_HELP_SHOWN, isShown).apply();
    }

    public void setPrefs(Prefs prefs) {
        String toSave = prefsGson.toJson(prefs);
        preferences.edit().putString(PREFS_CODE, toSave).apply();
    }

    public Prefs getPrefs() {
        String prefsJson = preferences.getString(PREFS_CODE, null);
        if (prefsJson == null) {
            return new Prefs();
        } else {
            return prefsGson.fromJson(prefsJson, Prefs.class);
        }
    }

    public void setAutorun(boolean isAutorunEnabled) {
        preferences.edit().putBoolean(AUTORUN, isAutorunEnabled).apply();
    }

    public boolean isAutorunEnabled() {
        return preferences.getBoolean(AUTORUN, false);
    }

    public void setPassword(String password) {
        Prefs prefs = getPrefs();
        String newPass = null;
        if (!Strings.isNullOrEmpty(password)) {
            password = password.replaceAll("\\s+", "");
            newPass = password.equals(prefs.password) ? password : FabricUtils.crypt(password);
        }
        if (newPass != null && (prefs.password == null || !newPass.equals(prefs.password))) {
            prefs.removeRegisterUsers();
        }
        prefs.password = newPass;
        setPrefs(prefs);
    }

    public String getPassword() {
        return getPrefs().password;
    }

    public void updateLastMdDetected() {
        preferences.edit().putLong(LAST_MD_DETECTED, System.currentTimeMillis()).apply();
    }

    public long getLastMdDetected() {
        return preferences.getLong(LAST_MD_DETECTED, 0L);
    }

    public void makePro() {
        preferences.edit().putBoolean(IS_PRO, true).apply();
    }

    public boolean isPro() {
        return preferences.getBoolean(IS_PRO, false);
    }

    public void unmakePro() {
        preferences.edit().putBoolean(IS_PRO, false).apply();
    }
}
