package com.bfr.cameracalibrationapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{
    protected static final String LOG_TAG="CameraCalibration";
    // UI attributes
    private EditText squareSizeEdit;
    private EditText horizontalCornersEdit;
    private EditText verticalCornersEdit;

    private Button saveSettingsButton;

    private Switch gatherDataSwitch;
    protected boolean gatherData = false;

    private Button calibrateButton;

    private TextView calibrationResultTextView;

    // calibration Settings
    protected TermCriteria criteria;
    protected int horizontalCornersNumber;
    protected int verticalCornersNumber;
    protected double squareSize;
    protected boolean settingSaved = false;

    // calibration attributes
    protected List<Mat> objPoints = new ArrayList<Mat>();
    protected List<Mat> imgPoints = new ArrayList<Mat>();
    protected Mat cameraMatrix = new Mat();
    protected Mat distortionVector = new Mat();
    protected int flag = Calib3d.CALIB_FIX_FOCAL_LENGTH;

    protected Mat chessBoardDescription;

    // OpenCv
    protected CameraBridgeViewBase cameraView;

    protected BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(LOG_TAG, "OpenCV loaded successfully");
                    cameraView.enableView();
                    break;
                default:
                    Log.d(LOG_TAG, "failed to load OpenCV");
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    protected Mat frameRgb;
    protected Mat frameGray;
    int frameCounter = 0;

    static {
        if(OpenCVLoader.initDebug()){
            Log.i(LOG_TAG, "OpenCV library found");
        }
        else {
            Log.e(LOG_TAG, "couldn't find OpenCV");
        }
    }

    private void askPermission(){
        requestPermissions(new String[] {Manifest.permission.CAMERA}, 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    cameraView.setCameraPermissionGranted();
                } else {
                    Log.e(LOG_TAG, "Permission to camera denied");
                }
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        askPermission();

        squareSizeEdit = (EditText) findViewById(R.id.squareSizeEdit);
        horizontalCornersEdit = (EditText) findViewById(R.id.horizontalCornerEdit);
        verticalCornersEdit = (EditText) findViewById(R.id.verticalCornerEdit);

        saveSettingsButton = (Button) findViewById(R.id.saveSettingButton);
        saveSettingsButton.setOnClickListener(view -> {
            saveSettings();
        });
        calibrateButton = (Button) findViewById(R.id.calibrationButton);
        calibrateButton.setOnClickListener(view -> {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    calibrateButton.setBackgroundColor(Color.RED);
                    calibrationResultTextView.setText("calibrating camera ...");
                }
            });
            calibrateCamera();
            calibrateButton.setBackgroundColor(Color.BLUE);
        });

        gatherDataSwitch = (Switch) findViewById(R.id.gatherDataSwitch);
        gatherDataSwitch.setChecked(false);
        gatherDataSwitch.setOnClickListener(view -> {
            gatherData = gatherDataSwitch.isChecked();
            if(gatherData){
                imgPoints.clear();
                objPoints.clear();
            }
        });

        calibrationResultTextView = (TextView) findViewById(R.id.calibrationResultTextView);
        calibrationResultTextView.setMovementMethod(new ScrollingMovementMethod());

        cameraView = (CameraBridgeViewBase) findViewById(R.id.cameraView);
        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.setCvCameraViewListener(this);
        cameraView.enableView();

        criteria = new TermCriteria(TermCriteria.COUNT+TermCriteria.EPS, 1, 2);
    }

    private void calibrateCamera() {
        if(objPoints.size() > 0){
            List<Mat> rvecs = new ArrayList<Mat>();
            List<Mat> tvecs = new ArrayList<Mat>();
            Mat reprojectionError = new Mat();
            Mat deviationIntrinsic = new Mat();
            Mat deviationExtrensic = new Mat();
            Mat perViewError = new Mat();
            Calib3d.calibrateCameraExtended(objPoints,
                                            imgPoints,
                                            frameGray.size(),
                                            cameraMatrix,
                                            distortionVector,
                                            rvecs,
                                            tvecs,
                                            deviationIntrinsic,
                                            deviationExtrensic,
                                            perViewError,
                                            flag,
                                            criteria);

            Scalar error = Core.mean(perViewError);
            Log.i(LOG_TAG, "error : " + error);
            logMat(cameraMatrix);
            logMat(distortionVector);
            showResult(error.val[0], cameraMatrix, distortionVector);

        }
        else{
            Log.e(LOG_TAG, "no point to calibrate");
        }
    }

    private void showResult(double error, Mat calibrationMatrix, Mat distortionVector) {
        String resultString = "";
        resultString += "camera matrix : (" + frameRgb.size() + ")\n";
        resultString += matToString(calibrationMatrix);
        resultString += "distortion vector : \n";
        resultString += matToString(distortionVector);
        resultString += "reprojection error : " + ((Double) error).toString();

        String finalResultString = resultString;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                calibrationResultTextView.setText(finalResultString);
            }
        });
    }

    protected String matToString(Mat mat){
        String result = "";
        for(int i=0; i<mat.size(0); i++){
            for(int j=0; j< mat.size(1); j++){
                double value = ((double) ((int) (mat.get(i, j)[0] * 10000) ) / 10000.0);
                result += ("|" + value);
            }
            result += "|\n";
        }
        return result;
    }

    protected void saveSettings(){
        if(isEmpty(squareSizeEdit) || isEmpty(horizontalCornersEdit) || isEmpty(verticalCornersEdit)){
            Toast.makeText(this, "all setting fields should be filled", Toast.LENGTH_SHORT).show();
            return;
        }
        squareSize = Double.parseDouble(squareSizeEdit.getText().toString());
        horizontalCornersNumber = Integer.parseInt(horizontalCornersEdit.getText().toString());
        verticalCornersNumber = Integer.parseInt(verticalCornersEdit.getText().toString());
        Toast.makeText(this, "settings saved", Toast.LENGTH_SHORT).show();
        Log.d(LOG_TAG, MessageFormat.format("Settings saved\nsquare size : {0}\nhorizontal corners : {1}\nvertical corners : {2}", squareSize, horizontalCornersNumber, verticalCornersNumber));
        createChessBoardDescriptionMat();
        settingSaved = true;

    }

    protected void createChessBoardDescriptionMat(){
        chessBoardDescription = Mat.zeros(horizontalCornersNumber * verticalCornersNumber, 1, CvType.CV_32FC3);
        Log.i(LOG_TAG, "" + chessBoardDescription.get(2, 0)[0]);
        Log.i(LOG_TAG, "" + chessBoardDescription.size(0));
        for(int row=0; row < chessBoardDescription.size(0); row++){
            double corner_u = (row % verticalCornersNumber) * squareSize;
            double corner_v = (row / verticalCornersNumber) * squareSize;
            double position[] = {corner_u, corner_v, 0};
            chessBoardDescription.put(row, 0, position);
        }
        logMat(chessBoardDescription);
    }

    protected boolean isEmpty(EditText editText){
        return editText.getText().toString().equals("");
    }



    @Override
    public void onCameraViewStarted(int width, int height) {
        frameRgb = new Mat(height, width, CvType.CV_8UC4);
        frameGray = new Mat(height, width, CvType.CV_8UC1);
        Log.d(LOG_TAG, MessageFormat.format("set frame size to {0}x{1}", width, height));
    }

    @Override
    public void onCameraViewStopped() {
        frameRgb.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        frameRgb = inputFrame.rgba();
        Imgproc.cvtColor(frameRgb, frameRgb, Imgproc.COLOR_RGBA2RGB);
        Imgproc.cvtColor(frameRgb, frameGray, Imgproc.COLOR_RGB2GRAY);

        if(gatherData){
            if(!settingSaved){
//                Toast.makeText(this, "setting should be saved first", Toast.LENGTH_SHORT).show();
                gatherData = false;
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        gatherDataSwitch.setChecked(false);
                        Toast.makeText(MainActivity.this, "should save settings first", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            else{
                MatOfPoint2f corners = new MatOfPoint2f();
                boolean chessBoardFound = Calib3d.findChessboardCorners(frameGray, new Size(verticalCornersNumber, horizontalCornersNumber), corners);
                if(chessBoardFound ){
                    if(frameCounter == 0){
                        objPoints.add(chessBoardDescription);
                        Imgproc.cornerSubPix(frameGray, corners, new Size(11, 11), new Size(-1, -1), criteria);
                        imgPoints.add(corners.clone());
                    }
                    Calib3d.drawChessboardCorners(frameRgb, new Size(9,6), corners, chessBoardFound);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            gatherDataSwitch.setText("gather images (" + imgPoints.size() + ")");
                        }
                    });
                }
            }
        }
        frameCounter = (frameCounter + 1) % 5;
        return frameRgb;
    }

    private void logMat(Mat mat){
        String result = "Mat (" + mat.size() + ")\n";
        result += matToString(mat);
        Log.i(LOG_TAG, result);
    }
}