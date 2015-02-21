package cmu.ruixin.mousebuddy;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;

/**
 * Created by Ruixin on 2/21/2015.
 */
public class CamPreview implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private SurfaceHolder mHolder;
    private Camera mCamera;

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startCamera();
    }

    public void startCamera() {
        mCamera = Camera.open();
        Camera.Parameters p = mCamera.getParameters();

        p.setFocusMode("auto");
        for (Integer i : p.getSupportedPreviewFormats()) {
            if (i == ImageFormat.RGB_565) {
                Log.d("test", "good");
            }
            Log.d("Test", String.format("%d, %d", i, ImageFormat.RGB_565));
        }
        mCamera.release();
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mCamera.release();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.release();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

    }
}
