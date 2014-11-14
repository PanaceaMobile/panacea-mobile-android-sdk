package com.panacea.sdk.db;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;

import com.panacea.sdk.PMUtils;
import com.panacea.sdk.R;

public class PMDatabaseCipherHelper extends SQLiteOpenHelper
{

	private static final String TAG = "PDDatabaseCipherHelper";

	/* Database Version */
	private static final int DATABASE_VERSION = 1;
	/* Database Name */
	private static final String DATABASE_NAME = "PanaceaEncrypted"; //Panacea.db
	/* Database Password */
	private static final String DATABASE_PASSWORD = "test";


	private Context mContext;

	/**
	 * Singleton instance of PDDatabaseCipherHelper
	 */
	private static PMDatabaseCipherHelper mInstance = null;

	/**
	 * Static access to singleton instance of PDDatabaseCipherHelper
	 * 
	 * @param context
	 * @return PDDatabaseCipherHelper static instance
	 */
	public static PMDatabaseCipherHelper getInstance(Context context)
	{
		if (mInstance == null)
		{
			SQLiteDatabase.loadLibs(context);
			mInstance = new PMDatabaseCipherHelper(context.getApplicationContext());
		}
		return mInstance;
	}

	private PMDatabaseCipherHelper(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		mContext = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		TableReceivedMessagesCipher.onCreate(db);
		TablesSentMessagesCipher.onCreate(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		TableReceivedMessagesCipher.onUpgrade(db, oldVersion, newVersion);
		TablesSentMessagesCipher.onUpgrade(db, oldVersion, newVersion);
	}

	//	@Override
	//	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion)
	//	{
	//		TableReceivedMessagesCipher.onDowngrade(db, oldVersion, newVersion);
	//		TablesSentMessagesCipher.onDowngrade(db, oldVersion, newVersion);
	//	}

	/**
	 * Drops all tables
	 * 
	 * @param db
	 */
	//	private void deleteMessageCache(SQLiteDatabase db)
	//	{
	//		Log.d(TAG, "deleteMessageCache");
	//
	//		db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECEIVED);
	//		db.execSQL("DROP TABLE IF EXISTS " + TABLE_SENT);
	//
	//		onCreate(db);
	//	}

	/**
	 * Drops all tables in Database
	 */
	public void deleteMessageCache()
	{
		SQLiteDatabase db = this.getWritableDatabase(DATABASE_PASSWORD);
		TableReceivedMessagesCipher.onClean(db);
		TablesSentMessagesCipher.onClean(db);
	}

	/**
	 * Adds a row to table for a {@link PMReceivedMessage}
	 * 
	 * @param message
	 */
	public void addReceivedMessage(PMReceivedMessage message)
	{
		Log.d("addReceivedMessage", message.toString());

		SQLiteDatabase db = this.getWritableDatabase(DATABASE_PASSWORD);
		TableReceivedMessagesCipher.addReceivedMessage(db, message);
	}

	/**
	 * Adds a row to table for a {@link PMSentMessage}
	 * 
	 * @param message
	 */
	public void addSentMessage(PMSentMessage message)
	{
		Log.d("addSentMessage", message.toString());

		SQLiteDatabase db = this.getWritableDatabase(DATABASE_PASSWORD);
		TablesSentMessagesCipher.addSentMessage(db, message);
	}

	/**
	 * Adds a list of messages to relevant db tables
	 * 
	 * @param messages
	 */
	public void addMessages(List<PMMessage> messages)
	{
		SQLiteDatabase db = this.getWritableDatabase(DATABASE_PASSWORD);
		db.beginTransaction();
		try
		{
			for (PMMessage message : messages)
			{
				if (message instanceof PMReceivedMessage)
					TableReceivedMessagesCipher.addReceivedMessage(db, (PMReceivedMessage) message);
				else
					TablesSentMessagesCipher.addSentMessage(db, (PMSentMessage) message);

				db.yieldIfContendedSafely();
			}
			db.setTransactionSuccessful();
		}
		finally
		{
			db.endTransaction();
		}
	}

	/**
	 * Retrieves a received message from database for given message id
	 * 
	 * @param messageID
	 * @return PMMessage
	 */
	public PMMessage getReceivedMessage(int messageID)
	{
		SQLiteDatabase db = this.getReadableDatabase(DATABASE_PASSWORD);
		return TableReceivedMessagesCipher.getReceivedMessage(db, messageID);
	}

	/**
	 * Retrieves a sent message from database for given message id
	 * 
	 * @param messageID
	 * @return PMMessage
	 */
	public PMMessage getSentMessage(int messageID)
	{
		// get reference to readable DB
		SQLiteDatabase db = this.getReadableDatabase(DATABASE_PASSWORD);
		return TablesSentMessagesCipher.getSentMessage(db, messageID);
	}

	/**
	 * List of all received messages WARNING: LARGE TASK NOT ASYNCHRONOUS
	 * 
	 * @return List of all received messages
	 */
	@Deprecated
	public List<PMReceivedMessage> getAllReceivedMessages()
	{
		SQLiteDatabase db = this.getReadableDatabase(DATABASE_PASSWORD);
		return TableReceivedMessagesCipher.getAllReceivedMessages(db);
	}


	/**
	 * returns the largest (and newest) received message id
	 * 
	 * @return latest message id
	 */
	public Integer getLastReceivedMessageId()
	{
		SQLiteDatabase db = this.getReadableDatabase(DATABASE_PASSWORD);
		return TableReceivedMessagesCipher.getLastReceivedMessageId(db);
	}

	/**
	 * returns the largest (and newest) sent message id
	 * 
	 * @return latest message id
	 */
	public Integer getLastSentMessageId()
	{
		SQLiteDatabase db = this.getReadableDatabase(DATABASE_PASSWORD);
		return TablesSentMessagesCipher.getLastSentMessageId(db);
	}

	/**
	 * Returns a list of unique subjects and their respective unread count. This
	 * includes "All updates" WARNING: LARGE TASK NOT ASYNCHRONOUS. Rather use
	 * {@link CursorAdapter} with {@link #getCursorSubjectCounts()}
	 * 
	 * @return HashMap of unique subjects and how many unread messages each
	 *         subject has
	 */
	@Deprecated
	public LinkedHashMap<String, Integer> getSubjectCounts()
	{
		Log.d(TAG, "getSubjectCounts");

		SQLiteDatabase db = this.getReadableDatabase(DATABASE_PASSWORD);
		return TableReceivedMessagesCipher.getSubjectCounts(db, mContext);
	}


	/**
	 * Returns the latest received message with a given subject for each
	 * different thread id. "All updates" will return the latest received
	 * message for each subject with a different thread id. WARNING: LARGE TASK
	 * NOT ASYNCHRONOUS. Rather use {@link CursorAdapter} with
	 * {@link #getCursorMessagesForSubject(String)}
	 * 
	 * @param subject
	 * @return List of latest receivedMessage for each theadID
	 */
	public List<PMReceivedMessage> getMessagesForSubject(String subject)
	{
		return TableReceivedMessagesCipher.getMessagesForSubject(
			getReadableDatabase(DATABASE_PASSWORD), subject);
	}

	/**
	 * Returns a sorted list of received and sent messages for the thread id.
	 * Sorted oldest to newest WARNING: NOT ASYNCHRONOUS Rather use
	 * {@link CursorAdapter} with {@link #getCursorMessagesForThreadId(String)}
	 * 
	 * @param threadId
	 * @return List of messages in given thread
	 */
	public List<PMMessage> getMessagesForThreadId(String threadId)
	{
		Log.d(TAG, "getMessagesForThreadId");

		List<PMMessage> messages = new ArrayList<PMMessage>();

		Cursor cursor = getCursorMessagesForThreadId(threadId);

		PMMessage msg = null;
		if (cursor.moveToFirst())
		{
			do
			{
				msg = PMDatabaseCipherHelper.cursorToMessage(cursor);
				messages.add(msg);
			}
			while (cursor.moveToNext());
		}
		cursor.close();

		return messages;
	}

	/**
	 * Get the {@link Cursor} used to display the unique subjects and counts. If
	 * no subjects then null is returned.
	 * 
	 * @return Cursor or null
	 */
	public Cursor getCursorSubjectCounts()
	{
		SQLiteDatabase db = this.getReadableDatabase(DATABASE_PASSWORD);
		String all = mContext.getResources().getString(R.string.all_subjects);
		return TableReceivedMessagesCipher.getCursorSubjectCounts(db, all);
	}

	/**
	 * Get the {@link Cursor} used to display Messages for a given subject
	 * 
	 * @return Cursor
	 */
	public Cursor getCursorMessagesForSubject(String subject)
	{
		SQLiteDatabase db = this.getReadableDatabase(DATABASE_PASSWORD);
		return TableReceivedMessagesCipher.getCursorMessagesForSubject(db, subject);
	}

	/**
	 * Get the {@link Cursor} used to display Messages for a given thread id.
	 * This returns {@link PMReceivedMessage}'s and {@link PMSentMessage}'s.
	 * {@link #cursorToMessage(Cursor)} can parse this cursor into the relevant
	 * message .
	 * 
	 * @return Cursor
	 */
	public Cursor getCursorMessagesForThreadId(String threadId)
	{
		SQLiteDatabase db = this.getReadableDatabase(DATABASE_PASSWORD);
		String query = "SELECT"
			+ " r._id as '_id', '' as 'received_message_id', r.subject as 'subject', r.status as 'status', r.text as 'text', r.created as 'created', r.thread_id as 'thread_id',  r.device_id as 'device_id', r.application_id as 'application_id', r.deleted as 'deleted'"
			+ " FROM pm_received_messages r"
			+ " WHERE r.deleted=0 AND r.thread_id = '"
			+ threadId
			+ "'"
			+ " UNION ALL"
			+ " SELECT"
			+ " s._id as '_id', s.received_message_id as 'received_message_id', '' as 'subject', '' as 'status', s.text as 'text', s.created as 'created', s.thread_id as 'thread_id',  s.device_id as 'device_id', s.application_id as 'application_id', s.deleted as 'deleted'"
			+ " FROM pm_sent_messages s" + " WHERE s.deleted=0 AND s.thread_id = '" + threadId
			+ "'" + " ORDER BY created";

		return db.rawQuery(query, null);
	}


	/**
	 * Returns all received messages that have not been read yet. This is used
	 * when displaying notifications. WARNING: NOT ASYNCHRONOUS
	 * 
	 * @return list of unread received messages
	 */
	public List<PMReceivedMessage> getAllUnreadMessages()
	{
		Log.d(TAG, "getAllUnreadMessages");

		SQLiteDatabase db = this.getReadableDatabase(DATABASE_PASSWORD);
		return TableReceivedMessagesCipher.getAllUnreadMessages(db);
	}

	/**
	 * Marks a single message as deleted.
	 * 
	 * @param message
	 * @param deleted
	 *        if true marks as deleted otherwise not deleted
	 */
	public void markMessage(PMMessage message, boolean deleted)
	{
		// get reference to writable DB
		SQLiteDatabase db = this.getWritableDatabase(DATABASE_PASSWORD);

		if (message instanceof PMReceivedMessage)
		{
			TableReceivedMessagesCipher.markMessage(db, (PMReceivedMessage) message, deleted);
		}
		else if (message instanceof PMSentMessage)
		{
			TablesSentMessagesCipher.markMessage(db, (PMSentMessage) message, deleted);
		}
	}

	/**
	 * Marks all the messages in a thread as deleted
	 * 
	 * @param threadID
	 * @param deleted
	 *        if true marks as deleted otherwise not deleted
	 */
	public void markThread(String threadID, boolean deleted)
	{
		// get reference to writable DB
		SQLiteDatabase db = this.getWritableDatabase(DATABASE_PASSWORD);

		TableReceivedMessagesCipher.markThread(db, threadID, deleted);
		TablesSentMessagesCipher.markThread(db, threadID, deleted);
	}

	/**
	 * Marks an entire subject as deleted
	 * 
	 * @param subject
	 * @param deleted
	 *        if true marks as deleted otherwise not deleted
	 */
	public void markSubject(String subject, boolean deleted)
	{
		// get reference to writable DB
		SQLiteDatabase db = this.getWritableDatabase(DATABASE_PASSWORD);
		TableReceivedMessagesCipher.markSubject(db, subject, deleted);
	}

	/**
	 * removes the deleted flag from all messages in the database, thereby
	 * 'undeleting' them
	 */
	public void markAll(boolean deleted)
	{
		// get reference to writable DB
		SQLiteDatabase db = this.getWritableDatabase(DATABASE_PASSWORD);

		TableReceivedMessagesCipher.markAll(db, deleted);
		TablesSentMessagesCipher.markAll(db, deleted);
	}


	/**
	 * updates the status of a received message message. AKA marks it as read.
	 * 
	 * @param receivedMessageId
	 */
	public void markMessageAsRead(int receivedMessageId)
	{
		SQLiteDatabase db = this.getWritableDatabase(DATABASE_PASSWORD);
		TableReceivedMessagesCipher.markMessageAsRead(db, receivedMessageId);
	}


	/**
	 * Only used for {@link #getCursorMessagesForThreadId}, Where both
	 * {@link PMReceivedMessage}'s and {@link PMSentMessage}'s are returned in
	 * the same query.
	 * 
	 * @param cursor
	 * @return PMMessage
	 */
	public static PMMessage cursorToMessage(Cursor cursor)
	{
		if (PMUtils.isBlankOrNull(cursor.getString(cursor.getColumnIndex("received_message_id"))))
			return TableReceivedMessagesCipher.cursorToReceivedMessage(cursor);
		else
			return TablesSentMessagesCipher.cursorToSentMessage(cursor);
	}


}
