package com.rai220.securityalarmbot.motiondetector;

import boofcv.alg.background.BackgroundModelMoving;
import boofcv.alg.distort.PointTransformHomography_F32;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.factory.background.ConfigBackgroundBasic;
import boofcv.factory.background.FactoryBackgroundModel;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import georegression.struct.homography.Homography2D_F32;
import georegression.struct.point.Point2D_I32;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by rai220 on 10/24/16.
 */
public class MotionDetector {
    public static float SENS_HIGH = 30f;
    public static float SENS_MEDIUM = 50f;
    public static float SENS_LOW = 70f;

    public static void main(String[] args) {
        System.out.println("Test");
    }

    private ExecutorService es = Executors.newCachedThreadPool();

    private BackgroundModelMoving background = null;
    private final ImageType imageType = ImageType.single(GrayU8.class);
    private volatile int oldWidth = 0;
    private volatile int oldHeight = 0;
    private final Homography2D_F32 firstToCurrent32 = new Homography2D_F32();
    private final Homography2D_F32 homeToWorld = new Homography2D_F32();
    private GrayS32 labeledObjects = null;
    private GrayU8 segmented = null;

    public synchronized void configure(float threshold) {
        ConfigBackgroundBasic configBasic = new ConfigBackgroundBasic(threshold, 1.0f);
        background = FactoryBackgroundModel
                .movingBasic(configBasic, new PointTransformHomography_F32(), imageType);
        oldWidth = 0;
        oldHeight = 0;
    }

    public synchronized GrayU8 addImage(GrayU8 newImage, int[][] matrix) {
        int width = newImage.width;
        int height = newImage.height;

        // Изменилось разрешение детектора - пересоздаем всё
        if (oldWidth != width || oldHeight != height) {
            //oldGrey = null;
            oldWidth = width;
            oldHeight = height;
            segmented = new GrayU8(oldWidth, oldHeight);
            homeToWorld.a13 = width / 2;
            homeToWorld.a23 = height / 2;

            background.initialize(width * 2, height * 2, homeToWorld);
            labeledObjects = new GrayS32(width, height);
        }

        background.segment(firstToCurrent32, newImage, segmented);
        background.updateBackground(firstToCurrent32, newImage);

        BinaryImageOps.removePointNoise(segmented, segmented);
        List<Contour> contours = BinaryImageOps.contour(segmented, ConnectRule.EIGHT, labeledObjects);

        int bigObjects = 0;
        if (contours.size() > 0) {
            for (Contour c : contours) {
                int minx = Integer.MAX_VALUE;
                int miny = Integer.MAX_VALUE;
                int maxx = Integer.MIN_VALUE;
                int maxy = Integer.MIN_VALUE;
                for (Point2D_I32 point : c.external) {
                    if (minx > point.getX()) {
                        minx = point.getX();
                    }
                    if (maxx < point.getX()) {
                        maxx = point.getX();
                    }
                    if (miny > point.getY()) {
                        miny = point.getY();
                    }
                    if (maxy < point.getY()) {
                        maxy = point.getY();
                    }
                }
                int length = Math.min((maxx - minx), (maxy - miny));
                //System.out.println("Length: " + length);
                if (length > 5) {
                    if (checkMatrix(width, height, matrix, minx, maxx, miny, maxy)) {
                        bigObjects++;
                    }
                }
            }
            if (bigObjects > 0) {
                return segmented;
            }
        }
        return null;
    }

    private boolean checkMatrix(int width, int height,
                                int[][] matrix, int minx,
                                int maxx, int miny, int maxy) {
        if (matrix == null) {
            return true;
        }
        int mat_minx = (int) Math.floor((double) minx / (double) width * (double) matrix.length);
        int mat_maxx = (int) Math.floor((double) maxx / (double) width * (double) matrix.length);
        int mat_miny = (int) Math.floor((double) miny / (double) height * (double) matrix.length);
        int mat_maxy = (int) Math.floor((double) maxy / (double) height * (double) matrix.length);

        if (mat_maxx >= matrix.length) {
            mat_maxx = matrix.length - 1;
        }
        if (mat_maxy >= matrix.length) {
            mat_maxy = matrix.length - 1;
        }

        if (matrix[mat_miny][mat_minx] > 0 || matrix[mat_miny][mat_maxx] > 0
                || matrix[mat_maxy][mat_minx] > 0 || matrix[mat_maxy][mat_maxx] > 0) {
            return true;
        }

        return false;
    }
}
