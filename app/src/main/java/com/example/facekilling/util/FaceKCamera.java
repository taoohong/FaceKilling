package com.example.facekilling.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class FaceKCamera {

    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    private Context context;
    private Activity activity;
    private int viewWidth, viewHeight;
    private int frontCameraId;
    private int backCameraId;
    private int numberOfCameras;
    private int usingCamera;
    private Bitmap mBitmap;


    public FaceKCamera(@NonNull SurfaceView surfaceView, @NonNull Activity activity){
        this.mSurfaceView = surfaceView;
        this.activity = activity;
        this.context = activity.getApplicationContext();
        initView();
    }


    private void initCameraInfo(){
        numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; ++i) {
            final Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(i, cameraInfo);
            //后置摄像头
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                backCameraId = i;
            }
            //前置摄像头
            else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                frontCameraId = i;
            }
        }

    }

    private void initFrontCamera() {
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
        usingCamera = frontCameraId;
    }

    private void initBackCamera() {
        mCamera = Camera.open(backCameraId);//默认开启后置
        mCamera.setDisplayOrientation(270);//摄像头进行旋转90°
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
        usingCamera = backCameraId;
    }

    public void stopCamera(){
        if(mCamera!=null){
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private void initView() {
        initCameraInfo();
        askForPermission();
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) { //SurfaceView创建
                initFrontCamera();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) { //SurfaceView销毁
                // 释放Camera资源
                if (mCamera != null) {
                    stopCamera();
                }
            }
        });
        //设置点击监听
        mSurfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCamera == null) return;
                //自动对焦后拍照
                //mCamera.autoFocus(autoFocusCallback);
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        Toast.makeText(context,"聚焦",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    public void changeCamera(){
        if (usingCamera==frontCameraId){
            stopCamera();
            initBackCamera();
        }else {
            stopCamera();
            initFrontCamera();
        }
    }

    public void takePicture(){
        if (mCamera==null){
            return;
        }
        else {
            mCamera.takePicture(new Camera.ShutterCallback() {//按下快门
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

    Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            final Bitmap resource = BitmapFactory.decodeByteArray(data, 0, data.length);
            if (resource == null) {
                Toast.makeText(context, "拍照失败", Toast.LENGTH_SHORT).show();
            }
            final Matrix matrix = new Matrix();
            matrix.setRotate(-90);
            mBitmap = Bitmap.createBitmap(resource, 0, 0, resource.getWidth(), resource.getHeight(), matrix, true);
            mCamera.stopPreview();
            savePic(mBitmap);
        }
    };

    private void savePic(final Bitmap bitmap){
            String sdStatus = Environment.getExternalStorageState();
            if (!sdStatus.equals(Environment.MEDIA_MOUNTED)) {
                return;
            }
            FileOutputStream fileOutputStream = null;
            String baseFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "FaceK"+File.separator+"Camera";
            File baseFile = new File(baseFilePath);
            if (!baseFile.exists()) {
                baseFile.mkdirs();
                Log.d("cameraMMMMM","file created");
            }
            String pickName = GetSysTime.getCurrTime() +".jpg";
            Log.d("cameraMMMMM",pickName);
            try {
                File picFile = new File(baseFile,pickName);
                fileOutputStream = new FileOutputStream(picFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                MediaStore.Images.Media.insertImage(context.getContentResolver(), picFile.getPath(), pickName, "description");
                Uri uri = Uri.fromFile(picFile);
                context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    fileOutputStream.flush();
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }catch (NullPointerException ne){
                    ne.printStackTrace();
                }
            }

    }


    private void askForPermission(){
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
        } else {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.CAMERA}, 1);
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED&&ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
        } else {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        }
    }

    public void showImg(ImageView imageView){
        if (mBitmap != null && imageView != null && imageView.getVisibility() == View.GONE) {
            imageView.setImageBitmap(mBitmap);
        }
    }

    public void startProView(){
        mCamera.startPreview();
    }

    public Camera getmCamera() {
        return mCamera;
    }
}
