package com.panacea.sdk.widget;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.AutoCompleteTextView;

public class PMInstantAutoComplete extends AutoCompleteTextView
{
	public PMInstantAutoComplete(Context context)
	{
		super(context);
	}

	public PMInstantAutoComplete(Context arg0, AttributeSet arg1)
	{
		super(arg0, arg1);
	}

	public PMInstantAutoComplete(Context arg0, AttributeSet arg1, int arg2)
	{
		super(arg0, arg1, arg2);
	}

	@Override
	public boolean enoughToFilter()
	{
		return true;
	}

	@Override
	protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect)
	{
		super.onFocusChanged(focused, direction, previouslyFocusedRect);
		if (focused)
		{
			performFiltering(getText(), 0);
		}
	}

}