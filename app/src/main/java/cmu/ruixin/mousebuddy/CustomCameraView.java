package cmu.ruixin.mousebuddy;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;

import org.opencv.android.JavaCameraView;

/**
 * Created by Ruixin on 2/22/2015.
 */
public class CustomCameraView extends JavaCameraView {
    public CustomCameraView(Context context, int cameraId) {
        super(context, cameraId);
        int framerates[] = new int[2];
        for (int[] framerate : mCamera.getParameters().getSupportedPreviewFpsRange()) {
            if (framerate[0] > framerates[0]) {
                framerates[0] = framerate[0];
                framerates[1] = framerates[1];
            }
        }
        Log.d("fps", String.format("%f, %f", framerates[0] / 1000, framerates[1] / 1000));
        setPreviewFPS(framerates[0], framerates[1]);
    }
    public CustomCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        int framerates[] = new int[2];
        for (int[] framerate : mCamera.getParameters().getSupportedPreviewFpsRange()) {
            if (framerate[0] > framerates[0]) {
                framerates[0] = framerate[0];
                framerates[1] = framerates[1];
            }
        }
        Log.d("fps", String.format("%f, %f", framerates[0] / 1000, framerates[1] / 1000));
        setPreviewFPS(framerates[0], framerates[1]);
    }

    public void setPreviewFPS(int min, int max){
        Camera.Parameters params = mCamera.getParameters();
        params.setPreviewFpsRange((int)(min), (int)(max));
        mCamera.setParameters(params);
    }
}