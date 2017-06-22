package com.rai220.securityalarmbot.photo;

import android.hardware.Camera;

import java.util.List;

/**
 * Created by rai220 on 11/3/16.
 */

public class CameraUtils {
    /** Goes through the size list and selects the one which is the closest specified size */
    public static int closest(List<Camera.Size> sizes , int width , int height ) {
        int best = -1;
        int bestScore = Integer.MAX_VALUE;

        for( int i = 0; i < sizes.size(); i++ ) {
            Camera.Size s = sizes.get(i);

            int dx = s.width-width;
            int dy = s.height-height;

            int score = dx*dx + dy*dy;
            if( score < bestScore ) {
                best = i;
                bestScore = score;
            }
        }

        return best;
    }
}
