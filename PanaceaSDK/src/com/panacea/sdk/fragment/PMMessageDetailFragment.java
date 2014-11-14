package com.panacea.sdk.fragment;

import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.panacea.sdk.PMUtils;
import com.panacea.sdk.PanaceaSDK.Result;
import com.panacea.sdk.R;
import com.panacea.sdk.db.PMDatabaseCipherHelper;
import com.panacea.sdk.db.PMMessage;
import com.panacea.sdk.db.PMReceivedMessage;
import com.panacea.sdk.db.PMSentMessage;
import com.panacea.sdk.fragment.PMMultiSelectionHelper.PMMultiSelect;
import com.panacea.sdk.widget.PMMessageDetailListItem;

/**
 * This fragment shows all {@link PMReceivedMessage}s and {@link PMSentMessage}s
 * in the same thread in a chat layout.
 * 
 * @author Cobi interactive
 */
public class PMMessageDetailFragment extends PMBaseFragment implements PMMultiSelect
{
	public static final String TAG = "PMMessageDetailFragment";

	/* Data Elements */
	private String threadId;

	/* UI Elements */
	private ListView listView;
	//	private MessageDetailAdapter mAdapter;
	private MessageDetailCursorAdapter cursorAdapter;
	private PMMultiSelectionHelper selectionHelper;

	private EditText replyEditText;
	private Button replyButton;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View result = inflater.inflate(R.layout.fragment_message_detail, container, false);

		listView = (ListView) result.findViewById(R.id.listViewMessageDetail);
		replyEditText = (EditText) result.findViewById(R.id.replyEditText);
		replyButton = (Button) result.findViewById(R.id.replyButton);

		listView.setDivider(null);
		listView.setDividerHeight(0);
		listView.setStackFromBottom(true);
		listView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);

		replyButton.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View v)
			{
				if (shouldSend())
				{
					replyToLatestMessage(replyEditText.getText().toString());
					replyEditText.setText("");
				}
			}
		});

		replyButton.setEnabled(shouldSend());
		replyEditText.addTextChangedListener(new TextWatcher()
		{
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after)
			{

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{

			}

			@Override
			public void afterTextChanged(Editable s)
			{
				replyButton.setEnabled(shouldSend());
			}
		});

		return result;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		cursorAdapter = new MessageDetailCursorAdapter(getActivity(), null);
		listView.setAdapter(cursorAdapter);

		selectionHelper = new PMMultiSelectionHelper(((ActionBarActivity) getActivity()), listView,
			true);
		selectionHelper.setListener(this);
		selectionHelper.setShowMarkAsRead(false);
		listView.setOnItemClickListener(selectionHelper);
		listView.setOnItemLongClickListener(selectionHelper);

	}

	@Override
	public void onResume()
	{
		super.onResume();
		reloadContent();
	}

	@Override
	public void onPause()
	{
		//Clean up cursor
		Cursor c = cursorAdapter.getCursor();
		if (c != null)
		{
			c.close();
			cursorAdapter.changeCursor(null);
		}

		super.onPause();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.menu_fragment_message_detail, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == R.id.menu_delete)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(getResources().getString(R.string.menu_delete));
			builder.setMessage(getResources().getString(R.string.confirm_delete));
			builder.setPositiveButton(getResources().getString(R.string.button_positive),
				new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						//DO delete
						sdk.markThreadDeleted(threadId);
						reloadContent();
					}
				});
			builder.setNegativeButton(R.string.button_negative, null);
			final AlertDialog alert = builder.create();
			alert.show();

			return true;
		}
		else
		{
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void deleteSelected(final List<Integer> selectedPositions)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(getResources().getString(R.string.menu_delete));
		builder.setMessage(getResources().getString(R.string.confirm_delete));
		builder.setPositiveButton(getResources().getString(R.string.button_positive),
			new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int id)
				{
					//DO delete
					for (Integer position : selectedPositions)
					{
						PMMessage msg = PMDatabaseCipherHelper.cursorToMessage((Cursor) cursorAdapter
							.getItem(position));
						sdk.markMessageDeleted(msg);
					}
					reloadContent();
				}
			});
		builder.setNegativeButton(R.string.button_negative, null);
		final AlertDialog alert = builder.create();
		alert.show();
	}

	@Override
	public void markAsReadSelected(List<Integer> selectedPositions)
	{
		//Do nothing. message is already marked as read and the option is disabled
	}

	private void reloadContent()
	{
		Cursor cursor = doQuery();

		if (cursor == null || cursor.getCount() < 1)
			getActivity().getSupportFragmentManager().popBackStack();

		cursorAdapter.changeCursor(cursor);
		cursorAdapter.notifyDataSetChanged();

		setActionBarTitle(getSubject());

		//mark as read (if necessary)
		sdk.markThreadAsRead(threadId);

	}

	private Cursor doQuery()
	{
		return PMDatabaseCipherHelper.getInstance(getActivity()).getCursorMessagesForThreadId(threadId);
	}

	public String getThreadId()
	{
		return threadId;
	}

	public void setThreadId(String threadId)
	{
		this.threadId = threadId;
	}

	private void replyToLatestMessage(String replyText)
	{
		if (PMUtils.isBlankOrNull(replyText))
			return;

		if (cursorAdapter.getCursor() == null || cursorAdapter.getCount() == 0)
			return;

		//find latest received message
		PMReceivedMessage receivedMessage = null;
		for (int i = cursorAdapter.getCount() - 1; i >= 0; i--)
		{
			PMMessage msg = PMDatabaseCipherHelper.cursorToMessage((Cursor) cursorAdapter.getItem(i));
			if (msg instanceof PMReceivedMessage)
			{
				receivedMessage = (PMReceivedMessage) msg;
				break;
			}
		}

		if (receivedMessage == null)
			return;

		sdk.sendReply(replyText, receivedMessage);

	}

	private boolean shouldSend()
	{
		return replyEditText.getText().toString().trim().length() > 0;
	}

	public String getSubject()
	{
		if (cursorAdapter.getCursor() == null || cursorAdapter.getCount() == 0)
			return null;

		//find latest received message
		PMReceivedMessage receivedMessage = null;
		for (int i = cursorAdapter.getCount() - 1; i >= 0; i--)
		{
			PMMessage msg = PMDatabaseCipherHelper.cursorToMessage((Cursor) cursorAdapter.getItem(i));
			if (msg instanceof PMReceivedMessage)
			{
				receivedMessage = (PMReceivedMessage) msg;
				break;
			}
		}

		if (receivedMessage == null)
			return null;

		return receivedMessage.getSubject();
	}


	@Override
	public void onPostDataSuccess(String tag, String result, int status, String message)
	{
		super.onPostDataSuccess(tag, result, status, message);

		if (Result.MESSAGE_UPDATED_RECEIVED.equals(result)
			|| Result.MESSAGE_UPDATED_SENT.equals(result))
		{
			reloadContent();
		}
	}

	public class MessageDetailCursorAdapter extends CursorAdapter
	{
		@SuppressWarnings("unused")
		private Context mContext;

		@SuppressWarnings("deprecation")
		public MessageDetailCursorAdapter(Context context, Cursor c)
		{
			super(context, c);
			mContext = context;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor)
		{
			PMMessageDetailListItem content = (PMMessageDetailListItem) view;
			PMMessage message = PMDatabaseCipherHelper.cursorToMessage(cursor);
			content.setMessage(message);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent)
		{
			return PMMessageDetailListItem.inflate(context);
		}

	}
}
