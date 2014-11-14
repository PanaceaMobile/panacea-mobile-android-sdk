package com.panacea.sdk.fragment;

import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.panacea.sdk.PanaceaSDK.Result;
import com.panacea.sdk.R;
import com.panacea.sdk.db.PMDatabaseCipherHelper;
import com.panacea.sdk.db.PMReceivedMessage;
import com.panacea.sdk.db.TableReceivedMessagesCipher;
import com.panacea.sdk.fragment.PMMultiSelectionHelper.PMMultiSelect;
import com.panacea.sdk.widget.PMMessageListItem;

/**
 * This fragment shows the latest received message with a given subject for each
 * different thread id.
 * 
 * @author Cobi interactive
 */
public class PMMessagesFragment extends PMBaseFragment implements OnItemClickListener,
	PMMultiSelect
{
	public static final String TAG = "PMMessagesFragment";

	/* Data Elements */
	private String subject = null;

	/* UI Elements */
	private ListView listView;
	private MessageCursorAdapter cursorAdapter;

	private PMMultiSelectionHelper selectionHelper;


	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View result = inflater.inflate(R.layout.fragment_messages, container, false);

		listView = (ListView) result.findViewById(R.id.listViewMessages);

		return result;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		cursorAdapter = new MessageCursorAdapter(getActivity(), null);
		listView.setAdapter(cursorAdapter);

		listView.setOnItemClickListener(this);

		selectionHelper = new PMMultiSelectionHelper(((ActionBarActivity) getActivity()), listView);
		selectionHelper.setListener(this);
		listView.setOnItemLongClickListener(selectionHelper);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		reloadContent();

		setActionBarTitle(subject == null ? getResources().getString(R.string.all_subjects)
			: subject);
	}

	@Override
	public void onPause()
	{
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
		inflater.inflate(R.menu.menu_fragment_messages, menu);
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
						sdk.markSubjectDeleted(subject);
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
						PMReceivedMessage msg = TableReceivedMessagesCipher
							.cursorToReceivedMessage((Cursor) cursorAdapter.getItem(position));
						sdk.markThreadDeleted(msg.getThreadID());
					}

					reloadContent();
				}
			});
		builder.setNegativeButton(R.string.button_negative, null);
		final AlertDialog alert = builder.create();
		alert.show();
	}

	@Override
	public void markAsReadSelected(final List<Integer> selectedPositions)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(getResources().getString(R.string.menu_read));
		builder.setMessage(getResources().getString(R.string.confirm_read));
		builder.setPositiveButton(getResources().getString(R.string.button_positive),
			new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int id)
				{
					//DO mark as read
					for (Integer position : selectedPositions)
					{
						PMReceivedMessage msg = TableReceivedMessagesCipher
							.cursorToReceivedMessage((Cursor) cursorAdapter.getItem(position));
						sdk.markThreadAsRead(msg.getThreadID());
					}

					reloadContent();
				}
			});
		builder.setNegativeButton(R.string.button_negative, null);
		final AlertDialog alert = builder.create();
		alert.show();

	}

	private void reloadContent()
	{
		Cursor cursor = doQuery();

		if (cursor == null || cursor.getCount() < 1)
			getActivity().getSupportFragmentManager().popBackStack();

		cursorAdapter.changeCursor(cursor);
		cursorAdapter.notifyDataSetChanged();
	}

	private Cursor doQuery()
	{
		return PMDatabaseCipherHelper.getInstance(getActivity()).getCursorMessagesForSubject(subject);
	}

	@Override
	public void onItemClick(AdapterView<?> adapter, View view, int position, long id)
	{
		if (selectionHelper.isSelecting())
		{
			selectionHelper.onItemClick(adapter, view, position, id);
			return;
		}
		PMMessageListItem li = (PMMessageListItem) view;
		getPMActivity().goToMessageDetails(li.getMessage().getThreadID());
	}

	public void setSubject(String subject)
	{
		this.subject = subject;
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

	public class MessageCursorAdapter extends CursorAdapter
	{
		@SuppressWarnings("unused")
		private Context mContext;

		@SuppressWarnings("deprecation")
		public MessageCursorAdapter(Context context, Cursor c)
		{
			super(context, c);
			mContext = context;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor)
		{
			PMMessageListItem content = (PMMessageListItem) view;
			PMReceivedMessage message = TableReceivedMessagesCipher.cursorToReceivedMessage(cursor);
			content.setMessage(message);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent)
		{
			return PMMessageListItem.inflate(context);
		}

	}
}