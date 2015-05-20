package com.harmonicprocesses.penelopefree.camera;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.harmonicprocesses.penelopefree.PenelopeMainActivity;
import com.harmonicprocesses.penelopefree.R;
import com.harmonicprocesses.penelopefree.audio.AudioThru;
import com.harmonicprocesses.penelopefree.openGL.MyGLSurfaceView;
import com.harmonicprocesses.penelopefree.settings.UpSaleDialog;
import com.hpp.openGL.MyEGLWrapper;
import com.hpp.openGL.STextureRender;
import com.hpp.openGL.SurfaceTextureManager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class Pcamera {
	private static final String TAG = "io.hpp.camera.Pcamera";
	PenelopeMainActivity mContext;
	Camera mCamera;
	private boolean isRecording = false;
    MediaRecorder mMediaRecorder;
    ImageButton captureButton, switchCameraButton;
    static int mCamId;
    ViewGroup videoSurfaceViewGroup;
    private FragmentManager mFrag;
	private CaptureManager captureManager=null;
    
	
	public Pcamera(PenelopeMainActivity context, ImageButton recordButton, ImageButton switchCamera,
                   MyGLSurfaceView mGLView, AudioThru audioThru, SurfaceView cameraSV) {
		mContext = context;
		mFrag = context.getFragmentManager();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
			captureManager = new CaptureManager(mContext,this,mGLView,audioThru,cameraSV);
			captureButton = recordButton;
			captureButton.setOnClickListener(CaptureButtonListener);
		}
        switchCameraButton = switchCamera;
        switchCameraButton.setOnClickListener(SwitchCameraListener);
				
		if (!checkCameraHardware()) {
			//TODO what if no camera
		}
		
		mCamera = getCameraInstance(mContext.mSharedPrefs.getBoolean("switch_camera_value", true));

		//mCameraPreview = new CameraPreview(mContext, mCamera);
		//prepareVideoRecorder();
	}

    public void initSurface() {
        captureManager.prepareSurfaceTexture(mCamera);
    }
	
	/**
	 * Set which mode to capture, either video or audio
	 * 
	 * @param videoMode, true of video, false for audio
	 */
	public void captureMode(boolean videoMode){
		if (videoMode){
			
		}
	}

    /**
     * checks that a instance exist and starts as necessary.
     * @return true if a instance of camera starts correctly
     */
	private boolean prepCam(){
		if (mCamera==null) {
			mCamera = getCameraInstance(mCamId==1);
		}
		//if (mCameraPreview==null){
		//	mCameraPreview = new CameraPreview(mContext, mCamera);
		//}
		return (mCamera!=null); 
	}



	void stopPreview(){
		if (mCamera!=null)	mCamera.stopPreview();

	}


    /**
     * Starts the camera preview
     * @return true if camera is able to start an instance.
     */
	public boolean start(){
		if (!prepCam()||(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2)) {
			Log.e(TAG,"failed to start camera");
			return false;
		}
		//mCameraPreview.set
        captureManager.startCameraPreview(mCamera);
		return true;
	}
	
	public void stop(FrameLayout vg) {
		if (captureManager!=null) captureManager.releaseSurfaceTexture();
		//releaseMediaRecorder();
		//releaseCamera();
	}
	
	private boolean checkCameraHardware() {
	    if (mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
	        // this device has a camera
	        return true;
	    } else {
	        // no camera on this device
	        return false;
	    }
	}

    /**
     * starts an instance of camera
     * @param useFaceCamera true if face camera is to be used
     * @return an instance of camera or null if fails to start
     */
	public static Camera getCameraInstance(Boolean useFaceCamera){
	    Camera c = null;
		if (useFaceCamera && Camera.getNumberOfCameras()>1){
			mCamId = 1; //this should be the face cam
		} else mCamId = 0;
		
	    try {
	        c = Camera.open(mCamId); // attempt to get a Camera instance
	    }
	    catch (Exception e){
	        // Camera is not available (in use or does not exist)
	    	Log.d(TAG,"Camera failed to open");
	    }
	    return c; // returns null if camera is unavailable
	}


	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;


	/** Create a File for saving an image or video */
	static File getOutputMediaFile(int type){
	    // To be safe, you should check that the SDCard is mounted
	    // using Environment.getExternalStorageState() before doing this.

	    File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
	              Environment.DIRECTORY_PICTURES), "Penelope");
	    // This location works best if you want the created images to be shared
	    // between applications and persist after your app has been uninstalled.

	    // Create the storage directory if it does not exist
	    if (! mediaStorageDir.exists()){
	        if (! mediaStorageDir.mkdirs()){
	            Log.d("Penelope", "failed to create directory");
	            return null;
	        }
	    }

	    // Create a media file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    File mediaFile;
	    if (type == MEDIA_TYPE_IMAGE){
	        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
	        "IMG_"+ timeStamp + ".jpg");
	    } else if(type == MEDIA_TYPE_VIDEO) {
	        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
	        "VID_"+ timeStamp + ".mp4");
	    } else {
	        return null;
	    }

	    return mediaFile;
	}
	
    private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

		
	OnClickListener CaptureButtonListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2){
				upSale();
				return;
			}
			if (isRecording) {
				//stop
				isRecording = false; // stops the incoming video
				captureManager.endCapture();
				//releaseSurfaceTexture(); 
				captureButton.setImageResource(R.drawable.capture_button);
			} else {
				//start
				isRecording = true;
				/*
				new Handler(Looper.getMainLooper()).post(
					new Runnable(){
						@Override
						public void run() {
							// This must be created on MainActivityThread
							prepareSurfaceTexture();
						}
				});
				//*/
				
				captureManager.beginCapture(videoSurfaceViewGroup,mCamId);
				captureButton.setImageResource(R.drawable.stop_button);
			}
		}
	};

    OnClickListener SwitchCameraListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (isRecording) return;

            int oldCamId = mCamId;

            if (mCamId == 0 && Camera.getNumberOfCameras()>1)
                mCamId = 1;
            else
                mCamId = 0;

            if (oldCamId != mCamId) {
                SharedPreferences.Editor editor = mContext.mSharedPrefs.edit();
                editor.putBoolean("switch_camera_key", mCamId==1);
                editor.commit();

                stop(mContext.mOpenGLViewGroup);
                start();

            }
        }
    };
	
	private void upSale(){
		Bundle bundle = new Bundle();
		bundle.putInt("messageId", R.string.dialog_penelope_full_messsage_capture);
		UpSaleDialog dialog = new UpSaleDialog();
		dialog.setArguments(bundle);
		dialog.show(mFrag,"PaidForVersionDialog");
	}

	

	public Surface getRecordingSurface() {
		return captureManager.getVideoSurface();
	}

	public MyEGLWrapper getEGLWrapper() {
		return captureManager.getMyEGLWrapper();
	}
	
	public void ChangeVideoEffect(String videoEffect){
		if (captureManager == null) return;
		captureManager.changeFragmentShader(videoEffect);
	}
}

