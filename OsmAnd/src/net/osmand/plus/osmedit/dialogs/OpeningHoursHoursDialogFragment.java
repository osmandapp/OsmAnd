package net.osmand.plus.osmedit.dialogs;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.TimePicker;

import net.osmand.plus.R;
import net.osmand.plus.osmedit.NormalDataFragment;
import net.osmand.util.OpeningHoursParser;

import java.util.Calendar;

public class OpeningHoursHoursDialogFragment extends DialogFragment {
	public static final String INITIAL_TIME = "initial_time";
	public static final String IS_START = "is_start";
	public static final String BASIC_OPENING_HOUR_RULE = "basic_opening_hour_rule";

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Bundle args = getArguments();
		final boolean isStart = args.getBoolean(IS_START);
		final OpeningHoursParser.BasicOpeningHourRule item = (OpeningHoursParser.BasicOpeningHourRule)
				args.getSerializable(BASIC_OPENING_HOUR_RULE);
		TimePickerDialog.OnTimeSetListener callback = new TimePickerDialog.OnTimeSetListener() {
			@Override
			public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
				int time = minute + hourOfDay * 60;
				if (isStart) {
					item.setStartTime(time);
					OpeningHoursHoursDialogFragment.createInstance(item, null, false)
							.show(getFragmentManager(), "TimePickerDialogFragment");
				} else {
					item.setEndTime(time);
					((NormalDataFragment) getParentFragment()).addBasicOpeningHoursRule(item);
				}
			}
		};
		Calendar initialState = (Calendar) args.getSerializable(INITIAL_TIME);
		if (initialState == null) {
			initialState = Calendar.getInstance();
			initialState.set(Calendar.HOUR_OF_DAY, isStart? 8 : 20);
			initialState.set(Calendar.MINUTE, 0);
		}
		TimePickerDialog timePickerDialog = new TimePickerDialog(getActivity(),
				R.style.OsmandLightDialogTheme,
				callback,
				initialState.get(Calendar.HOUR_OF_DAY),
				initialState.get(Calendar.MINUTE),
				DateFormat.is24HourFormat(getActivity()));

		int paddingInDp = 18;
		float density = getActivity().getResources().getDisplayMetrics().density;
		int paddingInPx = (int) (paddingInDp * density);

		TextView titleTextView = new TextView(getActivity());
		titleTextView.setText(isStart ? getActivity().getString(R.string.opening_at)
				: getActivity().getString(R.string.closing_at));
		titleTextView.setPadding(paddingInPx, paddingInPx, paddingInPx, paddingInPx);
		titleTextView.setGravity(Gravity.CENTER_VERTICAL);
		titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
		titleTextView.setTextColor(getActivity().getResources().getColor(R.color.color_black));
		Typeface typeface = titleTextView.getTypeface();
		titleTextView.setTypeface(typeface, Typeface.BOLD);
		timePickerDialog.setCustomTitle(titleTextView);
		return timePickerDialog;
	}

	public static OpeningHoursHoursDialogFragment createInstance(OpeningHoursParser.BasicOpeningHourRule item,
														  Calendar initialTime,
														  boolean isStart) {
		OpeningHoursHoursDialogFragment fragment = new OpeningHoursHoursDialogFragment();
		Bundle bundle = new Bundle();
		bundle.putSerializable(BASIC_OPENING_HOUR_RULE, item);
		bundle.putBoolean(IS_START, isStart);
		bundle.putSerializable(INITIAL_TIME, initialTime);
		fragment.setArguments(bundle);
		return fragment;
	}
}
