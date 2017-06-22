package com.rai220.securityalarmbot;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.rai220.securityalarmbot.commands.PhotoCommand;
import com.rai220.securityalarmbot.commands.types.SensitivityType;
import com.rai220.securityalarmbot.controllers.AudioRecordController;
import com.rai220.securityalarmbot.controllers.LocationController;
import com.rai220.securityalarmbot.listeners.SensorListener;
import com.rai220.securityalarmbot.photo.AlarmType;
import com.rai220.securityalarmbot.photo.HiddenCamera2;
import com.rai220.securityalarmbot.photo.ImageShot;
import com.rai220.securityalarmbot.photo.MotionDetectorController;
import com.rai220.securityalarmbot.photo.Observer;
import com.rai220.securityalarmbot.prefs.Prefs;
import com.rai220.securityalarmbot.prefs.PrefsController;
import com.rai220.securityalarmbot.receivers.BatteryReceiver;
import com.rai220.securityalarmbot.receivers.CallReceiver;
import com.rai220.securityalarmbot.receivers.SmsReceiver;
import com.rai220.securityalarmbot.receivers.TimeStatsSaver;
import com.rai220.securityalarmbot.telegram.IStartService;
import com.rai220.securityalarmbot.telegram.TelegramService;
import com.rai220.securityalarmbot.utils.AnimatedGifEncoder;
import com.rai220.securityalarmbot.utils.FabricUtils;
import com.rai220.securityalarmbot.utils.L;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Set;

import static android.hardware.SensorManager.SENSOR_DELAY_FASTEST;
import static android.hardware.SensorManager.SENSOR_DELAY_GAME;

/**
 * Created by rai220 on 10/12/16.
 */
public class BotService extends Service implements MotionDetectorController.MotionDetectorListener, IStartService {
    public static final String TELEPHOTO_SERVICE_STOPPED = "TELEPHOTO_SERVICE_STOPPED";

    private TelegramService telegramService;
    private BatteryReceiver batteryReceiver;
    private SmsReceiver smsReceiver;
    private CallReceiver callReceiver;
    private final HiddenCamera2 hiddenCamera2 = new HiddenCamera2(this);
    private Observer observer = new Observer();
    private MotionDetectorController detector = new MotionDetectorController();
    private LocationController locationController = new LocationController();
    private SensorListener sensorListener;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private TimeStatsSaver timeStatsSaver = new TimeStatsSaver();
    private AudioRecordController audioRecordController = new AudioRecordController();
    //private Sensor mTemperature;
    //private Sensor mHumidity;
    //private Sensor mProximity;
    private boolean isSensorStarted = false;

    public Handler handler = null;
    public TextToSpeech tts = null;
    public volatile boolean ttsInitialized = false;

    private volatile PowerManager.WakeLock wakeLock = null;

    @Override
    public void onCreate() {
        super.onCreate();
        L.i("Build version: " + BuildConfig.VERSION_NAME);

        subscribeToFirebase();

        PrefsController.instance.init(this);
        telegramService = new TelegramService(this);
        telegramService.init(this);

        Prefs prefs = PrefsController.instance.getPrefs();
        hiddenCamera2.init(this);

        preventSleepMode();

        FabricUtils.initFabric(this);
        handler = new Handler(Looper.getMainLooper());

        try {
            tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int i) {
                    ttsInitialized = true;
                }
            });
        } catch (Throwable ex) {
            L.e("Error initializing TTS " + ex.toString());
        }


        // Runs service in IDDQD mode :)
        Intent notificationIntent = new Intent(this, SettingsActivity.class);
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.icon)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.bot_running))
                .setContentIntent(pendingIntent).build();
        startForeground(1337, notification);

        batteryReceiver = new BatteryReceiver(this);
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_POWER_CONNECTED));
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_LOW));
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_OKAY));

        smsReceiver = new SmsReceiver(this);
        registerReceiver(smsReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));

        callReceiver = new CallReceiver(getTelegramService());
        registerReceiver(callReceiver, new IntentFilter("android.intent.action.PHONE_STATE"));

        FabricUtils.initFabric(this);
//        hiddenCamera.init(this);
        detector.init(hiddenCamera2, this);
        locationController.init(this);

        observer.init(hiddenCamera2, telegramService);
        observer.start(prefs.minutesPeriod);

        sensorListener = new SensorListener();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); // Получаем менеджер сенсоров
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); // Получаем датчик положения

        List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        L.i("---------------------------------------------------");
        L.i("Sensors count: " + sensors.size());
        for (Sensor s : sensors) {
            L.i(s.getName());
        }
        L.i("---------------------------------------------------");

        //todo do not work :(
//        int sensorType;

//        mTemperature = mSensorManager.getDefaultSensor(sensorType);
//        if (mTemperature != null) {
//            mSensorManager.registerListener(sensorListener, mTemperature, SensorManager.SENSOR_DELAY_NORMAL);
//        } else {
//            L.i("Temperature sensor not support!");
//        }
        //------------------------------------------------------------------------------------------

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
//            mHumidity = mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY); // Датчик влажности
//            if (mHumidity != null) {
//                mSensorManager.registerListener(sensorListener, mHumidity, SensorManager.SENSOR_DELAY_NORMAL);
//            } else {
//                L.i("Humidity sensor not support!");
//            }
//        }

//        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY); // Датчик приближения
//        if (mProximity != null) {
//            mSensorManager.registerListener(sensorListener, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
//        }

        prefs.removeOldTimeStats();
        PrefsController.instance.setPrefs(prefs);

        timeStatsSaver.start();

        locationController.start();

        L.i("Service created");
        Answers.getInstance().logCustom(new CustomEvent("Service started!"));
    }

    private void subscribeToFirebase() {
        try {
            //FirebaseMessaging.getInstance().subscribeToTopic("bye");
        } catch (Throwable ex) {
            L.e(ex);
        }
    }

    @Override
    public void onStartSuccess() {

    }

    @Override
    public void onStartFailed() {
        final BotService botService = this;
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(botService, R.string.error_bot_token, Toast.LENGTH_SHORT).show();
                stopSelf();
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        if (!PrefsController.instance.hasToken()) {
            Toast.makeText(this, R.string.error_no_bot_token, Toast.LENGTH_LONG).show();
            stopSelf();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        L.i("----- DESTROY service -----");

        try {
            hiddenCamera2.destroy();
//            hiddenCamera.destroy();
            tts.stop();
            tts.shutdown();
            if (telegramService != null) {
                telegramService.stop();
                telegramService.getBot().removeGetUpdatesListener();
            }
        } catch (Throwable ex) {
            L.e(ex);
        }

        try {
            if (wakeLock != null) {
                wakeLock.release();
            }
        } catch (Throwable ex) {
            L.e(ex);
        }

//        detector.stop();
        timeStatsSaver.stop();
        observer.stop();
        locationController.stop();
        unregisterReceiver(batteryReceiver);
        unregisterReceiver(smsReceiver);
        unregisterReceiver(callReceiver);
        stopSensor();
        L.i("Service stopped");

        Intent intent = new Intent(TELEPHOTO_SERVICE_STOPPED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public static byte[] buildGif(List<ImageShot> oldShots, Bitmap newBmp) {
        //ArrayList<Bitmap> bitmaps = adapter.getBitmapArray();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        AnimatedGifEncoder encoder = new AnimatedGifEncoder();
        encoder.start(bos);
        //encoder.setDelay(250);
        for (ImageShot is : oldShots) {
            byte[] imgByte = is.toYuvByteArray();
            Bitmap bmp = BitmapFactory.decodeByteArray(imgByte, 0, imgByte.length);
            encoder.addFrame(bmp);
        }
        if (newBmp != null) {
            encoder.addFrame(newBmp);
        }

        encoder.finish();
        return bos.toByteArray();
    }

    @Override
    public void motionDetected(byte[] debug, byte[] real, byte[] oldReal, boolean rareMotion, List<ImageShot> oldShots) {
        Set<Prefs.UserPrefs> listenerUsers = PrefsController.instance.getPrefs().getEventListeners();

        Bitmap newBmp = BitmapFactory.decodeByteArray(real, 0, real.length);
        byte[] gif = buildGif(oldShots, newBmp);


        for (Prefs.UserPrefs user : listenerUsers) {
            //telegramService.sendPhoto(user.lastChatId, gif);
            telegramService.sendDocument(user.lastChatId, gif, "gif.gif");
        }

        AlarmType currentAlarmType = PrefsController.instance.getPrefs().alarmType;
        if (currentAlarmType.equals(AlarmType.GIF_AND_PHOTO)) {
            PhotoCommand.getPhoto(this, null, null);
        } else if (currentAlarmType.equals(AlarmType.GIF_AND_3_PHOTOS)) {
            PhotoCommand.getPhoto(this, null, null);
            PhotoCommand.getPhoto(this, null, null);
            PhotoCommand.getPhoto(this, null, null);
        }
    }

    public void startSensor() {
        SensitivityType sensitivityType = PrefsController.instance.getPrefs().shakeSensitivity;
        sensorListener.init(telegramService, this, sensitivityType);
        int sensorDelay = SENSOR_DELAY_GAME;
        if (sensitivityType.equals(SensitivityType.HIGH)) {
            sensorDelay = SENSOR_DELAY_FASTEST;
        }
        mSensorManager.registerListener(sensorListener, mAccelerometer, sensorDelay);
        isSensorStarted = true;
    }

    public void stopSensor() {
        mSensorManager.unregisterListener(sensorListener);
        isSensorStarted = false;
    }

    public boolean isSensorStarted() {
        return isSensorStarted;
    }

    public HiddenCamera2 getCamera() {
        return hiddenCamera2;
    }

    public Observer getObserver() {
        return observer;
    }

    public BatteryReceiver getBatteryReceiver() {
        return batteryReceiver;
    }

    public TelegramService getTelegramService() {
        return telegramService;
    }

    public MotionDetectorController getDetector() {
        return detector;
    }

//    public Sensor getTemperature() {
//        return mTemperature;
//    }

    public LocationController getLocationController() {
        return locationController;
    }

    public AudioRecordController getAudioRecordController() {
        return audioRecordController;
    }

    private void preventSleepMode() {
        try {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "MyWakelockTag");
            wakeLock.acquire();
        } catch (Throwable ex) {
            L.e(ex);
        }
    }

}
