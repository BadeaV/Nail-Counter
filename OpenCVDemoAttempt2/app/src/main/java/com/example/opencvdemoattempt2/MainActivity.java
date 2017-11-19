package com.example.opencvdemoattempt2;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
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

import java.io.FileDescriptor;
import java.io.IOException;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 666;
    private static final int READ_REQUEST_CODE = 42;

    private static boolean permissionWasGranted = false;
    private static boolean fileSearchPerformed = false;

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

    @Override
    public void onResume() {
        super.onResume();
        if(!fileSearchPerformed) {
            performFileSearch();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                Log.i(TAG, "Uri: " + uri.toString());
                try {
                    segmentBmp(getBitmapFromUri(uri));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Fires an intent to spin up the "file chooser" UI and select an image.
     */
    public void performFileSearch() {
        fileSearchPerformed = true;

        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Filter to show only images, using the image MIME data type.
        // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
        // To search for all documents available via installed storage providers,
        // it would be "*/*".
        intent.setType("image/*");

        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }

    public void segmentBmp(Bitmap inputBitmap) {
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
