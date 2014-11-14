package com.panacea.sdk.fragment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;

import com.panacea.sdk.PMUtils;
import com.panacea.sdk.PanaceaSDK.Result;
import com.panacea.sdk.R;
import com.panacea.sdk.activity.PMBaseActivity;
import com.panacea.sdk.exception.PMInvalidPhoneNumberException;
import com.panacea.sdk.widget.PMInstantAutoComplete;

/**
 * Fragment to handle Registering device.
 * 
 * @author Cobi interactive
 */
public class PMLoginFragment extends PMBaseFragment implements PMBaseActivity.BackButtonCallbacks
{
	public static String TAG = "PMLoginFragment";

	private LinkedHashMap<String, String> countryCodes;
	private String currentPhoneNumber;

	private LinearLayout loginEnterNumberLayout;
	private TextView mobileCountryCodeEditText;
	private EditText mobileNumberEditText;

	private LinearLayout loginVerifyNumberLayout;
	private TextView mobileNumberSentEditText;
	private EditText verificationCodeEditText;
	private PMInstantAutoComplete countryEditText;

	// track the step in the registration process to enable/disable the back button
	private boolean verificationStep = false;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		((PMBaseActivity) getActivity()).mBackButtonCallbacks = this;

		View result = inflater.inflate(R.layout.fragment_login, container, false);

		loginEnterNumberLayout = (LinearLayout) result.findViewById(R.id.loginEnterNumberLayout);
		mobileCountryCodeEditText = (TextView) result.findViewById(R.id.mobileCountryCodeEditText);
		mobileNumberEditText = (EditText) result.findViewById(R.id.mobileNumberEditText);

		loginVerifyNumberLayout = (LinearLayout) result.findViewById(R.id.loginVerifyNumberLayout);
		mobileNumberSentEditText = (TextView) result.findViewById(R.id.mobileNumberSentEditText);
		verificationCodeEditText = (EditText) result.findViewById(R.id.verificationCodeEditText);

		verificationCodeEditText.addTextChangedListener(new TextWatcher()
		{
			public void afterTextChanged(Editable s)
			{
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after)
			{
			}

			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
				if (verificationCodeEditText.getText().toString().length() == 5)
				{
					InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
						Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(verificationCodeEditText.getWindowToken(), 0);
					sendVerify();

				}
			}
		});


		// Get a reference to the AutoCompleteTextView in the layout
		countryEditText = (PMInstantAutoComplete) result.findViewById(R.id.autocomplete_country);

		showEnterNumber();

		return result;
	}

	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		if (savedInstanceState != null)
		{
			//restore states
		}

		countryCodes = PMUtils.getCountryCodes(getActivity());
		final List<String> countries = new ArrayList<String>(countryCodes.keySet());//new ArrayList<String>(countryCodes.stringPropertyNames());

		// Create the adapter of countries and set it to the AutoCompleteTextView 
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
			android.R.layout.simple_list_item_1, countries);
		countryEditText.setAdapter(adapter);

		mobileNumberEditText.setOnEditorActionListener(new OnEditorActionListener()
		{
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
			{
				int result = actionId & EditorInfo.IME_MASK_ACTION;
				if (result == EditorInfo.IME_ACTION_DONE)
				{
					sendPhoneNumber();
				}
				return false;
			}
		});

		countryEditText.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				countryEditText.showDropDown();
			}
		});
		countryEditText.addTextChangedListener(new TextWatcher()
		{
			public void afterTextChanged(Editable s)
			{
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after)
			{
			}

			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
				int index = countries.indexOf(countryEditText.getText().toString());
				if (index >= 0 && index < countries.size())
				{
					mobileCountryCodeEditText.setText(countryCodes.get(countries.get(index)));
				}
				else
				{
					mobileCountryCodeEditText.setText("");
				}
			}
		});


		ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(getActivity(),
			android.R.layout.simple_spinner_item, countries);
		spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		int index = countries.indexOf("South Africa");
		if (index >= 0)
			countryEditText.setText(countries.get(index));
	}

	@Override
	public void onResume()
	{
		super.onResume();

		if (verificationStep)
		{
			setActionBarTitle("Verify");
		}
		else
		{
			setActionBarTitle("Register");
		}
	}

	private void showEnterNumber()
	{
		verificationStep = false;
		getActivity().invalidateOptionsMenu();
		setActionBarTitle("Register");

		loginEnterNumberLayout.setVisibility(View.VISIBLE);
		loginVerifyNumberLayout.setVisibility(View.GONE);
	}

	private void showVerifyNumber()
	{
		verificationStep = true;
		getActivity().invalidateOptionsMenu();
		setActionBarTitle("Verify");

		loginEnterNumberLayout.setVisibility(View.GONE);
		loginVerifyNumberLayout.setVisibility(View.VISIBLE);

		mobileNumberSentEditText.setText("+" + currentPhoneNumber);
	}

	private void sendPhoneNumber()
	{
		if (mobileNumberEditText.getText().toString().length() > 0)
		{
			currentPhoneNumber = mobileCountryCodeEditText.getText().toString()
				+ mobileNumberEditText.getText().toString();

			try
			{
				sdk.registerPhoneNumber(currentPhoneNumber);
			}
			catch (PMInvalidPhoneNumberException e)
			{
				getPMActivity().showPopupMessage("Invalid Phone Number",
					"Please enter a valid phone number");
				return;
			}

			showVerifyNumber();
		}
		else
		{
			if (PMUtils.isBlankOrNull(verificationCodeEditText.getText().toString()))
			{
				getPMActivity().showPopupMessage("No mobile number",
					"Please enter a valid mobile number");
				return;
			}
		}
	}

	private void sendVerify()
	{
		//verify pin is entered
		if (PMUtils.isBlankOrNull(verificationCodeEditText.getText().toString()))
		{
			getPMActivity().showPopupMessage("Invalid Verification Code",
				"Please enter a valid verification code");
			return;
		}

		sdk.registerVerification(verificationCodeEditText.getText().toString());
	}


	@Override
	public void onPostDataSuccess(String tag, String result, int status, String message)
	{
		super.onPostDataSuccess(tag, result, status, message);

		if (Result.REQUEST_PHONE_NUMBER.equals(result))
		{
			showEnterNumber();
		}
		else if (Result.REQUEST_VERIFICATION_CODE.equals(result))
		{
			showVerifyNumber();
		}
		else if (Result.INVALID_VERIFICATION_CODE.equals(result))
		{
			getPMActivity().showPopupMessage("Invalid Verification Code", message);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.menu_login, menu);

		if (!verificationStep)
		{
			MenuItem backItem = menu.findItem(R.id.menu_back);
			backItem.setVisible(false);
		}

		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int itemId = item.getItemId();

		if (!verificationStep)
		{
			if (itemId == R.id.menu_next)
			{
				sendPhoneNumber();
				return true;
			}
		}
		else
		{
			if (itemId == R.id.menu_next)
			{
				sendVerify();
			}
			else if (itemId == R.id.menu_back)
			{
				showEnterNumber();
			}

			return true;
		}

		return false;
	}

	public void onBackPressedCallback()
	{
		// go back a screen if verification step screen is shown, else pass to super
		if (verificationStep)
		{
			showEnterNumber();
		}
		else
		{
			((PMBaseActivity) getActivity()).mBackButtonCallbacks = null;
			((PMBaseActivity) getActivity()).onBackPressed();
		}
	}
}
