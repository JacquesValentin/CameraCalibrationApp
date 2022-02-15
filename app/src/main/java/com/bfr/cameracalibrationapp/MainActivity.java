package com.bfr.cameracalibrationapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
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
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;

import java.text.MessageFormat;

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

    // calibration attributes
    protected TermCriteria criteria;
    protected int horizontalCornersNumber;
    protected int verticalCornersNumber;
    protected double squareSize;

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
            calibrateCamera();
        });

        gatherDataSwitch = (Switch) findViewById(R.id.gatherDataSwitch);
        gatherDataSwitch.setChecked(false);
        gatherDataSwitch.setOnClickListener(view -> {
            gatherData = gatherDataSwitch.isChecked();
        });

        calibrationResultTextView = (TextView) findViewById(R.id.calibrationResultTextView);

        cameraView = (CameraBridgeViewBase) findViewById(R.id.cameraView);
        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.setCvCameraViewListener(this);
        cameraView.enableView();
    }

    private void calibrateCamera() {
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
        Log.d(LOG_TAG, MessageFormat.format("square size : {0}\nhorizontal corners : {1}\nvertical corners : {2}", squareSize, horizontalCornersNumber, verticalCornersNumber));
    }

    protected boolean isEmpty(EditText editText){
        return editText.getText().toString().equals("");
    }



    @Override
    public void onCameraViewStarted(int width, int height) {
        frameRgb = new Mat(height, width, CvType.CV_8UC4);
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
        return frameRgb;
    }
}