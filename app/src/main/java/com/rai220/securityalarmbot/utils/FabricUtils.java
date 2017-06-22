package com.rai220.securityalarmbot.utils;

import android.content.Context;
import android.database.Cursor;
import android.hardware.Camera;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.BuildConfig;
import com.crashlytics.android.core.CrashlyticsCore;
import com.google.common.base.Strings;
import com.rai220.securityalarmbot.photo.CameraMode;
import com.rai220.securityalarmbot.prefs.PrefsController;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import io.fabric.sdk.android.Fabric;

/**
 * Created by rai220 on 10/19/16.
 */

public class FabricUtils {
    private static MessageDigest messageDigest;

    static {
        try {
            messageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            L.e(ex);
        }
    }

    public static void initFabric(Context context) {
        CrashlyticsCore core = new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build();
        Fabric.with(context, new Crashlytics.Builder().core(core).build());
        Fabric.with(context, new Answers());
    }

    public static int[] getAvailableCameras() {
        int[] res = new int[Camera.getNumberOfCameras()];
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            res[i] = i;
        }
        return res;
    }

    public static int[] getSelectedCameras() {
        CameraMode cameraMode = PrefsController.instance.getPrefs().cameraMode;
        int[] res;
        if (cameraMode == null || cameraMode == CameraMode.ALL) {
            res = getAvailableCameras();
        } else {
            res = new int[1];
            res[0] = cameraMode.getNumber();
        }
        return res;
    }

    public static int[] getSelectedCamerasForMd() {
        CameraMode cameraMode = PrefsController.instance.getPrefs().mdMode;
        int[] res;
        if (cameraMode == null || cameraMode == CameraMode.ALL) {
            res = getAvailableCameras();
        } else {
            res = new int[1];
            res[0] = cameraMode.getNumber();
        }
        return res;
    }


    public static boolean isAvailable(CameraMode mode) {
        if (mode.equals(CameraMode.ALL)) {
            return true;
        }
        int[] supportedCameras = getAvailableCameras();
        for (int cameraId : supportedCameras) {
            if (cameraId == mode.getNumber()) {
                return true;
            }
        }
        return false;
    }

    public static void interruptThread(Thread thread) {
        try {
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
                thread.join(1000);
                L.i(String.format("Thread[%s] stopped", thread.getName()));
            }
        } catch (InterruptedException ignore) {
        } catch (Throwable ex) {
            L.e(ex);
        }
    }

    public static String defaultIfBlank(String str, String def) {
        return (str == null || str.trim().length() == 0) ? def : str;
    }

    public static String crypt(String pass) {
        byte[] digested = getDigest(pass);
        if (digested != null) {
            StringBuilder sb = new StringBuilder();
            for (byte dig : digested) {
                sb.append(Integer.toHexString(0xff & dig));
            }
            return sb.toString();
        }
        return null;
    }

    public static boolean isPassCorrect(String check, String expect) {
        String crypt = crypt(check);
        return Strings.isNullOrEmpty(expect) || (crypt != null && crypt.equals(expect));
    }

    private static byte[] getDigest(String pass) {
        byte[] result = null;
        if (messageDigest != null) {
            byte[] passBytes = pass.getBytes();
            messageDigest.reset();
            result = messageDigest.digest(passBytes);
        }
        return result;
    }

    public static String getNameByPhone(Context context, String phoneNumber) {
        try {
            String[] projection = new String[]
                    {ContactsContract.Data.CONTACT_ID,
                            ContactsContract.Contacts.LOOKUP_KEY,
                            ContactsContract.Contacts.DISPLAY_NAME,
                            ContactsContract.Contacts.STARRED,
                            ContactsContract.Contacts.CONTACT_STATUS,
                            ContactsContract.Contacts.CONTACT_PRESENCE};

            String selection = "PHONE_NUMBERS_EQUAL(" +
                    ContactsContract.CommonDataKinds.Phone.NUMBER + ",?) AND " +
                    ContactsContract.Data.MIMETYPE + "='" +
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "'";

            String selectionArgs[] = {PhoneNumberUtils.stripSeparators(phoneNumber)};

            String name = "unknown";
            if (context != null && context.getContentResolver() != null) {
                Cursor cursor = null;
                try {
                    cursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, projection, selection, selectionArgs, null);
                } catch (Exception ex) {
                    L.e(selection, ex);
                }

                if (cursor != null) {
                    if (cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        name = cursor.getString(2);
                    }
                    cursor.close();
                }
            }
            return name;
        } catch (Throwable ex) {
            L.e(ex);
            return "";
        }
    }

}
