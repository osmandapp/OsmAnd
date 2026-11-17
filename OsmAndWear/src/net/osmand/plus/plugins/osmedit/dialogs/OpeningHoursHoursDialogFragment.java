package net.osmand.plus.plugins.osmedit.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import net.osmand.plus.R;
import net.osmand.plus.plugins.osmedit.fragments.BasicEditPoiFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.OpeningHoursParser;
import net.osmand.util.OpeningHoursParser.BasicOpeningHourRule;

public class OpeningHoursHoursDialogFragment extends DialogFragment {
	private static final String IS_START = "is_start";
	private static final String BASIC_OPENING_HOUR_RULE = "basic_opening_hour_rule";
	private static final String RULE_POSITION = "rule_position";
	private static final String TIME_POSITION = "time_position";

	private static final int DEFAULT_START_TIME = 9 * 60;
	private static final int DEFAULT_END_TIME = 18 * 60;

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Bundle args = getArguments();
		boolean isStart = args.getBoolean(IS_START);
		BasicOpeningHourRule item = AndroidUtils.getSerializable(args, BASIC_OPENING_HOUR_RULE, BasicOpeningHourRule.class);
		int rulePosition = args.getInt(RULE_POSITION);
		int timePosition = args.getInt(TIME_POSITION);

		boolean newTimeSpan = timePosition == item.timesSize();
		boolean createNew = rulePosition == -1 || newTimeSpan;
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		int time;
		if (isStart) {
			time = newTimeSpan ? DEFAULT_START_TIME : item.getStartTime(timePosition);
		} else {
			time = newTimeSpan ? DEFAULT_END_TIME : item.getEndTime(timePosition);
		}
		int hour = time / 60;
		int minute = time - hour * 60;

		TimePicker timePicker = new TimePicker(getActivity());
		timePicker.setIs24HourView(DateFormat.is24HourFormat(getActivity()));
		timePicker.setHour(hour);
		timePicker.setMinute(minute);

		builder.setView(timePicker)
				.setPositiveButton(isStart && createNew ? R.string.next_proceed
								: R.string.shared_string_save,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								timePicker.clearFocus();
								int minute = timePicker.getCurrentMinute();
								int hourOfDay = timePicker.getCurrentHour();
								int time = minute + hourOfDay * 60;
								if (newTimeSpan) {
									item.addTimeRange(DEFAULT_START_TIME, DEFAULT_END_TIME);
								}
								if (isStart && createNew) {
									item.setStartTime(time, timePosition);
									createInstance(item, rulePosition, false, timePosition)
											.show(getFragmentManager(), "TimePickerDialogFragment");
								} else {
									if (isStart) {
										item.setStartTime(time, timePosition);
									} else {
										item.setEndTime(time, timePosition);
									}
									((BasicEditPoiFragment) getParentFragment())
											.setBasicOpeningHoursRule(item, rulePosition);
								}
							}
						})
				.setNegativeButton(R.string.shared_string_cancel, (dialog, which) -> {
					BasicEditPoiFragment editPoiFragment = ((BasicEditPoiFragment) getParentFragment());
					if (editPoiFragment != null) {
						editPoiFragment.removeUnsavedOpeningHours();
					}
				});

		int paddingInDp = 18;
		float density = getActivity().getResources().getDisplayMetrics().density;
		int paddingInPx = (int) (paddingInDp * density);

		TypedValue textColorTypedValue = new TypedValue();
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
			int rulePosition,
			boolean isStart,
			int timePosition) {
		OpeningHoursHoursDialogFragment fragment = new OpeningHoursHoursDialogFragment();
		Bundle bundle = new Bundle();
		bundle.putSerializable(BASIC_OPENING_HOUR_RULE, item);
		bundle.putInt(RULE_POSITION, rulePosition);
		bundle.putBoolean(IS_START, isStart);
		bundle.putInt(TIME_POSITION, timePosition);
		fragment.setArguments(bundle);
		return fragment;
	}
}
