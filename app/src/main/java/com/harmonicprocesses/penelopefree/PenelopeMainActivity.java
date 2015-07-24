package com.harmonicprocesses.penelopefree;

import java.lang.ref.WeakReference;

import java.lang.reflect.Field;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.harmonicprocesses.penelopefree.audio.AudioProcessor;
import com.harmonicprocesses.penelopefree.camera.Pcamera;
import com.harmonicprocesses.penelopefree.openGL.MyGLSurfaceView;
import com.harmonicprocesses.penelopefree.openGL.MyGLSurfaceViewLegacy;
import com.harmonicprocesses.penelopefree.settings.SettingsActivity;
import com.harmonicprocesses.penelopefree.settings.SettingsFragment;
import com.harmonicprocesses.penelopefree.settings.UpSaleDialog;
import com.harmonicprocesses.penelopefree.util.SystemUiHider;
import com.harmonicprocesses.penelopefree.util.SystemUiHiderBase;
import com.hpp.billing.PurchaseDialog;
import com.hpp.billing.PurchaseManager;
import com.hpp.ui.ButtonsManager;

import android.annotation.TargetApi;
import android.app.ActionBar.OnMenuVisibilityListener;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.TextureView;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;


/**
 * Penelope Main activity contains the highest level code for Penelope
 *  Real-time audio processor. Main sections are, camera, OpenGL, and Audio
 * 
 * @see SystemUiHider
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class PenelopeMainActivity extends Activity implements TextureView.SurfaceTextureListener {
    public final String TAG = "penelopeMainActivity";
	
	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static boolean AUTO_HIDE = true;

	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

	/**
	 * If set, will toggle the system UI visibility upon interaction. Otherwise,
	 * will hide the system UI visibility upon interaction.
	 */
	private static boolean TOGGLE_ON_CLICK = true;
	

	/**
	 * If set, will toggle the OnAir/OffAir background.,
	 * will also turn on and off main play back functionality.
	 */
	private static boolean ON_AIR_MODE = false;

    /**
     * Whether the camera is on or not. The camera can be off and
     * capture still enabled.
     */
    private static boolean CAMERA_MODE = false;

	/**
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */
	private static final int HIDER_FLAGS = SystemUiHiderBase.FLAG_FULLSCREEN;

	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	private SystemUiHider mSystemUiHider;
	
	
	/**
	 * The instance of the {@link GLSurfaceView} for this activity.
	 */
	public MyGLSurfaceView mGLView;

	/**
	 * The instance of the activity bar options MenuItem
	 */
	private MenuItem mMenuItem;
	

	private SettingsFragment mSettingsFragment;

    /**
     * Extra message sent with intent to launch the Settings Menu
     */
	public static final String EXTRA_MESSAGE = "com.harmonicprocesses.penelopefree.SETTINGS_MESSAGE";
	
	/**
	 * The instance of the {@link FrameLayout} for this activity
	 */
	public FrameLayout mFragmentViewGroup, mOpenGLViewGroup;

	Pcamera mPcamera;
	public SharedPreferences mSharedPrefs;
	public AudioProcessor mAudioProcessor;
	
	Handler procNoteHandler = null;
	
	TextView backgroundText;
	Button onAirButton;
    ButtonsManager buttons;
    View controlsView;
    View contentView;

	public PurchaseManager purchaseManager;

    /**
     * Main code initiallization section after activity has been created
     */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


		// connect to Google Play Billing service
		purchaseManager = new PurchaseManager(this);
		
		// check login stuff
		mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Send various start up messages, welcome, rate, and new features.
        //TODO move this off of screen orientation rebuilds
        startUpMessages();

		
		// set up screen
		getOverflowMenu();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.activity_fullscreen);
        } else {
            setContentView(R.layout.activity_fullscreen_land);
        }

        // grab things needed from view
        getUIHandles();

        // Set up OpenGL environment
		mGLView = new MyGLSurfaceViewLegacy(mContext);

        setUpAudio();

        setUpCamera();

		
		// Set up an instance of SystemUiHider to control the system UI for
		// this activity. This is still used in Stand By mode. which is only
        // accessible by the back button.
		mSystemUiHider = SystemUiHider.getInstance(this, contentView,
				HIDER_FLAGS);
		mSystemUiHider.setup();
		mSystemUiHider.setOnVisibilityChangeListener(new 
				SystemUiHider.OnVisibilityChangeListener() {
					// Cached values.
					int mControlsHeight;
					int mShortAnimTime;

					@Override
					@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
					public void onVisibilityChange(boolean visible) {
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
							// If the ViewPropertyAnimator API is available
							// (Honeycomb MR2 and later), use it to animate the
							// in-layout UI controls at the bottom of the
							// screen.
							if (mControlsHeight == 0) {
								mControlsHeight = controlsView.getHeight();
							}
							if (mShortAnimTime == 0) {
								mShortAnimTime = getResources().getInteger(
										android.R.integer.config_shortAnimTime);
							}
							controlsView
									.animate()
									.translationY(visible ? 0 : mControlsHeight)
									.setDuration(mShortAnimTime);
						} else {
							// If the ViewPropertyAnimator APIs aren't
							// available, simply show or hide the in-layout UI
							// controls.
							controlsView.setVisibility(visible ? View.VISIBLE
									: View.GONE);
						}

						if (visible && AUTO_HIDE) {
							// Schedule a hide().
							delayedHide(AUTO_HIDE_DELAY_MILLIS);
						}
					}
				});


		
		// Set up the user interaction to manually show or hide the system UI.
		//
		View.OnTouchListener onTouchGeneralListener = new View.OnTouchListener(){

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				mHideHandler.removeCallbacks(mToast1Runnable);
				if (event.getAction()==MotionEvent.ACTION_UP||
						event.getAction()==MotionEvent.ACTION_CANCEL){
					if (TOGGLE_ON_CLICK) {
						mSystemUiHider.toggle();
					
					} else {
						mSystemUiHider.hide();
					}
					
				}
				if (ON_AIR_MODE) {mGLView.listenForTouch.onTouch(v, event);}
				return true;//mGLView.listenForTouch.onTouch(v, event);
			}
		
		}; //*/
		
				
		contentView.setOnTouchListener(onTouchGeneralListener);


		//controlsView.setOnClickListener(onClickGeneralListener);


		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		onAirButton.setOnTouchListener(
				mDelayHideTouchListener);
		onAirButton.setOnClickListener(
                mOnAirButtonClickListener);

		
		mSharedPrefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        // this actually posts to onAir mode
        onAirButton.performClick();
	}

    /**
     * Ask various startup messages, welcome, rate, new features ect
     */
    private void startUpMessages(){
        // Message on first runs
        int number_of_runs = mSharedPrefs.getInt("number_of_runs", 0);
        int ask_to_rate = mSharedPrefs.getInt("ask_to_rate", 0);
        if (number_of_runs<=1){
            Bundle bundle = new Bundle();
            bundle.putInt("messageId", R.string.dialog_welcome_to_penelope);
            bundle.putInt("button1", R.string.dialog_button_ok);
            UpSaleDialog dialog = new UpSaleDialog();
            dialog.setArguments(bundle);
            dialog.show(getFragmentManager(),"PaidForVersionDialog");

        } else if (number_of_runs>=3 && ask_to_rate==0){ //ask to rate on third entry if not asked yet.
            Bundle bundle = new Bundle();
            bundle.putInt("messageId", R.string.dialog_rate_penelope);
            bundle.putInt("button1", R.string.dialog_button_ok);
            bundle.putInt("button2", R.string.dialog_button_next_time);
            UpSaleDialog dialog = new UpSaleDialog();
            dialog.setArguments(bundle);
            dialog.show(getFragmentManager(),"PaidForVersionDialog");
            mSharedPrefs.edit().putInt("ask_to_rate", 1).apply();
        }

        // increment number of runs
        mSharedPrefs.edit().putInt("number_of_runs", ++number_of_runs).apply();

        // New features dialog if customer hasn't seen it yet
        int version_code=13;
        try {
            version_code = this.getPackageManager()
                    .getPackageInfo(this.getPackageName(), 0).versionCode;
        } catch (NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        int new_features_level = mSharedPrefs.getInt("new_features_level", version_code);
        if (new_features_level<version_code) {
            //New Features Dialog Covering Special Effects Products
            UpSaleDialog.BuildUpSaleDialog(this, R.string.dialog_welcome_to_new_features,
                    R.string.dialog_button_buy_mug, R.string.dialog_button_ok, 3)
                    .show(getFragmentManager(),"PaidForVersionDialog");
            mSharedPrefs.edit().putInt("new_features_level", version_code).apply();
        }
    }

    /**
     * Grab contents of View
     */
    private void getUIHandles(){
        controlsView = findViewById(R.id.fullscreen_content_controls);
        contentView = findViewById(R.id.fullscreen_content);
        backgroundText = (TextView) findViewById(R.id.fullscreen_content);
        onAirButton = (Button) findViewById(R.id.dummy_button);
        buttons = new ButtonsManager(this, mSharedPrefs);
        mFragmentViewGroup = (FrameLayout) findViewById(R.id.fragment_container);
        mOpenGLViewGroup = (FrameLayout) findViewById(R.id.opengl_container);
    }

    /**
     * Set up audio processor and handlers
     */
    private void setUpAudio(){
        mAudioProcessor = new AudioProcessor(this,
                android.os.Process.THREAD_PRIORITY_AUDIO,
                android.os.Process.THREAD_PRIORITY_URGENT_AUDIO,
                mSharedPrefs.getInt("sound_buffer_size_key", 2));

        mAudioProcessor.setSprectrumUpdateHandler(new UpdateSpectrumHandler(this,getMainLooper()));
        ArrayList<String> skus = mAudioProcessor.checkSpecialEffects(this,purchaseManager);
        if (!skus.isEmpty()){
            for (String sku:skus){
                PurchaseDialog dialog = new PurchaseDialog()
                        .setPurchaseManager(purchaseManager)
                        .setAudioProcessor(mAudioProcessor)
                        .setSku(sku);
                dialog.show(getFragmentManager(), "Purchase " + sku);
            }
        }
        mAudioProcessor.updateToneGenerator(mSharedPrefs.getBoolean("enable_tone_generator_key", false));
        mAudioProcessor.updateReverb(mSharedPrefs.getBoolean("enable_reverb_key", false));
        mAudioProcessor.start();
        procNoteHandler = mAudioProcessor.getNoteUpdateHandler();
    }

    /**
     * Set up camera
     */
    private void setUpCamera(){

        mPcamera = new Pcamera(this,(ImageButton) findViewById(R.id.capture_button),
                (ImageButton) findViewById(R.id.switchCameraButton),
                mGLView, mAudioProcessor.getThru(),
                ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                        .getDefaultDisplay().getRotation(),
                getResources().getConfiguration().orientation);
    }
	
	
	@Override
	protected void onPause(){
		super.onPause();
		
        if (ON_AIR_MODE){
				findViewById(R.id.dummy_button).performClick();
        }

	}

    @Override
    protected void onResume(){
        super.onResume();
        if (!ON_AIR_MODE) {
            findViewById(R.id.dummy_button).performClick();
        }
        //mPcamera.start();
    }
	
	
	/**
	 * Check whether the device has been put to sleep (screenOff) or if
	 *  the user is on the phone (onPhoneCall)
	 * @param context the context to check.
	 * @return true if on call or asleep otherwise false.
	 */
	public static boolean checkSleep(Context context){
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		boolean screenOff = !pm.isScreenOn();
		int onPhoneCall = tm.getCallState();
		if (screenOff||(onPhoneCall!=0)){
			return true;

		}
		return false;
	}
	
	@Override 
	protected void onStop(){
		super.onStop();
		mAudioProcessor.stopAudio();
		mPcamera.stop();
		
		//stop google analytics tracker
		EasyTracker.getInstance(this).activityStop(this);
	}
	
	
	@Override 
	protected void onDestroy(){
		super.onDestroy();
		//mAudioOnAir.kill();
		mAudioProcessor.releaseAudio();
		mAudioProcessor.quit();
		//mUsbAudioManager.close(this);
		purchaseManager.unbind();

	}
	
	@Override
	protected void onRestart(){
		super.onRestart();
		//mAudioOnAir.StartAudio();
		if (ON_AIR_MODE){
			startAudio();
			if (CAMERA_MODE) { //TODO change this to and record
			//mPcamera.start(mOpenGLViewGroup);
			mPcamera.start();
			}
		}
	}
	
	@Override
	public void onStart(){
		super.onStart();
		
		//start the google analytics tracker
		EasyTracker.getInstance(this).activityStart(this);  
	}
	
	
	
	private void startAudio() {
		procNoteHandler.post(new Runnable(){

			@Override
			public void run() {
				mAudioProcessor.startAudio(mSharedPrefs.getBoolean("enable_reverb_key", true),
						mSharedPrefs.getBoolean("invert_audio_key", false), 
						mSharedPrefs.getInt("sound_buffer_size_key", 2),
						((float) mSharedPrefs.getInt("sound_wet_dry_key", 92)/100.0f));
				
			}
			
		});

	}


	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		delayedHide(100);
	}
	
	/*@Override
	public boolean onTouchEvent(MotionEvent e) {
		super.onTouchEvent(e);
		if (! (mGLView==null)){
			mGLView.performClick();
		}
		
		return false;
	}//*/

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.xml.menu, menu);
	    mMenuItem = menu.findItem(R.id.options_menu_item);
	    mMenuItem.setOnActionExpandListener(mOptionsExpandListener);
	    //mSettingsMenu = mMenuItem.getSubMenu();
	    //mMenu = menu;
	    return true;
	}
	
	boolean optionsMenuPrepared = false;
	
	public boolean onPrepareOptionsMenu(Menu menu){
		if (!optionsMenuPrepared){
			optionsMenuPrepared = true;
		} else {
			mHideHandler.removeCallbacks(mHideRunnable);
		}
		
		return true;
		
	}
	
	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the
	 * system UI. This is to prevent the jarring behavior of controls going away
	 * while interacting with activity UI.
	 */
	View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			if (AUTO_HIDE) {
				delayedHide(AUTO_HIDE_DELAY_MILLIS);
				
			} 
			return false;
		}
	};
	
	protected static class UpdateSpectrumHandler extends Handler {
		private final WeakReference<PenelopeMainActivity> pennyReference;
		
		public UpdateSpectrumHandler(PenelopeMainActivity penny, Looper looper){
			super(looper);
			pennyReference = new WeakReference<PenelopeMainActivity>(penny);
		}
		
		public void handleMessage(Message msg) {
			PenelopeMainActivity penny = pennyReference.get();
			if (penny!=null){
				penny.onNewSpectrum((float[]) msg.obj);
			}
		}
	}
	
	protected void onNewSpectrum(float[] newSpectrum) {
		int note = 0;
		if (ON_AIR_MODE){
			try {
				note = mGLView.updateAmplitudes(newSpectrum);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		synchronized(procNoteHandler){
			Message msg = procNoteHandler.obtainMessage(); 
			msg.obj = note;
            //if (procNoteHandler)
			procNoteHandler.sendMessage(msg); 
		}
	}

    /**
     * Listener for the On Air button. This starts the session, audio, openGL,
     * Camera, cymatic overlay, everything that is on by saved preference.
     */
	View.OnClickListener mOnAirButtonClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View view) {

            // Toggle to the mode we are setting up
			ON_AIR_MODE = !ON_AIR_MODE;

            // Hide the ui controls (only applicable for Standby mode now)
			mHideHandler.removeCallbacks(mToast2Runnable);

            // Swaps the background and button text.
			CharSequence temp = backgroundText.getText();
			backgroundText.setText(onAirButton.getText());
            onAirButton.setText(temp);

            // Go ON Air
			if (ON_AIR_MODE) {

                // hide standby/on air button
                onAirButton.setVisibility(view.GONE);
                buttons.setVisibility(view.VISIBLE);

                // Set background to openGL view
                mOpenGLViewGroup.addView(mGLView);
                // Hide the background text after the 3 seconds or so
				findViewById(R.id.fullscreen_content).animate()
					.alpha(0f)
					.setDuration(AUTO_HIDE_DELAY_MILLIS)
					.setListener(null);
				mGLView.setOnTouchListener(mGLView.listenForTouch);

                // Launch some things in the background.
                // Camera has to be initiallized before starting or problems persist
                // TODO don't wait on frame redraws due to device orientation changes
                // TODO under which case the camera should already be initiallized.
                class OnAirAsyncTask extends AsyncTask<String, Integer, String> {
                    protected String doInBackground(String... urls) {
                        startAudio();
                        publishProgress(1);
                        try {
                            Thread.currentThread().sleep(5000L);

                            mPcamera.initSurface();
                            publishProgress(2);
                            if(mSharedPrefs.getBoolean("turn_on_video_preview_key",true)) {
                                Thread.currentThread().sleep(2000L);
                                toggleCameraMode();
                                publishProgress(3);
                            }


                        } catch (Exception e){
                            Log.e(TAG, "Error while waiting in OnAirAsyncTask = " + e.getMessage());
                            e.printStackTrace();

                        }
                        return "Complete";
                    }

                    protected void onProgressUpdate(Integer... progress) {
                        int countdown = 4 - progress[0];
                        Log.v(TAG, "OnAirAsyncTask=" + String.valueOf(countdown));
                    }

                    protected void onPostExecute(String result) {
                        Log.v(TAG, "OnAirAsyncTask=" + result);

                    }

                }

                new OnAirAsyncTask().execute("Start Audio");


            // Turn off On Air mode, mostly used for going into background
			}else {
				if (CAMERA_MODE) {
					toggleCameraMode();
				}

                // TODO move everything to capture manager as we can capture without camera now.
                mPcamera.stopLoop();

				findViewById(R.id.fullscreen_content).setVisibility(View.VISIBLE);
				findViewById(R.id.fullscreen_content).animate()
					.alpha(1f)
					.setDuration(AUTO_HIDE_DELAY_MILLIS/3)
					.setListener(null);


                mOpenGLViewGroup.removeView(mGLView);
				
				mAudioProcessor.stopAudio();

                onAirButton.setVisibility(view.VISIBLE);
                buttons.setVisibility(view.GONE);

			}
			
		}
	};

    /**
     * Turns on and off video camera preview.
     * @return true if preview is up
     */
    public boolean toggleCameraMode(){
        CAMERA_MODE = !CAMERA_MODE;
        if (CAMERA_MODE) {
            //mPcamera.start(mOpenGLViewGroup);

            if (!mPcamera.start()){
                CAMERA_MODE = !CAMERA_MODE;
                //return true;
            }
            //UpSaleDialog.BuildUpSaleDialog(mContext,
            //        R.string.dialog_penelope_full_messsage_record)
            //        .show(getFragmentManager(),"PaidForVersionDialog");
        } else {
            mPcamera.stop();

        }
        return CAMERA_MODE;
    }
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)  {
	    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ECLAIR
	            && keyCode == KeyEvent.KEYCODE_BACK
	            && event.getRepeatCount() == 0) {
	        // Take care of calling this method on earlier versions of
	        // the platform where it doesn't exist.
	        onBackPressed();
	    }

	    return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onBackPressed() {
	    // This will be called either automatically for you on 2.0
	    // or later, or by the code above on earlier versions of the
	    // platform.
		if (mFragmentViewGroup.getVisibility() == View.VISIBLE){
			mSystemUiHider.enable();
			mFragmentViewGroup.setVisibility(View.GONE);
		//} else if (mSystemUiHider.isVisible()){
		//	mSystemUiHider.hide();
		} else if (ON_AIR_MODE) {
			findViewById(R.id.dummy_button).performClick();
		} else finish();

        // close menu buttons
        buttons.setRabbitButton(false);
        buttons.setParticlesButton(false);
		
	    return;
	}
	
	/*/
	public void OpenDevicePreferences(View view){
		// Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();	
	}//*/

	Handler mHideHandler = new Handler();
	Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			mSystemUiHider.hide();
		}
	};
	
	Context mContext = this;
	
	Runnable mToast1Runnable = new Runnable() {
		@Override
		public void run() {
			Toast.makeText(mContext, R.string.touch_screen, Toast.LENGTH_SHORT).show();
			mHideHandler.postDelayed(mToast1Runnable, 2*AUTO_HIDE_DELAY_MILLIS);
		}
	};
	
	Runnable mToast2Runnable = new Runnable() {
		@Override
		public void run() {
			Toast.makeText(mContext, R.string.go_onair, Toast.LENGTH_SHORT).show();
			mHideHandler.postDelayed(mToast2Runnable, 2*AUTO_HIDE_DELAY_MILLIS);
		}
	};

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}
	
	
	
	public boolean onOptionsItemSelected(MenuItem item){
		trackItemSelected(item);
		
		mHideHandler.removeCallbacks(mHideRunnable);
		if (item.getItemId() == R.id.options_menu_item) {
			launchSettingsMenu();
		} else if (item.getItemId()==R.id.options_menu_item_help) {
			Intent intent = new Intent(this, SettingsActivity.class);	
			intent.putExtra(EXTRA_MESSAGE,  R.xml.help);
			startActivity(intent);
		} else if (item.getItemId()==R.id.options_menu_special_efects) {
			//Intent intent = new Intent(this, SpecialEffects.class);
			launchEffectsMenu();
			mSystemUiHider.disable();
		}
		
		return false;
	}

    public void launchSettingsMenu(){
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra(EXTRA_MESSAGE, R.xml.settings);
        startActivity(intent);
    }


    public void launchEffectsMenu(){
        mSettingsFragment = new SettingsFragment().setXmlId(R.xml.special_effects);
        mSettingsFragment.setPurchaseManager(purchaseManager)
                .setAudioProcessor(mAudioProcessor)
                .setPcam(mPcamera)
                .setMyGLView(mGLView);

        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, mSettingsFragment)
                .commit();
        mFragmentViewGroup.setVisibility(View.VISIBLE);
    }
	
	private void trackItemSelected(MenuItem item) {
		EasyTracker easyTracker = EasyTracker.getInstance(this);

		// MapBuilder.createEvent().build() returns a Map of event fields and values
		// that are set and sent with the hit.
		easyTracker.send(MapBuilder.createEvent(
				"options_menu",     // Event category (required)
				(String) item.getTitle(),  // Event action (required)
				item.toString(),   // Event label
				null)            // Event value
		.build());
	}


	/**
	 * Remove the UI's hide routine when the options menu is expanded
	 * and hide UI when options menu is collapsed.
	 */
	private MenuItem.OnActionExpandListener mOptionsExpandListener = new MenuItem.OnActionExpandListener() {
		
		@Override
		public boolean onMenuItemActionExpand(MenuItem item) {
			mHideHandler.removeCallbacks(mHideRunnable);
			return false;
		}
		
		@Override
		public boolean onMenuItemActionCollapse(MenuItem item) {
			delayedHide(AUTO_HIDE_DELAY_MILLIS);
			return false;
		}
	};

	
	/**
	 * Open the Preference fragment associated with the item when 
	 * the item is selected.
	 */
	/*private MenuItem.OnMenuItemClickListener mMenuItemClickedListener = new MenuItem.OnMenuItemClickListener() {

		@Override
		public boolean onMenuItemClick(MenuItem item) {
			//mSystemUiHider.hide();
			//mSystemUiHider.disable();
			if (item.getItemId() == R.id.devices_menu_item) {
				
				TOGGLE_ON_CLICK = false; //turn off the UI while in settings.
				mSettingsFragment.addPreferencesFromResource(R.xml.devices);
			} else if (item.getItemId() == R.id.visualizations_menu_item) {
				
			} else if (item.getItemId() == R.id.addons_menu_item) {
				
			}
			return false;
		}
		

	};//*/

	
	
	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
			int height) {
        /*mCamera = Camera.open();

        try {
        	mGLView.getMyGLSurfaceView(this).;
            mCamera.setPreviewTexture(surface);
            mCamera.startPreview();
        } catch (IOException ioe) {
            // Something bad happened
        }*/
		//mGLTextureView.onSurfaceTextureAvailable(surface, width, height);
	}



	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width,
			int height) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		// TODO Auto-generated method stub
		return false;
	}



	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surface) {
		// TODO Auto-generated method stub
		
	}
	
	private void getOverflowMenu() {

	     try {
	        ViewConfiguration config = ViewConfiguration.get(this);
	        Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
	        if(menuKeyField != null) {
	            menuKeyField.setAccessible(true);
	            menuKeyField.setBoolean(config, false);
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
	private OnMenuVisibilityListener ActionBarMenuListerner = new OnMenuVisibilityListener(){

		@Override
		public void onMenuVisibilityChanged(boolean isVisible) {
			if (isVisible){
				mHideHandler.removeCallbacks(mHideRunnable);
			} else {
				delayedHide(AUTO_HIDE_DELAY_MILLIS);
			}
			
		}
		
		
	};
	
	OnSharedPreferenceChangeListener preferenceChangeListener = new OnSharedPreferenceChangeListener() {
		
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
				String key) {
			if (key.contains("turn_on_accelerometer_key")) {
				/*/
				if (sharedPreferences.getBoolean(key, true)){
					mGLView.mRenderer.mAccelmeter.start();
				} else {
					mGLView.mRenderer.mAccelmeter.stop();
					mGLView.mRenderer.mAccelmeter.linear_acceleration[0] = 0.0f;
					mGLView.mRenderer.mAccelmeter.linear_acceleration[1] = 0.0f;//reset
				}//*/
			} else if (key.contains("turn_on_visualization_key")){
                mGLView.setDrawOverlay(sharedPreferences.getBoolean(key,true));
            } else if (key.contains("turn_on_output_audio_key")){
                mAudioProcessor.setPlayback(sharedPreferences.getBoolean(key,false));
            } else if (key.contains("vis_number_of_particles_key")||
                    key.contains("size_of_particles_key")||
                    key.contains("opacity_of_particles_key")){
                mGLView.updateParticles(
                        sharedPreferences.getInt("vis_number_of_particles_key", 6000),
                        sharedPreferences.getFloat("size_of_particles_key", 1.0f),
                        sharedPreferences.getFloat("opacity_of_particles_key", 1.0f));
            }

		}
	};
	
//*
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) { 
		
		if (requestCode == purchaseManager.pitchCorrect_RequestCode) {           
			int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
			String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
			String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");
			
			if (resultCode == RESULT_OK) {
				try {
					JSONObject jo = new JSONObject(purchaseData);
					String msg = getString(R.string.dialog_purchase1) +
							jo.getString("productId") + getString(R.string.dialog_purchase2);
				
					Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
					mAudioProcessor.setPitchCorrect(true);
					
				}
				catch (JSONException e) {
					Toast.makeText(mContext, R.string.dialog_purchase_fail, Toast.LENGTH_LONG).show();
					// uncheck the box
					mAudioProcessor.setPitchCorrect(false);
					mSettingsFragment.checkPref.setChecked(false);
					e.printStackTrace();
				}
			} else {
				Toast.makeText(mContext, R.string.dialog_purchase_fail, Toast.LENGTH_LONG).show();
				// uncheck the box
				mAudioProcessor.setPitchCorrect(false);
				mSettingsFragment.checkPref.setChecked(false);
			}
		}
	}
	//*/

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setContentView(R.layout.activity_fullscreen_land);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            setContentView(R.layout.activity_fullscreen);
        }
        //onAirButton.performClick();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //outState.putBundle("bundleBuddy", bundleBuddy);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        //savedInstanceState.getBundle("bundleBuddy");
        //onAirButton.performClick();
    }

}


