package com.panacea.sdk.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.panacea.sdk.PMUtils;
import com.panacea.sdk.R;

/**
 * Fragment to simulate Panacea sending a message to device/user.
 * 
 * @author Cobi interactive
 */
public class DEVPMSendMessageFragment extends PMBaseFragment
{
	public static final String TAG = "DEVPMSendMessageFragment";
	private EditText sendSubjectEditText;
	private EditText sendMessageEditText;
	private Button sendButton;

	private Integer threadId = null;
	private String subject = null;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(false);
		setRetainInstance(true);
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View result = inflater.inflate(R.layout.fragment_dev_send_message, container, false);

		sendSubjectEditText = (EditText) result.findViewById(R.id.sendSubjectEditText);
		sendMessageEditText = (EditText) result.findViewById(R.id.sendMessageEditText);
		sendButton = (Button) result.findViewById(R.id.sendButton);

		if (getSubject() != null)
		{
			sendSubjectEditText.setFocusableInTouchMode(false);
			sendSubjectEditText.setText(subject);
		}

		getDialog().setTitle("DEVELOPER SEND");

		sendButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (PMUtils.isBlankOrNull(sendSubjectEditText.getText().toString()))
					return;
				if (PMUtils.isBlankOrNull(sendMessageEditText.getText().toString()))
					return;

				sdk.debug_push_outbound_message_send(sendSubjectEditText.getText().toString(),
					sendMessageEditText.getText().toString(), threadId);

				sendSubjectEditText.setText("");
				sendMessageEditText.setText("");
			}
		});
		return result;
	}

	public Integer getThreadId()
	{
		return threadId;
	}

	public void setThreadId(Integer threadId)
	{
		this.threadId = threadId;
	}

	public String getSubject()
	{
		return subject;
	}

	public void setSubject(String subject)
	{
		this.subject = subject;
	}
}
