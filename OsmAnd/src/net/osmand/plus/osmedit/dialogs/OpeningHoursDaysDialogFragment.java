package net.osmand.plus.osmedit.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;

import net.osmand.plus.R;
import net.osmand.util.OpeningHoursParser;

import java.util.Calendar;

public class OpeningHoursDaysDialogFragment extends DialogFragment {
	public static final String POSITION_TO_ADD = "position_to_add";
	public static final String ITEM = "item";

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final OpeningHoursParser.BasicOpeningHourRule item =
				(OpeningHoursParser.BasicOpeningHourRule) getArguments().getSerializable(ITEM);
		final int positionToAdd = getArguments().getInt(POSITION_TO_ADD);

		AlertDialog.Builder b = new AlertDialog.Builder(getActivity());

		boolean add = positionToAdd > -1;
		Calendar inst = Calendar.getInstance();
		final int first = inst.getFirstDayOfWeek();
		final boolean[] dayToShow = new boolean[7];
		String[] daysToShow = new String[7];
		for (int i = 0; i < 7; i++) {
			int d = (first + i - 1) % 7 + 1;
			inst.set(Calendar.DAY_OF_WEEK, d);
			daysToShow[i] = DateFormat.format("EEEE", inst).toString(); //$NON-NLS-1$
			final int pos = (d + 5) % 7;
			dayToShow[i] = item.getDays()[pos];
		}
		b.setMultiChoiceItems(daysToShow, dayToShow, new DialogInterface.OnMultiChoiceClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				dayToShow[which] = isChecked;

			}

		});
		b.setPositiveButton(add ? getActivity().getString(R.string.shared_string_add)
						: getActivity().getString(R.string.shared_string_apply),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						boolean[] days = item.getDays();
						for (int i = 0; i < 7; i++) {
							days[(first + 5 + i) % 7] = dayToShow[i];
						}
						OpeningHoursHoursDialogFragment.createInstance(item, null, true)
								.show(getFragmentManager(), "TimePickerDialogFragment");
						if (positionToAdd != -1) {

//								time.insert(item, positionToAdd);
//								selectedRule = positionToAdd;
						} else {
//								time.notifyDataSetChanged();
						}
//							updateTimePickers();

					}

				});

		b.setNegativeButton(getActivity().getString(R.string.shared_string_cancel), null);

		return b.create();
	}

	public static OpeningHoursDaysDialogFragment createInstance(final OpeningHoursParser.BasicOpeningHourRule item,
													final int positionToAdd) {
		OpeningHoursDaysDialogFragment daysDialogFragment = new OpeningHoursDaysDialogFragment();
		Bundle bundle = new Bundle();
		bundle.putSerializable(ITEM, item);
		bundle.putInt(POSITION_TO_ADD, positionToAdd);
		daysDialogFragment.setArguments(bundle);
		return daysDialogFragment;
	}
}