package cmu.ruixin.mousebuddy;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.engine.OpenCVEngineInterface;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.KeyPoint;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class Main extends Activity implements SensorEventListener, CameraBridgeViewBase.CvCameraViewListener2 {
    static{ System.loadLibrary("opencv_java"); }
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private long prevT;
    private float x, y;
    private CameraBridgeViewBase mOpenCvCameraView;
    private FeatureDetector myFeatures;
    private MatOfKeyPoint prevPoints;
    private static final double minDistance = 50.0;
    private static final double noiseThreshold = 0.05;
    private Mat rgb;
    private Mat outputImage;
    private MatOfKeyPoint oldKeyPoints;
    private MatOfKeyPoint keyPoints;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /*CamPreview camp = new CamPreview();
        camp.startCamera();*/
        /* accelerometer init */
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        myFeatures = FeatureDetector.create(FeatureDetector.ORB);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.HelloOpenCvView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setMaxFrameSize(180, 180);
        rgb = new Mat();
        outputImage = new Mat();
        oldKeyPoints = new MatOfKeyPoint();
    }
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("Stuff", "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startSensor() {
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    /* accelerometer stuff */
    @Override
    public void onSensorChanged(SensorEvent event) {
        long dt = event.timestamp - prevT;
        float accel_x = event.values[0];
        float accel_y = event.values[1];
        float dx = accel_x * dt * dt / 2;
        float dy = accel_y * dt * dt / 2;
        x += dx;
        y += dy;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        return;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
    }

    @Override
    public void onCameraViewStopped() {

    }

    public List<Integer> MatchPoints(MatOfKeyPoint p1, MatOfKeyPoint p2, Mat Transform) {
        /*ArrayList<Integer> p = new ArrayList<Integer>();
        int n = p1.toList().size();
        if (Transform == null) {
            for (int i = 0; i < n; i++){
                p.add(i);
            }*/
            /* shuffling cards algo to randomly generate some matches */
            /*for (int i = 0; i < n; i++) {
                int newi = (int) ((Math.random() * (n - i));
                int oldival = p.get(i);
                p.set(i, p.get(i + newi));
                p.set(i + newi, oldival);
            }
        } else {
            for (int i = 0; i < n; i++) {

            }
        }
        return p;*/
        return null;
    }

    public double Distance(Point p1, Point p2) {
        return Math.sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y));
    }

    /* to get the transform, match each point in p1 with the closest other
        point in p2. The transform returned is the average dx and dy of each
        match. Sometimes a point may not get matched and it will not contribute.
     */
    public Point getTranslation(List<KeyPoint> p1, List<KeyPoint> p2) {
        Point translation = new Point();
        int contributing_pts = 0;
        double x_transform = 0;
        double y_transform = 0;
        for (KeyPoint a : p1) {
            double min_dist = minDistance; // manually adjust this
            double dx = 0;
            double dy = 0;
            for(KeyPoint b : p2) {
                if (Distance(a.pt, b.pt) < min_dist) {
                    min_dist = Distance(a.pt, b.pt);
                    dx = b.pt.x - a.pt.x;
                    dy = b.pt.y - a.pt.y;

                }
            }
            if (min_dist < minDistance) {
                contributing_pts++;
            }
            x_transform += dx;
            y_transform += dy;
        }
        translation.x = x_transform / contributing_pts;
        translation.y = y_transform / contributing_pts;
        Log.d("translation", String.format("%f, %f, %d", translation.x, translation.y, contributing_pts));
        if (contributing_pts == 0 || Distance(translation, new Point(0, 0)) < noiseThreshold) {
            translation.x = 0;
            translation.y = 0;
        }
        return translation;
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        // Imgproc
        //Core
        keyPoints = new MatOfKeyPoint();
        Imgproc.cvtColor(inputFrame.rgba(), rgb, Imgproc.COLOR_RGBA2RGB);
        myFeatures.detect(rgb, keyPoints);
        Features2d.drawKeypoints(rgb, keyPoints, rgb);
        Imgproc.cvtColor(rgb, outputImage, Imgproc.COLOR_RGB2RGBA);


        Point translation = getTranslation(oldKeyPoints.toList(), keyPoints.toList());
        // update oldKeyPoints with new keyPoints
        oldKeyPoints = keyPoints;
        return outputImage;
    }
}
