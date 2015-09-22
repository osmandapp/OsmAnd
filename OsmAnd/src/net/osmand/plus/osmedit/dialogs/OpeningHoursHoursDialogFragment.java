package net.osmand.plus.osmedit.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.TimePicker;

import net.osmand.plus.R;
import net.osmand.plus.osmedit.BasicDataFragment;
import net.osmand.util.OpeningHoursParser;

public class OpeningHoursHoursDialogFragment extends DialogFragment {
	public static final String IS_START = "is_start";
	public static final String BASIC_OPENING_HOUR_RULE = "basic_opening_hour_rule";
	public static final String POSITION_TO_ADD = "position_to_add";

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Bundle args = getArguments();
		final boolean isStart = args.getBoolean(IS_START);
		final int positionToAdd = args.getInt(POSITION_TO_ADD);
		final boolean createNew = positionToAdd == -1;
		final OpeningHoursParser.BasicOpeningHourRule item = (OpeningHoursParser.BasicOpeningHourRule)
				args.getSerializable(BASIC_OPENING_HOUR_RULE);
		AlertDialog.Builder builder =
				new AlertDialog.Builder(getActivity());

		int time = isStart ? item.getStartTime() : item.getEndTime();
		int hour = time / 60;
		int minute = time - hour * 60;

		final TimePicker timePicker = new TimePicker(getActivity());
		timePicker.setIs24HourView(DateFormat.is24HourFormat(getActivity()));
		timePicker.setCurrentHour(hour);
		timePicker.setCurrentMinute(minute);

		builder.setView(timePicker)
				.setPositiveButton(isStart && createNew ? R.string.next_proceed
								: R.string.shared_string_save,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								int minute = timePicker.getCurrentMinute();
								int hourOfDay = timePicker.getCurrentHour();
								int time = minute + hourOfDay * 60;
								if (isStart && createNew) {
									item.setStartTime(time);
									OpeningHoursHoursDialogFragment
											.createInstance(item, positionToAdd, false)
											.show(getFragmentManager(), "TimePickerDialogFragment");
								} else {
									if (isStart) {
										item.setStartTime(time);
									} else {
										item.setEndTime(time);
									}
									((BasicDataFragment) getParentFragment())
											.setBasicOpeningHoursRule(item, positionToAdd);
								}
							}
						})
				.setNegativeButton(R.string.shared_string_cancel, null);

		int paddingInDp = 18;
		float density = getActivity().getResources().getDisplayMetrics().density;
		int paddingInPx = (int) (paddingInDp * density);

		final TypedValue textColorTypedValue = new TypedValue();
		getActivity().getTheme().resolveAttribute(android.R.attr.textColorPrimary,
				textColorTypedValue, true);
		int textColor = textColorTypedValue.data;

		TextView titleTextView = new TextView(getActivity());
		titleTextView.setText(isStart ? getActivity().getString(R.string.opening_at)
				: getActivity().getString(R.string.closing_at));
		titleTextView.setPadding(paddingInPx, paddingInPx, paddingInPx, paddingInPx);
		titleTextView.setGravity(Gravity.CENTER_VERTICAL);
		titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
		titleTextView.setTextColor(textColor);
		Typeface typeface = titleTextView.getTypeface();
		titleTextView.setTypeface(typeface, Typeface.BOLD);
		builder.setCustomTitle(titleTextView);
		return builder.create();
	}

	public static OpeningHoursHoursDialogFragment createInstance(
			@NonNull OpeningHoursParser.BasicOpeningHourRule item,
			int positionToAdd,
			boolean isStart) {
		OpeningHoursHoursDialogFragment fragment = new OpeningHoursHoursDialogFragment();
		Bundle bundle = new Bundle();
		bundle.putSerializable(BASIC_OPENING_HOUR_RULE, item);
		bundle.putSerializable(POSITION_TO_ADD, positionToAdd);
		bundle.putBoolean(IS_START, isStart);
		fragment.setArguments(bundle);
		return fragment;
	}
}
