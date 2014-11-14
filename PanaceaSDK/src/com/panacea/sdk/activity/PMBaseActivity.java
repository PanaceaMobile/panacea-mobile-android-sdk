package com.panacea.sdk.activity;

import java.io.File;

import net.sqlcipher.database.SQLiteDatabase;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;

import com.panacea.sdk.PMConstants;
import com.panacea.sdk.PMSettings;
import com.panacea.sdk.PanaceaSDK;
import com.panacea.sdk.PanaceaSDK.Result;
import com.panacea.sdk.R;
import com.panacea.sdk.fragment.DEVPMSendMessageFragment;
import com.panacea.sdk.fragment.PMLoginFragment;
import com.panacea.sdk.fragment.PMMessageDetailFragment;
import com.panacea.sdk.fragment.PMMessagesFragment;
import com.panacea.sdk.fragment.PMSettingsFragment;
import com.panacea.sdk.fragment.PMSubjectFragment;
import com.panacea.sdk.web.PMLocalBroadcastReceiver;
import com.panacea.sdk.web.PMLocalBroadcastReceiver.PMBroadcastListener;

public class PMBaseActivity extends ActionBarActivity implements PMBroadcastListener
{
	/* Handles Extras from intent */
	public static String EXTRA_SHOW_THREAD = "EXTRA_SHOW_THREAD";
	public static String EXTRA_SHOW_SUBJECT = "EXTRA_SHOW_SUBJECT";
	public static boolean didTapNotication = false;
	public static String openToSubject = null;
	public static String openToThread = null;

	private static String TAG = "PMBaseActivity";

	protected PanaceaSDK sdk;
	protected PMLocalBroadcastReceiver mReceiver = new PMLocalBroadcastReceiver();

	private FrameLayout baseContainer;

	private PMLoginFragment loginFragment;
	private PMSubjectFragment subjectFragment;
	private PMMessagesFragment messagesFragment;
	private PMMessageDetailFragment messageDetailFragment;
	private PMSettingsFragment settingsFragment;

	private static boolean startup = true;
	private static boolean hasResized = false;
	public static boolean isActivityActive = false;
	public static boolean isOfflineMode = false;

	public BackButtonCallbacks mBackButtonCallbacks;


	private void InitializeSQLCipher()
	{
		SQLiteDatabase.loadLibs(this);
		//	File databaseFile = getDatabasePath("demo.db");
		//	databaseFile.mkdirs();
		//	databaseFile.delete();
		//	SQLiteDatabase database = SQLiteDatabase
		//		.openOrCreateDatabase(databaseFile, "test123", null);
		//	database.execSQL("create table t1(a, b)");
		//	database.execSQL("insert into t1(a, b) values(?, ?)", new Object[]
		//		{ "one for the money", "two for the show" });
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// sql cipher test
		InitializeSQLCipher();


		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);


		setContentView(R.layout.activity_base);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

		setHomeEnabled(true);
		baseContainer = (FrameLayout) findViewById(R.id.baseContainter);
		sdk = PanaceaSDK.getInstance();

		//if this activity is created from push notification the sdk will be null here
		if (sdk == null)
			sdk = new PanaceaSDK(PMSettings.getApplicationKey(this), this);

		if (savedInstanceState == null)
			startup = true;

		checkNotificationIntent(getIntent());

		goToInbox();

		/* Here we handle the popup window effects for tablets */
		boolean popupWindow = getResources().getBoolean(R.bool.popupWindow);
		if (popupWindow)
		{
			hasResized = false;
			ViewTreeObserver vto = baseContainer.getViewTreeObserver();
			vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener()
			{

				@Override
				public void onGlobalLayout()
				{
					if (!hasResized)
					{
						hasResized = true;
						LayoutParams params = getWindow().getAttributes();
						int width = Math.min(baseContainer.getMeasuredWidth()
							- convertDpUnitsToPixels(20), convertDpUnitsToPixels(600));
						int height = Math.min(baseContainer.getMeasuredHeight()
							- convertDpUnitsToPixels(20), convertDpUnitsToPixels(1000));

						params.height = height;
						params.width = width;
						params.alpha = 1.0f;
						params.dimAmount = 0.5f;
						getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
					}
				}
			});
		}
		
		// set the callback for the back button to be null; will be assigned on a per fragment create basis
		mBackButtonCallbacks = null;
	}

	public void setHomeEnabled(boolean enabled)
	{
		getSupportActionBar();
		getSupportActionBar().setDisplayHomeAsUpEnabled(enabled);
		getSupportActionBar().setHomeButtonEnabled(enabled);
	}

	@Override
	protected void onResume()
	{
		isActivityActive = true;

		super.onResume();
		mReceiver.register(this, this);
		checkLoading();

		if (didTapNotication)
		{
			didTapNotication = false;
			startup = false;
			goToInbox();

			if (openToSubject != null)
			{
				goToMessagesForSubject(openToSubject);
			}
			if (openToThread != null)
			{
				goToMessageDetails(openToThread);
			}
		}

		if (startup)
		{
			sdk.registerDevice();
			startup = false;
		}
	}

	@Override
	protected void onPause()
	{
		mReceiver.unregister(this);
		super.onPause();
		isActivityActive = false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_activity, menu);

		menu.findItem(R.id.menu_send_message).setVisible(PMConstants.DEBUG);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int itemId = item.getItemId();
		if (itemId == android.R.id.home)
		{
			if (getSupportFragmentManager().getBackStackEntryCount() > 0)
				return super.onOptionsItemSelected(item);
			else
				finish();
			return true;
		}
		else if (itemId == R.id.menu_settings)
		{
			goToSettings();
			return true;
		}
		else if (itemId == R.id.menu_refresh)
		{
			sdk.updateMessages(false);
			return true;
		}
		else if (itemId == R.id.menu_send_message)
		{
			Integer threadId = null;
			String subject = null;

			PMMessageDetailFragment threadFragment = (PMMessageDetailFragment) getSupportFragmentManager()
				.findFragmentByTag(PMMessageDetailFragment.TAG);
			if (threadFragment != null && threadFragment.isVisible())
			{
				threadId = Integer.valueOf(threadFragment.getThreadId());
				subject = threadFragment.getSubject();
			}

			dev_showSendMessageFragment(subject, threadId);
			return true;
		}

		// return false to pass on to any fragment/child class
		return false;
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		checkNotificationIntent(intent);
		super.onNewIntent(intent);
	}

	private void checkNotificationIntent(Intent intent)
	{
		/*
		 * Check for extras - if there are extras the activity was probably
		 * started by tapping the notification
		 */
		if (intent.getExtras() != null)
		{
			didTapNotication = true;

			openToSubject = intent.getStringExtra(EXTRA_SHOW_SUBJECT);
			openToThread = intent.getStringExtra(EXTRA_SHOW_THREAD);

			if (openToSubject != null)
			{
				intent.removeExtra(EXTRA_SHOW_SUBJECT);
			}
			if (openToThread != null)
			{
				intent.removeExtra(EXTRA_SHOW_THREAD);
			}
		}
	}

	/**
	 * Checks if any WebService is busy and shows progress bar in ActionBar
	 */
	public void checkLoading()
	{
		boolean loading = sdk.isBusy();
		setProgressBarIndeterminateVisibility(loading);
	}

	public void goToLogin()
	{
		if (baseContainer != null)
		{
			loginFragment = (PMLoginFragment) getSupportFragmentManager().findFragmentByTag(
				PMLoginFragment.TAG);
			if (loginFragment == null)
			{
				loginFragment = new PMLoginFragment();
				getSupportFragmentManager().beginTransaction()
					.replace(R.id.baseContainter, loginFragment, PMLoginFragment.TAG).commit();
			}
		}
	}

	public void goToInbox()
	{
		if (baseContainer != null)
		{
			subjectFragment = (PMSubjectFragment) getSupportFragmentManager().findFragmentByTag(
				PMSubjectFragment.TAG);

			if (subjectFragment == null)
			{
				subjectFragment = new PMSubjectFragment();
			}

			openFragment(R.id.baseContainter, subjectFragment, PMSubjectFragment.TAG, true);
		}
	}

	public void goToMessagesForSubject(String subject)
	{
		if (baseContainer != null)
		{
			messagesFragment = (PMMessagesFragment) getSupportFragmentManager().findFragmentByTag(
				PMMessagesFragment.TAG);

			if (messagesFragment == null)
			{
				messagesFragment = new PMMessagesFragment();
			}

			messagesFragment.setSubject(subject);
			openFragment(R.id.baseContainter, messagesFragment, PMMessagesFragment.TAG, false);
		}
	}

	public void goToMessageDetails(String threadId)
	{
		if (baseContainer != null)
		{
			messageDetailFragment = (PMMessageDetailFragment) getSupportFragmentManager()
				.findFragmentByTag(PMMessageDetailFragment.TAG);

			if (messageDetailFragment == null)
			{
				messageDetailFragment = new PMMessageDetailFragment();
			}

			messageDetailFragment.setThreadId(threadId);
			openFragment(R.id.baseContainter, messageDetailFragment, PMMessageDetailFragment.TAG,
				false);
		}
	}

	public void goToSettings()
	{
		if (baseContainer != null)
		{
			settingsFragment = (PMSettingsFragment) getSupportFragmentManager().findFragmentByTag(
				PMSettingsFragment.TAG);

			if (settingsFragment == null)
			{
				settingsFragment = new PMSettingsFragment();
			}

			openFragment(R.id.baseContainter, settingsFragment, PMSettingsFragment.TAG, false);
		}
	}

	private void openFragment(int id, Fragment fragment, String tag, boolean isRootFragment)
	{
		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction transaction = fragmentManager.beginTransaction();

		// Animation code
		transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
			R.anim.slide_in_left, R.anim.slide_out_right);

		transaction.replace(id, fragment, tag);
		if (!isRootFragment)
		{
			transaction.addToBackStack(tag);
		}
		else
		{
			//pop the entire back stack
			fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		}
		transaction.commit();
	}

	private void dev_showSendMessageFragment(String subject, Integer threadId)
	{
		FragmentManager fm = getSupportFragmentManager();
		DEVPMSendMessageFragment frag = new DEVPMSendMessageFragment();
		frag.setThreadId(threadId);
		frag.setSubject(subject);
		frag.show(fm, DEVPMSendMessageFragment.TAG);
	}

	public void showPopupMessage(String title, String message)
	{
		showPopupMessage(title, message, false);
	}

	public void showPopupMessage(String title, String message, boolean finishOnClose)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(title);
		builder.setMessage(message);

		if (finishOnClose)
		{
			builder.setNegativeButton("OK", new OnClickListener()
			{

				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					finish();
				}
			});
		}
		else
		{
			builder.setNegativeButton("OK", null);
		}
		final AlertDialog alert = builder.create();
		alert.show();
	}

	@Override
	public void onPreDataExecute(String tag)
	{
		Log.d(TAG, "onPreDataExecute tag: " + tag);
		checkLoading();
	}

	public void onPostDataSuccess(String tag, String result, int status, String message)
	{
		Log.d(TAG, "onPostDataSuccess tag: " + tag + " result: " + result);
		checkLoading();
		isOfflineMode = false;


		if (Result.REGISTER_SUCCESS.equals(result))
		{
			sdk.updateLocation();
			sdk.updateMessages(false);
			goToInbox();
		}
		else if (Result.NOT_VERIFIED.equals(result))
		{
			showPopupMessage("Device Not Verified", message, true);
		}

		else if (Result.REQUEST_PHONE_NUMBER.equals(result))
		{
			goToLogin();
		}
		else if (Result.REQUEST_VERIFICATION_CODE.equals(result))
		{
			goToLogin();
		}
	}

	@Override
	public void onPostDataFailure(String tag, String result, int code, String message)
	{
		Log.d(TAG, "onPostDataFailure tag: " + tag + " result: " + result);
		checkLoading();

		if (!isOfflineMode)
		{
			showPopupMessage("Connection Error", message);
			isOfflineMode = true;
		}
	}

	public int convertDpUnitsToPixels(int dp)
	{
		// Get the screen's density scale
		final float scale = getResources().getDisplayMetrics().density;
		// Convert the dps to pixels, based on density scale
		return (int) (dp * scale + 0.5f);
	}

	public int convertPixelsToDpUnits(int px)
	{
		final float scale = getResources().getDisplayMetrics().density;
		return (int) ((px) / scale);
	}

	@Override
	public void onBackPressed()
	{
		Log.d("DetailActivity", "user pressed the back button");
		if (mBackButtonCallbacks != null)
			mBackButtonCallbacks.onBackPressedCallback();
		else
			super.onBackPressed();

	}

	public interface BackButtonCallbacks
	{
		public void onBackPressedCallback();
	}
}
