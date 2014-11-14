package com.panacea.sdk.widget;

import android.content.Context;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Checkable;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.panacea.sdk.PMUtils;
import com.panacea.sdk.R;
import com.panacea.sdk.db.PMMessage;
import com.panacea.sdk.db.PMReceivedMessage;
import com.panacea.sdk.fragment.PMMessageDetailFragment;

/**
 * Custom Layout for each list item (message) in {@link PMMessageDetailFragment}
 * 
 * @author Cobi Interactive
 */
public class PMMessageDetailListItem extends LinearLayout implements Checkable
{
	@SuppressWarnings("unused")
	private Context mContext;

	/* Data Elements */
	private PMMessage message;
	private boolean checked = false;

	/* UI Elements */
	private TextView messageSent;
	private TextView messageReceived;
	private TextView messageAge;

	public static PMMessageDetailListItem inflate(Context context)
	{
		LayoutInflater inflater = (LayoutInflater) context
			.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		return (PMMessageDetailListItem) inflater.inflate(R.layout.layout_list_item_message_detail,
			null);
	}

	public PMMessageDetailListItem(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		this.mContext = context;
	}

	@Override
	protected void onFinishInflate()
	{
		super.onFinishInflate();
		messageSent = (TextView) findViewById(R.id.messageSent);
		messageReceived = (TextView) findViewById(R.id.messageReceived);
		messageAge = (TextView) findViewById(R.id.messageAge);

		messageSent.setVisibility(View.GONE);
		messageReceived.setVisibility(View.GONE);

		//set colours
		//Left
		messageReceived.getBackground().setColorFilter(
			getResources().getColor(R.color.bg_chat_received), PorterDuff.Mode.MULTIPLY);
		//Right
		messageSent.getBackground().setColorFilter(getResources().getColor(R.color.bg_chat_sent),
			PorterDuff.Mode.MULTIPLY);
	}


	public void setMessage(PMMessage message)
	{
		this.message = message;

		if (message instanceof PMReceivedMessage)
		{
			messageReceived.setText(message.getText());
			messageReceived.setVisibility(View.VISIBLE);
			messageSent.setVisibility(View.GONE);
		}
		else
		{
			messageSent.setText(message.getText());
			messageSent.setVisibility(View.VISIBLE);
			messageReceived.setVisibility(View.GONE);
		}

		messageAge.setText(PMUtils.getRelativeDate(message.getCreated()));
	}

	public PMMessage getMessage()
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
