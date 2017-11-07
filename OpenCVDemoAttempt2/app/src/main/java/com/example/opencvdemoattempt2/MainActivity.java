package com.example.opencvdemoattempt2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;

import static org.opencv.core.Core.minMaxLoc;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 666;
    private static boolean permissionWasGranted = false;

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV initialization FAILED!");
        } else {
            Log.d(TAG, "OpenCV initialization SUCCESS!");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_READ_EXTERNAL_STORAGE);

        } else {
            permissionWasGranted = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionWasGranted = true;
                    Log.d(TAG, "READ_EXTERNAL_STORAGE permission was granted");
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {
                    permissionWasGranted = false;
                    Log.d(TAG, "READ_EXTERNAL_STORAGE permission was denied");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

   private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS ) {
                // now we can call opencv code !
                readFile();
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_5,this, mLoaderCallback);
        // you may be tempted, to do something here, but it's *async*, and may take some time,
        // so any opencv call here will lead to unresolved native errors.
    }

    public void readFile() {
        File imgFile = new File("/storage/emulated/0/DCIM/Camera/water_coins.jpg");
        Bitmap inputBitmap;
        Bitmap outputBitmap;

        Mat img = new Mat();
        Mat gray = new Mat();
        Mat thresh = new Mat();
        Mat opening = new Mat();
        Mat sure_bg = new Mat();
        Mat dist_transform = new Mat();
        Mat sure_fg = new Mat();
        Mat unknown = new Mat();
        Mat markers = new Mat();
        Mat kernel;

        if(permissionWasGranted && imgFile.exists()) {
            inputBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

            Utils.bitmapToMat(inputBitmap, img);

            Imgproc.cvtColor(img, img, Imgproc.COLOR_BGRA2BGR);
            Imgproc.cvtColor(img, gray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.threshold(gray, thresh, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);

            //noise removal
            kernel = new Mat(3, 3, CvType.CV_8UC1, new Scalar(1,1,1));
            Imgproc.morphologyEx(thresh, opening, Imgproc.MORPH_OPEN, kernel, new Point(-1,-1), 2);

            //sure background area
            Imgproc.dilate(opening, sure_bg, kernel, new Point(-1,-1), 3);

            //Finding sure foreground area
            Imgproc.distanceTransform(opening, dist_transform, Imgproc.DIST_L2, Imgproc.DIST_MASK_5);
            Imgproc.threshold(dist_transform, sure_fg, 0.7 * Core.minMaxLoc(dist_transform).maxVal, 255, Imgproc.THRESH_BINARY);

            //Finding unknown region
            sure_fg.convertTo(sure_fg, CvType.CV_8UC1);
            Core.subtract(sure_bg, sure_fg, unknown);

            //Marker labelling
            Imgproc.connectedComponents(sure_fg, markers);
            Log.d(TAG, "Objects found: " + Core.minMaxLoc(markers).maxVal);

            //Add one to all labels so that sure background is not 0, but 1
            Core.add(markers, new Scalar(1), markers);

            //Marking the region of unknown with zero
            for (int i = 0; i < markers.rows(); ++i)
                for (int j = 0; j < markers.cols(); ++j)
                    if (unknown.get(i, j)[0] == 255)
                        markers.put(i, j, 0);

            //Watershed application
            Imgproc.watershed(img, markers);
            double[] RED = {255, 0, 0};
            for (int i = 0; i < markers.rows(); ++i)
                for (int j = 0; j < markers.cols(); ++j)
                    if (markers.get(i, j)[0] == -1)
                        img.put(i, j, RED);

            outputBitmap = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(img, outputBitmap);
            ImageView iv = (ImageView) findViewById(R.id.imageView1);
            iv.setImageBitmap(outputBitmap);
        }
    }
}
