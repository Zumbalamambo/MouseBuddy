package cmu.ruixin.mousebuddy;

import android.hardware.Camera;
import android.view.SurfaceHolder;

/**
 * Created by Ruixin on 2/21/2015.
 */
public class CamPreview implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private SurfaceHolder mHolder;
    private Camera mCamera;

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Camera.Parameters p = mCamera.getParameters();
        
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

    }
}
