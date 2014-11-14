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
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.panacea.sdk.PanaceaSDK.Result;
import com.panacea.sdk.R;
import com.panacea.sdk.db.PMDatabaseCipherHelper;
import com.panacea.sdk.fragment.PMMultiSelectionHelper.PMMultiSelect;
import com.panacea.sdk.widget.PMSubjectListItem;

/**
 * This fragment shows all the different subjects and how many unread messages
 * they have.
 * 
 * @author Cobi interactive
 */
public class PMSubjectFragment extends PMBaseFragment implements OnItemClickListener, PMMultiSelect
{
	public static final String TAG = "PMSubjectFragment";

	/* Data Elements */

	/* UI Elements */
	private ListView listView;
	private TextView emptyListTextView;

	private SubjectCursorAdapter cursorAdapter;
	private PMMultiSelectionHelper selectionHelper;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View result = inflater.inflate(R.layout.fragment_subject, container, false);

		listView = (ListView) result.findViewById(R.id.listViewSubjects);
		emptyListTextView = (TextView) result.findViewById(R.id.emptyListTextView);

		return result;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		cursorAdapter = new SubjectCursorAdapter(getActivity(), null);
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

		setActionBarTitle("My Inbox");
		setHomeEnabled(false);
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
					boolean deleteAll = false; // this flag is used if the "All Updates" is selected

					for (Integer position : selectedPositions)
					{
						//if all is selected
						if (position == 0)
						{
							deleteAll = true;
							break;
						}
						Cursor cursor = (Cursor) cursorAdapter.getItem(position);
						String subject = cursor.getString(0);
						sdk.markSubjectDeleted(subject);
					}

					//if all is selected
					if (deleteAll)
					{
						sdk.markAllDeletedMessages(true);
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
					boolean deleteAll = false; // this flag is used if the "All Updates" is selected

					for (Integer position : selectedPositions)
					{
						//if all is selected
						if (position == 0)
						{
							deleteAll = true;
							break;
						}
						Cursor cursor = (Cursor) cursorAdapter.getItem(position);
						String subject = cursor.getString(0);
						sdk.markSubjectAsRead(subject);
					}

					//if all is selected
					if (deleteAll)
					{
						for (int i = 0; i < cursorAdapter.getCount(); i++)
						{
							Cursor cursor = (Cursor) cursorAdapter.getItem(i);
							String subject = cursor.getString(0);
							sdk.markSubjectAsRead(subject);
						}
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
		cursorAdapter.changeCursor(cursor);
		cursorAdapter.notifyDataSetChanged();

		if (cursor == null || cursor.getCount() == 0)
		{
			emptyListTextView.setVisibility(View.VISIBLE);
			listView.setVisibility(View.GONE);
		}
		else
		{
			emptyListTextView.setVisibility(View.GONE);
			listView.setVisibility(View.VISIBLE);
		}

	}

	private Cursor doQuery()
	{
		return PMDatabaseCipherHelper.getInstance(getActivity()).getCursorSubjectCounts();
	}

	@Override
	public void onItemClick(AdapterView<?> adapter, View view, int position, long id)
	{
		if (selectionHelper.isSelecting())
		{
			selectionHelper.onItemClick(adapter, view, position, id);
			return;
		}

		PMSubjectListItem item = (PMSubjectListItem) view;

		//if the position is 0 the subject will be "All updates" - so we pass through null to get all threads
		String subject = (position == 0) ? null : item.getSubject();
		getPMActivity().goToMessagesForSubject(subject);
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

	public class SubjectCursorAdapter extends CursorAdapter
	{
		@SuppressWarnings("unused")
		private Context mContext;

		@SuppressWarnings("deprecation")
		public SubjectCursorAdapter(Context context, Cursor c)
		{
			super(context, c);
			mContext = context;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor)
		{
			PMSubjectListItem content = (PMSubjectListItem) view;

			String subject = cursor.getString(0);
			String countString = cursor.getString(1);
			Integer unreadCount = countString == null ? 0 : Integer.valueOf(countString);
			content.setSubjectAndCount(subject, unreadCount);

		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent)
		{
			return PMSubjectListItem.inflate(context);
		}

	}

}
