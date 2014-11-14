package com.panacea.sdk.gcm;

import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

/**
 * GCM Helper to handle GCM registration.
 * 
 * @author Cobi interactive
 */
public class GcmHelper
{
	public interface GCMListener
	{
		public void onPreDataExecute(String tag);

		public void onPostDataSuccess(String tag);

		public void onPostDataFailure(String tag);

		public static final String TAG_REGISTER = "TAG_REGISTER";
	}

	public static final String EXTRA_MESSAGE = "message";
	public static final String PROPERTY_REG_ID = "registration_id";
	private static final String PROPERTY_APP_VERSION = "appVersion";
	private static final String PROPERTY_CHECK_PLAY_SERVICES = "checkPlayServices";
	private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

	/**
	 * Substitute you own sender ID here. This is the project number you got
	 * from the API Console, as described in "Getting Started."
	 */
	private static final String SENDER_ID = "399941107357";

	/**
	 * Tag used on log messages.
	 */
	static final String TAG = "GCMHelper";

	/**
	 * Checks if already registered with GooglePlayServices. If already
	 * registered the registration id is returned. If not registered, null will
	 * be returned and registration will occur - the response will be returned
	 * to the {@link GCMListener}.
	 * 
	 * @param activity
	 * @param listener
	 * @return the registration id if registered otherwise null.
	 */
	public static String getGCMRegistrationId(final Activity activity, final GCMListener listener)
	{
		String regid = null;

		if (checkPlayServices(activity))
		{
			regid = getRegistrationId(activity.getApplicationContext());

			if (isEmpty(regid))
			{
				registerInBackground(activity.getApplicationContext(), new GCMListener()
				{

					@Override
					public void onPreDataExecute(final String tag)
					{
						activity.runOnUiThread(new Runnable()
						{
							public void run()
							{
								if (listener != null)
									listener.onPreDataExecute(tag);
							}
						});
					}

					@Override
					public void onPostDataSuccess(final String tag)
					{
						activity.runOnUiThread(new Runnable()
						{
							public void run()
							{
								if (listener != null)
									listener.onPostDataSuccess(tag);
							}
						});
					}

					@Override
					public void onPostDataFailure(final String tag)
					{
						activity.runOnUiThread(new Runnable()
						{
							public void run()
							{
								if (listener != null)
									listener.onPostDataFailure(tag);
							}
						});
					}
				});
			}
		}

		return regid;
	}

	/**
	 * Check the device to make sure it has the Google Play Services APK. If it
	 * doesn't, display a dialog that allows users to download the APK from the
	 * Google Play Store or enable it in the device's system settings.
	 */
	private static boolean checkPlayServices(final Activity activity)
	{
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
		if (resultCode != ConnectionResult.SUCCESS)
		{
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode))
			{
				GooglePlayServicesUtil.getErrorDialog(resultCode, activity,
					PLAY_SERVICES_RESOLUTION_REQUEST).show();
			}
			else
			{
				//check if we should suppress message
				if (GcmHelper.shouldCheckPlayServices(activity))
				{
					AlertDialog.Builder builder = new AlertDialog.Builder(activity);
					builder.setTitle("Google Play Services");
					builder.setMessage("This device is not supported");
					builder.setNeutralButton("OK", null);
					builder.setNegativeButton("Don't ask again", new OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							GcmHelper.storeCheckPlayServices(activity, false);
						}
					});
					final AlertDialog alert = builder.create();
					alert.show();
				}

				Log.i(TAG, "This device is not supported.");
			}
			return false;
		}
		return true;
	}

	/**
	 * Stores the registration ID and the app versionCode in the application's
	 * {@code SharedPreferences}.
	 * 
	 * @param context
	 *        application's context.
	 * @param regId
	 *        registration ID
	 */
	private static void storeRegistrationId(Context context, String regId)
	{
		final SharedPreferences prefs = getGcmPreferences(context);
		int appVersion = getAppVersion(context);
		Log.i(TAG, "Saving regId on app version " + appVersion);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(PROPERTY_REG_ID, regId);
		editor.putInt(PROPERTY_APP_VERSION, appVersion);
		editor.commit();
	}

	/**
	 * Gets the current registration ID for application on GCM service, if there
	 * is one.
	 * <p>
	 * If result is empty, the app needs to register.
	 * 
	 * @return registration ID, or empty string if there is no existing
	 *         registration ID.
	 */
	public static String getRegistrationId(Context context)
	{
		final SharedPreferences prefs = getGcmPreferences(context);
		String registrationId = prefs.getString(PROPERTY_REG_ID, "");
		if (isEmpty(registrationId))
		{
			Log.i(TAG, "Registration not found.");
			return "";
		}
		// Check if app was updated; if so, it must clear the registration ID
		// since the existing regID is not guaranteed to work with the new
		// app version.
		int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
		int currentVersion = getAppVersion(context);
		if (registeredVersion != currentVersion)
		{
			Log.i(TAG, "App version changed.");
			return "";
		}
		return registrationId;
	}


	private static void storeCheckPlayServices(Context context, boolean checkPlayServices)
	{
		final SharedPreferences prefs = getGcmPreferences(context);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(PROPERTY_CHECK_PLAY_SERVICES, checkPlayServices);
		editor.commit();
	}

	private static boolean shouldCheckPlayServices(Context context)
	{
		final SharedPreferences prefs = getGcmPreferences(context);
		return prefs.getBoolean(PROPERTY_CHECK_PLAY_SERVICES, true);
	}


	/**
	 * Registers the application with GCM servers asynchronously.
	 * <p>
	 * Stores the registration ID and the app versionCode in the application's
	 * shared preferences.
	 */
	private static void registerInBackground(final Context context, final GCMListener listener)
	{
		new AsyncTask<Void, Void, String>()
		{
			@Override
			protected String doInBackground(Void... params)
			{
				if (listener != null)
					listener.onPreDataExecute(GCMListener.TAG_REGISTER);

				String msg = "";
				try
				{
					String regid = getGCM(context).register(SENDER_ID);
					msg = "Device registered, registration ID=" + regid;

					// Persist the regID - no need to register again.
					storeRegistrationId(context, regid);

					if (listener != null)
						listener.onPostDataSuccess(GCMListener.TAG_REGISTER);
				}
				catch (IOException ex)
				{
					msg = "Error :" + ex.getMessage();
					// If there is an error, don't just keep trying to register.
					// Require the user to click a button again, or perform
					// exponential back-off.

					if (listener != null)
						listener.onPostDataFailure(GCMListener.TAG_REGISTER);
				}
				return msg;
			}

			@Override
			protected void onPostExecute(String msg)
			{
				Log.i(TAG, "msg: " + msg);
				//				mDisplay.append(msg + "\n");
			}
		}.execute(null, null, null);
	}

	/**
	 * @return Application's version code from the {@code PackageManager}.
	 */
	private static int getAppVersion(Context context)
	{
		try
		{
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
				context.getPackageName(), 0);
			return packageInfo.versionCode;
		}
		catch (NameNotFoundException e)
		{
			// should never happen
			throw new RuntimeException("Could not get package name: " + e);
		}
	}

	/**
	 * @return Application's {@code SharedPreferences}.
	 */
	private static SharedPreferences getGcmPreferences(Context context)
	{
		return context.getSharedPreferences(GcmHelper.class.getSimpleName(), Context.MODE_PRIVATE);
	}

	private static GoogleCloudMessaging getGCM(Context context)
	{
		return GoogleCloudMessaging.getInstance(context);
	}

	private static boolean isEmpty(String string)
	{
		return string == null || string.length() == 0;
	}
}
