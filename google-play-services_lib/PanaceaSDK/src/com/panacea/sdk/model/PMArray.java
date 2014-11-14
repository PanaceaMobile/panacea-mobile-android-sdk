package com.panacea.sdk.model;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Used for passing Arrays, for example when issuing requests or receiving
 * response data. <br/>
 * The value may be one of the following types: <br/>
 * <ul>
 * <li>String
 * <li>boolean
 * <li>int
 * <li>PMDictionary
 * <li>PMArray
 * </ul>
 * 
 * @author Cobi Interactive
 */
public class PMArray
{
	private static final String NO_INDEX_EX = "PMArray does not contain a value at index ";

	private ArrayList<Object> array = new ArrayList<Object>();

	/**
	 * Constructs an empty PMArray
	 */
	public PMArray()
	{
	}

	/**
	 * Construct and parse a PMArray from a JSON String.
	 * 
	 * @param json
	 *        the JSON formatted string
	 */
	public PMArray(String json) throws Exception
	{
		this(new JSONArray(json));
	}

	protected PMArray(JSONArray jsonArray) throws Exception
	{
		for (int i = 0; i < jsonArray.length(); i++)
		{
			if (jsonArray.isNull(i))
			{
				array.add(null);
			}
			else
			{
				Object value = jsonArray.get(i);
				if (value.getClass().equals(JSONObject.class))
				{
					PMDictionary dictionary = new PMDictionary((JSONObject) value);
					array.add(dictionary);
				}
				else if (value.getClass().equals(JSONArray.class))
				{
					JSONArray a = (JSONArray) value;
					array.add(new PMArray(a));
				}
				else
				{
					array.add(value);
				}
			}
		}
	}

	/**
	 * return the array's length
	 * 
	 * @return the array's length
	 */
	public int length()
	{
		return this.array.size();
	}

	public void add(String val)
	{
		array.add(val);
	}

	public void add(boolean val)
	{
		array.add(val);
	}

	public void add(int val)
	{
		array.add(val);
	}

	public void add(long val)
	{
		array.add(val);
	}

	public void add(PMDictionary val)
	{
		array.add(val);
	}

	public void add(PMArray val)
	{
		array.add(val);
	}

	public void add(double val)
	{
		array.add(val);
	}

	public String getString(int index)
	{
		Object obj = array.get(index);
		if (obj == null)
			return null;
		else
			return obj.toString();
	}

	public boolean getBool(int index)
	{
		Object obj = array.get(index);
		if (obj == null)
			throw new NullPointerException(NO_INDEX_EX + index);

		if (obj.getClass().isAssignableFrom(Boolean.class))
		{
			return (Boolean) obj;
		}
		else
		{
			return obj.toString().toLowerCase().equals("true") || obj.toString().equals("1");
		}
	}

	public int getInt(int index)
	{
		Object obj = array.get(index);
		if (obj == null)
			throw new NullPointerException(NO_INDEX_EX + index);

		if (obj.getClass().isAssignableFrom(int.class))
		{
			return (Integer) obj;
		}
		else
		{
			return Integer.parseInt(getString(index));
		}
	}


	public double getDouble(int index)
	{
		Object obj = array.get(index);
		if (obj == null)
			throw new NullPointerException(NO_INDEX_EX + index);

		if (obj.getClass().isAssignableFrom(double.class))
		{
			return (Double) obj;
		}
		else
		{
			return Double.parseDouble(getString(index));
		}
	}

	public PMDictionary getObject(int index)
	{
		Object obj = array.get(index);
		if (obj == null)
			return null;
		else
			return (PMDictionary) obj;
	}

	public PMArray getArray(int index)
	{
		Object obj = array.get(index);
		if (obj == null)
			return null;
		else
			return (PMArray) obj;
	}

	@Override
	public String toString()
	{
		return array.toString();
	}
}
