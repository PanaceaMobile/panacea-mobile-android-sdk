package com.panacea.sdk;

import java.util.LinkedHashMap;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import com.panacea.sdk.activity.PMBaseActivity;
import com.panacea.sdk.db.PMDatabaseCipherHelper;
import com.panacea.sdk.db.PMMessage;
import com.panacea.sdk.db.PMReceivedMessage;
import com.panacea.sdk.exception.PMInvalidPhoneNumberException;
import com.panacea.sdk.gcm.GcmHelper;
import com.panacea.sdk.gcm.GcmHelper.GCMListener;
import com.panacea.sdk.web.PMWebServiceController;

/**
 * Point of initialization for user of the SDK. All interaction with the Panacea
 * SDK should be done through this object
 * 
 * @author Cobi Interactive
 **/
public class PanaceaSDK //implements PMResponseListener
{
	private static final String TAG = "PanaceaSDK";

	private static PanaceaSDK instance;

	private Activity parent;
	private Context mContext = null;


	/**
	 * Static access to showUI including initialization of the SDK object
	 * 
	 * @param applicationKey
	 * @param parent
	 */
	public static void showUI(String applicationKey, Activity parent)
	{
		if (instance == null)
		{
			instance = new PanaceaSDK(applicationKey, parent);
		}
		instance.showUI();
	}

	/**
	 * Access to a static instance of PanaceaSDK
	 * 
	 * @return static instance of the already created object, otherwise null
	 */
	public static PanaceaSDK getInstance()
	{
		return instance;
	}

	/**
	 * Create the Panacea object with your application key and push notification
	 * key
	 * 
	 * @param applicationKey
	 *        application key provided by Panacea
	 * @param parent
	 *        the android activity - cannot be null
	 */
	public PanaceaSDK(String applicationKey, Activity parent)
	{
		this.parent = parent;
		this.mContext = parent;

		PMSettings.setApplicationKey(applicationKey, mContext.getApplicationContext());

		instance = this;
	}

	/* PUBLIC CALLS */

	/**
	 * This single method can be called by the implementer to use the baked in
	 * UI.
	 */
	public void showUI()
	{
		Intent i = new Intent(mContext, PMBaseActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		mContext.startActivity(i);
	}

	/**
	 * Used to check if there are any active web service calls in
	 * {@link PMWebServiceController}
	 * 
	 * @return true if there are active web calls, otherwise false
	 */
	public boolean isBusy()
	{
		return PMWebServiceController.isBusy();
	}

	/* PUBLIC REGISTER WEB CALLS */

	/**
	 * The first web call done in the registration process. This method takes
	 * care of GCM registration automatically - if the GCM key has changed the
	 * Panacea server will be notified. Then determines if the device needs to
	 * be registered or only checked for verification.
	 */
	public void registerDevice()
	{
		//kick off GCM registration
		String channelKey = GcmHelper.getGCMRegistrationId(parent, new GCMListener()
		{

			@Override
			public void onPreDataExecute(String tag)
			{
			}

			@Override
			public void onPostDataSuccess(String tag)
			{
				String channelKey = GcmHelper.getGCMRegistrationId(parent, null);
				PMWebServiceController.device_update_preferences(mContext.getApplicationContext(),
					channelKey, null);
			}

			@Override
			public void onPostDataFailure(String tag)
			{
			}
		});

		//dont register channel key at this time
		if (channelKey == "")
		{
			channelKey = null;
		}

		if (PMSettings.hasDeviceConfigurationChanged(mContext))
		{
			PMWebServiceController.device_register(mContext.getApplicationContext(), channelKey);
		}
		else
		{
			if (PMSettings.getDeviceSignature(mContext) == null)
			{
				//if there is no signature, we need to re-register
				PMWebServiceController
					.device_register(mContext.getApplicationContext(), channelKey);
			}
			else
			{
				if (PMSettings.isVerified(mContext))
				{
					//if there is a signature, we ensure it is still verified
					PMWebServiceController.device_check_status(mContext.getApplicationContext());
				}
				else
				{
					PMWebServiceController.device_register(mContext.getApplicationContext(),
						channelKey);
				}
			}
		}
	}

	/**
	 * If the notification returned by {@link #registerDevice} is
	 * {@link Result#REQUEST_PHONE_NUMBER} the user should be prompted for phone
	 * number then this method should be called with the number. format should
	 * be international format with +country code
	 * 
	 * @param phoneNumber
	 *        String in international format with +country code
	 * @throws PMInvalidPhoneNumberException
	 *         if the phone number does not pass validation
	 */
	public void registerPhoneNumber(String phoneNumber) throws PMInvalidPhoneNumberException
	{
		//validate phone number
		if (!PMUtils.isValidPhoneNumber(phoneNumber))
			throw new PMInvalidPhoneNumberException("The phone number is invalid.");

		//if there is no device signature we register again (device_register)
		//if the device is already verified we go to the inbox
		if (PMSettings.getDeviceSignature(mContext) == null || PMSettings.isVerified(mContext))
		{
			registerDevice();
			return;
		}

		PMWebServiceController
			.device_register_msisdn(mContext.getApplicationContext(), phoneNumber);
	}

	/**
	 * If the notification returned by {@link #registerPhoneNumber} is
	 * {@link Result#REQUEST_VERIFICATION_CODE} the user should be prompted for
	 * the verification code this method should be called with the code.
	 * 
	 * @param verificationCode
	 *        sent via SMS (text)
	 */
	public void registerVerification(String verificationCode)
	{
		if (PMUtils.isBlankOrNull(verificationCode))
			return;

		//if there is no device signature we register again (device_register)
		//if the device is already verified we go to the inbox
		if (PMSettings.getDeviceSignature(mContext) == null || PMSettings.isVerified(mContext))
		{
			registerDevice();
			return;
		}

		PMWebServiceController.device_verification(mContext.getApplicationContext(),
			verificationCode);
	}

	public void updateDeviceGivenName(String name)
	{
		String oldName = PMSettings.getGivenName(mContext);

		if (!oldName.equals(name))
		{
			PMWebServiceController.device_update_preferences(mContext.getApplicationContext(),
				null, name);
		}
	}

	/* PUBLIC LOCATION WEB CALLS */

	/**
	 * Determines the best available location and updates the Panacea server.
	 */
	public void updateLocation()
	{
		LocationManager locationManager = (LocationManager) mContext
			.getSystemService(Context.LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		String provider = locationManager.getBestProvider(criteria, false);
		Location location = locationManager.getLastKnownLocation(provider);

		if (location == null) //low powered solution
		{
			criteria.setAccuracy(Criteria.ACCURACY_COARSE);
			criteria.setPowerRequirement(Criteria.POWER_LOW);
			provider = locationManager.getBestProvider(criteria, false);
			location = locationManager.getLastKnownLocation(provider);
		}

		Log.d(TAG, "Location Updated: " + provider + " : " + location);

		if (location == null)
		{
			return;
		}
		PMWebServiceController.device_location_update(mContext.getApplicationContext(), location); //fire and forget
	}

	/* PUBLIC MESSAGE WEB CALLS */

	/**
	 * Checks for new messages.
	 * 
	 * @param forceFullRefresh
	 *        if true deletes all existing data and downloads again
	 */
	public void updateMessages(boolean forceFullRefresh)
	{
		//redownload all messages
		if (forceFullRefresh)
		{
			getDB().deleteMessageCache();
		}

		//								  (last_id, start_date, end_date, limit, page, sort, direction)
		PMWebServiceController.device_push_outbound_messages_list(mContext.getApplicationContext(),
			getDB().getLastReceivedMessageId(), null, null, 100, null, null, null);

		PMWebServiceController.device_push_inbound_messages_list(mContext.getApplicationContext(),
			getDB().getLastSentMessageId(), null, null, 100, null, null, null);

	}


	/**
	 * Marks all messages in the given thread as read. The updated messages are
	 * downloaded from the server after the update.
	 * 
	 * @param threadId
	 *        of message(s) to mark as read
	 */
	public void markThreadAsRead(final String threadId)
	{
		new Thread(new Runnable()
		{
			public void run()
			{
				List<PMMessage> messages = getDB().getMessagesForThreadId(threadId);

				for (PMMessage message : messages)
				{
					if (message instanceof PMReceivedMessage)
					{
						PMReceivedMessage msg = (PMReceivedMessage) message;
						if (msg.isUnread())
						{
							PMWebServiceController.device_push_outbound_message_update(
								mContext.getApplicationContext(), msg.getReceivedMessageId());
						}
					}
				}
			}
		}).start();
	}

	/**
	 * Marks all messages in the given subject as read. The updated messages are
	 * downloaded from the server after the update.
	 * 
	 * @param subject
	 */
	public void markSubjectAsRead(final String subject)
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				List<PMReceivedMessage> messages = getDB().getMessagesForSubject(subject);
				for (PMMessage message : messages)
				{
					if (message instanceof PMReceivedMessage)
					{
						PMReceivedMessage msg = (PMReceivedMessage) message;
						if (msg.isUnread())
						{
							PMWebServiceController.device_push_outbound_message_update(
								mContext.getApplicationContext(), msg.getReceivedMessageId());
						}
					}
				}
			}
		}).start();
	}


	/**
	 * Reply to a {@link PMReceivedMessage} with message String.
	 * 
	 * @param message
	 * @param originalMessage
	 */
	public void sendReply(String message, PMReceivedMessage originalMessage)
	{
		PMWebServiceController.device_push_inbound_message_send(mContext.getApplicationContext(),
			message, originalMessage.getReceivedMessageId(), null, originalMessage.getThreadID());
	}


	/* PUBLIC MESSAGE DB CALLS */

	/**
	 * Returns a sorted list of received and sent messages for the thread id.
	 * Sorted oldest to newest WARNING NOT ASYNCHRONOUS
	 * 
	 * @param threadId
	 * @return List of messages in given thread
	 */
	@Deprecated
	public List<PMMessage> getThreadMessages(String threadId)
	{
		return getDB().getMessagesForThreadId(threadId);
	}

	/**
	 * Returns the latest received message with a given subject for each
	 * different thread id. "All updates" will return the latest received
	 * message for each subject with a different thread id. WARNING NOT
	 * ASYNCHRONOUS
	 * 
	 * @param subject
	 * @return List of latest receivedMessage for each theadID
	 */
	@Deprecated
	public List<PMReceivedMessage> getSubjectThreads(String subject)
	{
		return getDB().getMessagesForSubject(subject);
	}

	/**
	 * Returns a list of unique subjects and their respective unread count. This
	 * includes "All updates" WARNING NOT ASYNCHRONOUS
	 * 
	 * @return HashMap of unique subjects and how many unread messages each
	 *         subject has
	 */
	@Deprecated
	public LinkedHashMap<String, Integer> getSubjectCounts()
	{
		return getDB().getSubjectCounts();
	}

	/**
	 * removes the deleted flag from all messages in the database, thereby
	 * 'undeleting' them
	 */
	public void markAllDeletedMessages(boolean deleted)
	{
		getDB().markAll(deleted);
	}

	/**
	 * Marks a single message as deleted.
	 * 
	 * @param message
	 */
	public void markMessageDeleted(PMMessage message)
	{
		getDB().markMessage(message, true);
	}

	/**
	 * Marks all the messages in a thread as deleted
	 * 
	 * @param threadID
	 */
	public void markThreadDeleted(String threadID)
	{
		getDB().markThread(threadID, true);
	}

	/**
	 * Marks an entire subject as deleted
	 * 
	 * @param subject
	 */
	public void markSubjectDeleted(String subject)
	{
		getDB().markSubject(subject, true);
	}

//	/**
//	 * Private method to get DB instance
//	 * 
//	 * @return {@link PMDatabaseHelper}
//	 */
//	private PMDatabaseHelper getDB()
//	{
//		return PMDatabaseHelper.getInstance(mContext.getApplicationContext());
//	}

	/**
	 * Private method to get encrypted DB instance
	 * 
	 * @return {@link PMDatabaseHelper}
	 */
	private PMDatabaseCipherHelper getDB()
	{
		return PMDatabaseCipherHelper.getInstance(mContext.getApplicationContext());
	}

	/**
	 * Developer method to simulate sending a message from Panacea to
	 * device/client
	 * 
	 * @param subject
	 * @param message
	 * @param thread_id
	 */
	public void debug_push_outbound_message_send(String subject, String message, Integer thread_id)
	{
		PMWebServiceController.DEVELOPER_push_outbound_message_send(
			mContext.getApplicationContext(), subject, message, thread_id);
	}


	/**
	 * General labels given to a web service call
	 * 
	 * @author Cobi Interactive
	 */
	public static class Tag
	{
		public static final String REGISTER = "REGISTER";
		public static final String MESSAGE_RECEIVED = "MESSAGE_RECEIVED";
		public static final String MESSAGE_SENT = "MESSAGE_SENT";
		public static final String LOCATION = "LOCATION";

	}

	/**
	 * Results returned by
	 * {@link com.panacea.sdk.web.PMLocalBroadcastReceiver.PMBroadcastListener}
	 * 
	 * @author Cobi Interactive
	 */
	public static class Result
	{
		/* Errors all calls can return */
		public static final String CONNECTION_ERROR = "CONNECTION_ERROR";
		public static final String GENERAL_ERROR = "GENERAL_ERROR";
		public static final String HTTP_SERVER_ERROR = "HTTP_SERVER_ERROR";

		public static final String PANACEA_ERROR_CODE = "PANACEA_ERROR_CODE";

		/* Specific Registration results */
		public static final String REQUEST_PHONE_NUMBER = "REQUIRE_PHONE_NUMBER";
		public static final String REQUEST_VERIFICATION_CODE = "REQUIRE_VERIFICATION_CODE";
		public static final String REGISTER_SUCCESS = "REGISTER_SUCCESS";
		public static final String NOT_VERIFIED = "NOT_VERIFIED";
		public static final String INVALID_VERIFICATION_CODE = "INVALID_VERIFICATION_CODE";
		public static final String PUSH_KEY_UPDATED = "PUSH_KEY_UPDATED";
		public static final String PREFERENCE_UPDATED = "PREFERENCE_UPDATED";

		/* Specific Message results */
		public static final String MESSAGE_UPDATED_RECEIVED = "MESSAGE_UPDATED_RECEIVED";
		public static final String MESSAGE_UPDATED_SENT = "MESSAGE_UPDATED_SENT";

		/* Specific Location results */
		public static final String LOCATION_UPDATED = "LOCATION_UPDATED";
	}

}
