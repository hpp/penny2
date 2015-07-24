package com.harmonicprocesses.penelopefree.openGL.shapes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLUtils;

import com.harmonicprocesses.penelopefree.R;
import com.harmonicprocesses.penelopefree.openGL.MyGLRenderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * This file demonstrates the use of Harmonic Processes Inc. HPP Texture based audio
 * processing framework, better know as Penelope.
 *
 *  "We claim for our invention a utility or otherwise how be it patentable system for
 * using particle filters to model surface transitions and resonant timbres using
 * textures or lookup tables of nodal or other acoustical areas of interest in 2d or 3d."
 */
public class TransformParticles {
    private final String TAG = "TransformParticles";

    private final String vertexShaderCode =
            // This matrix member variable provides a hook to manipulate
            // the coordinates of the objects that use this vertex shader
            "#version 300 es\n" +
            "uniform mat4 uMVPMatrix;\n" +
            "in vec4 vPosition;\n" +
            "out vec4 vPositionOut;\n" +
            "void main() {\n" +
            // the matrix must be included as a modifier of gl_Position
            "   vPositionOut.xy = vPosition.xy + 0.005;\n" +
            "   gl_Position = vec4(vPosition.xy,0.0,1.0) * uMVPMatrix;\n" +
            "   gl_PointSize = 1.0;\n" +
            "}\n";

    private final String fragmentShaderCode =
            "#version 300 es\n" +
            "precision mediump float;\n" +
            "uniform vec4 vColor;\n" +
            "out vec4 my_FragColor;\n" +
            "void main() {\n" +
            "   my_FragColor = vColor;\n" +
            "}\n";
    private final int vertexMaxCount = 10000;
    private final float[] mColor = {0.0f,1.0f,0.0f,1.0f}; // = { 0.63671875f, 0.76953125f, 0.22265625f, 1.0f };
    private ByteBuffer bb;
    private FloatBuffer vertexBuffer;

    private int[] transformBuffers, transformFeedbackIds;
    private int mProgram, ping = 0, pong = 1,
            COORDS_PER_VERTEX = 4, vertexStride = 4 * COORDS_PER_VERTEX,
            mColorHandle, mMVPMatrixHandle, mRadiusHandle, mNoteAmplitued,
            mModeHandle, mCymaticSamplerHandle, mPositionHandle; //four bytes per float
    // Get uniform locations



    public TransformParticles(Context context) {

        float[] particleVBO = genParticleVBO();
        //vertexShaderCode = ReadFile("particleVertex.glsl");

        // initialize vertex byte buffer for shape coordinates
        bb = ByteBuffer.allocateDirect(
                // (number of coordinate values * 4 bytes per float)
                particleVBO.length * 4);
        // use the device hardware's native byte order
        bb.order(ByteOrder.nativeOrder());

        // create a floating point buffer from the ByteBuffer
        vertexBuffer = bb.asFloatBuffer();
        // add the coordinates to the FloatBuffer
        vertexBuffer.put(particleVBO);
        // set the buffer to read the first coordinate
        vertexBuffer.position(0);


        // prepare shaders and OpenGL program
        int vertexShader = MyGLRenderer.loadShader(GLES30.GL_VERTEX_SHADER,
                vertexShaderCode);
        int fragmentShader = MyGLRenderer.loadShader(GLES30.GL_FRAGMENT_SHADER,
                fragmentShaderCode);
        mProgram = GLES30.glCreateProgram();             // create empty OpenGL Program
        GLES30.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES30.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        MyGLRenderer.checkGlError(TAG + " glCreateProgram / glAttachShader");

        // Link transformfeedback varyings
        String[] transformFeedbackName = {"vPositionOut"};
        GLES30.glTransformFeedbackVaryings(mProgram, transformFeedbackName, GLES30.GL_INTERLEAVED_ATTRIBS);
        MyGLRenderer.checkGlError(TAG + " glTransformFeedbackVarings");

        GLES30.glLinkProgram(mProgram);                  // create OpenGL program executable
        MyGLRenderer.checkGlError(TAG + " glLinkProgram");

        GLES30.glUseProgram(mProgram);
        MyGLRenderer.checkGlError(TAG + " glUseProgram init");

        // Generate ping-pong buffer for transformFeedbacks
        transformBuffers = new int[2]; //for ping-ponging the information.
        GLES30.glGenBuffers(2, transformBuffers, 0);
        MyGLRenderer.checkGlError(TAG + " glGenBuffers");

        // bind ping and fill with data
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, transformBuffers[ping]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertexMaxCount*vertexStride, vertexBuffer, GLES30.GL_STATIC_DRAW);
        MyGLRenderer.checkGlError(TAG + " Setup Array buffer with data");

        // get handle to vertex shader's vPosition and link feedback in
        mPositionHandle = GLES30.glGetAttribLocation(mProgram, "vPosition");
        MyGLRenderer.checkGlError(TAG + " Enable vPosition for vertex array buffer");

        // Get uniform locations
        mColorHandle = GLES30.glGetUniformLocation(mProgram, "vColor");
        mMVPMatrixHandle = GLES30.glGetUniformLocation(mProgram, "uMVPMatrix");
        //mRadiusHandle = GLES30.glGetUniformLocation(mProgram, "radius");
        //mSizeHandle = GLES30.glGetUniformLocation(mProgram,"pointSize");
        //mNoteAmplitued = GLES30.glGetUniformLocation(mProgram,"noteAmplitude");
        //mModeHandle = GLES30.glGetUniformLocation(mProgram,"mode");
        //mCymaticSamplerHandle = GLES30.glGetUniformLocation(mProgram, "cymaticOverlay");
        MyGLRenderer.checkGlError(TAG + " get uniform locations");


        // bind pong and make space for data
        GLES30.glBindBuffer(GLES30.GL_TRANSFORM_FEEDBACK_BUFFER, transformBuffers[pong]);
        GLES30.glBufferData(GLES30.GL_TRANSFORM_FEEDBACK_BUFFER, vertexMaxCount*vertexStride, null, GLES30.GL_STATIC_READ);
        GLES30.glBindBuffer(GLES30.GL_TRANSFORM_FEEDBACK_BUFFER, 0);
        MyGLRenderer.checkGlError(TAG + " bind vertex array buffer");

        // Generate transform feedbacks
        transformFeedbackIds = new int[1];
        GLES30.glGenTransformFeedbacks(1,transformFeedbackIds,0);
        GLES30.glBindTransformFeedback(GLES30.GL_TRANSFORM_FEEDBACK, transformFeedbackIds[0]);
        MyGLRenderer.checkGlError(TAG + " gen and bind ping");

        // Bind buffer to transform feedback
        GLES30.glBindBufferBase(GLES30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, transformBuffers[pong]);
        GLES30.glBindTransformFeedback(GLES30.GL_TRANSFORM_FEEDBACK, 0);
        MyGLRenderer.checkGlError(TAG + " bind vertex array buffer");

    }

    int run = 0;

    public void draw(float[] mvpMatrix, int count, float particleSize,
                     float radius, int mMode, float mNoteAmp, float opacity) {

        // Add program to OpenGL environment
        GLES30.glUseProgram(mProgram);
        MyGLRenderer.checkGlError(TAG + " glUseProgram");        // unbind current


        ++run;

        // Fill ping with data
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, transformBuffers[ping]);
        GLES30.glEnableVertexAttribArray(mPositionHandle);
        GLES30.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES30.GL_FLOAT, false,
                vertexStride, 0);
        MyGLRenderer.checkGlError(TAG + " glBuffer bind Ping for render");


        // set uniform values
        mColor[3] = opacity;
        GLES30.glUniform4fv(mColorHandle, 1, mColor, 0);
        GLES30.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        //GLES30.glUniform1f(mRadiusHandle, radius);
        //GLES30.glUniform1f(mSizeHandle, particleSize);
        //GLES30.glUniform1f(mNoteAmplitued, mNoteAmp);
        //GLES30.glUniform1i(mModeHandle, mMode);
        MyGLRenderer.checkGlError(TAG + " set uniforms");

        // Setup feedback buffer for output capture
        GLES30.glBindBuffer(GLES30.GL_TRANSFORM_FEEDBACK_BUFFER, transformBuffers[pong]);
        GLES30.glBindBufferBase(GLES30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, transformBuffers[pong]);
        MyGLRenderer.checkGlError(TAG + " glBindBufferBase");

        // Draw the triangle
        GLES30.glBeginTransformFeedback(GLES30.GL_POINTS);
        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, count);
        GLES30.glEndTransformFeedback();

        // Disable vertex array
        GLES30.glDisableVertexAttribArray(mPositionHandle);
        GLES30.glBindBufferBase(GLES30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, 0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,0);
        GLES30.glBindBuffer(GLES30.GL_TRANSFORM_FEEDBACK_BUFFER, 0);


        // swap ping and pong for next run
        int val = ping;
        ping = pong;
        pong = val;//*/

    }

    private float[] genParticleVBO(){
        float[] particleVBO = new float[vertexMaxCount*COORDS_PER_VERTEX];
        for (int i = 0; i < vertexMaxCount; i++) {
            float randAngle = (float) (2.0*Math.PI*Math.random());
            float randDist = (float) (Math.random());
            particleVBO[COORDS_PER_VERTEX*i] = (float) (randDist*Math.cos(randAngle));
            particleVBO[COORDS_PER_VERTEX*i+1] = (float) (randDist*Math.sin(randAngle));
        }
        return particleVBO;
    }
}
