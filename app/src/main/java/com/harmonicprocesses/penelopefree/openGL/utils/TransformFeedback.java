package com.harmonicprocesses.penelopefree.openGL.utils;
import com.harmonicprocesses.penelopefree.openGL.MyGLRenderer;


import android.opengl.GLES30;
import android.util.Log;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by izzy on 6/24/15. TransformFeedback is a example of how
 *  to transform feedbac in OpenGLES 3.0 following the example found
 *  at https://open.gl/feedback.
 *
 * Assumes you have a working instance of GLSurfaceVew. Remember to call
 *  setEGLContextClientVersion(3) to set the OpenGLES context to API 3.0
 *  and have the appropriate OpenGLES specified in the Android manifest file.
 */
public class TransformFeedback {
    private final String TAG = "TransformFeedback";


// Vertex shader
    private final String vertexShaderSrc =
            "#version 300 es \n" +
            "in float inValue;\n" +
            "out float outValue;\n" +

            "void main() {\n" +
            "    outValue = sqrt(inValue);\n" +
            "}";

// Need a fragmentShader or glLinkProgram will throw
    private final String fragmentShaderCode =
            "#version 300 es \n" +
            "precision mediump float;\n" +
            "out vec4 fragColor;\n" +
            "void main() {\n" +
            "  fragColor = vec4(1.0,1.0,1.0,1.0);\n" +
            "}";

    private final int mProgram;

    /**
     * The TransformFeedback constructor contains all the code to initialize
     *  and draw the TransformFeedback, so create an instance of TransformFeedback
     *  i.e. 'new TransformFeedBack()' in your 'GLRenderer.OnDrawFrame()' method.
     */
    public TransformFeedback(){

        // Compile shaders
        int vertexShader = MyGLRenderer.loadShader(GLES30.GL_VERTEX_SHADER,
                vertexShaderSrc);
        int fragmentShader =  MyGLRenderer.loadShader(GLES30.GL_FRAGMENT_SHADER,
                fragmentShaderCode);

        // Create program and attach shaders
        mProgram = GLES30.glCreateProgram();
        GLES30.glAttachShader(mProgram, vertexShader);
        GLES30.glAttachShader(mProgram, fragmentShader);

        // Tell GL where the feedbackVaryings are in the shader before Linking
        //  the shaders together to form a program.
        final String[] feedbackVaryings = { "outValue" };
        GLES30.glTransformFeedbackVaryings(mProgram, feedbackVaryings, GLES30.GL_INTERLEAVED_ATTRIBS);
        MyGLRenderer.checkGlError(TAG + " glTransformFeedbackVaryings");

        // Link program and look for errors
        GLES30.glLinkProgram(mProgram);

        int[] linkSuccessful = new int[1];
        GLES30.glGetProgramiv(mProgram, GLES30.GL_LINK_STATUS, linkSuccessful, 0);

        if (linkSuccessful[0] != 1){
            Log.d(TAG, "glLinkProgram failed");
        }
        MyGLRenderer.checkGlError(TAG + " glLinkProgram");

        /***********
         * Begin Rendering process
         *  everything before this can be moved to an initialization stage
         ***********/

        // Bring the program into use
        GLES30.glUseProgram(mProgram);
        MyGLRenderer.checkGlError(TAG + " glUseProgram");


        // Create data to fill VBO
        int bufferLength = 5 * 4; //5 floats 4 bytes each
        FloatBuffer data = ByteBuffer.allocateDirect(bufferLength)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        float[] floatData = { 1.0f, 4.0f, 9.0f, 16.0f, 25.0f };
        data.put(floatData).position(0);

        // Create VBO and fill with data
        int[] vbo = new int[1];
        GLES30.glGenBuffers(1, vbo, 0);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, bufferLength, data, GLES30.GL_STATIC_READ);
        MyGLRenderer.checkGlError(TAG + " glBufferData GL_ARRAY_BUFFER");

        // Link created VBO to shader attribute "inValue"
        int inputAttrib = GLES30.glGetAttribLocation(mProgram, "inValue");
        GLES30.glEnableVertexAttribArray(inputAttrib);
        GLES30.glVertexAttribPointer(inputAttrib, 1, GLES30.GL_FLOAT, false, 4, 0);
        MyGLRenderer.checkGlError(TAG + " glVertexAttribPointer");


        // Create transform feedback buffer object and bind to transform feedback
        //  this creates space in a buffer object for the TransformFeedback.
        int[] tbo = new int[1];
        GLES30.glGenBuffers(1, tbo, 0);
        GLES30.glBindBuffer(GLES30.GL_TRANSFORM_FEEDBACK_BUFFER, tbo[0]);
        GLES30.glBufferData(GLES30.GL_TRANSFORM_FEEDBACK_BUFFER, bufferLength, null, GLES30.GL_STATIC_READ);
        GLES30.glBindBufferBase(GLES30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, tbo[0]); //very important

        MyGLRenderer.checkGlError(TAG + " glBindBufferBase");

        // Disable Rasterizer if you just need the data
        GLES30.glEnable(GLES30.GL_RASTERIZER_DISCARD);

        // Start the TransformFeedback, Draw the Arrays, then End the Transform Feedback
        GLES30.glBeginTransformFeedback(GLES30.GL_POINTS);
        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, 5);
        MyGLRenderer.checkGlError(TAG + " glDrawArrays");
        GLES30.glEndTransformFeedback();

        // Reenable Rasterizer if you need it, which you do if you are drawing anything.
        GLES30.glDisable(GLES30.GL_RASTERIZER_DISCARD);

        // Flush out anything in GL before mapping the buffer.
        GLES30.glFlush();
        MyGLRenderer.checkGlError(TAG + " pre-glMapBufferRange ");

        // Map the transform feedback buffer to local address space.
        Buffer mappedBuffer =  GLES30.glMapBufferRange(GLES30.GL_TRANSFORM_FEEDBACK_BUFFER,
                0, bufferLength, GLES30.GL_MAP_READ_BIT);
        MyGLRenderer.checkGlError(TAG + " glMapBufferRange");

        // Read out the data, here we write to logcat.
        if (mappedBuffer!=null){
            ByteBuffer bb = ((ByteBuffer) mappedBuffer);
            bb.order(ByteOrder.nativeOrder());
            FloatBuffer transformedData = bb.asFloatBuffer();

            Log.d(TAG, String.format("output values = %f %f %f %f %f\n", transformedData.get(),
                    transformedData.get(), transformedData.get(),
                    transformedData.get(), transformedData.get()));
        }
        // Don't forget to Unmap the Transform Feeback Buffer.
        GLES30.glUnmapBuffer(GLES30.GL_TRANSFORM_FEEDBACK_BUFFER);

    }



}
