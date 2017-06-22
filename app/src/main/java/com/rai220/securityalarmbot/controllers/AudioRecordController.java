package com.rai220.securityalarmbot.controllers;

import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;

import com.rai220.securityalarmbot.utils.L;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 *
 */

public class AudioRecordController {

    public static final int SECONDS = 20;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private MediaRecorder mRecorder = null;
    private Future future = null;

    public AudioRecordController() {
    }

    public void recordAndTransfer(final IAudioRecorder audioRecorder) {
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
        future = executor.submit(new Runnable() {
            @Override
            public void run() {
                String fileName = Environment.getExternalStorageDirectory().getAbsolutePath() +
                        "/Telephoto/audio_" + System.currentTimeMillis() + ".3gp";
                try {
                    L.i("Start audio recording");
                    audioRecorder.onRecordStarted();
                    startRecording(fileName);
                    int iteration = 0;
                    while (!Thread.currentThread().isInterrupted() && iteration <= SECONDS) {
                        L.i("sleep " + iteration);
                        Thread.sleep(1000);
                        iteration++;
                    }
                } catch (IOException | InterruptedException ex) {
                    if (!(ex instanceof InterruptedException)) {
                        L.e(ex);
                    }
                    audioRecorder.onRecordBreak();
                } finally {
                    L.i("Stop audio recording");
                    stopRecording();
                    audioRecorder.onRecordFinished(new File(fileName));
                }
            }
        });
    }

    private void startRecording(String fileName) throws IOException {
        if (mRecorder != null) {
            stopRecording();
        }

        String state = Environment.getExternalStorageState();
        if (!state.equals(Environment.MEDIA_MOUNTED)) {
            throw new IOException("SD Card is not mounted.  It is " + state + ".");
        }

        File directory = new File(fileName).getParentFile();
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Path to file could not be created.");
        }

        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//        int audioEncoder;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            audioEncoder = MediaRecorder.AudioEncoder.VORBIS;
//        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
//            audioEncoder = MediaRecorder.AudioEncoder.AAC_ELD;
//        } else {
//            audioEncoder = MediaRecorder.AudioEncoder.DEFAULT;
//        }
//        mRecorder.setAudioEncoder(audioEncoder);
        mRecorder.setOutputFile(fileName);

//        L.i("encoder: " + audioEncoder);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            L.e(e);
        }
        mRecorder.start();
    }

    private void stopRecording() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }

    public interface IAudioRecorder {
        void onRecordStarted();

        void onRecordFinished(File fileName);

        void onRecordBreak();
    }

}
