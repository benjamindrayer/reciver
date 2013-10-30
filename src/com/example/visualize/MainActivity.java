package com.example.visualize;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.AlertDialog;

import android.graphics.Color;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
//CSV writer

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
// Bluetooth stuff
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;

import java.util.Stack;

public class MainActivity extends Activity{
	private GLSurfaceView myGLview;
	curveRenderer myRenderer;
	LinearLayout myMasterLayout;
	LinearLayout myButtonLayout;
	float time_sample=0;
	
	TextView myTextField;
	
	Thread myDemoThread = null;
	Thread myBlueThread = null;
	boolean demoOn=false;
	
	byte[] bufferIn = new byte[100];
	byte[] bufferIn_temp = new byte[10];
	int bytesIn=0;
	int bytesIn_tmp=0;
	String strTempIn;
	String strTempIn2;
	String strBufferIn;
	int currentLast=-1;
	int myTimeOld=0;
	int myTimeNew=0;
	int fileNumber=0;
	Stack< Long > timeStack;
	Stack< Long > timeDeltaStack;
	Stack< Long > valueStack;
	
	private BluetoothAdapter mBluetoothAdapter = null;	
	private BluetoothDevice  mBluetoothDevice = null;
	protected BluetoothSocket mySocket;
	private InputStream MyInStream;
	private OutputStream MyOutStream;

	private static final int REQUEST_ENABLE_BT = 2;
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	
	AlertDialog.Builder builder;
	int DEV=-1;
	String[] addresses;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        myGLview = new GLSurfaceView(this);
        myRenderer=new curveRenderer(this);
        myGLview.setRenderer(myRenderer);
    	System.out.println("GL View created");
    	//Control buttons
        myButtonLayout = new LinearLayout(this);
        myButtonLayout.setOrientation(LinearLayout.HORIZONTAL);
        myButtonLayout.setBackgroundColor(Color.BLUE);

        Button buttonConnect = new Button(this); 
        buttonConnect.setText("Connect"); 
        buttonConnect.setOnClickListener(handleConnect);

        Button buttonSample = new Button(this); 
        buttonSample.setText("Sample"); 
        buttonSample.setOnClickListener(handleSample);

        Button buttonDemoSample = new Button(this); 
        buttonDemoSample.setText("DemoSample"); 
        buttonDemoSample.setOnClickListener(handleDemoSample);
        
        Button buttonSave = new Button(this); 
        buttonSave.setText("Save"); 
        buttonSave.setOnClickListener(handleSave);

        myButtonLayout.addView(buttonConnect);
        myButtonLayout.addView(buttonSample);
        myButtonLayout.addView(buttonDemoSample);
        myButtonLayout.addView(buttonSave);
        
        //textfield
        myTextField=new TextView(this);
        myTextField.setText("Bluetooth Status:\n");
        myMasterLayout = new LinearLayout(this);
        myMasterLayout.setOrientation(LinearLayout.VERTICAL);
        myMasterLayout.setBackgroundColor(Color.BLUE);
        myMasterLayout.addView(myGLview, LayoutParams.FILL_PARENT, 300);
        myMasterLayout.addView(myButtonLayout);
        myMasterLayout.addView(myTextField);
        
        setContentView(myMasterLayout);

        timeStack=new Stack<Long>();
        timeDeltaStack=new Stack<Long>();      
        valueStack=new Stack<Long>();
        // Set up the bluetooth device
        
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();	
        if(mBluetoothAdapter == null)
        {
            System.out.println("Bluetooth Adapter not available");
            myTextField.append("Adapter: null");
        }
        else{
            System.out.println("Adapter: "+mBluetoothAdapter.toString());
            myTextField.append("Adapter: "+mBluetoothAdapter.toString()+"\n");       	   	
        }
        
  	  final CharSequence[] items = {"Red", "Green", "Blue"};
	  
  	  builder = new AlertDialog.Builder(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        myTextField.setText("Bluetooth Status:\n");
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();	
        if(mBluetoothAdapter == null)
        {
            System.out.println("Bluetooth Adapter not available");
            myTextField.append("Adapter: null\n");
        }
        //Bluetooth adapter availabel
        else{
            System.out.println("Adapter: "+mBluetoothAdapter.toString());
            myTextField.append("Adapter: "+mBluetoothAdapter.toString()+"\n");       	
            //Turn on Bluetooth
            if (!mBluetoothAdapter.isEnabled()) 
            {
            	myTextField.append("Bluetooth is being activated"); 
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }     
            //Bluetooth should be turned on by now
        }
        
        
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    View.OnClickListener handleSample = new View.OnClickListener() {
        public void onClick(View v) {
          // it was the 1st button
        	for(int i=0;i<10;++i){
        		time_sample+=0.05f;
            	myRenderer.addValue((float) Math.random(), time_sample);     
       			messageHandler.sendMessage(Message.obtain(messageHandler, 4));

        	}
        	System.out.println("Test click listener");
        }
      };
      
      /**********************************************************/
      /*            Saving Data
       * 
       */
      
      View.OnClickListener handleSave = new View.OnClickListener() {
          public void onClick(View v) {
            // it was the 1st button
          	System.out.println("Saving Data");
          	String fileName="sdcard/log_"+String.valueOf(fileNumber)+".file";
          	fileNumber++;
          	File logFile = new File(fileName);
            if (!logFile.exists())
            {
            //1. create file
               try
               {
                  logFile.createNewFile();
               } 
               catch (IOException e)
               {
                  // TODO Auto-generated catch block
                  e.printStackTrace();
               }
               }
               //2. write text
               BufferedWriter buf;
			try {
				buf = new BufferedWriter(new FileWriter(logFile, false));
				//Saving in format time#value
				while(!timeStack.isEmpty()){
					String st=timeStack.pop().toString();
					String sv=valueStack.pop().toString();
					buf.append(st);
					buf.append('#'); 
					buf.append(sv);
                    buf.newLine();					
				}
			//	timeStack.push((long) 1);
			//	valueStack.push((long) 2);
				
               buf.close();
            	System.out.println("Saving Data - success");

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
             	System.out.println("Saving Data - failure");

			} 
            
          }
        };

      /*
       * Connect to Bluetooth device 
       */
      
      View.OnClickListener handleConnect = new View.OnClickListener() {
          public void onClick(View v) {
            // it was the connect button
        	  
        	  
        	  //Check if bluetooth is on
        	  if (mBluetoothAdapter.isEnabled()) 
              {
          		mBluetoothAdapter.startDiscovery();         		
          	    Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
          	    
          	    for (BluetoothDevice device : devices) myTextField.append("\nFound device: " + device);  
          	    myTextField.append("\nConnecting");  
          	    
          	    if(devices.size()>0){
          	    	DEV=-1;
          	    	CharSequence[] items=new CharSequence[devices.size()];
          	    	addresses=new String[devices.size()];
          	    	int i=0;
              	    for (BluetoothDevice device : devices){
              	    	items[i]=device.getName();
              	    	addresses[i]=device.getAddress();
              	    	i++;
//              	    	myTextField.append("\nFound device: " + device);  
              	    }
              	    builder.setTitle("Bound Devices:")
       	           .setItems(items, new DialogInterface.OnClickListener() {
       	               public void onClick(DialogInterface dialog, int which) {
       	               // The 'which' argument contains the index position
       	               // of the selected item
       	            	   DEV=which;
       	            	   myTextField.append(which+"\n");
       	              	 mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(addresses[DEV]);
       	            	 myTextField.append(mBluetoothDevice.toString() + "  " + mBluetoothDevice.getName()+ "\n");
       	            	BluetoothSocket tmp = null;
                        

                        try {
                            tmp = mBluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
                        } catch (IOException e) {
                            //Log.e(TAG, "CONNECTION IN THREAD DIDNT WORK");
                       	 myTextField.append("CONNECTION IN THREAD DIDNT WORK");
                        }
                        mySocket = tmp;
                        
                        try {
           				mySocket.connect();
           			} catch (IOException e) {
           				// TODO Auto-generated catch block
           				//e.printStackTrace();
           				myTextField.append("CONNECTION IN THREAD DIDNT WORK 2");
           			}   
           			try {
       					MyInStream = mySocket.getInputStream();
       				} catch (IOException e) {
       					// TODO Auto-generated catch block
       					e.printStackTrace();
       				}
           			
           			if(myBlueThread==null){
           				myBlueThread = new Thread() 
  	         	      {
  	         	          public void run() 
  	         	          {
  	         	              while (true) 
  	         	              {
  	         	            	try {
         	            	    	bytesIn=MyInStream.read(bufferIn);
         	            	    	strTempIn =  new String(bufferIn,0,bytesIn); 
         	            	    	strBufferIn +=  strTempIn;
									//bytesIn = MyInStream.available();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
           	            		//if ((bytesIn > 0 ) ) 	
           	            		//{
  	         	            	if(currentLast<0)
  	         	            		currentLast=0;
           	            			messageHandler.sendMessage(Message.obtain(messageHandler, 3));
  	         	            	  
  	         	              }
  	         	          };
  	         	      };
           				myBlueThread.start();
           			}
           			
           			
       	           }
       	               // Generate thread to recive data from bluetooth
       	               
		       	    }
       	           );
		       	    builder.create();
		       	    builder.show();

          	    }


          	  
        	  
            }
          }
        };

       /*
        * Message Handler
        *
        */
        
        // Instantiating the Handler associated with the main thread.
        private Handler messageHandler = new Handler() {

            @Override
            public void handleMessage(Message msg) { 
            	        	
               switch(msg.what) {
                    //handle update
                    //.....
               	
                case 1:    
 //               	myTextField.append("Test "+ strBufferIn+"\n");
//                	time_sample+=0.05f;
//                	myRenderer.addValue((float) Math.random(), time_sample);      
//               	myTextField.setText(strBufferIn);
                	//seperate the strings into time and value
                	int lastD=strBufferIn.lastIndexOf('$');
                	int secLastD=strBufferIn.lastIndexOf('$', lastD-1);
                	int thirdLastD=strBufferIn.lastIndexOf('$', secLastD-1);
                	if(lastD > -1 && secLastD > -1 && thirdLastD >-1){
                		String string1=strBufferIn.substring(thirdLastD+1, secLastD);
                		String string2=strBufferIn.substring(secLastD+1, lastD);
                		String number1=strBufferIn.substring(thirdLastD+2, secLastD);
                		String number2=strBufferIn.substring(secLastD+2, lastD);
                		int myTime=0, myVal=0;
                		if (strBufferIn.charAt(thirdLastD+1)=='T'){
                			myTime=Integer.valueOf(number1);
                		}
                		if (strBufferIn.charAt(thirdLastD+1)=='V'){
                			myVal=Integer.valueOf(number1);
                		}
                		if (strBufferIn.charAt(secLastD+1)=='T'){
                			myTime=Integer.valueOf(number2);
                		}
                		if (strBufferIn.charAt(secLastD+1)=='V'){
                			myVal=Integer.valueOf(number2);
                		}
                		myTextField.setText("Time:" + myTime+"\n");
                		myTextField.append("mV:" + myVal+"\n");
                		float Rtime=(float)myTime/200f;
                		float Rval=(float)myVal/20f-3.0f;
                		time_sample+=0.02f;
                		myRenderer.addValue(Rval, Rtime);    
                	}
                	break;
                case 2:
                	 //               	myTextField.append("Test "+ strBufferIn+"\n");
//                	time_sample+=0.05f;
//                	myRenderer.addValue((float) Math.random(), time_sample);      
//               	myTextField.setText(strBufferIn);
                	//seperate the strings into time and value
                	int myFirstD=strBufferIn.length(), mySecD, myThirdD=strBufferIn.length();
                	while(myThirdD>currentLast){
                		myFirstD=strBufferIn.lastIndexOf('$',myFirstD-1);
                		mySecD=strBufferIn.lastIndexOf('$',myFirstD-1);
                		myThirdD=strBufferIn.lastIndexOf('$',mySecD-1);
                    	if(myFirstD > -1 && mySecD > -1 && myThirdD >-1 && myThirdD>currentLast){
                    		String string1=strBufferIn.substring(myThirdD+1, mySecD);
                    		String string2=strBufferIn.substring(mySecD+1, myFirstD);
                    		String number1=strBufferIn.substring(myThirdD+2, mySecD);
                    		String number2=strBufferIn.substring(mySecD+2, myFirstD);
                    		int myTime=0, myVal=0;
                    		if (strBufferIn.charAt(myThirdD+1)=='T'){
                    			myTime=Integer.valueOf(number1);
                    		}
                    		if (strBufferIn.charAt(myThirdD+1)=='V'){
                    			myVal=Integer.valueOf(number1);
                    		}
                    		if (strBufferIn.charAt(mySecD+1)=='T'){
                    			myTime=Integer.valueOf(number2);
                    		}
                    		if (strBufferIn.charAt(mySecD+1)=='V'){
                    			myVal=Integer.valueOf(number2);
                    		}
                    		myTextField.setText("Time:" + myTime+"\n");
                    		myTextField.append("mV:" + myVal+"\n");
                    		float Rtime=(float)myTime/200f;
                    		float Rval=(float)myVal/50f-2.5f;
                    		time_sample+=0.01f;
                    		myRenderer.addValue(Rval, time_sample);    
                    		
                    	}                		
                	}
                	currentLast=strBufferIn.length()-1;
                break; 

                case 3:
               	 //               	myTextField.append("Test "+ strBufferIn+"\n");
//               	time_sample+=0.05f;
//               	myRenderer.addValue((float) Math.random(), time_sample);      
//              	myTextField.setText(strBufferIn);
               	//seperate the strings into time and value
               	int myPos1=currentLast, myPos2=myPos1+1, myPos3=currentLast;
               	int iter=0;
               	while(myPos3+1<strBufferIn.length() && myPos3>-1 && myPos1<myPos2 && myPos2>-1){
               		iter++;
              // 		if(myPos1>1) myPos1--;
              // 		else myPos1=0;
               		myPos1=strBufferIn.indexOf('$', max(myPos1,0));
               		myPos2=strBufferIn.indexOf('$', max(myPos1+1,0));
               		myPos3=strBufferIn.indexOf('$', max(myPos2+1,0));
               		System.out.println(myPos1 + " "+ myPos2+ " "+myPos3+" "+strBufferIn.length());
                   	if(myPos1 > -1 && myPos2 > -1 && myPos3 >-1 && myPos3<strBufferIn.length()){
                   		String string1=strBufferIn.substring(myPos1+1, myPos2);
                   		String string2=strBufferIn.substring(myPos2+1, myPos3);
                   		String number1=strBufferIn.substring(myPos1+2, myPos2);
                   		String number2=strBufferIn.substring(myPos2+2, myPos3);
                   		int myTime=0, myVal=0;
                   		if (strBufferIn.charAt(myPos1+1)=='T'){
                   			myTime=Integer.valueOf(number1);
                   		}
                   		if (strBufferIn.charAt(myPos1+1)=='V'){
                   			myVal=Integer.valueOf(number1);
                   		}
                   		if (strBufferIn.charAt(myPos2+1)=='T'){
                   			myTime=Integer.valueOf(number2);
                   		}
                   		if (strBufferIn.charAt(myPos2+1)=='V'){
                   			myVal=Integer.valueOf(number2);
                   		}
                   		myTextField.setText("Time:" + myTime+"\n");
                   		myTextField.append("mV:" + myVal+"\n");
                   		myTimeNew=myTime;
                   		int diff=myTimeNew-myTimeOld;
                   		myTimeOld=myTimeNew;
                   		myTextField.append("Diff(t): "+diff+"\n");
                   		System.out.println("Diff(t): "+diff);

                   		myTextField.append("Length(m): "+bytesIn+"\n");
                   		myTextField.append("Length(b): "+strBufferIn.length()+"\n");

                   		System.out.println("Length(m): "+bytesIn);

                   		float Rtime=(float)myTime/200f;
                   		float Rval=(float)myVal/900f-0.8f;
                   		time_sample+=0.02f;
                   		myRenderer.addValue(Rval, time_sample);    
//                       	currentLast=strBufferIn.length()-1;
                       	currentLast=myPos3;
                       	
                       	timeStack.push((long) myTime);
                       	valueStack.push((long) myVal);
                       	myPos1=myPos3;
                   	}        
               		myTextField.append("Iterations: "+iter+"\n");
               		System.out.println("Iterations: "+iter);

               	}
               	if(strBufferIn.length()>10000){
               		strBufferIn="";
               		currentLast=0;               		
              	}
               break; 
               
               //Case 4 is just for simulating input
                case 4:
               		//current time
               		Long ct= System.currentTimeMillis();
               		/*
               		if(timeDeltaStack.isEmpty()){
               			timeDeltaStack.push((long) 0);
               		}
               		else{
               			Long delta=ct-timeStack.peek();
               			System.out.println("Delta="+delta+"ms");
               			timeDeltaStack.push(ct-timeStack.peek());
               		}
               		*/
               		timeStack.push(ct);
               		//current value
               		Long cv= System.currentTimeMillis();
               		valueStack.push(cv);
             		myTextField.setText("Time:" + ct +"\n");
               		myTextField.append("mV:" + cv+"\n");

                }
            }

			private int max(int i, int j) {
				// TODO Auto-generated method stub
				if (i>j) return i;
				else return j;
			}

        };     
      /*
       * Demo function for plotting, just plots a sinus curve
       * uses thread to realize stuff
       */
      
      View.OnClickListener handleDemoSample = new View.OnClickListener() {
          public void onClick(View v) {
        	  if(demoOn) demoOn=false;
        	  else demoOn=true;
        	  // it was the demo button
       		if (myDemoThread==null)
       		{
         	      myDemoThread = new Thread() 
         	      {
         	          public void run() 
         	          {
         	              while (true) 
         	              {
         	            	  try {
								Thread.sleep(10);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
         	            	  if(demoOn){
  								time_sample+=0.05f;
				            	myRenderer.addValue((float) Math.sin(time_sample*2), time_sample);            	            		  
         	            	  }
         	              }
         	          };
         	      };
             		myDemoThread.start();     
       		}
       		System.out.println("Test click listener");
          }
        };
}
