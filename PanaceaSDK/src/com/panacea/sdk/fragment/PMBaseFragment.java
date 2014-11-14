package com.panacea.sdk.fragment;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.MenuItem;

import com.panacea.sdk.PanaceaSDK;
import com.panacea.sdk.activity.PMBaseActivity;
import com.panacea.sdk.web.PMLocalBroadcastReceiver;
import com.panacea.sdk.web.PMLocalBroadcastReceiver.PMBroadcastListener;

/**
 * Base fragment containing shared variables and methods.
 * 
 * @author Cobi interactive
 */
public abstract class PMBaseFragment extends DialogFragment implements PMBroadcastListener
{
	public static String TAG = "PMBaseFragment";

	protected PanaceaSDK sdk;
	protected PMLocalBroadcastReceiver mReceiver = new PMLocalBroadcastReceiver();

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		sdk = PanaceaSDK.getInstance();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		if (sdk == null)
			sdk = PanaceaSDK.getInstance();
	}

	@Override
	public void onResume()
	{
		super.onResume();
		mReceiver.register(this, getActivity());
		checkLoading();
		setHomeEnabled(true);
	}

	@Override
	public void onPause()
	{
		mReceiver.unregister(getActivity());
		super.onPause();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int itemId = item.getItemId();
		if (itemId == android.R.id.home)
		{
			getActivity().getSupportFragmentManager().popBackStack();
			return true;
		}
		else
		{
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Checks if any WebService is busy and shows progress bar in ActionBar
	 */
	public void checkLoading()
	{
		if (getPMActivity() != null)
			getPMActivity().checkLoading();

		//hide menu - currently not used
		//		boolean loading = sdk.isBusy();
		//		setMenuVisibility(!loading);
	}

	protected void setActionBarTitle(String title)
	{
		if (getActivity() != null)
			getPMActivity().getSupportActionBar().setTitle(title);
	}
	
	protected void setHomeEnabled(boolean enabled)
	{
		if (getActivity() != null)
			getPMActivity().setHomeEnabled(enabled);
	}

	public PMBaseActivity getPMActivity()
	{
		return (PMBaseActivity) getActivity();
	}

	@Override
	public void onPreDataExecute(String tag)
	{
		Log.d(TAG, "onPreDataExecute tag: " + tag);
		checkLoading();
	}

	@Override
	public void onPostDataSuccess(String tag, String result, int status, String message)
	{
		Log.d(TAG, "onPostDataSuccess tag: " + tag + " result: " + result);
		checkLoading();
	}

	@Override
	public void onPostDataFailure(String tag, String result, int code, String message)
	{
		Log.d(TAG, "onPostDataFailure tag: " + tag + " result: " + result);
		checkLoading();
		//error message handled by activity 
	}


}
