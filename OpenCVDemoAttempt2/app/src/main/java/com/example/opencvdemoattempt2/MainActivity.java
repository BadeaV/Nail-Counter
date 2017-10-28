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
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;


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
        File imgFile = new File("/storage/emulated/0/DCIM/Camera/test.jpg");
        Mat m;
        Bitmap inputBitmap = null;
        Bitmap outputBitmap;

        if(permissionWasGranted && imgFile.exists()) {
            inputBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

            m = new Mat (inputBitmap.getWidth(), inputBitmap.getHeight(), CvType.CV_8UC3);
            Utils.bitmapToMat(inputBitmap, m);

            //do something
            Imgproc.cvtColor(m, m, Imgproc.COLOR_RGB2GRAY);

            outputBitmap = Bitmap.createBitmap(m.cols(), m.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(m, outputBitmap);
            ImageView iv = (ImageView) findViewById(R.id.imageView1);
            iv.setImageBitmap(outputBitmap);
        }
    }
}
