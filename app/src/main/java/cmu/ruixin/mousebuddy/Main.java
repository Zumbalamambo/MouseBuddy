package cmu.ruixin.mousebuddy;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.support.v4.view.GestureDetectorCompat;
import android.widget.EditText;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
//import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
//import org.opencv.engine.OpenCVEngineInterface;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.KeyPoint;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.List;

public class Main extends Activity implements SensorEventListener,
        CameraBridgeViewBase.CvCameraViewListener2,
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener{
    static{ System.loadLibrary("opencv_java"); }
    private SensorManager mSensorManager;

    private Sensor mAccelerometer;
    private long prevT;
    private float x, y;
    private CameraBridgeViewBase mOpenCvCameraView;
    private FeatureDetector myFeatures;
    private MatOfKeyPoint prevPoints;
    private static final double minDistance = 100.0;
    private static final double noiseThreshold = 2;
    private static final double mergeDistance = 15.0;
    private Mat rgb;
    private Mat outputImage;
    private List<KeyPoint> oldKeyPoints;
    private List<KeyPoint> keyPoints;
    private GestureDetectorCompat mDetector;
    private Mat rotate;
    private String IP;

    private MouseActivity ma;
    private Thread[] childThreads;

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
        mOpenCvCameraView.setMaxFrameSize(240, 240);
        rgb = new Mat();
        outputImage = new Mat();
        oldKeyPoints = new ArrayList<KeyPoint>();
        rotate = Imgproc.getRotationMatrix2D(new Point(0, 0), 90, 1);
        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        Log.d("ipaddress", ip);
        getIP();
        /* Gesture detection */

        mDetector = new GestureDetectorCompat(this,this);
        mDetector.setOnDoubleTapListener(this);
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
    protected void onPause() {
        if (ma != null)
            ma.deactivate();
        super.onPause();
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
                    // open cv is weird and assumes camera is
                    dx = (b.pt.y - a.pt.y);
                    dy = -(b.pt.x - a.pt.x);

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
        //Log.d("translation", String.format("%f, %f, %d", translation.x, translation.y, contributing_pts));
        if (contributing_pts == 0 || Distance(translation, new Point(0, 0)) < noiseThreshold) {
            translation.x = 0;
            translation.y = 0;
        }
        return translation;
    }

    public List<KeyPoint> filter(List<KeyPoint> p) {
        List<KeyPoint> retval = new ArrayList<KeyPoint>();
        for (int i = 0; i < p.size(); i++) {
            KeyPoint k = p.get(i);
            for (int j = i + 1; j < p.size(); j++) {
                KeyPoint l = p.get(j);
                if (Distance(k.pt, l.pt) < mergeDistance) {
                    k.pt.x = (l.pt.x + k.pt.x) / 2;
                    k.pt.y = (l.pt.y + k.pt.y) / 2;
                    retval.add(k);
                    break;
                }
            }
            retval.add(k);
        }
        return retval;
    }

    public List<KeyPoint> centerOnly(List<KeyPoint> kps) {
        List<KeyPoint> retval = new ArrayList<KeyPoint>();
        Point center = new Point(mOpenCvCameraView.getWidth() / 2, mOpenCvCameraView.getHeight() / 2);
        for (KeyPoint kp : kps) {
            if (Distance(kp.pt, center) <=  mOpenCvCameraView.getHeight() / 4) {
                retval.add(kp);
            }
        }
        return retval;
    }
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        // Imgproc
        //Core
        MatOfKeyPoint k = new MatOfKeyPoint();
        Imgproc.cvtColor(inputFrame.rgba(), rgb, Imgproc.COLOR_RGBA2RGB);
        myFeatures.detect(rgb, k);
        keyPoints = k.toList();
        keyPoints = filter(keyPoints);
        Features2d.drawKeypoints(rgb, k, rgb);

        Imgproc.cvtColor(rgb, outputImage, Imgproc.COLOR_RGB2RGBA);

        Point translation = getTranslation(centerOnly(oldKeyPoints), keyPoints);
        if (ma != null && ma.isActive()) {
            /* transfer updates to server */
            ma.type = MouseActivity.MOUSEMOVEMENT;
            ma.deltaX += (float) translation.x;
            ma.deltaY += (float) translation.y;
        }
        // update oldKeyPoints with new keyPoints
        oldKeyPoints = keyPoints;
        return outputImage;
    }

    /* click code */
    public void leftClick(View v) {
        /* send left click */
        ma.type = MouseActivity.LEFTCLICK;
        Log.d("EzPz", "Left Click!");
    }
    public void rightClick(View v) {
        /* send right click */
        ma.type = MouseActivity.RIGHTCLICK;
        Log.d("EzPz", "Right Click!");
    }
    /* gesture code */
    @Override
    public boolean onTouchEvent(MotionEvent event){
        this.mDetector.onTouchEvent(event);
        // Be sure to call the superclass implementation
        return super.onTouchEvent(event);
    }
    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        Log.d("EzPz", String.format("scrolling: (%f, %f)", distanceX, distanceY));
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        /* todo forward or backward */
        Log.d("EzPz", String.format("flinging: (%f, %f)", velocityX, velocityY));
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    /* prompt for ip address */
    public void getIP() {

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final EditText input = new EditText(this);
        alert.setView(input);
        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                String out = input.getText().toString();
                IP = out;

                childThreads = new Thread[1];

                ma = new MouseActivity();
                try {
                    new ConnectServerAsyncTask(ma, IP, childThreads).execute();
                }
                catch (Exception e)
                {
                    Log.d("MBServerConection", e.getMessage());
                }

            }
        });
        alert.show();
    }
}
