package com.panacea.sdk.model;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Object representation of all responses returned from the Panacea server.
 * 
 * @author Cobi Interactive
 */
public class PMBaseResponse
{
	private int status;
	private String message;
	private String description;

	private Object details;

	private PMPagination pagination;

	public PMBaseResponse()
	{
	}

	public PMBaseResponse(String JSONStr) throws Exception
	{
		this(new JSONObject(JSONStr));
	}

	public PMBaseResponse(JSONObject JSONObj) throws Exception
	{
		super();
		parseJsonObject(JSONObj);
	}


	@Override
	public String toString()
	{
		return new String("status: " + status + "\nmessage: " + message + "\ndescription: "
			+ description + "\ndetails: " + details);
	}


	protected void parseJsonObject(JSONObject JSONObj) throws Exception
	{
		String key;
		Iterator<?> keys = JSONObj.keys();
		while (keys.hasNext())
		{
			key = keys.next().toString();
			if ("status".equals(key))
			{
				status = JSONObj.getInt(key);
			}
			else if ("message".equals(key))
			{
				message = JSONObj.getString(key);
			}
			else if ("description".equals(key))
			{
				description = JSONObj.getString(key);
			}
			else if ("details".equals(key))
			{
				Object detailsObject = JSONObj.get("details");

				if (detailsObject.getClass().equals(String.class))
				{
					details = (String) detailsObject;
				}
				else if (detailsObject.getClass().equals(JSONObject.class))
				{
					details = new PMDictionary((JSONObject) detailsObject);

					PMDictionary pages = ((PMDictionary) details).getObject("pagination", null);

					if (pages != null)
					{
						pagination = new PMPagination(pages);
						PMArray messagesArray = ((PMDictionary) details).getArray("data");
						details = messagesArray;
					}
				}
				else if (detailsObject.getClass().equals(JSONArray.class))
				{
					details = new PMArray((JSONArray) detailsObject);
				}
			}
		}
	}

	public int getStatus()
	{
		return status;
	}

	public String getMessage()
	{
		return message;
	}

	public String getDescription()
	{
		return description;
	}

	public Object getDetails()
	{
		return details;
	}

	public PMPagination getPagination()
	{
		return pagination;
	}
}
