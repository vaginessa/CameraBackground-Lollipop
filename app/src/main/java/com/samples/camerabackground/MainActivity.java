package com.samples.camerabackground;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.Switch;

import java.io.IOException;

public class MainActivity extends Activity {

    private final static String TAG = MainActivity.class.getSimpleName();

    private FrameLayout previewView;
    private Switch cameraSwitch;

    private Camera camera;
    private CameraPreview preview;
    private boolean isCameraActive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cameraSwitch != null && cameraSwitch.isChecked()) {
            acquireCamera();
        }
    }

    @Override
    protected void onPause() {
        if (isCameraActive) {
            releaseCamera();
        }
        super.onPause();
    }

    private void initializeView() {
        previewView = (FrameLayout)findViewById(R.id.preview_view);
        cameraSwitch = (Switch)findViewById(R.id.camera_switch);
        cameraSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    acquireCamera();
                } else {
                    releaseCamera();
                }
            }
        });
    }

    private void acquireCamera() {
        camera = safeCameraOpen();
        preview = new CameraPreview(this, camera);
        previewView.addView(preview);
        isCameraActive = true;
    }

    private Camera safeCameraOpen() {
        Camera newCamera = null;

        try {
            releaseCamera();
            newCamera = Camera.open();
            configureCamera(newCamera);

        } catch (Exception e) {
            Log.e(TAG, "Failed to open Camera", e);
        }

        return newCamera;
    }

    private void configureCamera(Camera c) {
        Camera.Parameters p = c.getParameters();
        Camera.Size pictureSize = p.getSupportedPictureSizes().get(0); // use max size
        p.setPictureSize(pictureSize.width, pictureSize.height);

        for (Camera.Size previewSize : p.getSupportedPreviewSizes()) {
            if (previewSize.width / (double)previewSize.height == 1.5) { // use 3:2 aspect ratio
                p.setPreviewSize(previewSize.width, previewSize.height);
                break;
            }
        }
        c.setParameters(p);
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
        if (preview != null) {
            previewView.removeAllViews();
            preview = null;
        }
        isCameraActive = false;
    }

    // From developer.android.com
    private class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

        private SurfaceHolder mHolder;
        private Camera mCamera;

        public CameraPreview(Context context, Camera camera) {
            super(context);
            mCamera = camera;

            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        public void surfaceCreated(SurfaceHolder holder) {
            // The Surface has been created, now tell the camera where to draw the preview.
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException e) {
                Log.e(TAG, "Error setting camera preview", e);
            } catch (NullPointerException e) {
                Log.e(TAG, "Error connecting to camera", e);
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // empty. Take care of releasing the Camera preview in your activity.
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.

            if (mHolder.getSurface() == null){
                // preview surface does not exist
                return;
            }

            // stop preview before making changes
            try {
                mCamera.stopPreview();
            } catch (Exception e){
                // ignore: tried to stop a non-existent preview
            }

            // set preview size and make any resize, rotate or
            // reformatting changes here

            // start preview with new settings
            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();

            } catch (Exception e){
                Log.e(TAG, "Error starting camera preview", e);
            }
        }
    }
}
