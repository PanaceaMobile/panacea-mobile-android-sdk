package com.panacea.sdk.web;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.panacea.sdk.PMConstants;
import com.panacea.sdk.PMParams;
import com.panacea.sdk.PMSettings;
import com.panacea.sdk.PMUtils;
import com.panacea.sdk.PanaceaSDK;
import com.panacea.sdk.PanaceaSDK.Tag;
import com.panacea.sdk.R;
import com.panacea.sdk.activity.PMBaseActivity;
import com.panacea.sdk.db.PMDatabaseCipherHelper;
import com.panacea.sdk.db.PMMessage;
import com.panacea.sdk.db.PMReceivedMessage;
import com.panacea.sdk.db.PMSentMessage;
import com.panacea.sdk.gcm.GcmHelper;
import com.panacea.sdk.model.PMArray;
import com.panacea.sdk.model.PMBaseResponse;
import com.panacea.sdk.model.PMDictionary;
import com.panacea.sdk.model.PMPagination;

/**
 * This {@link IntentService} handles the initiation and response handling of
 * all WebService calls. When the call and response handling is completed a a
 * local broadcast is send via {@link PMLocalBroadcastReceiver}.
 * 
 * @author Cobi Interactive
 * @see PMLocalBroadcastReceiver
 */
public class PMWebServiceController extends IntentService
{
	public static final String TAG = "PMWebServiceController";

	/**
	 * Used to keep track of the number of active webservices for each method
	 */
	private static Hashtable<String, Integer> busyRequests = new Hashtable<String, Integer>();

	/**
	 * Keeps track of the number of active requests.
	 * 
	 * @param method
	 */
	private synchronized void sendRequestStarted(String method)
	{
		if (method == null)
			return;

		if (busyRequests == null)
			busyRequests = new Hashtable<String, Integer>();

		Integer requestCount = busyRequests.get(method);
		if (requestCount == null)
		{
			busyRequests.put(method, Integer.valueOf(1));
		}
		else
		{
			busyRequests.put(method, ++requestCount);
		}
	}

	/**
	 * Keeps track of the number of active requests.
	 * 
	 * @param method
	 */
	private synchronized void sendRequestEnded(String method)
	{
		if (method == null)
			return;

		if (busyRequests == null)
			busyRequests = new Hashtable<String, Integer>();

		Integer requestCount = busyRequests.get(method);
		if (requestCount != null)
		{
			if (requestCount <= 1)
			{
				busyRequests.remove(method);
			}
			else
			{
				busyRequests.put(method, --requestCount);
			}
		}

	}

	/**
	 * Check if there are any busy Panacea web requests.
	 * 
	 * @return true if there are any requests, otherwise false
	 */
	public static boolean isBusy()
	{
		if (busyRequests == null)
			return false;

		return busyRequests.size() != 0;
	}

	public PMWebServiceController()
	{
		super(PMWebServiceController.class.getName());
	}

	@Override
	protected void onHandleIntent(Intent intent)
	{
		// get info from intent
		String action = intent.getAction();

		/* REGISTRATION */
		if (IntentAction.WebService.DEVICE_REGISTER.equals(action))
		{
			handle_device_register(intent);
		}
		else if (IntentAction.WebService.DEVICE_REGISTER_MSISDN.equals(action))
		{
			handle_device_register_msisdn(intent);
		}
		else if (IntentAction.WebService.DEVICE_REGISTER_VERIFICATION.equals(action))
		{
			handle_device_verification(intent);
		}
		else if (IntentAction.WebService.DEVICE_CHECK_STATUS.equals(action))
		{
			handle_device_check_status(intent);
		}
		else if (IntentAction.WebService.DEVICE_UPDATE_PREFERENCES.equals(action))
		{
			handle_device_update_preferences(intent);
		}

		/* LOCATION */
		else if (IntentAction.WebService.DEVICE_LOCATION_GET.equals(action))
		{
			handle_device_location_get(intent);
		}
		else if (IntentAction.WebService.DEVICE_LOCATION_UPDATE.equals(action))
		{
			handle_device_location_update(intent);
		}

		/* INBOX */
		else if (IntentAction.WebService.DEVICE_PUSH_OUTBOUND_MESSAGE_LIST.equals(action))
		{
			handle_device_push_outbound_messages_list(intent);
		}
		else if (IntentAction.WebService.DEVICE_PUSH_OUTBOUND_MESSAGE_GET.equals(action))
		{
			handle_device_push_outbound_message_get(intent);
		}
		else if (IntentAction.WebService.DEVICE_PUSH_OUTBOUND_MESSAGE_UPDATE.equals(action))
		{
			handle_device_push_outbound_message_update(intent);
		}

		/* OUTBOX */
		else if (IntentAction.WebService.DEVICE_PUSH_INBOUND_MESSAGE_SEND.equals(action))
		{
			handle_device_push_inbound_message_send(intent);
		}
		else if (IntentAction.WebService.DEVICE_PUSH_INBOUND_MESSAGE_LIST.equals(action))
		{
			handle_device_push_inbound_messages_list(intent);
		}
		else if (IntentAction.WebService.DEVICE_PUSH_INBOUND_MESSAGE_GET.equals(action))
		{
			handle_device_push_inbound_message_get(intent);
		}


		else if ("push_outbound_message_send".equals(action))
		{
			handle_DEVELOPER_push_outbound_message_send(intent);
		}

	}


	/**
	 * Schedules a retry. the retry wait is multiplied by a factor of 2 with
	 * every successive retry. the retries are limited to 20 attempts.
	 * 
	 * @param intent
	 */
	private void queueRetry(Intent intent)
	{
		/* Do calculations for retry back off */
		//cancel retry after so many retries
		int retries = intent.getIntExtra("RETRY_NUMBER", 0);
		Log.d(TAG, "retries: " + retries);
		if (retries >= 20)
		{
			return;
		}
		else
		{
			intent.putExtra("RETRY_NUMBER", ++retries);
		}

		//Next retry
		int nextRetryIn = intent.getIntExtra("NEXT_RETRY_IN", 10);
		intent.putExtra("NEXT_RETRY_IN", nextRetryIn * 2);

		Calendar now = Calendar.getInstance();
		// define intent to run each time alarm is triggered
		PendingIntent pending = PendingIntent.getService(getApplicationContext(), 0, intent,
			PendingIntent.FLAG_CANCEL_CURRENT);

		// set retry for next retry
		now.add(Calendar.SECOND, nextRetryIn);
		long newAlarm = now.getTimeInMillis();
		AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(
			Context.ALARM_SERVICE);
		alarmManager.set(AlarmManager.RTC, newAlarm, pending);
	}

	/**
	 * Generic Panacea sendRequest.
	 * <p>
	 * Unpacks URL parameters and performs web request.
	 * <p>
	 * Tracks active webcalls by using {@link #sendRequestStarted(String)} and
	 * {@link #sendRequestEnded(String)}
	 * <p>
	 * Notifies of failure case
	 * 
	 * @param method
	 *        {@link IntentAction.WebService}
	 * @param tag
	 * @param params
	 *        {@link PMParams}
	 * @return {@link WSResponse}
	 */
	private WSResponse sendRequest(String method, String tag, PMParams params)
	{
		int result = Result.FAILURE_GENERAL;
		String resultString = PanaceaSDK.Result.GENERAL_ERROR;
		String message = "Server Error. Please try again later.";
		PMBaseResponse pmResponse = null;
		HttpResponse httpResponse = null;

		if (method == null || method.length() == 0)
		{
			return new WSResponse(Result.INVALID_METHOD);
		}

		try
		{
			HttpClient client = getHttpClient();
			//			HttpClient client = new DefaultHttpClient();
			String postURL = PMConstants.PANACEA_API_URL + "?action=" + method
				+ (params != null ? params.getURLParameters() : "");

			Log.d(TAG, "requestURL: " + postURL);
			HttpPost post = new HttpPost(postURL);

			// add header
			post.setHeader("Content-Type", "application/json");

			sendRequestStarted(method);

			//Send PreDataExecute Broadcast
			PMLocalBroadcastReceiver.sendPreDataExecute(getApplicationContext(), tag);

			// call service
			httpResponse = client.execute(post);

			sendRequestEnded(method);

			// check response code
			StatusLine statusLine = httpResponse.getStatusLine();
			int responseCode = statusLine.getStatusCode();
			if (HttpStatus.SC_OK == responseCode)
			{
				HttpEntity resEntity = httpResponse.getEntity();
				if (resEntity != null)
				{
					String responseString = EntityUtils.toString(resEntity);
					Log.d(TAG, "RESPONSE: [" + responseString + "]");

					if (responseString != null)
					{
						//parse responseString
						try
						{
							pmResponse = new PMBaseResponse(responseString);

							//return so we dont send Failure broadcast
							return new WSResponse(Result.OK, pmResponse);
						}
						catch (Exception e)
						{
							Log.d(TAG, "PARSE FAILURE!");
							e.printStackTrace();
						}
					}
				}
			}
			else
			{
				Log.d(TAG, "GENERAL FAILURE - " + responseCode + " " + statusLine.getReasonPhrase());
			}
		}
		catch (IOException e)
		{
			Log.d(TAG, "CONNECTION FAILURE!");
			e.printStackTrace();

			result = Result.FAILURE_SERVER_UNAVAILABLE;
			resultString = PanaceaSDK.Result.CONNECTION_ERROR;
			message = "Server Unavailable. Please try again later.";
		}
		catch (Exception e)
		{
			Log.d(TAG, "GENERAL FAILURE!");
			e.printStackTrace();
		}

		sendRequestEnded(method);

		//Send failure broadcast
		PMLocalBroadcastReceiver.sendPostDataFailure(getApplicationContext(), tag, resultString,
			-1, message);

		return new WSResponse(result, pmResponse);
	}


	private HttpClient getHttpClient()
	{
		Log.d(TAG, "getHttpClient()");

		try
		{
			KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			trustStore.load(null, null);

			SSLSocketFactory sf = new AllTrustingSSLSocketFactory(trustStore);
			sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

			HttpParams params = new BasicHttpParams();
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
			registry.register(new Scheme("https", sf, 443));

			ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

			return new DefaultHttpClient(ccm, params);
		}
		catch (Exception e)
		{
			return new DefaultHttpClient();
		}
	}

	/**
	 * AllTrustingSSLSocketFactory
	 * 
	 * @author Mike Ignores SSL certificate errors
	 */
	public class AllTrustingSSLSocketFactory extends SSLSocketFactory
	{
		SSLContext sslContext = SSLContext.getInstance("TLS");

		public AllTrustingSSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException,
			KeyManagementException, KeyStoreException, UnrecoverableKeyException
		{
			super(truststore);

			TrustManager tm = new X509TrustManager()
			{
				public void checkClientTrusted(java.security.cert.X509Certificate[] chain,
					String authType) throws java.security.cert.CertificateException
				{
				}

				public void checkServerTrusted(java.security.cert.X509Certificate[] chain,
					String authType) throws java.security.cert.CertificateException
				{
				}

				public java.security.cert.X509Certificate[] getAcceptedIssuers()
				{
					return null;
				}
			};

			sslContext.init(null, new TrustManager[]
				{ tm }, null);
		}

		@Override
		public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
			throws IOException, UnknownHostException
		{
			return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
		}

		@Override
		public Socket createSocket() throws IOException
		{
			return sslContext.getSocketFactory().createSocket();
		}
	}


	/* REGISTER CALLS */

	/**
	 * Starts the {@link IntentAction.WebService#DEVICE_REGISTER} WebService.
	 * 
	 * @param context
	 * @param channelKey
	 *        Push Notification Channel Key
	 * @see #handle_device_register
	 */
	public static void device_register(Context context, String channelKey)
	{
		if (context == null)
			return;

		Intent intent = new Intent(context, PMWebServiceController.class);
		intent.setAction(IntentAction.WebService.DEVICE_REGISTER);

		/* MANDATORY PARAMETERS */

		String application_key = PMSettings.getApplicationKey(context.getApplicationContext());
		String unique_device_id = PMSettings.getUniqueDeviceId(context);
		String device_manufacturer = PMSettings.getDeviceManufacturer();
		String device_model = PMSettings.getDeviceModel();
		String operating_system = PMSettings.getOpereatingSystem();
		String operating_system_version = PMSettings.getOperatingSystemVersion();
		String timezone = PMSettings.getTimezone();
		String locale = PMSettings.getLocale();
		String push_channel_id = PMSettings.getPushChannelId();
		String given_name = PMSettings.getGivenName(context);

		if (application_key == null || unique_device_id == null || device_manufacturer == null
			|| device_model == null || operating_system == null || operating_system_version == null
			|| timezone == null || locale == null || push_channel_id == null)
			return;

		intent.putExtra("application_key", application_key);
		intent.putExtra("unique_device_id", unique_device_id);
		intent.putExtra("device_manufacturer", device_manufacturer);
		intent.putExtra("device_model", device_model);
		intent.putExtra("operating_system", operating_system);
		intent.putExtra("operating_system_version", operating_system_version);
		intent.putExtra("timezone", timezone);
		intent.putExtra("locale", locale);
		intent.putExtra("push_channel_id", push_channel_id);
		intent.putExtra("given_name", given_name);

		/* OPTIONAL PARAMETERS */

		if (channelKey != null)
			intent.putExtra("channel_key", channelKey);

		context.startService(intent);
	}

	/**
	 * Processes {@link Intent} from {@link #device_register(Context, String)}
	 * 
	 * @param intent
	 * @see #device_register(Context, String)
	 */
	private void handle_device_register(Intent intent)
	{
		PMParams params = new PMParams();

		/* MANDATORY PARAMETERS */
		String application_key = intent.getStringExtra("application_key");
		String unique_device_id = intent.getStringExtra("unique_device_id");
		String device_manufacturer = intent.getStringExtra("device_manufacturer");
		String device_model = intent.getStringExtra("device_model");
		String operating_system = intent.getStringExtra("operating_system");
		String operating_system_version = intent.getStringExtra("operating_system_version");
		String timezone = intent.getStringExtra("timezone");
		String locale = intent.getStringExtra("locale");
		String push_channel_id = intent.getStringExtra("push_channel_id");
		String given_name = intent.getStringExtra("given_name");

		params.put("application_key", application_key);
		params.put("unique_device_id", unique_device_id);
		params.put("device_manufacturer", device_manufacturer);
		params.put("device_model", device_model);
		params.put("operating_system", operating_system);
		params.put("operating_system_version", operating_system_version);
		params.put("timezone", timezone);
		params.put("locale", locale);
		params.put("push_channel_id", push_channel_id);
		params.put("given_name", given_name);

		/* OPTIONAL PARAMETERS */
		String channel_key = intent.getStringExtra("channel_key");

		if (channel_key != null)
			params.put("channel_key", channel_key);

		String method = intent.getAction();
		String tag = Tag.REGISTER;

		WSResponse rsp = sendRequest(method, tag, params);

		//Success
		if (Result.OK == rsp.result)
		{
			PMBaseResponse response = rsp.pmResponse;

			//Generic response - these variables will be changed for the specific responses
			String result = PanaceaSDK.Result.PANACEA_ERROR_CODE;
			int status = response.getStatus();
			String message = response.getDescription();

			if (response.getStatus() == PMConstants.Status.OK)
			{
				Object respObject = response.getDetails();

				if (respObject instanceof PMDictionary)
				{
					PMDictionary details = (PMDictionary) respObject;
					String signature = details.getString("signature", null);
					boolean verified = details.getBool("verified", false);
					PMSettings.setDeviceSignature(signature, getApplicationContext());
					PMSettings.setVerified(verified, getApplicationContext());
					PMSettings.setDeviceConfiguration(getApplicationContext());

					//if push notification key was included
					if (channel_key != null)
					{
						PMSettings.setPushNotificationKey(channel_key, getApplicationContext());
					}
					else
					{
						String unregisteredChannelKey = GcmHelper.getRegistrationId(getApplicationContext());
						device_update_preferences(getApplicationContext(), unregisteredChannelKey, null);
					}

					if (PMSettings.isVerified(getApplicationContext()))
					{
						result = PanaceaSDK.Result.REGISTER_SUCCESS;
					}
					else
					{
						result = PanaceaSDK.Result.REQUEST_PHONE_NUMBER;
					}
				}
			}

			//Send successful broadcast
			PMLocalBroadcastReceiver.sendPostDataSuccess(getApplicationContext(), tag, result,
				status, message);
		}

		//no retry for register
	}

	/**
	 * Starts the {@link IntentAction.WebService#DEVICE_REGISTER_MSISDN}
	 * WebService.
	 * 
	 * @param context
	 * @param phoneNumber
	 */
	public static void device_register_msisdn(Context context, String phoneNumber)
	{
		if (context == null || phoneNumber == null)
			return;

		Intent intent = new Intent(context, PMWebServiceController.class);
		intent.setAction(IntentAction.WebService.DEVICE_REGISTER_MSISDN);

		/* MANDATORY PARAMETERS */
		String appKey = PMSettings.getApplicationKey(context.getApplicationContext());
		String devSignature = PMSettings.getDeviceSignature(context.getApplicationContext());

		if (appKey == null || devSignature == null)
			return;

		intent.putExtra("application_key", appKey);
		intent.putExtra("device_signature", devSignature);
		intent.putExtra("msisdn", phoneNumber);

		context.startService(intent);
	}

	/**
	 * Processes {@link Intent} from
	 * {@link #device_register_msisdn(Context, String)}
	 * 
	 * @param intent
	 * @see #device_register_msisdn(Context, String)
	 */
	private void handle_device_register_msisdn(Intent intent)
	{
		PMParams params = new PMParams();

		/* MANDATORY PARAMETERS */
		String application_key = intent.getStringExtra("application_key");
		String device_signature = intent.getStringExtra("device_signature");
		String msisdn = intent.getStringExtra("msisdn");

		params.put("application_key", application_key);
		params.put("device_signature", device_signature);
		params.put("msisdn", msisdn);

		String method = intent.getAction();
		String tag = Tag.REGISTER;

		WSResponse rsp = sendRequest(method, tag, params);

		//Success
		if (Result.OK == rsp.result)
		{
			PMBaseResponse response = rsp.pmResponse;

			//Generic response - these variables will be changed for the specific responses
			String result = PanaceaSDK.Result.PANACEA_ERROR_CODE;
			int status = response.getStatus();
			String message = response.getDescription();

			//save phone number after success
			if (response.getStatus() == PMConstants.Status.OK)
			{
				//save phone number
				PMSettings.setPhoneNumber(msisdn, getApplicationContext());

				result = PanaceaSDK.Result.REQUEST_VERIFICATION_CODE;
			}

			//Send successful broadcast
			PMLocalBroadcastReceiver.sendPostDataSuccess(getApplicationContext(), tag, result,
				status, message);
		}

		//no retry for register
	}

	/**
	 * Starts the {@link IntentAction.WebService#DEVICE_REGISTER_VERIFICATION}
	 * WebService.
	 * 
	 * @param context
	 * @param verificationCode
	 */
	public static void device_verification(Context context, String verificationCode)
	{
		if (context == null || verificationCode == null)
			return;

		Intent intent = new Intent(context, PMWebServiceController.class);
		intent.setAction(IntentAction.WebService.DEVICE_REGISTER_VERIFICATION);

		/* MANDATORY PARAMETERS */
		String appKey = PMSettings.getApplicationKey(context.getApplicationContext());
		String devSignature = PMSettings.getDeviceSignature(context.getApplicationContext());

		if (appKey == null || devSignature == null)
			return;

		intent.putExtra("application_key", appKey);
		intent.putExtra("device_signature", devSignature);
		intent.putExtra("verification_code", verificationCode);

		context.startService(intent);
	}

	/**
	 * Processes {@link Intent} from
	 * {@link #device_verification(Context, String)}
	 * 
	 * @param intent
	 * @see #device_verification(Context, String)
	 */
	private void handle_device_verification(Intent intent)
	{
		PMParams params = new PMParams();

		/* MANDATORY PARAMETERS */
		String application_key = intent.getStringExtra("application_key");
		String device_signature = intent.getStringExtra("device_signature");
		String verification_code = intent.getStringExtra("verification_code");

		params.put("application_key", application_key);
		params.put("device_signature", device_signature);
		params.put("verification_code", verification_code);

		String method = intent.getAction();
		String tag = Tag.REGISTER;

		WSResponse rsp = sendRequest(method, tag, params);

		//Success
		if (Result.OK == rsp.result)
		{
			PMBaseResponse response = rsp.pmResponse;

			//Generic response - these variables will be changed for the specific responses
			String result = PanaceaSDK.Result.PANACEA_ERROR_CODE;
			int status = response.getStatus();
			String message = response.getDescription();

			//set verified and update channel_key if necessary 
			if (response.getStatus() == PMConstants.Status.OK)
			{
				PMSettings.setVerified(true, getApplicationContext());

				//				String channelKey = GcmHelper.getGCMRegistrationId(parent, null);
//				String channelKey = GcmHelper.getRegistrationId(getApplicationContext());
//				device_update_preferences(getApplicationContext(), channelKey, null);

				//registration good go to inbox
				result = PanaceaSDK.Result.REGISTER_SUCCESS;
			}
			else if (response.getStatus() == PMConstants.Status.GENERIC_ERROR)
			{
				result = PanaceaSDK.Result.INVALID_VERIFICATION_CODE;
			}

			//Send successful broadcast
			PMLocalBroadcastReceiver.sendPostDataSuccess(getApplicationContext(), tag, result,
				status, message);
		}

		//no retry for register
	}

	/**
	 * Starts the {@link IntentAction.WebService#DEVICE_CHECK_STATUS}
	 * WebService.
	 * 
	 * @param context
	 */
	public static void device_check_status(Context context)
	{
		if (context == null)
			return;

		Intent intent = new Intent(context, PMWebServiceController.class);
		intent.setAction(IntentAction.WebService.DEVICE_CHECK_STATUS);

		/* MANDATORY PARAMETERS */
		String appKey = PMSettings.getApplicationKey(context.getApplicationContext());
		String devSignature = PMSettings.getDeviceSignature(context.getApplicationContext());

		if (appKey == null || devSignature == null)
			return;

		intent.putExtra("application_key", appKey);
		intent.putExtra("device_signature", devSignature);

		context.startService(intent);
	}

	/**
	 * Processes {@link Intent} from {@link #device_check_status(Context)}
	 * 
	 * @param intent
	 * @see #device_check_status(Context)
	 */
	private void handle_device_check_status(Intent intent)
	{
		PMParams params = new PMParams();

		/* MANDATORY PARAMETERS */
		String application_key = intent.getStringExtra("application_key");
		String device_signature = intent.getStringExtra("device_signature");

		params.put("application_key", application_key);
		params.put("device_signature", device_signature);

		String method = intent.getAction();
		String tag = Tag.REGISTER;

		WSResponse rsp = sendRequest(method, tag, params);

		//Success
		if (Result.OK == rsp.result)
		{
			PMBaseResponse response = rsp.pmResponse;

			//Generic response - these variables will be changed for the specific responses
			String result = PanaceaSDK.Result.PANACEA_ERROR_CODE;
			int status = response.getStatus();
			String message = response.getDescription();

			//save messages device settings after a successful response
			if (response.getStatus() == PMConstants.Status.OK)
			{
				//				String channelKey = GcmHelper.getGCMRegistrationId(parent, null);
				String channelKey = GcmHelper.getRegistrationId(getApplicationContext());
				device_update_preferences(getApplicationContext(), channelKey, null);

				//registration good go to inbox
				result = PanaceaSDK.Result.REGISTER_SUCCESS;
			}
			else
			{
				//clear DB
				PMDatabaseCipherHelper.getInstance(getApplicationContext()).deleteMessageCache();

				//clear settings and dont allow access to app
				PMSettings.clearSettings(getApplicationContext());

				result = PanaceaSDK.Result.NOT_VERIFIED;
				message = "This Device is not verified. Please re-verify to proceed";
			}

			//Send successful broadcast
			PMLocalBroadcastReceiver.sendPostDataSuccess(getApplicationContext(), tag, result,
				status, message);
		}

		//Queue retry if connection error
		else if (Result.FAILURE_SERVER_UNAVAILABLE == rsp.result)
		{
			queueRetry(intent);
		}
	}

	/**
	 * Starts the {@link IntentAction.WebService#DEVICE_CHECK_STATUS}
	 * WebService.
	 * <p>
	 * Updates the devices Push Notification Channel Key. Should be called if
	 * the device gets a new GCM Registration ID. Can only be called on Verified
	 * devices.
	 * 
	 * @param context
	 * @param notificationKey
	 *        the GCM Registration ID for push notifications
	 */
	public static void device_update_preferences(Context context, String notificationKey,
		String given_name)
	{
		if (context == null)
			return;

		if (PMUtils.isBlankOrNull(notificationKey))
			notificationKey = null;

		//only if its different from current key
		if (notificationKey != null
			&& notificationKey.equals(PMSettings.getPushNotificationKey(context)))
			notificationKey = null;
		
		//if there is nothing being set in the update dont send it
		if(notificationKey == null && given_name ==null)
			return;

		Intent intent = new Intent(context, PMWebServiceController.class);
		intent.setAction(IntentAction.WebService.DEVICE_UPDATE_PREFERENCES);

		/* MANDATORY PARAMETERS */
		String appKey = PMSettings.getApplicationKey(context.getApplicationContext());
		String devSignature = PMSettings.getDeviceSignature(context.getApplicationContext());

		if (appKey == null || devSignature == null)
			return;

		intent.putExtra("application_key", appKey);
		intent.putExtra("device_signature", devSignature);


		if (notificationKey != null)
			intent.putExtra("channel_key", notificationKey);

		if (given_name != null)
			intent.putExtra("given_name", given_name);

		context.startService(intent);
	}

	/**
	 * Processes {@link Intent} from
	 * {@link #device_update_preferences(Context, String, String)}
	 * 
	 * @param intent
	 * @see #device_update_preferences(Context, String, String)
	 */
	private void handle_device_update_preferences(Intent intent)
	{
		PMParams params = new PMParams();

		/* MANDATORY PARAMETERS */
		String application_key = intent.getStringExtra("application_key");
		String device_signature = intent.getStringExtra("device_signature");
		String channel_key = intent.getStringExtra("channel_key");
		String given_name = intent.getStringExtra("given_name");

		params.put("application_key", application_key);
		params.put("device_signature", device_signature);
		params.put("channel_key", channel_key);
		params.put("given_name", given_name);

		String method = intent.getAction();
		String tag = Tag.REGISTER;

		WSResponse rsp = sendRequest(method, tag, params);

		//Success
		if (Result.OK == rsp.result)
		{
			PMBaseResponse response = rsp.pmResponse;

			//Generic response - these variables will be changed for the specific responses
			String result = PanaceaSDK.Result.PANACEA_ERROR_CODE;
			int status = response.getStatus();
			String message = response.getDescription();

			//save push notification key
			if (response.getStatus() == PMConstants.Status.OK)
			{
				//if given_name was updated
				if (given_name != null)
					PMSettings.setGivenName(getApplicationContext(), given_name);

				//if push notification key was included
				if (channel_key != null)
				{
					PMSettings.setPushNotificationKey(channel_key, getApplicationContext());

					//registration good go to inbox
					result = PanaceaSDK.Result.PUSH_KEY_UPDATED;
				}
				else
				{
					result = PanaceaSDK.Result.PREFERENCE_UPDATED;
				}
			}

			//Send successful broadcast
			PMLocalBroadcastReceiver.sendPostDataSuccess(getApplicationContext(), tag, result,
				status, message);
		}

		//no retry for register
	}

	/* LOCATION CALLS */

	/**
	 * Starts the {@link IntentAction.WebService#DEVICE_LOCATION_GET}
	 * WebService.
	 * 
	 * @param context
	 */
	public static void device_location_get(Context context)
	{
		if (context == null)
			return;

		Intent intent = new Intent(context, PMWebServiceController.class);
		intent.setAction(IntentAction.WebService.DEVICE_LOCATION_GET);

		/* MANDATORY PARAMETERS */
		String appKey = PMSettings.getApplicationKey(context.getApplicationContext());
		String devSignature = PMSettings.getDeviceSignature(context.getApplicationContext());

		if (appKey == null || devSignature == null)
			return;

		intent.putExtra("application_key", appKey);
		intent.putExtra("device_signature", devSignature);

		context.startService(intent);
	}

	/**
	 * Processes {@link Intent} from {@link #device_location_get(Context)}
	 * 
	 * @param intent
	 * @see #device_location_get(Context)
	 */
	private void handle_device_location_get(Intent intent)
	{
		PMParams params = new PMParams();

		/* MANDATORY PARAMETERS */
		String application_key = intent.getStringExtra("application_key");
		String device_signature = intent.getStringExtra("device_signature");

		params.put("application_key", application_key);
		params.put("device_signature", device_signature);

		String method = intent.getAction();
		String tag = Tag.LOCATION;

		WSResponse rsp = sendRequest(method, tag, params);

		//Success
		if (Result.OK == rsp.result)
		{
			PMBaseResponse response = rsp.pmResponse;

			//Generic response - these variables will be changed for the specific responses
			String result = PanaceaSDK.Result.PANACEA_ERROR_CODE;
			int status = response.getStatus();
			String message = response.getDescription();

			//save messages device settings after a successful response
			if (response.getStatus() == PMConstants.Status.OK)
			{
				result = PanaceaSDK.Result.LOCATION_UPDATED;
			}

			//Send successful broadcast
			PMLocalBroadcastReceiver.sendPostDataSuccess(getApplicationContext(), tag, result,
				status, message);
		}

		//no retry
	}

	/**
	 * Starts the {@link IntentAction.WebService#DEVICE_LOCATION_UPDATE}
	 * WebService.
	 * 
	 * @param context
	 * @param location
	 */
	public static void device_location_update(Context context, Location location)
	{
		if (context == null || location == null)
			return;

		Intent intent = new Intent(context, PMWebServiceController.class);
		intent.setAction(IntentAction.WebService.DEVICE_LOCATION_UPDATE);

		/* MANDATORY PARAMETERS */
		String appKey = PMSettings.getApplicationKey(context.getApplicationContext());
		String devSignature = PMSettings.getDeviceSignature(context.getApplicationContext());

		if (appKey == null || devSignature == null)
			return;

		intent.putExtra("application_key", appKey);
		intent.putExtra("device_signature", devSignature);
		intent.putExtra("longitude", location.getLongitude() + "");
		intent.putExtra("latitude", location.getLatitude() + "");
		intent.putExtra("source",
			LocationManager.GPS_PROVIDER.equals(location.getProvider()) ? "gps" : "network");
		intent.putExtra("accuracy", location.getAccuracy() + "");

		context.startService(intent);
	}

	/**
	 * Processes {@link Intent} from
	 * {@link #device_location_update(Context, Location)}
	 * 
	 * @param intent
	 * @see #device_location_update(Context, Location)
	 */
	private void handle_device_location_update(Intent intent)
	{
		PMParams params = new PMParams();

		/* MANDATORY PARAMETERS */
		String application_key = intent.getStringExtra("application_key");
		String device_signature = intent.getStringExtra("device_signature");
		String longitude = intent.getStringExtra("longitude");
		String latitude = intent.getStringExtra("latitude");
		String source = intent.getStringExtra("source");
		String accuracy = intent.getStringExtra("accuracy");

		params.put("application_key", application_key);
		params.put("device_signature", device_signature);
		params.put("longitude", longitude);
		params.put("latitude", latitude);
		params.put("source", source);
		params.put("accuracy", accuracy);

		String method = intent.getAction();
		String tag = Tag.LOCATION;

		WSResponse rsp = sendRequest(method, tag, params);

		//Success
		if (Result.OK == rsp.result)
		{
			PMBaseResponse response = rsp.pmResponse;

			//Generic response - these variables will be changed for the specific responses
			String result = PanaceaSDK.Result.PANACEA_ERROR_CODE;
			int status = response.getStatus();
			String message = response.getDescription();

			//save messages device settings after a successful response
			if (response.getStatus() == PMConstants.Status.OK)
			{
				result = PanaceaSDK.Result.LOCATION_UPDATED;
			}

			//Send successful broadcast
			PMLocalBroadcastReceiver.sendPostDataSuccess(getApplicationContext(), tag, result,
				status, message);
		}

		//no retry
	}

	/* INBOX MESSAGE CALLS */

	/**
	 * Starts the
	 * {@link IntentAction.WebService#DEVICE_PUSH_OUTBOUND_MESSAGE_LIST}
	 * WebService.
	 * 
	 * @param context
	 */
	public static void device_push_outbound_messages_list(Context context, Integer last_id,
		Date start_date, Date end_date, Integer limit, Integer page, String sort, String direction)
	{
		if (context == null)
			return;

		Intent intent = new Intent(context, PMWebServiceController.class);
		intent.setAction(IntentAction.WebService.DEVICE_PUSH_OUTBOUND_MESSAGE_LIST);

		/* MANDATORY PARAMETERS */
		String appKey = PMSettings.getApplicationKey(context.getApplicationContext());
		String devSignature = PMSettings.getDeviceSignature(context.getApplicationContext());

		if (appKey == null || devSignature == null)
			return;

		intent.putExtra("application_key", appKey);
		intent.putExtra("device_signature", devSignature);

		/* OPTIONAL PARAMETERS */

		final String STRING_DATE_FORMAT = "yyyy-MM-dd";

		if (last_id != null)
			intent.putExtra("last_id", last_id.toString());
		if (start_date != null)
			intent.putExtra("start_date", PMUtils.dateToString(STRING_DATE_FORMAT, start_date));
		if (end_date != null)
			intent.putExtra("end_date", PMUtils.dateToString(STRING_DATE_FORMAT, end_date));
		if (limit != null)
			intent.putExtra("limit", limit + "");
		if (page != null)
			intent.putExtra("page", page + "");
		if (sort != null)
			intent.putExtra("sort", sort);
		if (direction != null)
			intent.putExtra("direction", direction);

		context.startService(intent);
	}

	/**
	 * Processes {@link Intent} from
	 * {@link #device_push_outbound_messages_list(Context, Integer, Date, Date, Integer, Integer, String, String)}
	 * 
	 * @param intent
	 * @see #device_push_outbound_messages_list(Context, Integer, Date, Date,
	 *      Integer, Integer, String, String)
	 */
	private void handle_device_push_outbound_messages_list(Intent intent)
	{
		PMParams params = new PMParams();

		/* MANDATORY PARAMETERS */
		String application_key = intent.getStringExtra("application_key");
		String device_signature = intent.getStringExtra("device_signature");

		params.put("application_key", application_key);
		params.put("device_signature", device_signature);

		/* OPTIONAL PARAMETERS */

		String last_id = intent.getStringExtra("last_id");
		String start_date = intent.getStringExtra("start_date");
		String end_date = intent.getStringExtra("end_date");
		String limit = intent.getStringExtra("limit");
		String page = intent.getStringExtra("page");
		String sort = intent.getStringExtra("sort");
		String direction = intent.getStringExtra("direction");

		if (last_id != null)
			params.put("last_id", last_id);
		if (start_date != null)
			params.put("start_date", start_date);
		if (end_date != null)
			params.put("end_date", end_date);
		if (limit != null)
			params.put("limit", limit);
		if (page != null)
			params.put("page", page);
		if (sort != null)
			params.put("sort", sort);
		if (direction != null)
			params.put("direction", direction);

		String method = intent.getAction();
		String tag = Tag.MESSAGE_RECEIVED;

		WSResponse rsp = sendRequest(method, tag, params);

		//Success
		if (Result.OK == rsp.result)
		{
			PMBaseResponse response = rsp.pmResponse;

			//Generic response - these variables will be changed for the specific responses
			String result = PanaceaSDK.Result.PANACEA_ERROR_CODE;
			int status = response.getStatus();
			String message = response.getDescription();

			//save messages device settings after a successful response
			if (response.getStatus() == PMConstants.Status.OK)
			{
				Object respObject = response.getDetails();

				if (respObject instanceof PMArray)
				{
					PMArray details = (PMArray) respObject;
					List<PMMessage> messages = PMReceivedMessage
						.parseReceivedMessagesArray(details);

					//save to DB
					PMDatabaseCipherHelper.getInstance(getApplicationContext()).addMessages(messages);
					checkNotifications();

					PMPagination p = response.getPagination();
					if (p.hasMorePages())
					{
						device_push_outbound_messages_list(getApplicationContext(), null, null,
							null, p.getLimit(), p.getPage() + 1, null, null);
					}

					result = PanaceaSDK.Result.MESSAGE_UPDATED_RECEIVED;
				}
			}

			//Send successful broadcast
			PMLocalBroadcastReceiver.sendPostDataSuccess(getApplicationContext(), tag, result,
				status, message);
		}

		//Queue retry if connection error
		else if (Result.FAILURE_SERVER_UNAVAILABLE == rsp.result)
		{
			queueRetry(intent);
		}

	}

	/**
	 * Starts the
	 * {@link IntentAction.WebService#DEVICE_PUSH_OUTBOUND_MESSAGE_UPDATE}
	 * WebService.
	 * <p>
	 * Update the message. Currently this function marks the message as being
	 * read
	 * 
	 * @param context
	 * @param outboundMessageId
	 *        id of the message to mark as read
	 */
	public static void device_push_outbound_message_update(Context context,
		Integer outboundMessageId)
	{
		if (context == null || outboundMessageId == null)
			return;

		Intent intent = new Intent(context, PMWebServiceController.class);
		intent.setAction(IntentAction.WebService.DEVICE_PUSH_OUTBOUND_MESSAGE_UPDATE);

		/* MANDATORY PARAMETERS */
		String appKey = PMSettings.getApplicationKey(context.getApplicationContext());
		String devSignature = PMSettings.getDeviceSignature(context.getApplicationContext());

		if (appKey == null || devSignature == null)
			return;

		intent.putExtra("application_key", appKey);
		intent.putExtra("device_signature", devSignature);
		intent.putExtra("outbound_message_id", outboundMessageId + "");

		context.startService(intent);

		//mark as read immediately while webservice completes
		PMDatabaseCipherHelper.getInstance(context.getApplicationContext()).markMessageAsRead(
			outboundMessageId);
		//notify immediately
		PMLocalBroadcastReceiver.sendPostDataSuccess(context.getApplicationContext(),
			Tag.MESSAGE_RECEIVED, PanaceaSDK.Result.MESSAGE_UPDATED_RECEIVED, 0,
			"Message Updated pending WebService");
	}

	/**
	 * Processes {@link Intent} from
	 * {@link #device_push_outbound_message_update(Context, Integer)}
	 * 
	 * @param intent
	 * @see #device_push_outbound_message_update(Context, Integer)
	 */
	private void handle_device_push_outbound_message_update(Intent intent)
	{
		PMParams params = new PMParams();

		/* MANDATORY PARAMETERS */
		String application_key = intent.getStringExtra("application_key");
		String device_signature = intent.getStringExtra("device_signature");
		String outbound_message_id = intent.getStringExtra("outbound_message_id");

		params.put("application_key", application_key);
		params.put("device_signature", device_signature);
		params.put("outbound_message_id", outbound_message_id);

		String method = intent.getAction();
		String tag = Tag.MESSAGE_RECEIVED;

		WSResponse rsp = sendRequest(method, tag, params);

		//Success
		if (Result.OK == rsp.result)
		{
			PMBaseResponse response = rsp.pmResponse;

			//Generic response - these variables will be changed for the specific responses
			String result = PanaceaSDK.Result.PANACEA_ERROR_CODE;
			int status = response.getStatus();
			String message = response.getDescription();

			//save messages device settings after a successful response
			if (response.getStatus() == PMConstants.Status.OK)
			{
				int updatedMsgId = Integer.parseInt(outbound_message_id);

				//request update from server
				device_push_outbound_message_get(getApplicationContext(), updatedMsgId);
				return; //don't send broadcast until message is updated
			}

			//Send successful broadcast
			PMLocalBroadcastReceiver.sendPostDataSuccess(getApplicationContext(), tag, result,
				status, message);
		}

		//Queue retry if connection error
		else if (Result.FAILURE_SERVER_UNAVAILABLE == rsp.result)
		{
			queueRetry(intent);
		}

	}

	/**
	 * Starts the
	 * {@link IntentAction.WebService#DEVICE_PUSH_OUTBOUND_MESSAGE_GET}
	 * WebService.
	 * <p>
	 * Returns the details on outbound message for the given outbound id
	 * 
	 * @param context
	 * @param outboundMessageId
	 *        id of the message to retrieve
	 */
	public static void device_push_outbound_message_get(Context context, Integer outboundMessageId)
	{
		if (context == null || outboundMessageId == null)
			return;

		Intent intent = new Intent(context, PMWebServiceController.class);
		intent.setAction(IntentAction.WebService.DEVICE_PUSH_OUTBOUND_MESSAGE_GET);

		/* MANDATORY PARAMETERS */
		String appKey = PMSettings.getApplicationKey(context.getApplicationContext());
		String devSignature = PMSettings.getDeviceSignature(context.getApplicationContext());

		if (appKey == null || devSignature == null)
			return;

		intent.putExtra("application_key", appKey);
		intent.putExtra("device_signature", devSignature);
		intent.putExtra("outbound_message_id", outboundMessageId + "");

		context.startService(intent);
	}

	/**
	 * Processes {@link Intent} from
	 * {@link #device_push_outbound_message_get(Context, Integer)}
	 * 
	 * @param intent
	 * @see #device_push_outbound_message_get(Context, Integer)
	 */
	private void handle_device_push_outbound_message_get(Intent intent)
	{
		PMParams params = new PMParams();

		/* MANDATORY PARAMETERS */
		String application_key = intent.getStringExtra("application_key");
		String device_signature = intent.getStringExtra("device_signature");
		String outbound_message_id = intent.getStringExtra("outbound_message_id");

		params.put("application_key", application_key);
		params.put("device_signature", device_signature);
		params.put("outbound_message_id", outbound_message_id);

		String method = intent.getAction();
		String tag = Tag.MESSAGE_RECEIVED;

		WSResponse rsp = sendRequest(method, tag, params);

		//Success
		if (Result.OK == rsp.result)
		{
			PMBaseResponse response = rsp.pmResponse;

			//Generic response - these variables will be changed for the specific responses
			String result = PanaceaSDK.Result.PANACEA_ERROR_CODE;
			int status = response.getStatus();
			String message = response.getDescription();

			//save messages device settings after a successful response
			if (response.getStatus() == PMConstants.Status.OK)
			{
				Object respObject = response.getDetails();

				if (respObject instanceof PMDictionary)
				{
					PMDictionary details = (PMDictionary) respObject;
					PMReceivedMessage msg = new PMReceivedMessage(details);

					PMDatabaseCipherHelper.getInstance(getApplicationContext()).addReceivedMessage(msg);
					checkNotifications();

					result = PanaceaSDK.Result.MESSAGE_UPDATED_RECEIVED;
				}
			}

			//Send successful broadcast
			PMLocalBroadcastReceiver.sendPostDataSuccess(getApplicationContext(), tag, result,
				status, message);
		}

		//Queue retry if connection error
		else if (Result.FAILURE_SERVER_UNAVAILABLE == rsp.result)
		{
			queueRetry(intent);
		}
	}


	/* OUTBOX MESSAGE CALLS */

	/**
	 * Starts the
	 * {@link IntentAction.WebService#DEVICE_PUSH_INBOUND_MESSAGE_SEND}
	 * WebService.
	 * 
	 * @param context
	 * @param message
	 * @param messageID
	 * @param batchID
	 * @param threadID
	 */
	public static void device_push_inbound_message_send(Context context, String message,
		Integer messageID, Integer batchID, String threadID)
	{
		if (context == null || PMUtils.isBlankOrNull(message))
			return;

		Intent intent = new Intent(context, PMWebServiceController.class);
		intent.setAction(IntentAction.WebService.DEVICE_PUSH_INBOUND_MESSAGE_SEND);

		/* MANDATORY PARAMETERS */
		String appKey = PMSettings.getApplicationKey(context.getApplicationContext());
		String devSignature = PMSettings.getDeviceSignature(context.getApplicationContext());

		if (appKey == null || devSignature == null)
			return;

		intent.putExtra("application_key", appKey);
		intent.putExtra("device_signature", devSignature);
		intent.putExtra("data", message);

		/* OPTIONAL PARAMETERS */

		if (messageID != null)
			intent.putExtra("outbound_message_id", messageID.toString());
		if (batchID != null)
			intent.putExtra("outbound_message_batch_id", batchID.toString());
		if (threadID != null)
			intent.putExtra("thread_id", threadID);

		context.startService(intent);
	}

	/**
	 * Processes {@link Intent} from
	 * {@link #device_push_inbound_message_send(Context, String, Integer, Integer, String)}
	 * 
	 * @param intent
	 * @see #device_push_inbound_message_send(Context, String, Integer, Integer,
	 *      String)
	 */
	private void handle_device_push_inbound_message_send(Intent intent)
	{
		PMParams params = new PMParams();

		/* MANDATORY PARAMETERS */
		String application_key = intent.getStringExtra("application_key");
		String device_signature = intent.getStringExtra("device_signature");
		String data = intent.getStringExtra("data");

		params.put("application_key", application_key);
		params.put("device_signature", device_signature);
		params.put("data", data);

		/* OPTIONAL PARAMETERS */

		String outbound_message_id = intent.getStringExtra("outbound_message_id");
		String outbound_message_batch_id = intent.getStringExtra("outbound_message_batch_id");
		String thread_id = intent.getStringExtra("thread_id");

		if (outbound_message_id != null)
			params.put("outbound_message_id", outbound_message_id);
		if (outbound_message_batch_id != null)
			params.put("outbound_message_batch_id", outbound_message_batch_id);
		if (thread_id != null)
			params.put("thread_id", thread_id);

		String method = intent.getAction();
		String tag = Tag.MESSAGE_SENT;

		WSResponse rsp = sendRequest(method, tag, params);

		//Success
		if (Result.OK == rsp.result)
		{
			PMBaseResponse response = rsp.pmResponse;

			//Generic response - these variables will be changed for the specific responses
			String result = PanaceaSDK.Result.PANACEA_ERROR_CODE;
			int status = response.getStatus();
			String message = response.getDescription();

			if (response.getStatus() == PMConstants.Status.OK)
			{
				Object respObject = response.getDetails();

				if (respObject instanceof PMDictionary)
				{
					PMDictionary details = (PMDictionary) respObject;
					int sentId = details.getInt("id", -1);

					if (sentId != -1)
					{
						device_push_inbound_message_get(getApplicationContext(), sentId);
						return; //don't send broadcast until message is updated 
					}

				}
			}

			//Send successful broadcast
			PMLocalBroadcastReceiver.sendPostDataSuccess(getApplicationContext(), tag, result,
				status, message);
		}

		//Queue retry if connection error
		else if (Result.FAILURE_SERVER_UNAVAILABLE == rsp.result)
		{
			queueRetry(intent);
		}
	}

	/**
	 * Starts the
	 * {@link IntentAction.WebService#DEVICE_PUSH_INBOUND_MESSAGE_LIST}
	 * WebService.
	 * 
	 * @param context
	 * @param last_id
	 * @param start_date
	 * @param end_date
	 * @param limit
	 * @param page
	 * @param sort
	 * @param direction
	 */
	public static void device_push_inbound_messages_list(Context context, Integer last_id,
		Date start_date, Date end_date, Integer limit, Integer page, String sort, String direction)
	{
		if (context == null)
			return;

		Intent intent = new Intent(context, PMWebServiceController.class);
		intent.setAction(IntentAction.WebService.DEVICE_PUSH_INBOUND_MESSAGE_LIST);

		/* MANDATORY PARAMETERS */
		String appKey = PMSettings.getApplicationKey(context.getApplicationContext());
		String devSignature = PMSettings.getDeviceSignature(context.getApplicationContext());

		if (appKey == null || devSignature == null)
			return;

		intent.putExtra("application_key", appKey);
		intent.putExtra("device_signature", devSignature);

		/* OPTIONAL PARAMETERS */

		final String STRING_DATE_FORMAT = "yyyy-MM-dd";

		if (last_id != null)
			intent.putExtra("last_id", last_id.toString());
		if (start_date != null)
			intent.putExtra("start_date", PMUtils.dateToString(STRING_DATE_FORMAT, start_date));
		if (end_date != null)
			intent.putExtra("end_date", PMUtils.dateToString(STRING_DATE_FORMAT, end_date));
		if (limit != null)
			intent.putExtra("limit", limit + "");
		if (page != null)
			intent.putExtra("page", page + "");
		if (sort != null)
			intent.putExtra("sort", sort);
		if (direction != null)
			intent.putExtra("direction", direction);

		context.startService(intent);
	}

	/**
	 * Processes {@link Intent} from
	 * {@link #device_push_inbound_messages_list(Context, Integer, Date, Date, Integer, Integer, String, String)}
	 * 
	 * @param intent
	 * @see #device_push_inbound_messages_list(Context, Integer, Date, Date,
	 *      Integer, Integer, String, String)
	 */
	private void handle_device_push_inbound_messages_list(Intent intent)
	{
		PMParams params = new PMParams();

		/* MANDATORY PARAMETERS */
		String application_key = intent.getStringExtra("application_key");
		String device_signature = intent.getStringExtra("device_signature");

		params.put("application_key", application_key);
		params.put("device_signature", device_signature);

		/* OPTIONAL PARAMETERS */

		String last_id = intent.getStringExtra("last_id");
		String start_date = intent.getStringExtra("start_date");
		String end_date = intent.getStringExtra("end_date");
		String limit = intent.getStringExtra("limit");
		String page = intent.getStringExtra("page");
		String sort = intent.getStringExtra("sort");
		String direction = intent.getStringExtra("direction");

		if (last_id != null)
			params.put("last_id", last_id);
		if (start_date != null)
			params.put("start_date", start_date);
		if (end_date != null)
			params.put("end_date", end_date);
		if (limit != null)
			params.put("limit", limit);
		if (page != null)
			params.put("page", page);
		if (sort != null)
			params.put("sort", sort);
		if (direction != null)
			params.put("direction", direction);

		String method = intent.getAction();
		String tag = Tag.MESSAGE_SENT;

		WSResponse rsp = sendRequest(method, tag, params);

		//Success
		if (Result.OK == rsp.result)
		{
			PMBaseResponse response = rsp.pmResponse;

			//Generic response - these variables will be changed for the specific responses
			String result = PanaceaSDK.Result.PANACEA_ERROR_CODE;
			int status = response.getStatus();
			String message = response.getDescription();

			//save messages device settings after a successful response
			if (response.getStatus() == PMConstants.Status.OK)
			{
				Object respObject = response.getDetails();

				if (respObject instanceof PMArray)
				{
					PMArray details = (PMArray) respObject;
					List<PMMessage> messages = PMSentMessage.parseSentMessagesArray(details);
					PMDatabaseCipherHelper.getInstance(getApplicationContext()).addMessages(messages);

					PMPagination p = response.getPagination();
					if (p.hasMorePages())
					{
						device_push_inbound_messages_list(getApplicationContext(), null, null,
							null, p.getLimit(), p.getPage() + 1, null, null);
					}

					result = PanaceaSDK.Result.MESSAGE_UPDATED_SENT;
				}
			}

			//Send successful broadcast
			PMLocalBroadcastReceiver.sendPostDataSuccess(getApplicationContext(), tag, result,
				status, message);
		}

		//Queue retry if connection error
		else if (Result.FAILURE_SERVER_UNAVAILABLE == rsp.result)
		{
			queueRetry(intent);
		}

	}

	/**
	 * Starts the
	 * {@link IntentAction.WebService#DEVICE_PUSH_INBOUND_MESSAGE_GET}
	 * WebService.
	 * 
	 * @param context
	 * @param inboundMessageId
	 */
	public static void device_push_inbound_message_get(Context context, Integer inboundMessageId)
	{
		if (context == null || inboundMessageId == null)
			return;

		Intent intent = new Intent(context, PMWebServiceController.class);
		intent.setAction(IntentAction.WebService.DEVICE_PUSH_INBOUND_MESSAGE_GET);

		/* MANDATORY PARAMETERS */
		String appKey = PMSettings.getApplicationKey(context.getApplicationContext());
		String devSignature = PMSettings.getDeviceSignature(context.getApplicationContext());

		if (appKey == null || devSignature == null)
			return;

		intent.putExtra("application_key", appKey);
		intent.putExtra("device_signature", devSignature);
		intent.putExtra("inbound_message_id", inboundMessageId + "");

		context.startService(intent);
	}

	/**
	 * Processes {@link Intent} from
	 * {@link #device_push_inbound_message_get(Context, Integer)}
	 * 
	 * @param intent
	 * @see #device_push_inbound_message_get(Context, Integer)
	 */
	private void handle_device_push_inbound_message_get(Intent intent)
	{
		PMParams params = new PMParams();

		/* MANDATORY PARAMETERS */
		String application_key = intent.getStringExtra("application_key");
		String device_signature = intent.getStringExtra("device_signature");
		String inbound_message_id = intent.getStringExtra("inbound_message_id");

		params.put("application_key", application_key);
		params.put("device_signature", device_signature);
		params.put("inbound_message_id", inbound_message_id);

		String method = intent.getAction();
		String tag = Tag.MESSAGE_SENT;

		WSResponse rsp = sendRequest(method, tag, params);

		//Success
		if (Result.OK == rsp.result)
		{
			PMBaseResponse response = rsp.pmResponse;

			//Generic response - these variables will be changed for the specific responses
			String result = PanaceaSDK.Result.PANACEA_ERROR_CODE;
			int status = response.getStatus();
			String message = response.getDescription();

			if (response.getStatus() == PMConstants.Status.OK)
			{
				Object respObject = response.getDetails();

				if (respObject instanceof PMDictionary)
				{
					PMDictionary details = (PMDictionary) respObject;
					PMSentMessage msg = new PMSentMessage(details);

					PMDatabaseCipherHelper.getInstance(getApplicationContext()).addSentMessage(msg);

					result = PanaceaSDK.Result.MESSAGE_UPDATED_SENT;
				}
			}

			//Send successful broadcast
			PMLocalBroadcastReceiver.sendPostDataSuccess(getApplicationContext(), tag, result,
				status, message);
		}

		//Queue retry if connection error
		else if (Result.FAILURE_SERVER_UNAVAILABLE == rsp.result)
		{
			queueRetry(intent);
		}
	}

	/* DEVELOPER CALLS */

	/**
	 * Starts the "push_outbound_message_send" WebService.
	 * 
	 * @param context
	 * @param subject
	 * @param message
	 * @param thread_id
	 */
	public static void DEVELOPER_push_outbound_message_send(Context context, String subject,
		String message, Integer thread_id)
	{
		if (context == null || subject == null || message == null)
			return;

		Intent intent = new Intent(context, PMWebServiceController.class);
		intent.setAction("push_outbound_message_send");

		/* MANDATORY PARAMETERS */
		String appKey = PMSettings.getApplicationKey(context.getApplicationContext());
		String phoneNumber = PMSettings.getPhoneNumber(context.getApplicationContext());

		if (appKey == null || phoneNumber == null)
			return;

		intent.putExtra("application_key", appKey);
		intent.putExtra("msisdn", phoneNumber);
		intent.putExtra("subject", subject);
		intent.putExtra("message", message);

		/* THIS IS ONLY A DEBUG/DEVELOPER USERNAME/PASSWORD. */
		/* If you have your own credentials you can substitute them here */
		intent.putExtra("username", "donald");
		intent.putExtra("password", "abc123");

		/* OPTIONAL PARAMETERS */

		if (thread_id != null)
			intent.putExtra("thread_id", thread_id + "");

		context.startService(intent);
	}

	/**
	 * Processes {@link Intent} from
	 * {@link #DEVELOPER_push_outbound_message_send(Context, String, String, Integer)}
	 * 
	 * @param intent
	 * @see #DEVELOPER_push_outbound_message_send(Context, String, String,
	 *      Integer)
	 */
	private void handle_DEVELOPER_push_outbound_message_send(Intent intent)
	{
		PMParams params = new PMParams();

		/* MANDATORY PARAMETERS */
		String application_key = intent.getStringExtra("application_key");
		String msisdn = intent.getStringExtra("msisdn");
		String username = intent.getStringExtra("username");
		String password = intent.getStringExtra("password");
		String subject = intent.getStringExtra("subject");
		String data = intent.getStringExtra("message");

		params.put("application_key", application_key);
		params.put("msisdn", msisdn);
		params.put("username", username);
		params.put("password", password);
		params.put("subject", subject);
		params.put("message", data);

		/* OPTIONAL PARAMETERS */
		String thread_id = intent.getStringExtra("thread_id");

		if (thread_id != null)
			params.put("thread_id", thread_id);

		String method = intent.getAction();
		String tag = Tag.MESSAGE_RECEIVED;

		WSResponse rsp = sendRequest(method, tag, params);

		//Success
		if (Result.OK == rsp.result)
		{
			device_push_inbound_messages_list(getApplicationContext(), PMDatabaseCipherHelper
				.getInstance(getApplicationContext()).getLastSentMessageId(), null, null, 20, null,
				null, null);
		}

		//Queue retry if connection error
		else if (Result.FAILURE_SERVER_UNAVAILABLE == rsp.result)
		{
			queueRetry(intent);
		}
	}

	/* NOTIFICATIONS */

	public static final int NOTIFICATION_ID = 1;

	/**
	 * Checks if the android notifications need to be sent
	 */
	private void checkNotifications()
	{
		if (PMBaseActivity.isActivityActive)
			return;

		List<PMReceivedMessage> unreadMessages = PMDatabaseCipherHelper.getInstance(
			getApplicationContext()).getAllUnreadMessages();
		PMWebServiceController.sendNotification(getApplicationContext(), unreadMessages);
	}

	/**
	 * Sends android notification via {@link NotificationManager}
	 * 
	 * @param context
	 * @param messages
	 */
	private static void sendNotification(Context context, List<PMReceivedMessage> messages)
	{
		if (context == null || messages == null || messages.size() == 0)
		{
			return;
		}

		Context appContext = context.getApplicationContext();

		NotificationManager mNotificationManager = (NotificationManager) appContext
			.getSystemService(Context.NOTIFICATION_SERVICE);

		Intent intent = new Intent(appContext, PMBaseActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		//		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);


		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(appContext);
		mBuilder.setSmallIcon(R.drawable.ic_launcher_pm);

		/* TEXT */
		NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
		mBuilder.setContentInfo(messages.size() + "");
		mBuilder.setTicker(messages.get(0).getText());
		if (messages.size() == 1)
		{
			/* SMALL */
			mBuilder.setContentTitle(messages.get(0).getSubject());
			mBuilder.setContentText(messages.get(0).getText());

			/* LARGE */
			inboxStyle.setBigContentTitle(messages.get(0).getSubject());
			inboxStyle.addLine(messages.get(0).getText());

			/* Intent */
			intent.putExtra(PMBaseActivity.EXTRA_SHOW_THREAD, messages.get(0).getThreadID());
			intent.putExtra(PMBaseActivity.EXTRA_SHOW_SUBJECT, messages.get(0).getSubject());

		}
		else
		{
			HashSet<String> uniqueSubjects = new HashSet<String>();
			HashSet<String> uniqueThreads = new HashSet<String>();
			for (PMReceivedMessage pmReceivedMessage : messages)
			{
				uniqueSubjects.add(pmReceivedMessage.getSubject());
				uniqueThreads.add(pmReceivedMessage.getThreadID());

				/* LARGE */
				inboxStyle.addLine(pmReceivedMessage.getText());
			}
			String allSubjects = uniqueSubjects.toString();
			allSubjects = allSubjects.substring(1, allSubjects.length() - 1);

			/* SMALL */
			mBuilder.setContentTitle(allSubjects);
			mBuilder.setContentText(messages.size() + " new messages.");

			/* LARGE */
			inboxStyle.setBigContentTitle(allSubjects);
			inboxStyle.setSummaryText(messages.size() + " new messages.");

			/* Intent */
			if (uniqueThreads.size() == 1)
				intent.putExtra(PMBaseActivity.EXTRA_SHOW_THREAD, messages.get(0).getThreadID());
			if (uniqueSubjects.size() == 1)
				intent.putExtra(PMBaseActivity.EXTRA_SHOW_SUBJECT, messages.get(0).getSubject());
		}

		mBuilder.setStyle(inboxStyle);


		/* INTENT */
		PendingIntent contentIntent = PendingIntent.getActivity(appContext, 0, intent,
			PendingIntent.FLAG_CANCEL_CURRENT);
		mBuilder.setContentIntent(contentIntent);

		/* SOUND */
		mBuilder.setAutoCancel(true);
		mBuilder.setOnlyAlertOnce(true);
		//			Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		//			mBuilder.setSound(notificationSound);
		mBuilder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);

		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
	}

	private class WSResponse
	{
		public int result;
		public PMBaseResponse pmResponse;

		public WSResponse(int result, PMBaseResponse pmResponse)
		{
			this.result = result;
			this.pmResponse = pmResponse;
		}

		public WSResponse(int result)
		{
			this(result, null);
		}
	}

	/* Panacea API method names */
	private static class IntentAction
	{
		private static class WebService
		{
			//REGISTER
			private static final String DEVICE_CHECK_STATUS = "device_check_status";
			private static final String DEVICE_REGISTER = "device_register";
			private static final String DEVICE_REGISTER_MSISDN = "device_register_msisdn";
			private static final String DEVICE_REGISTER_VERIFICATION = "device_verification";
			private static final String DEVICE_UPDATE_PREFERENCES = "device_update_preferences";

			//LOCATION
			private static final String DEVICE_LOCATION_GET = "device_location_get";
			private static final String DEVICE_LOCATION_UPDATE = "device_location_update";

			//MESSAGE_RECEIVED
			private static final String DEVICE_PUSH_OUTBOUND_MESSAGE_LIST = "device_push_outbound_messages_list";
			private static final String DEVICE_PUSH_OUTBOUND_MESSAGE_UPDATE = "device_push_outbound_message_update";
			private static final String DEVICE_PUSH_OUTBOUND_MESSAGE_GET = "device_push_outbound_message_get";

			//MESSAGE_SENT
			private static final String DEVICE_PUSH_INBOUND_MESSAGE_LIST = "device_push_inbound_messages_list";
			private static final String DEVICE_PUSH_INBOUND_MESSAGE_SEND = "device_push_inbound_message_send";
			private static final String DEVICE_PUSH_INBOUND_MESSAGE_GET = "device_push_inbound_message_get";
		}

	}

	/**
	 * Defines result constants for the web service calls.
	 * 
	 * @see Result#OK
	 * @see Result#FAILURE_GENERAL
	 * @see Result#INVALID_METHOD
	 * @see Result#FAILURE_SERVER_UNAVAILABLE
	 */
	private static class Result
	{
		private static final int OK = -1;
		private static final int INVALID_METHOD = 0;
		private static final int FAILURE_GENERAL = 1;
		private static final int FAILURE_SERVER_UNAVAILABLE = 2;

	}
}
