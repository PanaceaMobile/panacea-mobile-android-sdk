package com.panacea.sdk.db;

import java.util.LinkedList;
import java.util.List;

import com.panacea.sdk.model.PMArray;
import com.panacea.sdk.model.PMDictionary;

/**
 * Object representation for data saved in the database -
 * {@link PMDatabaseHelper}. Messages sent to Panacea
 * 
 * @see PMMessage
 * @see PMReceivedMessage
 * @author Cobi Interactive
 */
public class PMSentMessage extends PMMessage
{
	private int sentMessageId;

	public PMSentMessage()
	{
	}

	public PMSentMessage(PMDictionary message)
	{
		super(message);

		this.setSentMessageId(message.getInt("inbound_message_id", -1));
	}

	@Override
	public String toString()
	{
		return super.toString() + "\nPMSentMessage [sentMessageId=" + sentMessageId + "]";
	}

	public int getSentMessageId()
	{
		return sentMessageId;
	}

	public void setSentMessageId(int sentMessageId)
	{
		this.sentMessageId = sentMessageId;
	}

	public static List<PMMessage> parseSentMessagesArray(PMArray messagesArray)
	{
		List<PMMessage> messages = new LinkedList<PMMessage>();

		for (int i = 0; i < messagesArray.length(); i++)
		{
			messages.add(new PMSentMessage(messagesArray.getObject(i)));
		}

		return messages;
	}
}
