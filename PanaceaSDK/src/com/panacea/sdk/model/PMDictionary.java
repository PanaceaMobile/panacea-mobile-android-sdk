package com.panacea.sdk.model;

import java.io.InvalidClassException;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import com.panacea.sdk.PMConstants;
import com.panacea.sdk.exception.PMKeyNotFoundException;

/**
 * Inspired by iOS NSDictionary, contains key-value pairs. Used in PANACEA API
 * web calls. The key is a parameter name (String) and the value may be one of
 * the following types: <br/>
 * <ul>
 * <li>String
 * <li>boolean
 * <li>int
 * <li>long
 * <li>double
 * <li>PMDictionary
 * <li>PMArray
 * </ul>
 * 
 * @author Cobi Interactive
 **/
public class PMDictionary
{
	/** Dictionary contains key-value pair mappings */
	private HashMap<String, Object> dictionary = new HashMap<String, Object>();


	/**
	 * Constructs an empty PMDictionary
	 */
	public PMDictionary()
	{

	}

	/**
	 * Construct and parse a PMDictionary from a JSON String.
	 * 
	 * @param json
	 *        the JSON formatted string
	 */
	public PMDictionary(String json) throws Exception
	{
		JSONObject JSONObj = new JSONObject(json);
		parseJsonObject(JSONObj, this);

	}

	protected PMDictionary(JSONObject JSONObj) throws Exception
	{
		parseJsonObject(JSONObj, this);
	}

	/* SETTERS */

	/**
	 * Associates the specified value with the specified key in this object.
	 * 
	 * @param key
	 *        key with which the specified value is to be associated
	 * @param value
	 *        a string value to be associated with the specified key
	 */
	public void put(String key, String value)
	{
		if (key == null || value == null)
			return;
		dictionary.put(key, value);
	}

	/**
	 * Associates the specified value with the specified key in this object.
	 * 
	 * @param key
	 *        key with which the specified value is to be associated
	 * @param value
	 *        a int value to be associated with the specified key
	 */
	public void put(String key, int value)
	{
		if (key == null)
			return;
		dictionary.put(key, value);
	}

	/**
	 * Associates the specified value with the specified key in this object.
	 * 
	 * @param key
	 *        key with which the specified value is to be associated
	 * @param value
	 *        a boolean value to be associated with the specified key
	 */
	public void put(String key, boolean value)
	{
		if (key == null)
			return;
		dictionary.put(key, value);
	}

	/**
	 * Associates the specified value with the specified key in this object.
	 * 
	 * @param key
	 *        key with which the specified value is to be associated
	 * @param value
	 *        a PMDictionary value to be associated with the specified key
	 */
	public void put(String key, PMDictionary value)
	{
		if (key == null)
			return;
		dictionary.put(key, value);
	}

	/**
	 * Associates the specified value with the specified key in this object.
	 * 
	 * @param key
	 *        key with which the specified value is to be associated
	 * @param value
	 *        a PMArray value to be associated with the specified key
	 */
	public void put(String key, PMArray value)
	{
		if (key == null)
			return;
		dictionary.put(key, value);
	}


	/* GETTERS */

	/**
	 * Returns the boolean to which the specified key is mapped, or the
	 * <em>defaultValue</em> if this object contains no mapping for the key.
	 * 
	 * @param key
	 *        the key whose associated boolean is to be returned
	 * @param defaultValue
	 *        the boolean value to be returned if this object doesn't contain
	 *        the specified key.
	 * @return the boolean value to which the specified key is mapped, or the
	 *         <em>defaultValue</em> if this object contains no mapping for the
	 *         key.
	 */
	public boolean getBool(String key, boolean defaultValue)
	{
		try
		{
			return getBool(key);
		}
		catch (Exception ex)
		{
			return defaultValue;
		}
	}

	/**
	 * Returns the boolean to which the specified key is mapped, or null if this
	 * map contains no mapping for the key.
	 * 
	 * @param key
	 *        the key whose associated boolean is to be returned
	 * @return the value to which the specified key is mapped, or null if this
	 *         map contains no mapping for the key.
	 */
	public boolean getBool(String key) throws PMKeyNotFoundException, NullPointerException,
		InvalidClassException
	{
		if (!dictionary.containsKey(key))
			throw new PMKeyNotFoundException(PMConstants.Exceptions.KEY_NOT_FOUND + " " + key);

		Object obj = dictionary.get(key);
		if (obj == null)
			throw new NullPointerException(PMConstants.Exceptions.KEY_NOT_FOUND + " " + key);

		if (obj.getClass().isAssignableFrom(Boolean.class))
		{
			return (Boolean) obj;
		}
		else
		{
			return obj.toString().toLowerCase().equals("true") || obj.toString().equals("1");
		}

	}

	public String getString(String key, String defaultValue)
	{
		try
		{
			return getString(key);
		}
		catch (Exception ex)
		{
			return defaultValue;
		}
	}

	public String getString(String key) throws PMKeyNotFoundException
	{
		if (!dictionary.containsKey(key))
			throw new PMKeyNotFoundException(PMConstants.Exceptions.KEY_NOT_FOUND + " " + key);

		Object obj = dictionary.get(key);
		if (obj == null)
			return null;
		else
			return obj.toString();

	}

	public int getInt(String key, int defaultValue)
	{
		try
		{
			return getInt(key);
		}
		catch (Exception ex)
		{
			return defaultValue;
		}
	}

	public int getInt(String key) throws PMKeyNotFoundException, NullPointerException,
		InvalidClassException
	{
		if (!dictionary.containsKey(key))
			throw new PMKeyNotFoundException(PMConstants.Exceptions.KEY_NOT_FOUND + " " + key);

		Object obj = dictionary.get(key);
		if (obj == null)
			throw new NullPointerException(PMConstants.Exceptions.KEY_NOT_FOUND + " " + key);

		if (obj.getClass().isAssignableFrom(int.class))
		{
			return (Integer) obj;
		}
		else
		{
			return Integer.parseInt(getString(key));
		}

	}

	public PMDictionary getObject(String key, PMDictionary defaultValue)
	{
		try
		{
			PMDictionary d = getObject(key);
			return d != null ? d : defaultValue;
		}
		catch (PMKeyNotFoundException ex)
		{
			return defaultValue;
		}
	}


	public PMDictionary getObject(String key) throws PMKeyNotFoundException
	{
		if (!dictionary.containsKey(key))
			throw new PMKeyNotFoundException(PMConstants.Exceptions.KEY_NOT_FOUND + " " + key);
		Object obj = dictionary.get(key);
		if (obj == null)
			return null;
		else
			return (PMDictionary) obj;
	}

	public PMArray getArray(String key, PMArray defaultValue)
	{
		try
		{
			PMArray a = getArray(key);
			return a != null ? a : defaultValue;
		}
		catch (PMKeyNotFoundException ex)
		{
			return defaultValue;
		}
	}

	public PMArray getArray(String key) throws PMKeyNotFoundException
	{
		if (!dictionary.containsKey(key))
			throw new PMKeyNotFoundException(PMConstants.Exceptions.KEY_NOT_FOUND + " " + key);

		Object obj = dictionary.get(key);
		if (obj == null)
			return null;
		else
			return (PMArray) obj;
	}


	/**
	 * Recursive method to populate PMDictionary from JSON
	 * 
	 * @param JSONObj
	 *        input JSONObject
	 * @param parentObj
	 *        PMDictionary parent
	 * @throws Exception
	 */
	private static void parseJsonObject(JSONObject JSONObj, PMDictionary parentObj)
		throws Exception
	{
		String key;
		Iterator<?> keys = JSONObj.keys();
		while (keys.hasNext())
		{
			key = keys.next().toString();
			Object value = JSONObj.get(key);
			if (value == null)
			{
				parentObj.put(key, (String) null);
			}
			if (value.getClass().equals(String.class))
			{
				parentObj.put(key, (String) value);
			}
			if (value.getClass().equals(Boolean.class))
			{
				parentObj.put(key, (Boolean) value);
			}
			if (value.getClass().equals(Integer.class))
			{
				parentObj.put(key, (Integer) value);
			}
			if (value.getClass().equals(JSONObject.class))
			{
				JSONObject childJSONObj = (JSONObject) value;
				PMDictionary childPMDictionary = new PMDictionary();
				parseJsonObject(childJSONObj, childPMDictionary);
				parentObj.put(key, childPMDictionary);
			}
			if (value.getClass().equals(JSONArray.class))
			{
				JSONArray jsonArray = (JSONArray) value;
				parentObj.put(key, new PMArray(jsonArray));
			}
		}
	}


	@Override
	public String toString()
	{
		return dictionary.toString();
	}
}
