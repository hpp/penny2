package com.harmonicprocesses.penelopefree.openGL.shapes;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.harmonicprocesses.penelopefree.R;
import com.harmonicprocesses.penelopefree.openGL.MyGLRenderer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.util.Log;

/**
 * This file demonstrates the use of Harmonic Processes Inc. HPP Texture based audio
 * processing framework, better know as Penelope.
 *
 *  "We claim for our invention a utility or otherwise how be it patentable system for
 * using particle filters to model surface transitions and resonant timbres using
 * textures or lookup tables of nodal or other acoustical areas of interest in 2d or 3d."
 * 
 *
 */
public class Particles {

    private final String TAG = "Particles";
    
    private String vertexShaderCode() {
    	return
            "#version 300 es\n" +
            "uniform mat4 uMVPMatrix;\n" +
            "uniform float pointSize;\n" +
            "uniform float radius;\n" +
            "uniform float noteAmplitude;\n" +
            "uniform sampler2D cymaticOverlay;\n" +
	        "in vec4 vPosition;\n" + //[x,y,mom.x,mom.y]
	        "vec2 pos;\n" +
	        "vec2 mom;\n" +
	        "out vec4 vPositionOut;\n"  + //line 10
            "void init();\n" +
            "vec4 getNextPosition();\n" +
            "float rand(vec2 co);\n" +
			"\n"  +
	        "void main() {\n" +
	        // the matrix must be included as a modifier of gl_Position
	        "	init();\n" +
            "	gl_PointSize = pointSize;\n" +
            "	vPositionOut = getNextPosition();\n" +
            "   gl_Position =  vec4(pos, 0.0, 1.0) * uMVPMatrix;\n" +
	        "}\n" + //line 20
	        "\n"  +
	        "vec4 getNextPosition() {\n" +
            // move particle inside the circle if out
            "  	float dist2center = sqrt(pos.x*pos.x + pos.y*pos.y);\n" +
            "   if (dist2center>radius){\n" +
	        "		float angle = atan(pos.y, pos.x);\n" +
            "       pos.x = radius*cos(angle);\n" +
            "       pos.y = radius*sin(angle);\n" +
	        "	}\n" +
            "   vec4 nodalLevel = texture(cymaticOverlay, ((pos/radius)+1.0)/2.0);\n" +
            "   float levelOfExcitement = noteAmplitude*nodalLevel.x;\n" + //line 30
            "	float angle = 2.0*3.14159265358*rand(pos.xy+mom.xy);\n" +
            "   mom.x += levelOfExcitement*cos(angle);\n" +
	        "	mom.y += levelOfExcitement*sin(angle);\n" +
            "   mom *= nodalLevel.xy;\n" +
	        "	pos.x += mom.x;\n" +
            "   pos.y += mom.y;\n" +
	        "\n" +
	        "	return vec4(pos, mom);\n" +
	        "}\n" +
	        "\n" +
	        "void init() {\n" +
	        "	pos.x = vPosition[0];\n" +
	        "	pos.y = vPosition[1];\n" +
        	"	mom.x = vPosition[2];\n" +
        	"	mom.y = vPosition[3];\n" +
        	"}\n" +
            "\n" +
            "float rand(vec2 co){\n" +
            "    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);\n" +
            "}\n";
    }
    
    private final String fragmentShaderCode =
        "#version 300 es\n" +
        "precision mediump float;\n" +
        "uniform vec4 vColor;\n" +
        "out vec4 my_FragColor;\n" +
        "void main() {\n" +
        "  my_FragColor = vColor;\n" +
        "}";

    private FloatBuffer vertexBuffer;
    private final int mProgram;
    private int mPositionHandle;
    private int mRadiusHandle, mSizeHandle, mNoteAmplitued, mModeHandle, mCymaticSamplerHandle;
    private int mColorHandle;
    private int mMVPMatrixHandle;

    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 4;

    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    // Set color with red, green, blue and alpha (opacity) values
    float[] mColor = {0.0f,1.0f,0.0f,1.0f}; // = { 0.63671875f, 0.76953125f, 0.22265625f, 1.0f };
    ByteBuffer bb;

    public static int vertexMaxCount = 100000;
    private int ping = 0, pong = 1;
    int[] transformBuffers, cymaticTextures, cymaticSamplers, mSamplerHandle;

    public Particles(Context context) {
        float[] particleVBO = genParticleVBO();

        //vertexShaderCode = ReadFile("particleVertex.glsl");

        // Put the particle position VBO into a buffer.
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
                vertexShaderCode());
        int fragmentShader = MyGLRenderer.loadShader(GLES30.GL_FRAGMENT_SHADER,
                fragmentShaderCode);
        mProgram = GLES30.glCreateProgram();             // create empty OpenGL Program
        GLES30.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES30.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        MyGLRenderer.checkGlError(TAG + " glCreateProgram / glAttachShader");

        // Link transformfeedback varyings
        String[] trasformFeedbackIds = {"vPositionOut"};
        GLES30.glTransformFeedbackVaryings(mProgram, trasformFeedbackIds, GLES30.GL_INTERLEAVED_ATTRIBS);
        MyGLRenderer.checkGlError(TAG + " glTransformFeedbackVarings");

        GLES30.glLinkProgram(mProgram);                  // create OpenGL program executable
        MyGLRenderer.checkGlError(TAG + " glLinkProgram");

        GLES30.glUseProgram(mProgram);
        MyGLRenderer.checkGlError(TAG + " glUseProgram init");

        // Get uniform locations
        mColorHandle = GLES30.glGetUniformLocation(mProgram, "vColor");
        mMVPMatrixHandle = GLES30.glGetUniformLocation(mProgram, "uMVPMatrix");
        mRadiusHandle = GLES30.glGetUniformLocation(mProgram, "radius");
        mSizeHandle = GLES30.glGetUniformLocation(mProgram,"pointSize");
        mNoteAmplitued = GLES30.glGetUniformLocation(mProgram,"noteAmplitude");
        mModeHandle = GLES30.glGetUniformLocation(mProgram,"mode");
        mCymaticSamplerHandle = GLES30.glGetUniformLocation(mProgram, "cymaticOverlay");
        MyGLRenderer.checkGlError(TAG + " get uniform locations");

        // Generate ping-pong buffer for transformFeedbacks
        transformBuffers = new int[2]; //for ping-ponging the information.
        GLES30.glGenBuffers(2, transformBuffers, 0);
        MyGLRenderer.checkGlError(TAG + " glGenBuffers");

        // Fill ping with data
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, transformBuffers[ping]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertexMaxCount*vertexStride, vertexBuffer, GLES30.GL_STATIC_DRAW);
        MyGLRenderer.checkGlError(TAG + " glBufferData Ping with data");

        // Create buffer space for pong
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, transformBuffers[pong]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertexMaxCount*vertexStride, null, GLES30.GL_STATIC_READ);
        MyGLRenderer.checkGlError(TAG + " glBufferData Ping with data");

        // unbind current
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);

        // gen cymatic textures
        cymaticTextures = new int[4]; // one texture per mode
        GLES30.glGenTextures(4, cymaticTextures, 0);

        // import cymatic texture as a bitmap and push to GL
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;   // No pre-scaling

        // Read in the resource
        Bitmap[] bitmap = new Bitmap[4];
        bitmap[0] = BitmapFactory.decodeResource(context.getResources(), R.drawable.cymatic_plate0_1, options);
        bitmap[1] = BitmapFactory.decodeResource(context.getResources(), R.drawable.cymatic_plate1_1, options);
        bitmap[2] = BitmapFactory.decodeResource(context.getResources(), R.drawable.cymatic_plate2_1, options);
        bitmap[3] = BitmapFactory.decodeResource(context.getResources(), R.drawable.cymatic_plate3_2, options);


        for (int i = 0; i < 4; i++) {
            // Bind to the texture in OpenGL
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, cymaticTextures[i]);
            //GLES30.glBindSampler(cymaticTextures[i], cymaticSamplers[i++]);
            MyGLRenderer.checkGlError(TAG + " bindTexture " + i);

            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap[i], 0);
            MyGLRenderer.checkGlError(TAG + " texImage2D " + i);

            // Set filtering
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
            MyGLRenderer.checkGlError(TAG + " texParameteri " + i);

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
            MyGLRenderer.checkGlError(TAG + " unBind Text " + i);

            // Recycle the bitmaps, since its data has been loaded into OpenGL.
            bitmap[i].recycle();
        }

        // Bind sampler to texture
        mSamplerHandle = new int[1];
        GLES30.glGenSamplers(1, mSamplerHandle, 0);

    }

    
	public void draw(float[] mvpMatrix, int count, float particleSize,
                        float radius, int mMode, float mNoteAmp, float opacity) {

    	// Add program to OpenGL environment
        GLES30.glUseProgram(mProgram);
        MyGLRenderer.checkGlError(TAG + " glUseProgram");

        // get handle to vertex shader's vPosition and link feedback
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, transformBuffers[ping]);
        mPositionHandle = GLES30.glGetAttribLocation(mProgram, "vPosition");
        GLES30.glEnableVertexAttribArray(mPositionHandle);
        GLES30.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES30.GL_FLOAT, false,
                vertexStride, 0);
        MyGLRenderer.checkGlError(TAG + " bind vertex array buffer");


        // set uniform values
        mColor[3] = opacity;
        GLES30.glUniform4fv(mColorHandle, 1, mColor, 0);
        GLES30.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GLES30.glUniform1f(mRadiusHandle, radius);
        GLES30.glUniform1f(mSizeHandle, particleSize);
        GLES30.glUniform1f(mNoteAmplitued, mNoteAmp);
        GLES30.glUniform1i(mModeHandle, mMode);
        MyGLRenderer.checkGlError(TAG + " set uniforms");

        // set up sampler for render
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, cymaticTextures[mMode-1]);
        //GLES30.glBindSampler(0, mSamplerHandle[0]);
        GLES30.glUniform1i(mCymaticSamplerHandle,0);
        MyGLRenderer.checkGlError(TAG + " glBindSampler");

        // Setup feedback buffer for output capture
        GLES30.glBindBuffer(GLES30.GL_TRANSFORM_FEEDBACK_BUFFER, transformBuffers[pong]);
        GLES30.glBindBufferBase(GLES30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, transformBuffers[pong]);
        MyGLRenderer.checkGlError(TAG + " glBindBufferBase");

        // Draw the triangle
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        GLES30.glEnable(GLES20.GL_BLEND);
        GLES30.glBeginTransformFeedback(GLES30.GL_POINTS);
        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, count);
        GLES30.glEndTransformFeedback();

        GLES30.glDisable(GLES30.GL_BLEND);
        GLES30.glDisableVertexAttribArray(mPositionHandle);
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
        MyGLRenderer.checkGlError(TAG + " unbind all");


        // swap ping and pong for next run
        int val = ping;
        ping = pong;
        pong = val;

    }

    public float[] onPostFlush(int count){

        // Map the transform feedback buffer to local address space.
        Buffer mappedBuffer =  GLES30.glMapBufferRange(GLES30.GL_TRANSFORM_FEEDBACK_BUFFER,
                0, count*vertexStride, GLES30.GL_MAP_READ_BIT);
        MyGLRenderer.checkGlError(TAG + " glMapBufferRange");

        // Read out the data, to a float array.
        float[] particleVBO = new float[vertexStride * count];

        if (mappedBuffer!=null){
            ByteBuffer bb = ((ByteBuffer) mappedBuffer);
            bb.order(ByteOrder.nativeOrder());
            FloatBuffer transformedData = bb.asFloatBuffer();

            for (int i = 0; i < count; i++) {
                particleVBO[i*COORDS_PER_VERTEX] = transformedData.get();
                particleVBO[i*COORDS_PER_VERTEX+1] = transformedData.get();
                particleVBO[i*COORDS_PER_VERTEX+2] = transformedData.get();
                particleVBO[i*COORDS_PER_VERTEX+3] = transformedData.get();
                if (particleVBO[i*COORDS_PER_VERTEX+3] > 0.0){
                    particleVBO[i*COORDS_PER_VERTEX+3] += .01;
                }
            }
        }
        // Don't forget to Unmap the Transform Feeback Buffer.
        GLES30.glUnmapBuffer(GLES30.GL_TRANSFORM_FEEDBACK_BUFFER);
        MyGLRenderer.checkGlError(TAG + " glUnmapBufferRange");


        // Disable vertex array and unbind buffers n bases, very important

        //GLES30.glBindBufferBase(GLES30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, 0);
        //GLES30.glBindBuffer(GLES30.GL_TRANSFORM_FEEDBACK_BUFFER, 0);
        //GLES30.glBindSampler(cymaticTextures[mMode-1], 0);
        MyGLRenderer.checkGlError(TAG + " unbind all");


        return particleVBO;
    }
	
	private String ReadFile(String fileName) {
    	String output = null;
    	// try opening the myfilename.txt
    	// from http://huuah.com/android-writing-and-reading-files/
    	try {
    	    // open the file for reading
    		BufferedReader buf = new BufferedReader(new FileReader(fileName));
    		
    	    //InputStream instream = openFileInput(fileName, Context.MODE_WORLD_WRITEABLE);
    	        	 
    	    // if file the available for reading
    	    if (buf.ready()) {
    	      // prepare the file for reading
    	      //InputStreamReader inputreader = new InputStreamReader(instream);
    	      //BufferedReader buffreader = new BufferedReader(inputreader);
    	                 
    	      String line = buf.readLine();
    	      
    	      // read every line of the file into the line-variable, on line at the time
    	      while(line!=null) {
    	    	// do something with the settings from the file
    	    	output+=line;
    	    	line = buf.readLine();
    	      }
    	 
    	    }
    	     
    	    // close the file again       
    	    buf.close();
    	    
    	  } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return output;
	}

    private float[] genParticleVBO() {
        float[] particleVBO = new float[vertexMaxCount * COORDS_PER_VERTEX];
        for (int i = 0; i < vertexMaxCount; i++) {
            float randAngle = (float) (2.0 * Math.PI * Math.random());
            float randDist = (float) (Math.random());
            particleVBO[COORDS_PER_VERTEX * i] = (float) (randDist * Math.cos(randAngle));
            particleVBO[COORDS_PER_VERTEX * i + 1] = (float) (randDist * Math.sin(randAngle));
        }
        return particleVBO;
    }

}