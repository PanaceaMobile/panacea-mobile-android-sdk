package com.panacea.sdk.fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.panacea.sdk.PMSettings;
import com.panacea.sdk.PanaceaSDK.Result;
import com.panacea.sdk.R;

/**
 * This fragment shows the Settings available to the user.
 * 
 * @author Cobi interactive
 */
public class PMSettingsFragment extends PMBaseFragment
{
	public static String TAG = "PMSettingsFragment";

	private TextView version;
	private TextView given_name;
	private TableRow clear;
	private TextView device_id;
	private TableRow undelete;

	private String currentGivenName;


	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View result = inflater.inflate(R.layout.fragment_settings, container, false);

		version = (TextView) result.findViewById(R.id.settings_version);
		given_name = (TextView) result.findViewById(R.id.settings_given_name);
		clear = (TableRow) result.findViewById(R.id.settings_clear);
		device_id = (TextView) result.findViewById(R.id.settings_device_id);
		undelete = (TableRow) result.findViewById(R.id.settings_undelete);

		given_name.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				showNameOptions();
			}
		});

		device_id.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				showDeviceIdOptions();
			}
		});

		clear.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				refreshMessages();
			}
		});

		undelete.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				undeleteMessages();
			}
		});

		return result;
	}

	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		if (savedInstanceState != null)
		{
			//restore states
		}

		reloadContent();
	}


	private void reloadContent()
	{
		currentGivenName = PMSettings.getGivenName(getActivity());
		given_name.setText(currentGivenName);

		/* VERSION */
		try
		{
			String versionName = getActivity().getPackageManager().getPackageInfo(
				getActivity().getPackageName(), 0).versionName;
			version.setText(versionName);
		}
		catch (NameNotFoundException e)
		{
			version.setText("Unknown");
		}

		/* DEVICE ID */
		String str_device_id = getActivity().getSharedPreferences("PMSDK", Context.MODE_PRIVATE)
			.getString("unique_device_id", "");
		device_id.setText(str_device_id);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		setActionBarTitle("Settings");
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu)
	{
		super.onPrepareOptionsMenu(menu);

		menu.setGroupVisible(R.id.menu_general, false);
	}

	@Override
	public void onPostDataSuccess(String tag, String result, int status, String message)
	{
		super.onPostDataSuccess(tag, result, status, message);

		if (Result.PREFERENCE_UPDATED.equals(result))
		{
			reloadContent();
		}
	}

	private void refreshMessages()
	{
		sdk.updateMessages(true);
	}

	private void undeleteMessages()
	{
		sdk.markAllDeletedMessages(false);
	}

	private void showNameOptions()
	{
		final AlertDialog.Builder singlechoicedialog = new AlertDialog.Builder(getActivity());
		final CharSequence[] Report_items =
			{ "Edit", "Copy to clipboard", "Share" };
		singlechoicedialog.setTitle("Device Name");
		singlechoicedialog.setSingleChoiceItems(Report_items, -1,
			new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int item)
				{
					switch (item)
					{
						case 0:
							editName();
							break;
						case 1:
							copyToClipboard(currentGivenName);
							break;
						case 2:
							shareText(currentGivenName);
							break;

						default:
							break;
					}

					dialog.cancel();
				}
			});
		AlertDialog alert_dialog = singlechoicedialog.create();
		alert_dialog.show();
	}

	private void showDeviceIdOptions()
	{
		final AlertDialog.Builder singlechoicedialog = new AlertDialog.Builder(getActivity());
		final CharSequence[] Report_items =
			{ "Copy to clipboard", "Share" };

		singlechoicedialog.setTitle("Unique Device ID");
		singlechoicedialog.setSingleChoiceItems(Report_items, -1,
			new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int item)
				{
					switch (item)
					{
						case 0:
							copyToClipboard(device_id.getText().toString());
							break;
						case 1:
							shareText(device_id.getText().toString());
							break;

						default:
							break;
					}

					dialog.cancel();
				}
			});
		AlertDialog alert_dialog = singlechoicedialog.create();
		alert_dialog.show();
	}


	private void editName()
	{
		AlertDialog.Builder editalert = new AlertDialog.Builder(getActivity());

		editalert.setCancelable(false);
		editalert.setTitle("Enter your Name");
		//		editalert.setMessage("here is the message");


		final EditText input = new EditText(getActivity());
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
		input.setLayoutParams(lp);
		input.setText(currentGivenName);
		editalert.setView(input);

		editalert.setNegativeButton("Cancel", null);
		editalert.setPositiveButton("OK", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int whichButton)
			{
				sdk.updateDeviceGivenName(input.getText().toString());
			}
		});


		editalert.show();
	}


	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	private void copyToClipboard(String copyText)
	{
		int sdk = android.os.Build.VERSION.SDK_INT;
		if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB)
		{
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getActivity()
				.getSystemService(Context.CLIPBOARD_SERVICE);
			clipboard.setText(copyText);
		}
		else
		{
			android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getActivity()
				.getSystemService(Context.CLIPBOARD_SERVICE);
			android.content.ClipData clip = android.content.ClipData.newPlainText("text label",
				copyText);
			clipboard.setPrimaryClip(clip);
		}
	}

	private void shareText(String shareText)
	{
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
		sendIntent.setType("text/plain");
		startActivity(sendIntent);
	}
}