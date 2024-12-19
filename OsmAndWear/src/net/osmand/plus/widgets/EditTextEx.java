package net.osmand.plus.widgets;

import android.content.Context;
import android.text.InputFilter.LengthFilter;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatEditText;


public class EditTextEx extends AppCompatEditText {

	private int maxSymbolsCount = -1;

	public int getMaxSymbolsCount() {
		return maxSymbolsCount;
	}

	public void setMaxSymbolsCount(int maxSymbolsCount) {
		this.maxSymbolsCount = maxSymbolsCount;
		setMaxSymbols();
	}

	public EditTextEx(Context context) {
		super(context);
	}

	public EditTextEx(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public EditTextEx(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	private void setMaxSymbols() {
		if (maxSymbolsCount > 0) {
			setFilters(new LengthFilter[]{new LengthFilter(maxSymbolsCount)});
		}
	}
}
