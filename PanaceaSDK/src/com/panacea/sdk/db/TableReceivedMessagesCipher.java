package com.panacea.sdk.db;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import net.sqlcipher.database.SQLiteDatabase;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.panacea.sdk.PMUtils;
import com.panacea.sdk.R;

public class TableReceivedMessagesCipher
{
	/* TABLE NAME */
	public static final String TABLE_NAME = "pm_received_messages";

	/* COLUMN NAMES */
	private static final String KEY_RECEIVED_MESSAGE_ID = "_id";
	private static final String KEY_SUBJECT = "subject";
	private static final String KEY_STATUS = "status";
	private static final String KEY_TEXT = "text";
	private static final String KEY_CREATED = "created";
	private static final String KEY_THREAD_ID = "thread_id";
	private static final String KEY_DEVICE_ID = "device_id";
	private static final String KEY_APPLICATION_ID = "application_id";
	private static final String KEY_DELETED = "deleted";

	private static final String[] COLUMNS_RECEIVED =
		{ KEY_RECEIVED_MESSAGE_ID, KEY_SUBJECT, KEY_STATUS, KEY_TEXT, KEY_CREATED, KEY_THREAD_ID,
			KEY_DEVICE_ID, KEY_APPLICATION_ID, KEY_DELETED };


	public static void onCreate(SQLiteDatabase db)
	{
		String CREATE_RECEIVED_TABLE = String
			.format(
				"CREATE TABLE %s (%s INTEGER PRIMARY KEY, %s TEXT, %s INTEGER, %s TEXT, %s TEXT, %s TEXT, %s INTEGER, %s INTEGER, %s INTEGER DEFAULT 0)",
				TABLE_NAME, //COLUMNS_RECEIVED);
				KEY_RECEIVED_MESSAGE_ID, KEY_SUBJECT, KEY_STATUS, KEY_TEXT, KEY_CREATED,
				KEY_THREAD_ID, KEY_DEVICE_ID, KEY_APPLICATION_ID, KEY_DELETED);
		//"CREATE TABLE %s, %s FROM %s ORDER BY %s",
		//			DatabaseHelper.TITLE, DatabaseHelper.VALUE,
		//			DatabaseHelper.TABLE, DatabaseHelper.TITLE);

		db.execSQL(CREATE_RECEIVED_TABLE);
	}

	public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
		onCreate(db);
	}

	public static void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
		onCreate(db);
	}

	public static void onClean(SQLiteDatabase database)
	{
		database.execSQL("DELETE FROM " + TABLE_NAME);
	}

	public static PMReceivedMessage cursorToReceivedMessage(Cursor cursor)
	{
		PMReceivedMessage message = new PMReceivedMessage();

		//efficient
		//		message.setReceivedMessageId(Integer.parseInt(cursor.getString(0)));
		//		message.setSubject(cursor.getString(1));
		//		message.setStatus(Integer.parseInt(cursor.getString(2)));
		//		message.setText(cursor.getString(3));
		//		message.setCreated(cursor.getString(4));
		//		message.setThreadID(Integer.parseInt(cursor.getString(5)));
		//		message.setDeviceID(Integer.parseInt(cursor.getString(6)));
		//		message.setApplicationID(Integer.parseInt(cursor.getString(7)));


		//robust
		message.setReceivedMessageId(Integer.parseInt(cursor.getString(cursor
			.getColumnIndex(KEY_RECEIVED_MESSAGE_ID))));
		message.setSubject(cursor.getString(cursor.getColumnIndex(KEY_SUBJECT)));
		message.setStatus(Integer.parseInt(cursor.getString(cursor.getColumnIndex(KEY_STATUS))));
		message.setText(cursor.getString(cursor.getColumnIndex(KEY_TEXT)));
		message.setCreated(cursor.getString(cursor.getColumnIndex(KEY_CREATED)));
		message
			.setThreadID(cursor.getString(cursor.getColumnIndex(KEY_THREAD_ID)));
		message
			.setDeviceID(Integer.parseInt(cursor.getString(cursor.getColumnIndex(KEY_DEVICE_ID))));
		message.setApplicationID(Integer.parseInt(cursor.getString(cursor
			.getColumnIndex(KEY_DELETED))));


		return message;
	}

	/**
	 * Adds a row to table for a {@link PMReceivedMessage}
	 * 
	 * @param message
	 */
	public static void addReceivedMessage(SQLiteDatabase db, PMReceivedMessage message)
	{
		//create ContentValues to add key "column"/value
		ContentValues values = new ContentValues();
		values.put(KEY_RECEIVED_MESSAGE_ID, message.getReceivedMessageId());
		values.put(KEY_SUBJECT, message.getSubject());
		values.put(KEY_STATUS, message.getStatus());
		values.put(KEY_TEXT, message.getText());
		values.put(KEY_CREATED, message.getCreatedString());
		values.put(KEY_THREAD_ID, message.getThreadID());
		values.put(KEY_APPLICATION_ID, message.getApplicationID());
		values.put(KEY_DEVICE_ID, message.getDeviceID());


		// try updating row
		int i = db.update(TABLE_NAME, //table
			values, // column/value
			KEY_RECEIVED_MESSAGE_ID + " = ?", // selections
			new String[]
				{ String.valueOf(message.getReceivedMessageId()) }); //selection args

		// if no row updated then insert
		if (i == 0)
		{
			db.insert(TABLE_NAME, // table
				null, //nullColumnHack
				values); // key/value -> keys = column names/ values = column values
		}
	}

	/**
	 * removes given message from table
	 * 
	 * @param message
	 */
	public static void deleteMessage(SQLiteDatabase db, PMMessage message)
	{
		// delete
		db.delete(TABLE_NAME, //table name
			KEY_RECEIVED_MESSAGE_ID + " = ?", // selections
			new String[]
				{ String.valueOf(message.getReceivedMessageId()) }); //selections args
	}

	/**
	 * Retrieves a received message from database for given message id
	 * 
	 * @param messageID
	 * @return PMMessage
	 */
	public static PMMessage getReceivedMessage(SQLiteDatabase db, int messageID)
	{
		// build query
		Cursor cursor = db.query(TABLE_NAME, // a. table
			COLUMNS_RECEIVED, // b. column names
			" " + KEY_RECEIVED_MESSAGE_ID + "  = ?", // c. selections 
			new String[]
				{ String.valueOf(messageID) }, // d. selections args
			null, // e. group by
			null, // f. having
			null, // g. order by
			null); // h. limit

		// if we got results get the first one
		if (cursor != null)
			cursor.moveToFirst();

		// build object
		PMMessage message = cursorToReceivedMessage(cursor);

		cursor.close();

		return message;
	}

	public static Cursor getCursorAllReceivedMessages(SQLiteDatabase db)
	{
		String query = "SELECT  * FROM " + TABLE_NAME;
		return db.rawQuery(query, null);
	}

	/**
	 * List of all received messages
	 * 
	 * @return List of all received messages
	 */
	public static List<PMReceivedMessage> getAllReceivedMessages(SQLiteDatabase db)
	{
		List<PMReceivedMessage> messages = new LinkedList<PMReceivedMessage>();

		Cursor cursor = getCursorAllReceivedMessages(db);

		db.beginTransaction();
		try
		{
			cursor.moveToFirst();
			while (!cursor.isAfterLast())
			{
				PMReceivedMessage message = cursorToReceivedMessage(cursor);
				messages.add(message);
				cursor.moveToNext();
				//db.yieldIfContendedSafely();
			}
			db.setTransactionSuccessful();
		}
		finally
		{
			db.endTransaction();
		}
		cursor.close();

		return messages;
	}


	public static Cursor getCursorMessagesForSubject(SQLiteDatabase db, String subject)
	{
		String WHERE = "WHERE deleted=0 "
			+ ((subject != null) ? ("AND subject='" + subject + "' ") : "");
		String query = "SELECT * FROM pm_received_messages " + WHERE
			+ "GROUP BY thread_id ORDER BY created DESC";

		return db.rawQuery(query, null);
	}

	public static List<PMReceivedMessage> getMessagesForSubject(SQLiteDatabase db, String subject)
	{
		List<PMReceivedMessage> messages = new ArrayList<PMReceivedMessage>();

		Cursor cursor = getCursorMessagesForSubject(db, subject);

		cursor.moveToFirst();
		while (!cursor.isAfterLast())
		{
			PMReceivedMessage message = cursorToReceivedMessage(cursor);
			messages.add(message);
			cursor.moveToNext();
		}
		cursor.close();

		return messages;
	}


	public static Cursor getCursorSubjectCounts(SQLiteDatabase db, String allUpdatesText)
	{
		if (allUpdatesText == null)
			allUpdatesText = "All updates";

		String query = "SELECT '"
			+ allUpdatesText
			+ "' AS _id, COUNT(status)AS unread FROM pm_received_messages WHERE status<>128 AND deleted=0"
			+ " UNION ALL"
			+ " SELECT subjects.subject AS _id, counts.unread FROM (SELECT subject FROM pm_received_messages WHERE deleted=0 GROUP BY subject ORDER BY created DESC) AS subjects "
			+ " LEFT JOIN"
			+ " (SELECT subject, COUNT(status)AS unread FROM pm_received_messages WHERE status<>128 AND deleted=0 GROUP BY subject) AS counts"
			+ " ON subjects.subject=counts.subject";

		//		String query = "SELECT subjects.subject, counts.unread FROM (SELECT subject FROM pm_received_messages WHERE deleted=0 GROUP BY subject ORDER BY created DESC) AS subjects "
		//			+ "LEFT JOIN "
		//			+ "(SELECT subject, COUNT(status)AS unread FROM pm_received_messages WHERE status<>128 AND deleted=0 GROUP BY subject) AS counts "
		//			+ "ON subjects.subject=counts.subject";

		Cursor result = db.rawQuery(query, null);

		if (result.getCount() <= 1)
			return null;

		return result;
	}

	/**
	 * Returns a list of unique subjects and their respective unread count. This
	 * includes "All updates"
	 * 
	 * @return HashMap of unique subjects and how many unread messages each
	 *         subject has
	 */
	public static LinkedHashMap<String, Integer> getSubjectCounts(SQLiteDatabase db, Context context)
	{
		LinkedHashMap<String, Integer> map = new LinkedHashMap<String, Integer>();

		String all = context.getResources().getString(R.string.all_subjects);
		Cursor cursor = getCursorSubjectCounts(db, all);
		if (cursor.moveToFirst())
		{
			do
			{
				String subject = cursor.getString(0);

				String countString = cursor.getString(1);
				Integer unreadCount = countString == null ? 0 : Integer.valueOf(countString);

				map.put(subject, unreadCount);

			}
			while (cursor.moveToNext());

		}

		cursor.close();

		return map;
	}

	/**
	 * returns the largest (and newest) received message id
	 * 
	 * @return latest message id
	 */
	public static Integer getLastReceivedMessageId(SQLiteDatabase db)
	{
		String query = "SELECT MAX(" + KEY_RECEIVED_MESSAGE_ID + ") FROM " + TABLE_NAME;

		Cursor cursor = db.rawQuery(query, null);
		Integer last = null;
		if (cursor.moveToFirst())
		{
			do
			{
				String int_string = cursor.getString(0);
				if (int_string != null)
					last = Integer.parseInt(int_string);
			}
			while (cursor.moveToNext());
		}

		cursor.close();

		return last;
	}

	/**
	 * Returns all received messages that have not been read yet. This is used
	 * when displaying notifications.
	 * 
	 * @return list of unread received messages
	 */
	public static List<PMReceivedMessage> getAllUnreadMessages(SQLiteDatabase db)
	{
		String query = "SELECT * " + "FROM pm_received_messages "
			+ "WHERE status<>128 AND deleted=0 " + "ORDER BY created DESC";

		List<PMReceivedMessage> messages = new ArrayList<PMReceivedMessage>();
		Cursor cursor = db.rawQuery(query, null);
		PMReceivedMessage message = null;
		if (cursor.moveToFirst())
		{
			do
			{
				message = cursorToReceivedMessage(cursor);
				messages.add(message);
			}
			while (cursor.moveToNext());
		}

		cursor.close();

		return messages;
	}


	/**
	 * Marks a single message as deleted.
	 * 
	 * @param message
	 * @param deleted
	 *        if true marks as deleted otherwise not deleted
	 */
	public static void markMessage(SQLiteDatabase db, PMReceivedMessage message, boolean deleted)
	{
		// create ContentValues to add key "column"/value
		ContentValues values = new ContentValues();
		values.put(KEY_DELETED, deleted ? "1" : "0");

		// update row
		db.update(TABLE_NAME, //table
			values, // column/value
			KEY_RECEIVED_MESSAGE_ID + " = ?", // selections
			new String[]
				{ String.valueOf(message.getReceivedMessageId()) }); //selection arg
	}

	/**
	 * Marks all the messages in a thread as deleted
	 * 
	 * @param threadID
	 * @param deleted if true marks as deleted otherwise not deleted
	 */
	public static void markThread(SQLiteDatabase db, String threadID, boolean deleted)
	{
		// create ContentValues to add key "column"/value
		ContentValues values = new ContentValues();
		values.put(KEY_DELETED, deleted ? "1" : "0");

		// update row
		db.update(TABLE_NAME, //table
			values, // column/value
			KEY_THREAD_ID + " = ?", // selections
			new String[]
				{ threadID }); //selection args
	}

	/**
	 * Marks an entire subject as deleted
	 * 
	 * @param subject
	 * @param deleted
	 *        if true marks as deleted otherwise not deleted
	 */
	public static void markSubject(SQLiteDatabase db, String subject, boolean deleted)
	{
		// create ContentValues to add key "column"/value
		ContentValues values = new ContentValues();
		values.put(KEY_DELETED, deleted ? "1" : "0");

		if (PMUtils.isNonBlankString(subject))
		{
			// update row
			db.update(TABLE_NAME, //table
				values, // column/value
				KEY_SUBJECT + " = ?", // selections
				new String[]
					{ String.valueOf(subject) }); //selection args
		}
		else
		{
			db.update(TABLE_NAME, //table
				values, // column/value
				null, // selections
				null); //selection args
		}
	}

	/**
	 * removes the deleted flag from all messages in the database, thereby
	 * 'undeleting' them
	 */
	public static void markAll(SQLiteDatabase db, boolean deleted)
	{
		// create ContentValues to add key "column"/value
		ContentValues values = new ContentValues();
		values.put(KEY_DELETED, deleted ? "1" : "0");

		// update row
		db.update(TABLE_NAME, //table
			values, // column/value
			KEY_DELETED + " = ?", // selections
			new String[]
				{ !deleted ? "1" : "0" }); //selection args
	}

	/**
	 * updates the status of a received message message. AKA marks it as read.
	 * 
	 * @param receivedMessageId
	 */
	public static void markMessageAsRead(SQLiteDatabase db, int receivedMessageId)
	{
		// create ContentValues to add key "column"/value
		ContentValues values = new ContentValues();

		values.put(KEY_STATUS, "128");

		//update row
		db.update(TABLE_NAME, //table
			values, // column/value
			KEY_RECEIVED_MESSAGE_ID + " = ?", // selections
			new String[]
				{ String.valueOf(receivedMessageId) }); //selection args
	}
}
