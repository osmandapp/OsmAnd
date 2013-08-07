/* 
 * Copyright (c) 2010 Jordan Frank, HumanSense Project, McGill University
 * Licensed under the MIT license: http://www.opensource.org/licenses/mit-license.php
 * See LICENSE for more information 
 */
package net.osmand.plus.views;

import net.osmand.plus.R;
import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * A preference item that uses a seek-bar, or slider to set the value.
 * 
 * @author Jordan Frank <jordan.frank@cs.mcgill.ca>
 * 
 */
public class SeekBarPreference extends DialogPreference implements
		SeekBar.OnSeekBarChangeListener {

	private static final String MAX_VALUE_ID = "max";
	private static final String DEFAULT_VALUE_ID = "defaultValue";
	private static final String DIALOG_TEXT_ID = "text";
	private static final String DIALOG_MESSAGE_ID = "dialogMessage";
	private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

	private SeekBar seekBar;
	private TextView dialogTextView, valueTextView;
	private final Context context;

	private final String dialogText, valueText;
	private final int defaultValue;

	private int maxValue, value, valueToSave = 0;

	/**
	 * Default constructor.
	 * 
	 * @param context
	 *            The application context.
	 * @param attrs
	 *            The attribute set, containing the text, title, values, and
	 *            range for the slider dialog.
	 */
	public SeekBarPreference(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		this.context = context;

		dialogText = context.getResources().getString(
				attrs.getAttributeResourceValue(ANDROID_NS, DIALOG_MESSAGE_ID,
						R.string.app_name));
		valueText = attrs.getAttributeValue(ANDROID_NS, DIALOG_TEXT_ID);
		defaultValue = attrs.getAttributeIntValue(ANDROID_NS, DEFAULT_VALUE_ID,
				0);
		maxValue = attrs.getAttributeIntValue(ANDROID_NS, MAX_VALUE_ID, 100);

	}
	
	/**
	 * Default constructor.
	 * 
	 * @param context
	 *            The application context.
	 * @param attrs
	 *            The attribute set, containing the text, title, values, and
	 *            range for the slider dialog.
	 */
	public SeekBarPreference(final Context context, int dialogTextId, int defValue, int maxValue) {
		super(context, null);
		this.context = context;
		dialogText = context.getResources().getString(dialogTextId);
		valueText = null;
		this.defaultValue = defValue;
		this.maxValue = maxValue;

	}

	public int getMax() {
		return maxValue;
	}

	public int getValue() {
		return value;
	}

	@Override
	protected void onBindDialogView(final View v) {
		super.onBindDialogView(v);
		seekBar.setMax(maxValue);
		seekBar.setProgress(value);
	}

	@Override
	protected View onCreateDialogView() {
		LinearLayout.LayoutParams params;
		final LinearLayout layout = new LinearLayout(context);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setPadding(6, 6, 6, 6);

		dialogTextView = new TextView(context);
		if (dialogText != null) {
			dialogTextView.setText(dialogText);
		}
		layout.addView(dialogTextView);

		valueTextView = new TextView(context);
		valueTextView.setGravity(Gravity.CENTER_HORIZONTAL);
		valueTextView.setTextSize(32);
		params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		layout.addView(valueTextView, params);

		seekBar = new SeekBar(context);
		seekBar.setOnSeekBarChangeListener(this);
		layout.addView(seekBar, new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));

		if (shouldPersist()) {
			value = getPersistedInt(defaultValue);
		}

		seekBar.setMax(maxValue);
		seekBar.setProgress(value);
		return layout;
	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);
		if(positiveResult){
			if (shouldPersist()) {
				persistInt(valueToSave);
			}
			callChangeListener(Integer.valueOf(valueToSave));
		} else {
			this.valueToSave = value;
		}
	}

	@Override
	public void onProgressChanged(final SeekBar seek, final int value,
			final boolean fromTouch) {
		final String t = String.valueOf(value);
		valueTextView.setText(valueText == null ? t : t + valueText);
		valueToSave = value;
	}

	@Override
	protected void onSetInitialValue(final boolean restore,
			final Object defaultValue) {
		super.onSetInitialValue(restore, defaultValue);
		if (restore) {
			value = shouldPersist() ? getPersistedInt(this.defaultValue) : 0;
		} else {
			value = (Integer) defaultValue;
		}
		valueToSave = value;
	}

	@Override
	public void onStartTrackingTouch(final SeekBar seek) {
	}

	@Override
	public void onStopTrackingTouch(final SeekBar seek) {
	}

	public void setMax(final int max) {
		maxValue = max;
	}

	public void setValue(final int value) {
		this.value = value;
		this.valueToSave = value;
		persistInt(value);
		if (seekBar != null) {
			seekBar.setProgress(value);
		}
	}

}