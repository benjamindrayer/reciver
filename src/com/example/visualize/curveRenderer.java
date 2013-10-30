package com.example.visualize;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.os.SystemClock;
import android.view.SurfaceHolder;

public class curveRenderer extends GLSurfaceView implements SurfaceHolder.Callback, Renderer {

	Vector<Float> myVector; 
	
	float coordValues[];
	float values[], time[], raw_values[];
	float color1[], color2[], color3[], color4[];
    int nValues;
	FloatBuffer color_buffer1, color_buffer2, color_buffer3, color_buffer4;
	FloatBuffer coord_buffer;
	int nGridPoints;
	float gridCoordinates[], gridColor[];
	FloatBuffer grid_buffer, grid_color_buffer;
	
	public curveRenderer(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		nValues=400;
		coordValues=new float[3*nValues];
		raw_values=new float[nValues];
		values=new float[nValues];
		time=new float[nValues];
		
		color1=new float[4*nValues];		color2=new float[4*nValues];
		color3=new float[4*nValues];		color4=new float[4*nValues];
		
		for(int i=0;i<nValues;++i){
			coordValues[3*i+0]=0.f; coordValues[3*i+1]=0.f; coordValues[3*i+2]=0.f;
			color1[4*i+0]=1.f;	color1[4*i+1]=1.f;	color1[4*i+2]=1.f;	color1[4*i+3]=1.f;
			color2[4*i+0]=0.2f;	color2[4*i+1]=0.7f;	color2[4*i+2]=0.9f;	color2[4*i+3]=0.2f;
			color3[4*i+0]=0.1f;	color3[4*i+1]=0.6f;	color3[4*i+2]=0.9f;	color3[4*i+3]=0.2f;
			color4[4*i+0]=0.f;	color4[4*i+1]=0.5f;	color4[4*i+2]=0.1f;	color4[4*i+3]=0.2f;
		}

      	ByteBuffer tempColorBuffer = ByteBuffer.allocateDirect(color1.length * 4);
      	tempColorBuffer.order(ByteOrder.nativeOrder());  	      	
      	color_buffer1 = tempColorBuffer.asFloatBuffer();
      	color_buffer1.put(color1);
      	color_buffer1.position(0);

      	tempColorBuffer = ByteBuffer.allocateDirect(color2.length * 4);
      	tempColorBuffer.order(ByteOrder.nativeOrder());  	      	
      	color_buffer2 = tempColorBuffer.asFloatBuffer();
      	color_buffer2.put(color2);
      	color_buffer2.position(0);

      	tempColorBuffer = ByteBuffer.allocateDirect(color3.length * 4);
      	tempColorBuffer.order(ByteOrder.nativeOrder());  	      	
      	color_buffer3 = tempColorBuffer.asFloatBuffer();
      	color_buffer3.put(color3);
      	color_buffer3.position(0);

      	tempColorBuffer = ByteBuffer.allocateDirect(color4.length * 4);
      	tempColorBuffer.order(ByteOrder.nativeOrder());  	      	
      	color_buffer4 = tempColorBuffer.asFloatBuffer();
      	color_buffer4.put(color4);
      	color_buffer4.position(0);


		myVector= new Vector<Float>();
		myVector.add(new Float(3));
		
		/*
		 * Compute the grid
		 */
		int nVerticalLines=24;
		int nHorizontalLines=18;
		nGridPoints=nVerticalLines+nHorizontalLines;
		gridCoordinates=new float[3*nGridPoints*2];
		gridColor=new float[4*nGridPoints*2];
		float maxX=0.9f;
		float minX=-0.9f;
		float maxY=0.9f;
		float minY=-0.9f;
		for(int i=0;i<nVerticalLines;++i){
			int index1=2*i;
			int index2=2*i+1;
			float t=((float)i/(float)(nVerticalLines-1));
			float x=maxX*t+(1-t)*minX;
			gridCoordinates[3*index1+0]=x;
			gridCoordinates[3*index1+1]=minY;
			gridCoordinates[3*index1+2]=0;
			gridCoordinates[3*index2+0]=x;
			gridCoordinates[3*index2+1]=maxY;
			gridCoordinates[3*index2+2]=0;
		}

		int maxInd=((nVerticalLines)*2);
		for(int i=0;i<nHorizontalLines;++i){
			int index1=2*i+maxInd;
			int index2=2*i+1+maxInd;
			float t=((float)i/(float)(nHorizontalLines-1));
			float y=maxY*t+(1-t)*minY;
			gridCoordinates[3*index1+0]=minX;
			gridCoordinates[3*index1+1]=y;
			gridCoordinates[3*index1+2]=0;
			gridCoordinates[3*index2+0]=maxX;
			gridCoordinates[3*index2+1]=y;
			gridCoordinates[3*index2+2]=0;
		}
		//grid color
		for(int i=0;i<nGridPoints*2;++i){
			gridColor[i*4+0]=0.3f;
			gridColor[i*4+1]=0.3f;
			gridColor[i*4+2]=0.3f;
			gridColor[i*4+3]=0.3f;
		}
		ByteBuffer tempGridColorBuffer = ByteBuffer.allocateDirect(gridColor.length * 4);
      	tempGridColorBuffer.order(ByteOrder.nativeOrder());  	      	
      	grid_color_buffer = tempColorBuffer.asFloatBuffer();
      	grid_color_buffer.put(gridColor);
      	grid_color_buffer.position(0);
		
	}
	
	public void addValue(float newvalue, float newtime){
		for(int i=0;i<nValues-1;++i){
			time[i]=time[i+1];
			raw_values[i]=raw_values[i+1];
			values[i]=raw_values[i+1];
		}
		
		time[nValues-1]=newtime;
		raw_values[nValues-1]=newvalue;
		values[nValues-1]=newvalue;

//		for(int i=3;i<nValues-3;++i){
//			values[i]=(raw_values[i-2]+raw_values[i-1]+raw_values[i]+raw_values[i+1]+raw_values[i+2])/5;
//		}

		//compute coordinates from time and value measurement
		float xstart=-1f;
		float xstop=0.7f;
		float mu=0f;
		for(int i=nValues-1;i>=0;--i){
			coordValues[3*i+0]=xstop+(time[i]-time[nValues-1]);
			coordValues[3*i+1]=values[i];			
			if(coordValues[3*i+1]>0.9f)
				coordValues[3*i+1]=0.9f;
			if(coordValues[3*i+1]<-0.9f)
				coordValues[3*i+1]=-0.9f;
			
			coordValues[3*i+2]=0.f;
			if(coordValues[3*i+0]<-0.9f && i<nValues-1){
				coordValues[3*i+0]=-0.9f;
				coordValues[3*i+1]=coordValues[3*(i+1)+1];				
			}
		//	mu+=coordValues[3*i+1];
		}
	/*	mu=mu/nValues;
		float cov=0f;
		for(int i=0;i<nValues;++i){
			cov+=(mu-coordValues[3*i+1])*(mu-coordValues[3*i+1]);
		}
		cov=cov/nValues;
		for(int i=0;i<nValues;++i){
			coordValues[3*i+1]=(float) ((coordValues[3*i+1]-mu)/(1f+Math.sqrt(cov)));
		}
		*/
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		// TODO Auto-generated method stub
    	gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);   
        
        /*
         * Draw a grid
         */
        ByteBuffer vbb2 = ByteBuffer.allocateDirect(nGridPoints* 2 * 3 * 4);   // (# of coordinate values * 4 bytes per float)
        vbb2.order(ByteOrder.nativeOrder());// use the device hardware's native byte order
        grid_buffer = vbb2.asFloatBuffer();  // create a floating point buffer from the ByteBuffer
        grid_buffer.put(gridCoordinates);    		// add the coordinates to the FloatBuffer
        grid_buffer.position(0);            // set the buffer to read the first coordinate
        if(grid_buffer!=null && grid_color_buffer!=null){
        	//System.out.println(" values in drwaw "+lineVB.get(10));
        	gl.glLineWidth(2f);
        	gl.glPointSize(2f);
        	gl.glColor4f(0.0f, 1.0f, 0.0f, 1.0f);
        	gl.glVertexPointer(3, GL10.GL_FLOAT, 0, grid_buffer);
        	gl.glColorPointer(4, GL10.GL_FLOAT, 0, grid_color_buffer);
        	gl.glDrawArrays(GL10.GL_LINES, 0, nGridPoints*2);     
        }
        /*
         * Draw the curve
         * 
         */
    	ByteBuffer vbb = ByteBuffer.allocateDirect(nValues * 3 * 4);   // (# of coordinate values * 4 bytes per float)
        vbb.order(ByteOrder.nativeOrder());// use the device hardware's native byte order
        coord_buffer = vbb.asFloatBuffer();  // create a floating point buffer from the ByteBuffer
        coord_buffer.put(coordValues);    		// add the coordinates to the FloatBuffer
        coord_buffer.position(0);            // set the buffer to read the first coordinate
        float scaling=0.8f;
        if(coord_buffer!=null && color_buffer4!=null){
        	//System.out.println(" values in drwaw "+lineVB.get(10));
        	gl.glLineWidth(7f*scaling);
        	gl.glPointSize(7f*scaling);
        	gl.glColor4f(0.0f, 0.5f, 1.0f, 0.2f);
        	gl.glVertexPointer(3, GL10.GL_FLOAT, 0, coord_buffer);
        	gl.glColorPointer(4, GL10.GL_FLOAT, 0, color_buffer4);
        	gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, nValues);     
        	gl.glDrawArrays(GL10.GL_POINTS, nValues-1, 1);     
        }

        if(coord_buffer!=null && color_buffer3!=null){
        	//System.out.println(" values in drwaw "+lineVB.get(10));
        	gl.glLineWidth(5.5f*scaling);
        	gl.glPointSize(5.5f*scaling);
        	gl.glColor4f(0.0f, 0.5f, 1.0f, 0.2f);
        	gl.glVertexPointer(3, GL10.GL_FLOAT, 0, coord_buffer);
        	gl.glColorPointer(4, GL10.GL_FLOAT, 0, color_buffer3);
        	gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, nValues);     
        	gl.glDrawArrays(GL10.GL_POINTS, nValues-1, 1);     
        }

        if(coord_buffer!=null && color_buffer2!=null){
        	//System.out.println(" values in drwaw "+lineVB.get(10));
        	gl.glLineWidth(4f*scaling);
        	gl.glPointSize(4f*scaling);
        	gl.glColor4f(0.0f, 0.5f, 1.0f, 0.2f);
        	gl.glVertexPointer(3, GL10.GL_FLOAT, 0, coord_buffer);
        	gl.glColorPointer(4, GL10.GL_FLOAT, 0, color_buffer2);
        	gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, nValues);     
        	gl.glDrawArrays(GL10.GL_POINTS, nValues-1, 1);     
        }

        if(coord_buffer!=null && color_buffer1!=null){
        	//System.out.println(" values in drwaw "+lineVB.get(10));
        	gl.glLineWidth(1f*scaling);
        	gl.glPointSize(4f*scaling);
        	gl.glColor4f(0.0f, 0.5f, 1.0f, 0.2f);
        	gl.glVertexPointer(3, GL10.GL_FLOAT, 0, coord_buffer);
        	gl.glColorPointer(4, GL10.GL_FLOAT, 0, color_buffer1);
        	gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, nValues);     
        	gl.glDrawArrays(GL10.GL_POINTS, nValues-1, 1);     
        }

        long curve_gl=SystemClock.elapsedRealtime();

	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		// TODO Auto-generated method stub
        gl.glViewport(0, 0, width, height);
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		// TODO Auto-generated method stub
		
	}

}
