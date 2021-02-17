package com.jiangdg.usbcamera.view;

import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.jiangdg.usbcamera.R;
import com.jiangdg.usbcamera.UVCCameraHelper;
import com.jiangdg.usbcamera.application.MyApplication;
import com.jiangdg.usbcamera.utils.FileUtils;
import com.jiangdg.usbcamera.utils.HttpAsynTackCallback;
import com.jiangdg.usbcamera.utils.HttpAsynTask;
import com.jiangdg.usbcamera.utils.SocketThread;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.common.AbstractUVCCameraHandler;
import com.serenegiant.usb.encoder.RecordParams;
import com.serenegiant.usb.widget.CameraViewInterface;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * UVCCamera use demo
 * <p>
 * Created by jiangdongguo on 2017/9/30.
 */

public class USBCameraActivity extends AppCompatActivity implements CameraDialog.CameraDialogParent, CameraViewInterface.Callback {
    private static final String TAG = "Debug";
    @BindView(R.id.camera_view)
    public View mTextureView;

    @BindView(R.id.btn_fore)
    public Button mBtn;

    @BindView(R.id.pi)
    public CircularProgressIndicator pi;

    private UVCCameraHelper mCameraHelper;
    private CameraViewInterface mUVCCameraView;
    private AlertDialog mDialog;

    private boolean isRequest;
    private boolean isPreview;

    private HttpAsynTackCallback callback = new HttpAsynTackCallback() {
        @Override
        public void onSuccess(JSONObject result) {
            SocketThread thread = new SocketThread(result, mHandler);
            thread.start();
        }

        @Override
        public void onFailure(Exception e) {
            e.printStackTrace();
            showShortMsg("Server connection failed. Please check internet connection.");
        }
    };

    private Handler mHandler = new Handler(){
        public void handleMessage(Message msg) {
            pi.setVisibility(View.INVISIBLE);
            if(msg.what == 1) {
                showShortMsg("Successfully send.");
            }
            else{
                showShortMsg("Send failed. Please check internet connection");
            }
            /***
             * IF NEEDS, TURN OFF APP
             */
        }
    };

    private UVCCameraHelper.OnMyDevConnectListener listener = new UVCCameraHelper.OnMyDevConnectListener() {

        @Override
        public void onAttachDev(UsbDevice device) {
            // request open permission
            if (!isRequest) {
                isRequest = true;
                if (mCameraHelper != null) {
                    mCameraHelper.requestPermission(0);
                }
            }
        }

        @Override
        public void onDettachDev(UsbDevice device) {
            // close camera
            if (isRequest) {
                isRequest = false;
                mCameraHelper.closeCamera();
                showShortMsg(device.getDeviceName() + " is out");
            }
        }

        @Override
        public void onConnectDev(UsbDevice device, boolean isConnected) {
            if (!isConnected) {
                showShortMsg("fail to connect,please check resolution params");
                isPreview = false;
            } else {
                isPreview = true;
                showShortMsg("connecting");
                // initialize seekbar
                // need to wait UVCCamera initialize over
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(2500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Looper.prepare();
                        Looper.loop();
                    }
                }).start();
            }
        }

        @Override
        public void onDisConnectDev(UsbDevice device) {
            showShortMsg("disconnecting");
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usbcamera);
        ButterKnife.bind(this);
        initView();

        // step.1 initialize UVCCameraHelper
        mUVCCameraView = (CameraViewInterface) mTextureView;
        mUVCCameraView.setCallback(this);
        mCameraHelper = UVCCameraHelper.getInstance();
        mCameraHelper.setDefaultFrameFormat(UVCCameraHelper.FRAME_FORMAT_MJPEG);
        mCameraHelper.initUSBMonitor(this, mUVCCameraView, listener);
        mCameraHelper.updateResolution(1280, 720);

        mCameraHelper.setOnPreviewFrameListener(new AbstractUVCCameraHandler.OnPreViewResultListener() {
            @Override
            public void onPreviewResult(byte[] nv21Yuv) {
                Log.d(TAG, "onPreviewResult: "+nv21Yuv.length);
            }
        });
    }

    private void initView() {
    }

    @Override
    protected void onStart() {
        super.onStart();
        pi.setVisibility(View.INVISIBLE);
        // step.2 register USB event broadcast
        if (mCameraHelper != null) {
            mCameraHelper.registerUSB();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // step.3 unregister USB event broadcast
        if (mCameraHelper != null) {
            mCameraHelper.unregisterUSB();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @OnClick(R.id.btn_fore)
    void pick()
    {
        pi.setVisibility(View.VISIBLE);

        String resourcePath = UVCCameraHelper.ROOT_PATH + MyApplication.RESOURCE_DIRECTORY_NAME;
        String picPath = UVCCameraHelper.ROOT_PATH + MyApplication.DIRECTORY_NAME +"/images/"
                + System.currentTimeMillis() + UVCCameraHelper.SUFFIX_JPEG;
        String debugPicPath = "/storage/sdcard/Download/input.jpg";

        if (mCameraHelper == null || !mCameraHelper.isCameraOpened()) {
            doHttpAsyncTask(debugPicPath, resourcePath, "Debug mode on", "No server connection");
        }
        else {
            mCameraHelper.capturePicture(picPath, new AbstractUVCCameraHandler.OnCaptureListener() {
                @Override
                public void onCaptureResult(String path) {
                    if(TextUtils.isEmpty(path)) {
                        return;
                    }
                    new Handler(getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            doHttpAsyncTask(path, resourcePath, "Analyzing...", "No server connection");
                        }
                    });
                }
            });
        }
    }

    void doHttpAsyncTask(String picPath, String resourcePath, String startToastMsg, String endToastMsg)
    {
        Toast.makeText(USBCameraActivity.this, startToastMsg, Toast.LENGTH_SHORT).show();
        try {
            new HttpAsynTask(callback).execute(picPath, resourcePath); //for debug
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Toast.makeText(USBCameraActivity.this, endToastMsg, Toast.LENGTH_SHORT).show();
        }
        return;
    }

    // example: {640x480,320x240,etc}
    private List<String> getResolutionList() {
        List<Size> list = mCameraHelper.getSupportedPreviewSizes();
        List<String> resolutions = null;
        if (list != null && list.size() != 0) {
            resolutions = new ArrayList<>();
            for (Size size : list) {
                if (size != null) {
                    resolutions.add(size.width + "x" + size.height);
                }
            }
        }
        return resolutions;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FileUtils.releaseFile();
        // step.4 release uvc camera resources
        if (mCameraHelper != null) {
            mCameraHelper.release();
        }
    }

    private void showShortMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public USBMonitor getUSBMonitor() {
        return mCameraHelper.getUSBMonitor();
    }

    @Override
    public void onDialogResult(boolean canceled) {
        if (canceled) {
            showShortMsg("Complete");
        }
    }

    public boolean isCameraOpened() {
        return mCameraHelper.isCameraOpened();
    }

    @Override
    public void onSurfaceCreated(CameraViewInterface view, Surface surface) {
        if (!isPreview && mCameraHelper.isCameraOpened()) {
            mCameraHelper.startPreview(mUVCCameraView);
            isPreview = true;
        }
        pick();
    }

    @Override
    public void onSurfaceChanged(CameraViewInterface view, Surface surface, int width, int height) {

    }

    @Override
    public void onSurfaceDestroy(CameraViewInterface view, Surface surface) {
        if (isPreview && mCameraHelper.isCameraOpened()) {
            mCameraHelper.stopPreview();
            isPreview = false;
        }
    }
}
