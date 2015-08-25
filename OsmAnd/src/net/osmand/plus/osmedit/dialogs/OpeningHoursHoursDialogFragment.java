package net.osmand.plus.osmedit.dialogs;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;
import android.widget.TimePicker;

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
		TimePickerDialog timePickerDialog = new TimePickerDialog(getActivity(), callback,
				initialState.get(Calendar.HOUR_OF_DAY),
				initialState.get(Calendar.MINUTE),
				DateFormat.is24HourFormat(getActivity()));

		timePickerDialog.setTitle(isStart ? "Opening" : "Closing");
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
