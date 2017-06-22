package com.rai220.securityalarmbot.photo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;

import com.rai220.securityalarmbot.prefs.PrefsController;

import java.io.ByteArrayOutputStream;

import boofcv.android.VisualizeImageData;

public class ImageShot {
    private int cameraId;
    private byte[] image;
    private Camera.Parameters parameters;

    public ImageShot(byte[] image, Camera.Parameters parameters, int cameraId) {
        this.image = image;
        this.parameters = parameters;
        this.cameraId = cameraId;
    }

    public int getCameraId() {
        return cameraId;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public Camera.Parameters getParameters() {
        return parameters;
    }

    public void setParameters(Camera.Parameters parameters) {
        this.parameters = parameters;
    }

    public byte[] toGoodQuality() {
        return imgToByte(true);
    }

    public byte[] toYuvByteArray() {
        return imgToByte(false);
    }

    private byte[] imgToByte(boolean quality) {
        Camera.Parameters parameters = getParameters();
        int width = parameters.getPreviewSize().width;
        int height = parameters.getPreviewSize().height;

        YuvImage yuv = new YuvImage(getImage(), parameters.getPreviewFormat(), width, height, null);
        ByteArrayOutputStream out =
                new ByteArrayOutputStream();
        yuv.compressToJpeg(new Rect(0, 0, width, height), 100, out);

        byte[] compressed = out.toByteArray();

        Bitmap newBmp = BitmapFactory.decodeByteArray(compressed, 0, compressed.length);
        Matrix mat = new Matrix();
        mat.postRotate(PrefsController.instance.getPrefs().getCameraPrefs(cameraId).angle);
        newBmp = Bitmap.createBitmap(newBmp, 0, 0, newBmp.getWidth(), newBmp.getHeight(), mat, true);
        ByteArrayOutputStream out2 = new ByteArrayOutputStream();
        if (quality) {
            newBmp.compress(Bitmap.CompressFormat.PNG, 100, out2);
        } else {
            newBmp.compress(Bitmap.CompressFormat.JPEG, 80, out2);
        }

        return out2.toByteArray();
    }
}
