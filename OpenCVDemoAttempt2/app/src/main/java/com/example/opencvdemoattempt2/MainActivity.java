package com.example.opencvdemoattempt2;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

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

        if(imgFile.exists()) {
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
