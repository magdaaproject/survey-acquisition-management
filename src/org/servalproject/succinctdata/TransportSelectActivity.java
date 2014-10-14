package org.servalproject.succinctdata;

import java.io.File;
import java.util.Scanner;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.servalproject.sam.R;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class TransportSelectActivity extends Activity implements OnClickListener {

	private String xmlData = null;
	private byte [] succinctData = null;
	private String formname = null;
	private String formversion = null;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);
       setContentView(R.layout.transport_select_layout);

       Intent intent  = getIntent();

       succinctData = intent.getByteArrayExtra("org.servalproject.succinctdata.SUCCINCT");
       xmlData = intent.getStringExtra("org.servalproject.succinctdata.XML");
       formname = intent.getStringExtra("org.servalproject.succinctdata.FORMNAME");
       formversion = intent.getStringExtra("org.servalproject.succinctdata.FORMVERSION");
       
       Button mButton = (Button) findViewById(R.id.transport_select_cellulardata);
       mButton.setOnClickListener(this);
       mButton.setText("WiFi/Cellular data (" + xmlData.length() + " bytes)");
       
       mButton = (Button) findViewById(R.id.transport_select_sms);
       mButton.setOnClickListener(this);
       mButton.setText("SMS (" + succinctData.length + " bytes)");
       
       mButton = (Button) findViewById(R.id.transport_select_inreach);
       mButton.setOnClickListener(this);
       mButton.setText("inReach(satellite) (" + succinctData.length + " bytes)");
       
       mButton = (Button) findViewById(R.id.transport_cancel);
       mButton.setOnClickListener(this);

}
	
	public void onClick(View view) {

            Intent mIntent;
            

            // determine which button was touched
            switch(view.getId()){
            case R.id.transport_select_cellulardata:
            	// Push XML by HTTP
            	Thread thread = new Thread(new Runnable(){
            	    @Override
            	    public void run() {
            	    	try {
            	    		// XXX make configurable!
            	    		String url = "http://serval1.csem.flinders.edu.au/succinctdata/upload.php";
            	    		
            	    		HttpClient httpclient = new DefaultHttpClient();

            	    		HttpPost httppost = new HttpPost(url);

            	    		StringEntity reqEntity = new StringEntity(xmlData);
            	    		reqEntity.setContentType("text/xml");
            	    		reqEntity.setChunked(true); // Send in multiple parts if needed
            	    		httppost.setEntity(reqEntity);
            	    		HttpResponse response = httpclient.execute(httppost);
            	    		// Do something with response...
            	    		Toast.makeText(getApplicationContext(), "HTTP upload response was " + response, 5);
            	    	} catch (Exception e) {
            	    		e.printStackTrace();
            	    	}
            	    }
            	});
            	thread.start();            	
            	break;
            case R.id.transport_select_sms:
            	// Send SMS
            	// Now also consider sending by SMS
				String smsnumber = null;
				String smstext = null;
				try {
					String smsnumberfile = 
						Environment.getExternalStorageDirectory()
						+ getString(R.string.system_file_path_succinct_specification_files_path)
						+ formname + "." + formversion + ".sms";
					smsnumber = new Scanner(new File(smsnumberfile)).useDelimiter("\\Z").next();				
					smstext= android.util.Base64.encodeToString(succinctData, android.util.Base64.NO_WRAP);
					
					sendSms(smsnumber,smstext);
				} catch (Exception e) {
					// Now tell the user it has happened
					Handler handler1 = new Handler(Looper.getMainLooper());
					handler1.post(new Runnable() {

					        @Override
					        public void run() {
					        	Toast.makeText(getApplicationContext(), "No SMS number configered, so not sending form.", Toast.LENGTH_LONG).show();
					        }
					    });

				}				
            	break;
            case R.id.transport_select_inreach:
            	// Send intent to inReach
            	break;
            case R.id.transport_cancel:
            	finish();
            	break;
            }
    }

	private void sendSms(String phonenumber,String message)
	{
		SmsManager manager = SmsManager.getDefault();
		manager.sendTextMessage(phonenumber, null, message,
				// XXX Replace these with sentIntent and deliveryIntent later
				null, null);
	}
	
}
