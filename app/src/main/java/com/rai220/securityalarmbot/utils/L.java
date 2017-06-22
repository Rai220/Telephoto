package com.rai220.securityalarmbot.utils;

import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.common.base.Joiner;

import java.util.LinkedList;

/**
 * Created by rai220 on 10/12/16.
 */
public class L {
    private static final String TAG = "SecurityAlarmBot";

    private static final int MAX_LOG_SIZE = 10 * 1000;
    private static final LinkedList<String> logs = new LinkedList<>();

    private L() {
    }

    public static void e(String error) {
        Log.e(TAG, error);
        saveToLog("e", error);
    }

    public static void e(Throwable th) {
        e(null, th);
    }

    public static void e(String message, Throwable th) {
        Log.e(TAG, "", th);
        StackTraceElement[] stack = th.getStackTrace();
        StackTraceElement lastElement = stack[stack.length - 1];
        String where = lastElement.getClassName() + ":" + lastElement.getLineNumber();
        Answers.getInstance().logCustom(new CustomEvent("Error").putCustomAttribute("error_text", where));
        if (message != null) {
            Crashlytics.log(message);
        }
        Crashlytics.logException(th);

        saveToLog("e", where);
    }

    public static void i(Object obj) {
        saveToLog("I", "" + obj);
        Log.i(TAG, "" + obj);
    }

    public static void d(Object obj) {
        Log.d(TAG, "" + obj);
    }

    public static String logsToString() {
        synchronized (logs) {
            return Joiner.on("\n").join(logs);
        }
    }

    private static void saveToLog(String level, String text) {
        synchronized (logs) {
            logs.addLast("" + System.currentTimeMillis() + " (" + level + ") " + text);
            if (logs.size() > MAX_LOG_SIZE) {
                logs.removeFirst();
            }
        }
    }
}
