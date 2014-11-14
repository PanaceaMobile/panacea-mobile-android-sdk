package com.panacea.sdk.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Checkable;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.panacea.sdk.PMUtils;
import com.panacea.sdk.R;
import com.panacea.sdk.db.PMReceivedMessage;
import com.panacea.sdk.fragment.PMMessagesFragment;

/**
 * Custom Layout for each list item (newest message in thread) in {@link PMMessagesFragment}
 * 
 * @author Cobi Interactive
 */
public class PMMessageListItem extends LinearLayout implements Checkable
{
	@SuppressWarnings("unused")
	private Context mContext;

	/* Data Elements */
	private PMReceivedMessage message;
	private boolean checked;

	/* UI Elements */
	private View inboxRead;
	private TextView inboxSubject;
	private TextView inboxAge;
	private TextView inboxMessage;

	public static PMMessageListItem inflate(Context context)
	{
		LayoutInflater inflater = (LayoutInflater) context
			.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		return (PMMessageListItem) inflater.inflate(R.layout.layout_list_item_message, null);
	}

	public PMMessageListItem(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		this.mContext = context;
	}

	@Override
	protected void onFinishInflate()
	{
		super.onFinishInflate();
		inboxRead = (View) findViewById(R.id.inboxRead);
		inboxSubject = (TextView) findViewById(R.id.inboxSubject);
		inboxAge = (TextView) findViewById(R.id.inboxAge);
		inboxMessage = (TextView) findViewById(R.id.inboxMessage);
	}


	public void setMessage(PMReceivedMessage message)
	{
		this.message = message;

		inboxSubject.setText(message.getSubject());
		inboxRead.setVisibility(message.isUnread() ? View.VISIBLE : View.INVISIBLE);
		inboxAge.setText(PMUtils.getRelativeDate(message.getCreated()));
		inboxMessage.setText(message.getText());
	}

	public PMReceivedMessage getMessage()
	{
		return message;
	}

	@Override
	public boolean isChecked()
	{
		return checked;
	}

	@Override
	public void setChecked(boolean checked)
	{
		this.checked = checked;
		setSelected(checked);
	}

	@Override
	public void toggle()
	{
		setChecked(!checked);
	}
}
