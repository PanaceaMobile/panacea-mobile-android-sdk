package com.panacea.sdk;

import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.panacea.sdk.gcm.GcmHelper;

/**
 * This class is responsible for checking device settings and persisting them to
 * shared preferences.
 * 
 * @author Cobi Interactive
 */
public class PMSettings
{
	private static final String TAG = "PMSettings";
	private static final String OPERATING_SYSTEM = "Android";

	private static SharedPreferences getDeviceSettings(Context context)
	{
		return context.getSharedPreferences("PMSDK", Context.MODE_PRIVATE);
	}

	private static void setSetting(String key, String value, Context context)
	{
		if (context == null)
			return;

		getDeviceSettings(context).edit().putString(key, value).commit();
	}

	/**
	 * Get a setting for a given key
	 * 
	 * @param key
	 * @param context
	 * @return string value of the key or null if not found
	 */
	public static String getSetting(String key, Context context)
	{
		if (context == null)
			return null;

		return getDeviceSettings(context).getString(key, null);
	}

	/**
	 * Checks if there are any differences in current configuration and saved
	 * configuration
	 * 
	 * @return true if there are differences, otherwise false
	 */
	public static boolean hasDeviceConfigurationChanged(Context context)
	{
		String saved_uid = getSetting("unique_device_id", context);
		String unique_device_id = getUniqueDeviceId(context);
		String device_manufacturer = getDeviceManufacturer();
		String device_model = getDeviceModel();
		String operating_system = getOpereatingSystem();
		String operating_system_version = getOperatingSystemVersion();
		String timezone = getTimezone();
		String locale = getLocale();
		String push_channel_id = getPushChannelId();

		if (!unique_device_id.equals(saved_uid))
		{
			Log.d(TAG, "config changed: " + "unique_device_id");
			return true;
		}

		if (!device_manufacturer.equals(getSetting("device_manufacturer", context)))
		{
			Log.d(TAG, "config changed: " + "device_manufacturer");
			return true;
		}

		if (!device_model.equals(getSetting("device_model", context)))
		{
			Log.d(TAG, "config changed: " + "device_model");
			return true;
		}

		if (!operating_system.equals(getSetting("operating_system", context)))
		{
			Log.d(TAG, "config changed: " + "operating_system");
			return true;
		}

		if (!operating_system_version.equals(getSetting("operating_system_version", context)))
		{
			Log.d(TAG, "config changed: " + "operating_system_version");
			return true;
		}

		if (!timezone.equals(getSetting("timezone", context)))
		{
			Log.d(TAG, "config changed: " + "timezone");
			return true;
		}

		if (!locale.equals(getSetting("locale", context)))
		{
			Log.d(TAG, "config changed: " + "locale");
			return true;
		}

		if (!push_channel_id.equals(getSetting("push_channel_id", context)))
		{
			Log.d(TAG, "config changed: " + "push_channel_id");
			return true;
		}

		Log.d(TAG, "config changed: " + "NOTHING");
		return false;
	}


	/**
	 * Checks if the push notification has changed
	 * 
	 * @return true if there are differences, otherwise false
	 */
	public static boolean hasPushKeyChanged(Context context)
	{
		//Get the GCM key registered with Google. will be "" or null if not registered
		String GCMKeyGoogle = GcmHelper.getRegistrationId(context);

		//Get the GCM key registered with Panacea. will be null if not registered
		String GCMKeyPanacea = getPushNotificationKey(context);

		return !GCMKeyGoogle.equals(GCMKeyPanacea);
	}

	/**
	 * Saves the current device configuration to SharedPreferences
	 * 
	 * @param context
	 */
	public static void setDeviceConfiguration(Context context)
	{
		String unique_device_id = getUniqueDeviceId(context);
		String device_manufacturer = getDeviceManufacturer();
		String device_model = getDeviceModel();
		String operating_system = getOpereatingSystem();
		String operating_system_version = getOperatingSystemVersion();
		String timezone = getTimezone();
		String locale = getLocale();
		String push_channel_id = getPushChannelId();

		setSetting("unique_device_id", unique_device_id, context);
		setSetting("device_manufacturer", device_manufacturer, context);
		setSetting("device_model", device_model, context);
		setSetting("operating_system", operating_system, context);
		setSetting("operating_system_version", operating_system_version, context);
		setSetting("timezone", timezone, context);
		setSetting("locale", locale, context);
		setSetting("push_channel_id", push_channel_id, context);
	}

	/**
	 * Generates a UUID
	 * 
	 * @param context
	 * @return the generated or saved UUID
	 */
	public static String getUniqueDeviceId(Context context)
	{
		//get existing id
		String uid = getSetting("unique_device_id", context);

		//if it does not exist
		if (uid == null)
		{
			//generate new one
			uid = UUID.randomUUID().toString();
			
			//save new one
			setSetting("unique_device_id", uid , context);
			Log.d(TAG, "uid generated: " + uid);
		}

		Log.d(TAG, "uid: " + uid);
		return uid;
	}

	/**
	 * Gets device Manufacturer
	 * 
	 * @return string of Device Manufacturer
	 */
	public static String getDeviceManufacturer()
	{
		String deviceManufacturer = android.os.Build.MANUFACTURER;
		Log.d(TAG, "deviceManufacturer: " + deviceManufacturer);
		return deviceManufacturer;
	}

	/**
	 * Gets device Model
	 * 
	 * @return string of Device Model
	 */
	public static String getDeviceModel()
	{
		String deviceModel = android.os.Build.MODEL;
		Log.d(TAG, "deviceModel: " + deviceModel);

		return deviceModel;
	}

	/**
	 * Gets Operating System Version
	 * 
	 * @return string of Operating System Version
	 */
	public static String getOperatingSystemVersion()
	{
		String osVersion = android.os.Build.VERSION.RELEASE;
		Log.d(TAG, "Operating System Version: " + osVersion);

		return osVersion;
	}

	/**
	 * Gets current Time zone
	 * 
	 * @return string of current Time zone
	 */
	public static String getTimezone()
	{
		TimeZone tz = TimeZone.getDefault();
		//String timezone = tz.getDisplayName(false, TimeZone.SHORT) + " Timezone id :: " + tz.getID();
		String timezone = tz.getID();
		Log.d(TAG, "TimeZone: " + timezone);
		return timezone;
	}

	/**
	 * Gets current Locale
	 * 
	 * @return string of current Locale
	 */
	public static String getLocale()
	{
		String locale = Locale.getDefault().toString();
		Log.d(TAG, "locale: " + locale);
		return locale;
	}

	/**
	 * Gets android push channel id - {@link PMConstants.PushChannel#ANDROID}
	 * 
	 * @return string android push channel id
	 */
	public static String getPushChannelId()
	{
		return PMConstants.PushChannel.ANDROID;
	}

	/**
	 * Gets operating system - {@link #OPERATING_SYSTEM}
	 * 
	 * @return string of operating system
	 */
	public static String getOpereatingSystem()
	{
		return OPERATING_SYSTEM;
	}

	/**
	 * Saves application key to SharedPreferences
	 * 
	 * @param applicationKey
	 * @param context
	 */
	public static void setApplicationKey(String applicationKey, Context context)
	{
		setSetting("application_key", applicationKey, context);
	}

	/**
	 * Retrieves the application key from SharedPreferences
	 * 
	 * @param context
	 * @return Application Key
	 */
	public static String getApplicationKey(Context context)
	{
		return getSetting("application_key", context);
	}

	/**
	 * Saves Device Signature to SharedPreferences
	 * 
	 * @param deviceSignature
	 * @param context
	 */
	public static void setDeviceSignature(String deviceSignature, Context context)
	{
		setSetting("device_signature", deviceSignature, context);
	}

	/**
	 * Retrieves the Device Signature from SharedPreferences
	 * 
	 * @param context
	 * @return Device Signature
	 */
	public static String getDeviceSignature(Context context)
	{
		return getSetting("device_signature", context);
	}

	/**
	 * Saves Push Notification Key to SharedPreferences
	 * 
	 * @param key
	 * @param context
	 */
	public static void setPushNotificationKey(String key, Context context)
	{
		setSetting("channel_key", key, context);
	}

	/**
	 * Retrieves the Push Notification Key from SharedPreferences
	 * 
	 * @param context
	 * @return Push Notification Key
	 */
	public static String getPushNotificationKey(Context context)
	{
		return getSetting("channel_key", context);
	}

	/**
	 * Checks if the device is verified and saved in SharedPreferences
	 * 
	 * @param context
	 * @return true if it is verified otherwise false
	 */
	public static boolean isVerified(Context context)
	{
		if (context == null)
			return false;

		return getDeviceSettings(context).getBoolean("verified", false);
	}

	/**
	 * Saves a Verified boolean to SharedPreferences
	 * 
	 * @param verified
	 * @param context
	 */
	public static void setVerified(boolean verified, Context context)
	{
		getDeviceSettings(context).edit().putBoolean("verified", verified).commit();
	}

	/**
	 * Saves phone number to SharedPreferences
	 * 
	 * @param phoneNumber
	 * @param context
	 */
	public static void setPhoneNumber(String phoneNumber, Context context)
	{
		setSetting("phone_number", phoneNumber, context);
	}

	/**
	 * Retrieves the Phone Number from SharedPreferences
	 * 
	 * @param context
	 * @return Phone Number
	 */
	public static String getPhoneNumber(Context context)
	{
		return getSetting("phone_number", context);
	}
	
	
	public static String getGivenName(Context context)
	{
		String storedName = getSetting("given_name", context);
		
		if(storedName != null)
			return storedName;
		else
		{
			setSetting("given_name", android.os.Build.MODEL, context);
			return android.os.Build.MODEL;
		}
			
	}
	
	public static void setGivenName(Context context, String name)
	{
		setSetting("given_name",name, context);
	}
	

	/**
	 * Clears saved settings excluding Application Key, Push Notification Key
	 * and UUID
	 * 
	 * @param context
	 */
	public static void clearSettings(Context context)
	{
		//backup persisted settings
		String appKey = getApplicationKey(context);
		String pushKey = getPushNotificationKey(context);
		String uuid = getUniqueDeviceId(context);
		String given_name = getGivenName(context);

		//clear
		getDeviceSettings(context).edit().clear().commit();

		//restore persisted settings
		setApplicationKey(appKey, context);
		setPushNotificationKey(pushKey, context);
		setSetting("unique_device_id", uuid, context);
		setGivenName(context,given_name);
	}
}
