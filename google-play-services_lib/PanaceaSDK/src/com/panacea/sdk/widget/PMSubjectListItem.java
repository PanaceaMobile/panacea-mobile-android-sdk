package com.panacea.sdk.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Checkable;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.panacea.sdk.R;
import com.panacea.sdk.fragment.PMSubjectFragment;

/**
 * Custom Layout for each list item (subject and unread message count) in
 * {@link PMSubjectFragment}
 * 
 * @author Cobi Interactive
 */
public class PMSubjectListItem extends LinearLayout implements Checkable
{
	@SuppressWarnings("unused")
	private Context mContext;

	/* Data Elements */
	private String subject;
	private int unreadCount;
	private boolean checked;

	/* UI Elements */
	private View colour;
	private TextView subjectTextView;
	private TextView numberOfMessagesTextView;

	public static PMSubjectListItem inflate(Context context)
	{
		LayoutInflater inflater = (LayoutInflater) context
			.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		return (PMSubjectListItem) inflater.inflate(R.layout.layout_list_item_subject, null);
	}

	public PMSubjectListItem(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		this.mContext = context;
	}

	@Override
	protected void onFinishInflate()
	{
		super.onFinishInflate();
		colour = (View) findViewById(R.id.colour);
		subjectTextView = (TextView) findViewById(R.id.subject);
		numberOfMessagesTextView = (TextView) findViewById(R.id.numberOfMessages);
	}


	public void setSubjectAndCount(String text, int count)
	{
		this.subject = text;
		this.unreadCount = count;

		subjectTextView.setText(subject);

		setShapeColour(subject);

		//update new message count
		numberOfMessagesTextView.setVisibility(unreadCount == 0 ? View.GONE : View.VISIBLE);
		numberOfMessagesTextView.setText(unreadCount + "");
	}


	/**
	 * Calculates a color value from the hashcode of a string
	 * 
	 * @param string
	 */
	private void setShapeColour(String string)
	{
		int hash = string.hashCode();
		int r = (hash & 0xFF0000) >> 16;
		int g = (hash & 0x00FF00) >> 8;
		int b = hash & 0x0000FF;

		GradientDrawable d = (GradientDrawable) colour.getBackground();
		d.setColor(Color.rgb(r, g, b));
	}

	public String getSubject()
	{
		return subject;
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
