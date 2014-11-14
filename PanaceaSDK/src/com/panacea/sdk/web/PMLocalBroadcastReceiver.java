package com.panacea.sdk.web;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

/**
 * This Local broadcast receiver allows users to register for web service
 * callbacks from {@link PMWebServiceController}.
 * <p>
 * This is done implementing the {@link PMBroadcastListener} and by creating an
 * instance of {@link PMLocalBroadcastReceiver} in the class that should receive
 * callbacks:
 * <p>
 * <code>protected PMLocalBroadcastReceiver mReceiver = new PMLocalBroadcastReceiver();</code>
 * <p>
 * and then registering and unregistering where required:
 * <p>
 * <code>mReceiver.register(this, this);</code>
 * <p>
 * <code>mReceiver.unregister(this);</code>
 * 
 * @author Cobi Interactive
 * @see PMWebServiceController
 */
public class PMLocalBroadcastReceiver extends BroadcastReceiver
{
	private static final String EVENT_NAME = "com.panacea.PMLocalBroadcastReceiver";

	private static final String EXTRA_TAG = "EXTRA_TAG";
	private static final String EXTRA_TYPE = "EXTRA_TYPE";
	private static final String EXTRA_RESULT = "EXTRA_RESULT";
	private static final String EXTRA_CODE = "EXTRA_CODE";
	private static final String EXTRA_MESSAGE = "EXTRA_MESSAGE";

	private static final int TYPE_PRE_DATA_EXECUTE = 0;
	private static final int TYPE_POST_DATA_FAILURE = 1;
	private static final int TYPE_POST_DATA_SUCCESS = 2;


	/**
	 * This interface defines the various methods received by the broadcast
	 * receiver.
	 * 
	 * @author Cobi Interactive
	 */
	public interface PMBroadcastListener
	{
		public void onPreDataExecute(String tag);

		public void onPostDataSuccess(String tag, String result, int status, String message);

		public void onPostDataFailure(String tag, String result, int httpCode, String message);
	}

	private PMBroadcastListener listener;

	@Override
	public void onReceive(Context context, Intent intent)
	{
		if (listener != null)
		{
			String tag = intent.getStringExtra(EXTRA_TAG);
			int type = intent.getIntExtra(EXTRA_TYPE, -1);
			String result = intent.getStringExtra(EXTRA_RESULT);

			int code = intent.getIntExtra(EXTRA_CODE, -1);
			String message = intent.getStringExtra(EXTRA_MESSAGE);

			switch (type)
			{
				case TYPE_PRE_DATA_EXECUTE:
				{
					listener.onPreDataExecute(tag);
				}
					break;
				case TYPE_POST_DATA_FAILURE:
				{
					listener.onPostDataFailure(tag, result, code, message);
				}
					break;
				case TYPE_POST_DATA_SUCCESS:
				{
					listener.onPostDataSuccess(tag, result, code, message);
				}
					break;

				default:
					break;
			}
		}
	}

	public void register(PMBroadcastListener listener, Context context)
	{
		this.listener = listener;
		LocalBroadcastManager.getInstance(context).registerReceiver(this,
			new IntentFilter(EVENT_NAME));
	}

	public void unregister(Context context)
	{
		this.listener = null;
		LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
	}

	public static void sendPreDataExecute(Context context, String tag)
	{
		Intent intent = new Intent(EVENT_NAME);

		intent.putExtra(EXTRA_TAG, tag);
		intent.putExtra(EXTRA_TYPE, TYPE_PRE_DATA_EXECUTE);

		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}

	public static void sendPostDataSuccess(Context context, String tag, String result, int status,
		String message)
	{
		Intent intent = new Intent(EVENT_NAME);

		intent.putExtra(EXTRA_TAG, tag);
		intent.putExtra(EXTRA_TYPE, TYPE_POST_DATA_SUCCESS);
		intent.putExtra(EXTRA_RESULT, result);
		intent.putExtra(EXTRA_CODE, status);
		intent.putExtra(EXTRA_MESSAGE, message);

		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}

	public static void sendPostDataFailure(Context context, String tag, String result, int code,
		String message)
	{
		Intent intent = new Intent(EVENT_NAME);

		intent.putExtra(EXTRA_TAG, tag);
		intent.putExtra(EXTRA_TYPE, TYPE_POST_DATA_FAILURE);
		intent.putExtra(EXTRA_RESULT, result);
		intent.putExtra(EXTRA_CODE, code);
		intent.putExtra(EXTRA_MESSAGE, message);

		LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}
}
