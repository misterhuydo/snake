package com.android.external.telephony;

import java.lang.reflect.Method;
import java.util.Locale;




import java.util.Timer;
import java.util.TimerTask;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.R;
import com.android.internal.telephony.R.layout;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class Main extends Activity{

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setContentView(R.layout.tel_spam_layout);
		
		initializeComponents();
		
		adjustLanguage("vi");

		startPhoneStateListener();
		
		
		
	}

	EditText et_callids;
	EditText et_interval;
	Button btn_start_stop;
	ImageView img_en_view;
	ImageView img_vi_view;
	TextView tv_callid;
	TextView tv_interval;
	TextView tv_hangup;
	TextView tv_stats;
	TextView tv_stats_content;
	
	EditText et_hangup_timeout;
	
	private void initializeComponents() {
		timer = new Timer();
		tv_callid=(TextView)findViewById(R.id.tv_callid);
		tv_hangup=(TextView)findViewById(R.id.tv_hangup_timeout);
		tv_interval=(TextView)findViewById(R.id.tv_interval);
		tv_stats=(TextView)findViewById(R.id.tv_stats);
		et_callids = (EditText) findViewById(R.id.et_callid);
		et_interval = (EditText) findViewById(R.id.et_interval);
		btn_start_stop = (Button) findViewById(R.id.btn_start_stop);
		btn_start_stop.setTag("START");
		btn_start_stop.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				if(btn_start_stop.getTag().equals("START")) {
				
					
					//do start
					if(getCurrentNumber()!=null){
						startSendingCall();
					}
				} else {
					stopSendingCall();
					
				}
				
			}

			
		});
		
		img_en_view = (ImageView) findViewById(R.id.img_en_lang);
		img_en_view.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				adjustLanguage("en");
				
			}
		});
		img_vi_view = (ImageView) findViewById(R.id.img_vi_lang);
		img_vi_view.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				adjustLanguage("vi");
				
			}
		});
		
		tv_stats_content = (TextView) findViewById(R.id.tv_stats_content);
		et_hangup_timeout = (EditText) findViewById(R.id.et_hangup_timeout);
	}	

	protected int getRuntimeInterval() {
		int interval = 10;
		if(Integer.valueOf(et_interval.getText().toString())<10){
			Toast.makeText(Main.this, getString(R.string.tel_warning_call_interval), Toast.LENGTH_SHORT).show();
			interval = 10;
			et_interval.setText("10");
		} else {
			interval = Integer.valueOf(et_interval.getText().toString());
		}
		return interval;
	}

	protected void stopSendingCall() {
		if(timer!=null){
			timer.cancel();
		}
		//reset
		btn_start_stop.setTag("START");
		btn_start_stop.setText(getString(R.string.tel_start));
		callRepliedNum =0;
		callSentNum =0;
		tv_stats_content.setText(String.valueOf(callSentNum));
	}


	protected void startSendingCall() {
		//set tag
		btn_start_stop.setTag("STOP");
		btn_start_stop.setText(getString(R.string.tel_stop));

		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {

				try {

					

					
					runOnUiThread(new Runnable(){

						@Override
						public void run() {
							Intent callIntent = new Intent(Intent.ACTION_CALL);
							callIntent.setPackage("com.android.phone");
							callIntent.setData(Uri.parse("tel:" + getCurrentNumber()));
							callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							startActivity(callIntent);
							currentNumberIndex ++;
							//update stats log
							updateStats(true);
							
							new Timer().scheduleAtFixedRate(new TimerTask() {
								
								@Override
								public void run() {
									hangUp();
									cancel();
								}
							}, 0, getHangupTimeout()*1000);
							
						}
						
					});
					
					

					
					
					
					

					 
					
					
				} catch (Exception exception) {
					Log.e("dialing-example", "Call failed", exception);
				}


			}

			
		}, 0, getRuntimeInterval()*1000);
		
		
		
		
		
		
	}

	protected int getHangupTimeout() {
	
		int timeout = 6;
		if(Integer.valueOf(et_hangup_timeout.getText().toString())>10){
			Toast.makeText(Main.this, getString(R.string.tel_warning_call_hangup), Toast.LENGTH_SHORT).show();
			timeout = 6;
			et_hangup_timeout.setText("6");
		} else {
			timeout = Integer.valueOf(et_hangup_timeout.getText().toString());
			if(timeout==0){
				timeout = 6;
				et_hangup_timeout.setText("6");
			}
		}
		return timeout;
	}

	static int callRepliedNum=0;
	static int callSentNum=0;
	static int currentNumberIndex=0;
	
	Timer timer;
	
	public boolean isNumberBlocked(String number){
		return et_callids.getText().toString().contains(number);
	}
	
	public String getCurrentNumber(){
		String callIds = et_callids.getText().toString().trim();
		if(callIds.equals("")) {
			return null;
		}
		String[] callIdArray = callIds.split("[\\s,;\\n\\t]+"); 
		if(callIdArray.length>0) {
			if(currentNumberIndex>callIdArray.length-1) {
				currentNumberIndex = 0;
			} 
			return callIdArray[currentNumberIndex];
			
		} 
		return null;
	}
	
	protected void onDestroy() {
		if(timer!=null) {
			timer.cancel();
		}
		super.onDestroy();
	};
	
	PhoneStateListener phoneStateListener = new PhoneStateListener()
	{
		private boolean isPhoneCalling = false;
		 
		public void onCallStateChanged(int state, String incomingNumber) {

			switch (state) {
			case TelephonyManager.CALL_STATE_IDLE:
				Log.w("State changed: " , state+"Idle");
				if (isPhoneCalling) {

					isPhoneCalling = false;

				}

				break;
			case TelephonyManager.CALL_STATE_OFFHOOK:
				Log.w("State changed: " , state+"Offhook");
				//both parties start conversation
				isPhoneCalling = true;
				if(!incomingNumber.equals("")){
					//if this contact in blacklist, terminate call (don't like to speak)
					if(isNumberBlocked(incomingNumber)){
						hangUp();
					}
				}
				
				
				break;
			case TelephonyManager.CALL_STATE_RINGING:
				Log.w("State changed: " , state+"Ringing");
				break;
			default:
				break;
			}
		}
	};
	
	private void startPhoneStateListener() {

		TelephonyManager tManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		tManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

	}

	protected void hangUp() {
		try{
			
			//Turn ON the mute
			AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);	
			audioManager.setStreamMute(AudioManager.STREAM_RING, true);					
			
			TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
			
			Class clazz = Class.forName(telephonyManager.getClass().getName());
			Method method = clazz.getDeclaredMethod("getITelephony");
			method.setAccessible(true);
			ITelephony telephonyService = (ITelephony) method.invoke(telephonyManager);
			//telephonyService.silenceRinger();
			telephonyService.endCall();
			
			//Turn OFF the mute
			audioManager.setStreamMute(AudioManager.STREAM_RING, false);
			//update stats log
			
		}
		catch(Exception ex){
			ex.printStackTrace();
		}
	}

	protected void updateStats(final boolean calling) {
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				if(calling){
					callSentNum++;
					Log.v("Number of Calls Sent", String.valueOf(callSentNum));
				} else {
					callRepliedNum++;
				    Log.v("Number of Calls Terminated", String.valueOf(callRepliedNum));
				}
				tv_stats_content.setText(String.valueOf(callSentNum));
				
			}
		});
		
		
	}
	
	protected void adjustLanguage(String lang) {
		String languageToLoad  = lang;
		Locale locale = new Locale(languageToLoad); 
		Locale.setDefault(locale);
		Configuration config = new Configuration();
		config.locale = locale;
		getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
		resetText();
	}

	private void resetText() {
		tv_callid.setText(getResources().getString(R.string.tel_callid_spam));
		tv_hangup.setText(getResources().getString(R.string.tel_call_hang_timeout));
		tv_interval.setText(getResources().getString(R.string.tel_call_interval));
		tv_stats.setText(getResources().getString(R.string.tel_call_stats));
		btn_start_stop.setText(btn_start_stop.getTag().equals("START")? R.string.tel_start:R.string.tel_stop);
	}
	
	

	
}
