package com.example.opencvdemoattempt2;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

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
    private static final int READ_REQUEST_CODE = 666;

    private static boolean shouldPerformFileSearch = true;

    private ImageView mSegmentationResultsDisplay;
    private TextView mErrorMessageDisplay;
    private ProgressBar mLoadingIndicator;

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

        mSegmentationResultsDisplay = (ImageView) findViewById(R.id.iv_segmentation_results_display);
        mErrorMessageDisplay = (TextView) findViewById(R.id.tv_error_message_display);
        mLoadingIndicator = (ProgressBar) findViewById(R.id.pb_loading_indicator);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(shouldPerformFileSearch) {
            performFileSearch();
        }
        shouldPerformFileSearch = true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        shouldPerformFileSearch = false;

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
                new OpenCVTask().execute(uri);
            }
        }
    }

    /**
     * Fires an intent to spin up the "file chooser" UI and select an image.
     */
    public void performFileSearch() {
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

    public Bitmap segmentBmp(Bitmap inputBitmap) {
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

        return outputBitmap;
    }

    private void showSegmentationResultsView() {
        mErrorMessageDisplay.setVisibility(View.INVISIBLE);
        mSegmentationResultsDisplay.setVisibility(View.VISIBLE);
    }

    private void showErrorMessage() {
        mSegmentationResultsDisplay.setVisibility(View.INVISIBLE);
        mErrorMessageDisplay.setVisibility(View.VISIBLE);
    }

    public class OpenCVTask extends AsyncTask<Uri, Void, Bitmap> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mLoadingIndicator.setVisibility(View.VISIBLE);
        }

        @Override
        protected Bitmap doInBackground(Uri... params) {
            Uri uri = params[0];
            Bitmap segmentedImage = null;

            try {
                segmentedImage = segmentBmp(getBitmapFromUri(uri));
                return segmentedImage;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap segmentedImage) {
            mLoadingIndicator.setVisibility(View.INVISIBLE);

            if (segmentedImage != null) {
                showSegmentationResultsView();
                mSegmentationResultsDisplay.setImageBitmap(segmentedImage);
            } else {
                showErrorMessage();
            }
        }
    }
}
