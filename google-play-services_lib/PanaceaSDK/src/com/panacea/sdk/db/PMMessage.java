package com.panacea.sdk.db;

import java.util.Date;

import com.panacea.sdk.PMUtils;
import com.panacea.sdk.model.PMDictionary;

/**
 * Object representation for data saved in the database -
 * {@link PMDatabaseHelper}. Base PMMessage contains shared fields for both
 * {@link PMReceivedMessage} and {@link PMSentMessage}
 * 
 * @see PMReceivedMessage
 * @see PMSentMessage
 * @author Cobi Interactive
 */
public abstract class PMMessage implements Comparable<PMMessage>
{
	private static final String CREATED_FORMAT = "yyyy-MM-dd hh:mm:ss";

	//	ISO8601 formats
	//	"yyyy-MM-dd'T'HH:mm:ss.SSSZ"	2001-07-04T12:08:56.235-0700
	//	"yyyy-MM-dd'T'HH:mm:ss.SSSXXX"	2001-07-04T12:08:56.235-07:00
	//	"yyyy-MM-dd'T'HH:mm:ssZ"	    2013-12-20T08:58:28+00:00

	private static final String CREATED_ISO8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

	//	private Date age;
	private int receivedMessageId;
	private String text;
	private Date created;
	private int applicationID;
	private int deviceID;
	private String threadID;

	public PMMessage()
	{
	}

	public PMMessage(PMDictionary message)
	{
		super();

		this.setApplicationID(message.getInt("application_id", -1));
		this.setDeviceID(message.getInt("device_id", -1));
		this.setText(message.getString("data", null));
		this.setReceivedMessageId(message.getInt("outbound_message_id", -1));
		this.setThreadID(message.getString("thread_id", "-1"));

		this.setCreated(message.getString("created_iso8601", null));

		//fall back to created field format
		if (this.created == null)
			this.setCreated(message.getString("created", null));
	}

	@Override
	public String toString()
	{
		return "PMMessage [receivedMessageId=" + receivedMessageId + ", text=" + text
			+ ", created=" + created + ", applicationID=" + applicationID + ", deviceID="
			+ deviceID + ", threadID=" + threadID + "]";
	}

	public Date getCreated()
	{
		return created;
	}

	public String getCreatedString()
	{
		return PMUtils.dateToString(CREATED_ISO8601_FORMAT, created);
	}

	public void setCreated(String created)
	{
		this.created = PMUtils.stringToDate(CREATED_ISO8601_FORMAT, created);

		//fall back to created field format
		if (this.created == null)
			this.created = PMUtils.stringToDate(CREATED_FORMAT, created);
	}

	public void setCreated(Date created)
	{
		this.created = created;
	}

	public String getThreadID()
	{
		return threadID;
	}

	public void setThreadID(String thread_id)
	{
		this.threadID = thread_id;
	}

	public int getReceivedMessageId()
	{
		return receivedMessageId;
	}

	public void setReceivedMessageId(int receivedMessageId)
	{
		this.receivedMessageId = receivedMessageId;
	}

	public String getText()
	{
		return text;
	}

	public void setText(String text)
	{
		this.text = text;
	}

	public int getApplicationID()
	{
		return applicationID;
	}

	public void setApplicationID(int applicationID)
	{
		this.applicationID = applicationID;
	}

	public int getDeviceID()
	{
		return deviceID;
	}

	public void setDeviceID(int deviceID)
	{
		this.deviceID = deviceID;
	}

	@Override
	public int compareTo(PMMessage another)
	{
		return created.compareTo(another.getCreated());
	}
}
