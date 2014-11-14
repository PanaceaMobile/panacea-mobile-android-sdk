package com.panacea.sdk.fragment;

import java.util.ArrayList;
import java.util.List;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.panacea.sdk.R;

/**
 * This is a helper class to handle the Multi-Selection on ListViews
 * @author Cobi Interactive
 */
public class PMMultiSelectionHelper implements OnItemClickListener, OnItemLongClickListener
{
	public interface PMMultiSelect
	{
		/**
		 * This method returns when the user presses delete
		 * @param selectedPositions the positions of all selected list items
		 */
		public void deleteSelected(List<Integer> selectedPositions);
		
		/**
		 * This method returns when the user presses delete
		 * @param selectedPositions the positions of all selected list items
		 */
		public void markAsReadSelected(List<Integer> selectedPositions);
	}

	private boolean scrollsToBottom;
	private boolean showMarkRead = true;
	private ListView listView;
	private BaseAdapter adapter;
	private ActionBarActivity activity;
	private PMMultiSelect listener;

	public PMMultiSelectionHelper(ActionBarActivity activity, ListView listView)
	{
		this(activity, listView, false);
	}

	public PMMultiSelectionHelper(ActionBarActivity activity, ListView listView,
		boolean scrollsToBottom)
	{
		this.activity = activity;
		this.scrollsToBottom = scrollsToBottom;
		this.listView = listView;
		this.adapter = (BaseAdapter) listView.getAdapter();
	}

	private ActionMode mActionMode;
	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback()
	{

		// Called when the action mode is created; startActionMode() was called
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu)
		{
			// Inflate a menu resource providing context menu items
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.menu_context, menu);
			return true;
		}

		// Called each time the action mode is shown. Always called after onCreateActionMode, but
		// may be called multiple times if the mode is invalidated.
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu)
		{
			if(!showMarkRead) // hide the mark as read item if flag is set to false
			{
				MenuItem item = menu.findItem(R.id.menu_read);
				if(item!=null)
				{
					item.setVisible(false);
				}
			}
			
			//WORKAROUND to ensure listview doesn't jump to the bottom after entering selection mode
			listView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
			return false; // Return false if nothing is done
		}

		// Called when the user selects a contextual menu item
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item)
		{
			if (item.getItemId() == R.id.menu_delete)
			{
				if (listener != null)
				{
					SparseBooleanArray checked = listView.getCheckedItemPositions();
					List<Integer> checkedIds = new ArrayList<Integer>();
					for (int i = 0; i < checked.size(); i++)
					{
						if (checked.valueAt(i))
						{
							checkedIds.add(checked.keyAt(i));
						}
					}
					listener.deleteSelected(checkedIds);
				}

				mode.finish(); // Action picked, so close the CAB
				return true;
			}
			else if (item.getItemId() == R.id.menu_read)
			{
				if (listener != null)
				{
					SparseBooleanArray checked = listView.getCheckedItemPositions();
					List<Integer> checkedIds = new ArrayList<Integer>();
					for (int i = 0; i < checked.size(); i++)
					{
						if (checked.valueAt(i))
						{
							checkedIds.add(checked.keyAt(i));
						}
					}
					listener.markAsReadSelected(checkedIds);
				}

				mode.finish(); // Action picked, so close the CAB
				return true;
			}
			else
			{
				return false;
			}
		}

		// Called when the user exits the action mode
		@Override
		public void onDestroyActionMode(ActionMode mode)
		{
			listView.clearChoices();
			listView.setChoiceMode(ListView.CHOICE_MODE_NONE);

			adapter.notifyDataSetInvalidated();
			mActionMode = null;


			//WORKAROUND to ensure listview doesn't jump to the bottom after ending selection mode
			if (scrollsToBottom)
				listView.postDelayed(new Runnable()
				{
					@Override
					public void run()
					{
						listView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
					}
				}, 100);
		}
	};

	/**
	 * Check whether the ListView is in selection mode
	 * @return true if the list is in selection mode, otherwise false
	 */
	public boolean isSelecting()
	{
		return mActionMode != null;
	}

	/**
	 * an accurate way to get the selected item count
	 * @return number of selected items
	 */
	public int selectedCount()
	{
		int count = 0;
		SparseBooleanArray checked = listView.getCheckedItemPositions();
		for (int i = 0; i < checked.size(); i++)
		{
			if (checked.valueAt(i))
			{
				count++;
			}
		}
		return count;
	}
	
	/**
	 * Controls whether or not the mark as read button will be visible
	 * @param show
	 */
	public void setShowMarkAsRead(boolean show)
	{
		showMarkRead = show;
	}

	/**
	 * updates text at the top of the action bar while in selection mode
	 */
	private void updateTitle()
	{
		mActionMode.setTitle("" + selectedCount());
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> adapter, View view, int position, long id)
	{
		if (isSelecting())
		{
			listView.setItemChecked(position, !view.isSelected());
			updateTitle();
			return true;
		}

		//WORKAROUND to ensure listview doesn't jump to the bottom after entering selection mode
		if (scrollsToBottom)
			listView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);

		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		// Start the CAB using the ActionMode.Callback defined above
		mActionMode = activity.startSupportActionMode(mActionModeCallback);
		listView.setItemChecked(position, !view.isSelected());
		updateTitle();
		return true;
	}

	@Override
	public void onItemClick(AdapterView<?> adapter, View view, int position, long id)
	{
		if (isSelecting())
		{
			updateTitle();
		}
	}

	public PMMultiSelect getListener()
	{
		return listener;
	}

	public void setListener(PMMultiSelect listener)
	{
		this.listener = listener;
	}
}