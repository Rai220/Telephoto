package com.rai220.securityalarmbot.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.support.v4.util.Pair;

import com.rai220.securityalarmbot.BotService;
import com.rai220.securityalarmbot.R;
import com.rai220.securityalarmbot.utils.ChargingType;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class BatteryReceiver extends BroadcastReceiver {
    private BotService botService;
    private float mBatteryLevel;
    private String mPowerStatus;
    private boolean mIsCharging;
    private int lastTemperature = Integer.MIN_VALUE;
    private int lastSegmentNotify = -1;
    private boolean isSentBatteryLow = false;
    private List<Pair<Integer, Integer>> mSegments = new ArrayList<>();

    public BatteryReceiver(BotService botService) {
        this.botService = botService;
        fillSegments();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String message = null;
        switch (intent.getAction()) {
            case Intent.ACTION_BATTERY_LOW:
                if (!isSentBatteryLow) {
                    message = botService.getString(R.string.battery_low);
                    isSentBatteryLow = true;
                }
                break;
            case Intent.ACTION_BATTERY_OKAY:
                isSentBatteryLow = false;
                break;
            case Intent.ACTION_POWER_DISCONNECTED:
                message = botService.getString(R.string.power_disconnected) + getStatus();
                break;
            case Intent.ACTION_POWER_CONNECTED:
                message = botService.getString(R.string.power_connected);
                break;
            case Intent.ACTION_BATTERY_CHANGED:
                mBatteryLevel = getBatteryLevel(intent);
                mPowerStatus = getBatteryStatus(intent);
                ReceiverStorage.getInstance().setBatteryLevel(mBatteryLevel);
                int segment = getSegment(mBatteryLevel);
                if (segment < lastSegmentNotify) {
                    String attention = mIsCharging ? botService.getString(R.string.battery_discharging) : "";
                    message = attention + getStatus();
                }
                lastSegmentNotify = segment;
                break;
        }
        if (message != null) {
            botService.getTelegramService().sendMessageToAll(message);
        }
    }

    public String getStatus() {
        return String.format(botService.getString(R.string.battery_status), Float.toString(mBatteryLevel), mPowerStatus);
    }

    public Float getLastTemperature() {
        if (lastTemperature != Integer.MIN_VALUE) {
            return (float) lastTemperature / 10;
        } else {
            return null;
        }
    }

    private float getBatteryLevel(Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        return Math.round((level / (float) scale) * 100f);
    }

    private String getBatteryStatus(Intent intent) {
        // Are we charging / charged?
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        mIsCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        lastTemperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Integer.MIN_VALUE);
        ReceiverStorage.getInstance().setBatteryTemperature(getLastTemperature());

        // How are we charging?
        int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        ChargingType chargingType = ChargingType.getType(chargePlug);
        String chargeSource = chargingType != null ? chargingType.toString() : botService.getString(R.string.unknown);
        return mIsCharging ? String.format(botService.getString(R.string.charging), chargeSource) : botService.getString(R.string.discharging);
    }

    private void fillSegments() {
        int lowValue = 15; // maximal value, when battery low
        int segment = 20; // notify every segment's (percent)
        mSegments.add(new Pair<>(0, 5));
        mSegments.add(new Pair<>(5, 10));
        mSegments.add(new Pair<>(10, lowValue));
        int segmentLength = mSegments.size();
        int i = 100;
        while (i > lowValue) {
            int from = i - segment;
            if (from > lowValue) {
                mSegments.add(segmentLength, new Pair<>(from, i));
            } else {
                mSegments.add(segmentLength, new Pair<>(lowValue, i));
            }
            i -= segment;
        }
    }

    private boolean isIntoSegment(int segment, float value) {
        Pair<Integer, Integer> pair = null;
        try {
            pair = mSegments.get(segment);
        } catch (Exception ignore) {
        }
        return pair != null && (pair.first < value && value <= pair.second);
    }

    private int getSegment(float value) {
        for (Pair<Integer, Integer> pair : mSegments) {
            if (pair.first < value && value <= pair.second) {
                return mSegments.indexOf(pair);
            }
        }
        return 0;
    }

}
