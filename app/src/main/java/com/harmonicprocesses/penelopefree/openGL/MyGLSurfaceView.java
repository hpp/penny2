package com.harmonicprocesses.penelopefree.openGL;

import javax.microedition.khronos.egl.EGLContext;

import com.harmonicprocesses.penelopefree.audio.AudioConstants;
import com.hpp.openGL.SurfaceTextureManager;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.View;

public abstract class MyGLSurfaceView extends GLSurfaceView {

   
	public static double AMPLITUDE_THRESHOLD =  AudioConstants.AMPLITUDE_THRESHOLD;
	public View.OnTouchListener listenForTouch;
	public int maxAmpIdx;
	public float maxAmplitude;
	public MyGLRenderer mRenderer;
	protected boolean drawCameraFrame = false, drawOverlay;
	EGLContext mEGLContext;
    protected SurfaceTextureManager mST;

	public MyGLSurfaceView(Context context) {
		super(context);
	}

	public abstract int updateAmplitudes(float[] newSpectrum) throws Exception;

	public abstract EGLContext getEGLContext(); 

	public abstract void makeCurrent();

    public abstract void updateParticles(int numberParticles,
                                         float sizeParticles,
                                         float opacityParticles);

    public void setDrawOverlay(Boolean val) {
        drawOverlay = val;
    }

    public void beginCapture(){
    	drawCameraFrame = true;
    }
    
    public void endCapture(SurfaceTextureManager st){
    	drawCameraFrame = false;
        mST = st;
    }

}

