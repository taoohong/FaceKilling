package com.example.facekilling.fragments;


import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.facekilling.R;

import java.io.IOException;


public class MyCamera extends Fragment {

    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    private ImageView iv_show;
    private int viewWidth, viewHeight;//mSurfaceView的宽和高
    private View mView;
    private int frontCameraId;
    private int backCameraId;
    private int numberOfCameras;


    public MyCamera() {
        // Required empty public constructor
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_my_camera, container, false);
        initView();
        return mView;
    }

    private void initView() {
        iv_show = (ImageView) mView.findViewById(R.id.iv_show_camera);
        //mSurfaceView
        mSurfaceView = (SurfaceView) mView.findViewById(R.id.surface_view_camera);
        mSurfaceHolder = mSurfaceView.getHolder();
        // mSurfaceView 不需要自己的缓冲区
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        // mSurfaceView添加回调
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) { //SurfaceView创建
                // 初始化Camera
                initCamera();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) { //SurfaceView销毁
                // 释放Camera资源
                if (mCamera != null) {
                    mCamera.stopPreview();
                    mCamera.release();
                }
            }
        });
        //设置点击监听
        mSurfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCamera == null) return;
                //自动对焦后拍照
                mCamera.autoFocus(autoFocusCallback);
            }
        });
    }

    public void initCameraInfo(){
        numberOfCameras = Camera.getNumberOfCameras();

        for (int i = 0; i < numberOfCameras; ++i) {
            final Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

            Camera.getCameraInfo(i, cameraInfo);
            //后置摄像头
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                frontCameraId = i;
            }
            //前置摄像头
            else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                frontCameraId = i;
            }
        }
    }

    /**
     * SurfaceHolder 回调接口方法
     */
    private void initCamera() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i("TEST","Granted");
        } else {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.CAMERA}, 1);
        }
        initCameraInfo();
        mCamera = Camera.open(frontCameraId);//默认开启后置
        mCamera.setDisplayOrientation(90);//摄像头进行旋转90°
        if (mCamera != null) {
            try {
                Camera.Parameters parameters = mCamera.getParameters();
                //设置预览照片的大小
                parameters.setPreviewFpsRange(viewWidth, viewHeight);
                //设置相机预览照片帧数
                parameters.setPreviewFpsRange(4, 10);
                //设置图片格式
                parameters.setPictureFormat(ImageFormat.JPEG);
                //设置图片的质量
                parameters.set("jpeg-quality", 90);
                //设置照片的大小
                parameters.setPictureSize(viewWidth, viewHeight);
                //通过SurfaceView显示预览
                mCamera.setPreviewDisplay(mSurfaceHolder);
                //开始预览
                mCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    /**
     * 自动对焦 对焦成功后 就进行拍照
     */
    Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if (success) {//对焦成功

                camera.takePicture(new Camera.ShutterCallback() {//按下快门
                    @Override
                    public void onShutter() {
                        //按下快门瞬间的操作
                    }
                }, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {//是否保存原始图片的信息

                    }
                }, pictureCallback);
            }
        }
    };
    /**
     * 获取图片
     */
    Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            final Bitmap resource = BitmapFactory.decodeByteArray(data, 0, data.length);
            if (resource == null) {
                Toast.makeText(getContext(), "拍照失败", Toast.LENGTH_SHORT).show();
            }
            final Matrix matrix = new Matrix();
            matrix.setRotate(90);
            final Bitmap bitmap = Bitmap.createBitmap(resource, 0, 0, resource.getWidth(), resource.getHeight(), matrix, true);
            if (bitmap != null && iv_show != null && iv_show.getVisibility() == View.GONE) {
                mCamera.stopPreview();
                iv_show.setVisibility(View.VISIBLE);
                mSurfaceView.setVisibility(View.GONE);
                Toast.makeText(getContext(), "拍照", Toast.LENGTH_SHORT).show();
                iv_show.setImageBitmap(bitmap);
                mCamera.release();
                mCamera = null;
            }
        }
    };
}
