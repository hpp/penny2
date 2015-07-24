/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.harmonicprocesses.penelopefree.openGL;    
import com.harmonicprocesses.penelopefree.*;
import com.harmonicprocesses.penelopefree.openGL.shapes.DarkParticles;
import com.harmonicprocesses.penelopefree.openGL.shapes.LightParticles;
import com.harmonicprocesses.penelopefree.openGL.shapes.NoteBillboard;
import com.harmonicprocesses.penelopefree.openGL.shapes.OuterCircle;
import com.harmonicprocesses.penelopefree.openGL.shapes.Particles;
import com.harmonicprocesses.penelopefree.openGL.shapes.Square;
import com.harmonicprocesses.penelopefree.openGL.utils.Accelmeter;
import com.harmonicprocesses.penelopefree.renderscript.ScriptC_particleFilter;
import com.harmonicprocesses.penelopefree.renderscript.ScriptField_particle;
import com.hpp.openGL.STextureRender;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Build;
import android.preference.PreferenceManager;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class MyGLRenderer implements GLSurfaceView.Renderer {
	
	private Context mContext;
	private int subDivision = 12;
	private int numSquares = subDivision*4*subDivision;
	private float particleDelta = 0.01f; //distance sound travels per iteration on plate
    private final int maxNumParticles = Particles.vertexMaxCount;
	
    private static final String TAG = "MyGLRenderer";
    private Square mSquare[] = new Square[96];
    private NoteBillboard mBillboard;
    private OuterCircle mOuterCircle;
    private Particles mParticles;
    private LightParticles mLightParticles; 
    private DarkParticles mDarkParticles; 
    
    private float[] particleVBO;

    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjMatrix = new float[16];
    private final float[] mVMatrix = new float[16];
    private final float[] mTransposeMatrix = new float[16];
    private final float[] mScaleMatrix = new float[16];
    
    private int mTextureID;
    private Paint mLabelPaint;
    private int mWidth = 1;
    private int mHeight = 1;

    
    // Declare as volatile because we are updating it from another thread
    public volatile float particleSize, mNoteAmp, particleOpacity;
    public volatile float[] mAmplitude = new float[numSquares];
    public volatile int mNote = 1, mMode = 4, numParticles = 100000;
    
    
    //Declare variables for renderscript
    private RenderScript mRS;
    private Allocation mInAllocation;
    private Allocation mOutAllocation;
    private ScriptC_particleFilter mScript;
    private SharedPreferences mSharedPrefs;
    public Accelmeter mAccelmeter;
	private MyGLSurfaceView mGLSurfaceView;

    boolean useTransformFeedback = true;
    
    
    
    public MyGLRenderer(Context context, MyGLSurfaceView glSurfaceView){
    	mContext = context;
    	mGLSurfaceView = glSurfaceView;
    	mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mLabelPaint = new Paint();
        mLabelPaint.setTextSize(32);
        mLabelPaint.setAntiAlias(true);
        mLabelPaint.setARGB(0xff, 0x00, 0x00, 0x00);
        mAccelmeter = new Accelmeter(context);
        particleSize = mSharedPrefs.getFloat("size_of_particles_key", 1.0f); //
        particleOpacity = mSharedPrefs.getFloat("opacity_of_particles_key", 1.0f); // for these variables.
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        
    	if (mSharedPrefs.getBoolean("turn_on_accelerometer_key", true)){
    		mAccelmeter.start();
    	}
        // Set the background frame color
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        int[] textures = new int[1];
        GLES30.glGenTextures(1, textures, 0);

        mTextureID = textures[0];
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextureID);

        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER,
        		GLES30.GL_NEAREST);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D,
        		GLES30.GL_TEXTURE_MAG_FILTER,
        		GLES30.GL_LINEAR);

        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S,
        		GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameterf(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T,
        		GLES30.GL_CLAMP_TO_EDGE);


		float len = 1.0f/subDivision;
		int dist = 2*subDivision;
		
		for (int i = 0; i<96; i++){
			mSquare[i] = new Square(0.5f-i*0.01f,0.0f,0.01f);
		}

        mBillboard = new NoteBillboard(mContext);
        mOuterCircle = new OuterCircle(mNote, 64); // start at A,    


        if (!useTransformFeedback) {
            numParticles = mSharedPrefs.getInt("vis_number_of_particles_key", 6000);

            mLightParticles = new LightParticles(numParticles);
            mDarkParticles = new DarkParticles(numParticles);

        } else {

            mParticles = new Particles(mContext);
        }
        numParticles = mSharedPrefs.getInt("vis_number_of_particles_key", 6000);


    }


	@Override
    public void onDrawFrame(GL10 gl) {
		drawFrame(null, null);
		
    }

	public void drawFrame(STextureRender textureRender, SurfaceTexture texture) {
		// Draw background color
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mVMatrix, 0, 0f, 0f, -3f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mVMatrix, 0);

        if (textureRender!=null && mGLSurfaceView.drawCameraFrame){
        	//textureRender.surafaceChanged(mWidth, mHeight);
        	textureRender.drawFrame(texture,mMVPMatrix,mProjMatrix);
        }

        if (!mGLSurfaceView.drawOverlay){
            return;
        }
        // move particles and calc bins
        //mParticleBins.clear();
        
        /*****************************************
        for (int i = 0;i<numParticles;i++){
        	//Matrix.setIdentityM(mTranslationMatrix, 0);
        	mParticle[i].next();
        	mParticleBins.add(mParticle[i]);
            particleVBO[i*3] = mParticle[i].location[0];
        	particleVBO[i*3+1] = mParticle[i].location[1];
        }
        mParticleBins.normallize();
        //*/
        
        int j = 0;
        for (int i = 0; i < mSquare.length; i++){
        	Matrix.setIdentityM(mScaleMatrix, 0);
        	//Matrix.setIdentityM(mTranslationMatrix, 0);
        	Matrix.scaleM(mScaleMatrix, 0, 1.0f, 1000.0f*mAmplitude[j++], 1.0f);
        	Matrix.multiplyMM(mTransposeMatrix, 0, mScaleMatrix , 0, mMVPMatrix, 0);
        	//Matrix.translateM(mTranslationMatrix, 0, 0.3f, 0.3f, 0.0f);
        	//Matrix.multiplyMM(mTransposeMatrix, 0, mScaleMatrix, 0, mRotationMatrix, 0);
        	if (i == 12+mMode*12+mNote) { // is fundamental
        		mSquare[i].draw(mTransposeMatrix, 1.0f);//mParticleBins.normallizedBins[i]);
        	} else if ( i > 12+mMode*12 && i < 24+mMode*12) { // in mode
        		mSquare[i].draw(mTransposeMatrix, 0.3f);//mParticleBins.normallizedBins[i]);
        	} else {
        		mSquare[i].draw(mTransposeMatrix, 0.0f);//mParticleBins.normallizedBins[i]);
        	}
        }
        //***************************************/
        
        //Matrix.multiplyMM(mMVPMatrix, 0,mMVPMatrix , 0,mTransposeMatrix , 0);
        // Create a rotation for the triangle
//        long time = SystemClock.uptimeMillis() % 4000L;
//        float angle = 0.090f * ((int) time);
        //Matrix.setRotateM(mRotationMatrix, 0, mAngle, 0, 0, -1.0f);
        //Matrix.setIdentityM(mRotationMatrix, 0);
        //Matrix.translateM(mRotationMatrix, 0, 0.3f, 0.3f, 0.0f);
               
        // Combine the rotation matrix with the projection and camera view
        //Matrix.multiplyMM(mMVPMatrix, 0, mRotationMatrix, 0, mMVPMatrix, 0);
        
        //mScript.invoke_getNextPosition();
        double ay = Math.abs(mAccelmeter.linear_acceleration[1]);
        if (ay>1.0){
        	mMode = 1;
        }
        
        // Draw triangle
        mBillboard.draw(mMVPMatrix,mNote);
        mOuterCircle.draw(mMVPMatrix, mNote);


        if (!useTransformFeedback) {
            mLightParticles.draw(mMVPMatrix, mOuterCircle.getRadius(), mMode, mNoteAmp);
            mDarkParticles.draw(mMVPMatrix, mOuterCircle.getRadius(), mMode, mNoteAmp);
        } else {
            //nextParticleVBO();
            mParticles.draw(mMVPMatrix, numParticles, particleSize,
                    mOuterCircle.getRadius(), mMode, mNoteAmp, particleOpacity);
        }

        //if (textureRender!=null){
        GLES30.glFlush();
        //  if (useTransformFeedback) mParticles.onPostFlush(numParticles);
        //}

	}
		
	float mRatio;
	
	@Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        // Adjust the viewport based on geometry changes,
        // such as screen rotation
        GLES30.glViewport(0, 0, width, height);

        mRatio = (float) width / (float) height;
        mHeight = height;
        mWidth = width;
        
        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(mProjMatrix, 0, -mRatio, mRatio, -1, 1, 3, 7);

    }

    public static int loadShader(int shaderType, String source) {
        int shader = GLES30.glCreateShader(shaderType);
        if (shader != 0) {
            GLES30.glShaderSource(shader, source);
            GLES30.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e(TAG, "Could not compile shader " + shaderType + ":\n" + source);
                Log.e(TAG, GLES30.glGetShaderInfoLog(shader));
                GLES30.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    public void sizeChanged(GL10 gl, int w, int h) {
        mWidth = w;
        mHeight = h;
    }
    
    /**
     * Utility method for debugging OpenGL calls. Provide the name of the call
     * just after making it:
     *
     * <pre>
     * mColorHandle = GLES30.glGetUniformLocation(mProgram, "vColor");
     * MyGLRenderer.checkGlError("glGetUniformLocation");</pre>
     *
     * If the operation is not successful, the check throws an error.
     *
     * @param glOperation - Name of the OpenGL call to check.
     */
    public static void checkGlError(String glOperation) {
        int error;
        while ((error = GLES30.glGetError()) != GLES30.GL_NO_ERROR) {

            Log.e(TAG, glOperation + ": glError " + error);
            //throw new RuntimeException(glOperation + ": glError " + error);
        }
    }
    
    private void createParticleScript(float[] inits) {
    	mRS = RenderScript.create(mContext);
        mInAllocation = Allocation.createSized(mRS, ScriptField_particle.createElement(mRS), numParticles);
        mOutAllocation = Allocation.createTyped(mRS, mInAllocation.getType());
        mScript = new ScriptC_particleFilter(mRS, mContext.getResources(), R.raw.particlefilter);
        nextParticleVBO();
        //return output;
	}
    
    private void nextParticleVBO() {
	    mInAllocation.copy1DRangeFromUnchecked(0, numParticles, particleVBO);
	    //mScript.forEach_root(mInAllocation, mOutAllocation);
	     
	    //mOutAllocation.;
	    
	}


    
       
}

