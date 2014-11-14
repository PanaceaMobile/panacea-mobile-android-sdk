package com.panacea.sdk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.StringTokenizer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;

/**
 * This class contains various static utility methods.
 * 
 * @author Cobi Interactive
 */
public class PMUtils
{
	/**
	 * Generates a user friendly string describing date relative to now.
	 * 
	 * @param dateCreated
	 * @return string of relative date
	 */
	public static String getRelativeDate(Date dateCreated)
	{
		Calendar cal = Calendar.getInstance();
		cal.setTime(dateCreated);
		//
		//		return DateUtils.getRelativeTimeSpanString(cal.getTimeInMillis()).toString();

		long time = (System.currentTimeMillis() - cal.getTimeInMillis()) / 1000;

		if (time < 60 * 60 * 24)
		{
			return dateToString("HH:mm", dateCreated);
		}
		else
		{
			return dateToString("dd/MM/yy HH:mm", dateCreated);
		}
	}

	/**
	 * Reads in the country codes from the resources and returns a map keyed on
	 * country name
	 * 
	 * @param context
	 *        Context
	 * @return LinkedHashMap key= country name, value= country code
	 */
	public static LinkedHashMap<String, String> getCountryCodes(Context context)
	{
		if (context == null)
			return null;

		InputStream inputStream = context.getApplicationContext().getResources()
			.openRawResource(R.raw.country_codes);

		InputStreamReader inputreader = new InputStreamReader(inputStream);
		BufferedReader buffreader = new BufferedReader(inputreader);
		String line;

		LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();

		try
		{
			while ((line = buffreader.readLine()) != null)
			{
				StringTokenizer st = new StringTokenizer(line, ";");
				String countryName = st.nextToken().trim();
				String countryCode = st.nextToken().trim();
				map.put(countryName, countryCode);
			}
		}
		catch (IOException e)
		{
			return null;
		}
		return map;
	}

	/**
	 * Converts a String of a date in the provided dateFormat into a Date object
	 * 
	 * @param dateFormat
	 *        format of the input String
	 * @param date
	 *        String representation of the date
	 * @return Date object
	 */
	@SuppressLint("SimpleDateFormat")
	public static Date stringToDate(String dateFormat, String date)
	{
		try
		{
			DateFormat df = new SimpleDateFormat(dateFormat);
			return df.parse(date);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	/**
	 * Converts a Date object into a String with the provided dateFormat
	 * 
	 * @param dateFormat
	 *        format of the String to be return
	 * @param date
	 *        Date object to convert to String
	 * @return string representation of the date
	 */
	@SuppressLint("SimpleDateFormat")
	public static String dateToString(String dateFormat, Date date)
	{
		DateFormat df = new SimpleDateFormat(dateFormat);
		return df.format(date);
	}

	/**
	 * Converts an InputStream into a String
	 * 
	 * @param is
	 *        InputStream to convert
	 * @return String representation of the InputStream
	 */
	public static String convertStreamToString(java.io.InputStream is)
	{
		if (is == null)
			return null;

		java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
		return s.hasNext() ? s.next() : "";
	}


	/**
	 * Tests if a string is a non-null, non-empty string. This can be called to
	 * determine if the string should be displayed, or not.
	 * 
	 * @param text
	 *        String to test.
	 * @return If <code>text</code> is <code>null</code>, returns
	 *         <code>false</code>. <br>
	 *         If <code>text</code> is an empty string (""), returns
	 *         <code>false</code>. <br>
	 *         Else returns <code>true</code>.
	 */
	public static boolean isNonBlankString(String text)
	{
		// null text -> false
		if (text == null)
			return false;

		// empty text -> false
		if ("".equals(text))
			return false;

		return true;
	}

	/**
	 * Tests if a string is a blank string, or is null. This can be called to
	 * determine if the string should be displayed, or not. </p> This is exactly
	 * the opposite result to {@link #isNonBlankString(String)}.
	 * 
	 * @param text
	 *        String to test.
	 * @return If <code>text</code> is <code>null</code>, returns
	 *         <code>true</code>. <br>
	 *         If <code>text</code> is an empty string (""), returns
	 *         <code>true</code>. <br>
	 *         Else returns <code>null</code>.
	 * @see #isNonBlankString(String)
	 */
	public static boolean isBlankOrNull(String text)
	{
		return !isNonBlankString(text);
	}

	/**
	 * Tests if a string is a non-null, non-empty string. And if it is a valid number.
	 * 
	 * @param number
	 * @return
	 */
	public static boolean isNonBlankNumber(String number)
	{
		// null text -> false
		if (number == null)
			return false;

		// empty text -> false
		if ("".equals(number.trim()))
			return false;
		try
		{
			Integer.parseInt(number);
			return true;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}


	/**
	 * Validates a phone number string
	 * 
	 * @param phoneNumber
	 *        input phone number String
	 * @return true if the phone number is in a valid format, otherwise false
	 */
	public static boolean isValidPhoneNumber(String phoneNumber)
	{
		if (PMUtils.isBlankOrNull(phoneNumber))
			return false;

		return TextUtils.isDigitsOnly(phoneNumber);
	}
}
