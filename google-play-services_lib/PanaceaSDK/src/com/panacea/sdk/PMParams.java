package com.panacea.sdk;

import java.net.URLEncoder;
import java.util.Hashtable;

/**
 * Used to pass input parameters to Panacea Web Calls
 * 
 * @author Cobi Interactive
 **/
public class PMParams
{
	/* Key/Value Pairs */
	private Hashtable<String, String> keysValues = new Hashtable<String, String>();

	public PMParams()
	{
	}

	public void put(String key, String value)
	{
		if (key == null || value == null)
			return;
		keysValues.put(key, value);
	}

	public String get(String key)
	{
		if (key == null)
			return null;

		return keysValues.get(key);
	}


	/**
	 * Constructs the keys and values as URL parameters
	 * 
	 * @return URL encoded parameter String
	 */
	public String getURLParameters()
	{
		StringBuilder urlParams = new StringBuilder();
		String value;
		for (String key : keysValues.keySet())
		{
			try
			{
				value = keysValues.get(key);
				if (value != null)
				{
					urlParams.append('&');
					urlParams.append(key);
					urlParams.append('=');
					urlParams.append(URLEncoder.encode(value, "UTF-8"));
				}
			}
			catch (Exception e)
			{
				//continue
				e.printStackTrace();
			}
		}
		return urlParams.toString();
	}

}
