package com.panacea.sdk.db;

import net.sqlcipher.database.SQLiteDatabase;
import android.content.ContentValues;
import android.database.Cursor;

public class TablesSentMessagesCipher
{
	/* Tables name */
	private static final String TABLE_NAME = "pm_sent_messages";

	/* Table Columns names */
	private static final String KEY_SENT_MESSAGE_ID = "_id";
	private static final String KEY_RECEIVED_MESSAGE_ID = "received_message_id";
	private static final String KEY_TEXT = "text";
	private static final String KEY_CREATED = "created";
	private static final String KEY_THREAD_ID = "thread_id";
	private static final String KEY_DEVICE_ID = "device_id";
	private static final String KEY_APPLICATION_ID = "application_id";
	private static final String KEY_DELETED = "deleted";

	private static final String[] COLUMNS_SENT =
		{ KEY_SENT_MESSAGE_ID, KEY_RECEIVED_MESSAGE_ID, KEY_TEXT, KEY_CREATED, KEY_THREAD_ID,
			KEY_DEVICE_ID, KEY_APPLICATION_ID, KEY_DELETED };


	public static void onCreate(SQLiteDatabase db)
	{
		String CREATE_SENT_TABLE = String
			.format(
				"CREATE TABLE %s (%s INTEGER PRIMARY KEY, %s INTEGER, %s TEXT, %s TEXT, %s TEXT, %s INTEGER, %s INTEGER, %s INTEGER DEFAULT 0, FOREIGN KEY (%s) REFERENCES %s(%s))",
				TABLE_NAME,
				//				COLUMNS_SENT,
				KEY_SENT_MESSAGE_ID, KEY_RECEIVED_MESSAGE_ID, KEY_TEXT, KEY_CREATED, KEY_THREAD_ID,
				KEY_DEVICE_ID, KEY_APPLICATION_ID, KEY_DELETED, KEY_RECEIVED_MESSAGE_ID,
				TableReceivedMessagesCipher.TABLE_NAME, KEY_RECEIVED_MESSAGE_ID);

		db.execSQL(CREATE_SENT_TABLE);
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

	public static PMSentMessage cursorToSentMessage(Cursor cursor)
	{
		PMSentMessage message = new PMSentMessage();

		//efficient
		//		message.setSentMessageId(Integer.parseInt(cursor.getString(0)));
		//		message.setReceivedMessageId(Integer.parseInt(cursor.getString(1)));
		//		message.setText(cursor.getString(2));
		//		message.setCreated(cursor.getString(3));
		//		message.setThreadID(Integer.parseInt(cursor.getString(4)));
		//		message.setDeviceID(Integer.parseInt(cursor.getString(5)));
		//		message.setApplicationID(Integer.parseInt(cursor.getString(6)));

		//robust
		message.setSentMessageId(Integer.parseInt(cursor.getString(cursor
			.getColumnIndex(KEY_SENT_MESSAGE_ID))));
		message.setReceivedMessageId(Integer.parseInt(cursor.getString(cursor
			.getColumnIndex(KEY_RECEIVED_MESSAGE_ID))));
		message.setText(cursor.getString(cursor.getColumnIndex(KEY_TEXT)));
		message.setCreated(cursor.getString(cursor.getColumnIndex(KEY_CREATED)));
		message
			.setThreadID(cursor.getString(cursor.getColumnIndex(KEY_THREAD_ID)));
		message
			.setDeviceID(Integer.parseInt(cursor.getString(cursor.getColumnIndex(KEY_DEVICE_ID))));
		message.setApplicationID(Integer.parseInt(cursor.getString(cursor
			.getColumnIndex(KEY_APPLICATION_ID))));

		return message;
	}


	/**
	 * Adds a row to table for a {@link PMSentMessage}
	 * 
	 * @param message
	 */
	public static void addSentMessage(SQLiteDatabase db, PMSentMessage message)
	{
		// create ContentValues to add key "column"/value
		ContentValues values = new ContentValues();

		values.put(KEY_SENT_MESSAGE_ID, message.getSentMessageId());
		values.put(KEY_RECEIVED_MESSAGE_ID, message.getReceivedMessageId());
		values.put(KEY_TEXT, message.getText());
		values.put(KEY_CREATED, message.getCreatedString());
		values.put(KEY_THREAD_ID, message.getThreadID());
		values.put(KEY_APPLICATION_ID, message.getApplicationID());
		values.put(KEY_DEVICE_ID, message.getDeviceID());


		// try updating row
		int i = db.update(TABLE_NAME, //table
			values, // column/value
			KEY_SENT_MESSAGE_ID + " = ?", // selections
			new String[]
				{ String.valueOf(message.getSentMessageId()) }); //selection args

		// if no row updated then insert
		if (i == 0)
		{
			db.insert(TABLE_NAME, // table
				null, //nullColumnHack
				values); // key/value -> keys = column names/ values = column values
		}

	}

	/**
	 * Retrieves a sent message from database for given message id
	 * 
	 * @param messageID
	 * @return PMMessage
	 */
	public static PMMessage getSentMessage(SQLiteDatabase db, int messageID)
	{
		// build query
		Cursor cursor = db.query(TABLE_NAME, // a. table
			COLUMNS_SENT, // b. column names
			" " + KEY_SENT_MESSAGE_ID + "  = ?", // c. selections 
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
		PMMessage message = cursorToSentMessage(cursor);

		cursor.close();

		return message;
	}

	//	public static Cursor getCursorAllSentMessages(SQLiteDatabase db)
	//	{
	//		String query = "SELECT  * FROM " + TABLE_NAME;
	//		return db.rawQuery(query, null);
	//	}


	/**
	 * returns the largest (and newest) sent message id
	 * 
	 * @return latest message id
	 */
	public static Integer getLastSentMessageId(SQLiteDatabase db)
	{
		String query = "SELECT MAX(" + KEY_SENT_MESSAGE_ID + ") FROM " + TABLE_NAME;

		Cursor cursor = db.rawQuery(query, null);
		Integer last = null;
		if (cursor.moveToFirst())
		{
			do
			{
				String int_string = cursor.getString(0);
				if (int_string != null)
					last = Integer.parseInt(cursor.getString(0));
			}
			while (cursor.moveToNext());
		}

		cursor.close();

		return last;
	}


	/**
	 * Marks a single message as deleted.
	 * 
	 * @param message
	 * @param deleted
	 *        if true marks as deleted otherwise not deleted
	 */
	public static void markMessage(SQLiteDatabase db, PMSentMessage message, boolean deleted)
	{
		// create ContentValues to add key "column"/value
		ContentValues values = new ContentValues();
		values.put(KEY_DELETED, deleted ? "1" : "0");

		// update row
		db.update(TABLE_NAME, //table
			values, // column/value
			KEY_SENT_MESSAGE_ID + " = ?", // selections
			new String[]
				{ String.valueOf(message.getSentMessageId()) }); //selection arg
	}

	/**
	 * 
	 * 
	 * @param threadID
	 * @param deleted if true marks as deleted otherwise not deleted
	 */
	
	/**
	 * Marks all the messages in a thread as deleted
	 * 
	 * @param db WriteableDB
	 * @param threadID 
	 * @param deleted
	 */
	public static void markThread(SQLiteDatabase db, String threadID, boolean deleted)
	{
		// create ContentValues to add key "column"/value
		ContentValues values = new ContentValues();
		values.put(KEY_DELETED, deleted ? "1" : "0");

		// 3. try updating row
		db.update(TABLE_NAME, //table
			values, // column/value
			KEY_THREAD_ID + " = ?", // selections
			new String[]
				{ threadID }); //selection args
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
}
