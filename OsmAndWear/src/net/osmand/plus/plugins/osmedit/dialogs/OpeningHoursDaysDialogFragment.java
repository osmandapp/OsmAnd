package net.osmand.plus.plugins.osmedit.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.plugins.osmedit.fragments.BasicEditPoiFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.OpeningHoursParser;
import net.osmand.util.OpeningHoursParser.BasicOpeningHourRule;

import org.apache.commons.logging.Log;

import java.util.Calendar;

public class OpeningHoursDaysDialogFragment extends DialogFragment {
	private static final Log LOG = PlatformUtil.getLog(OpeningHoursDaysDialogFragment.class);
	public static final String POSITION_TO_ADD = "position_to_add";
	public static final String ITEM = "item";

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		BasicOpeningHourRule item = AndroidUtils.getSerializable(getArguments(), ITEM, BasicOpeningHourRule.class);
		int positionToAdd = getArguments().getInt(POSITION_TO_ADD);

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

		boolean createNew = positionToAdd == -1;
		Calendar inst = Calendar.getInstance();
		int first = inst.getFirstDayOfWeek();
		boolean[] dayToShow = new boolean[7];
		String[] daysToShow = new String[7];
		for (int i = 0; i < 7; i++) {
			int d = (first + i - 1) % 7 + 1;
			inst.set(Calendar.DAY_OF_WEEK, d);
			CharSequence dayName = DateFormat.format("EEEE", inst);
			String result = "" + Character.toUpperCase(dayName.charAt(0)) +
					dayName.subSequence(1, dayName.length());
			daysToShow[i] = result; //$NON-NLS-1$
			int pos = (d + 5) % 7;
			dayToShow[i] = item.getDays()[pos];
		}
		builder.setTitle(getResources().getString(R.string.working_days));
		builder.setMultiChoiceItems(daysToShow, dayToShow, new DialogInterface.OnMultiChoiceClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				dayToShow[which] = isChecked;

			}

		});
		builder.setPositiveButton(createNew ? R.string.next_proceed : R.string.shared_string_save,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						boolean[] days = item.getDays();
						boolean activeDaysAvailable = false;
						for (int i = 0; i < 7; i++) {
							days[(first + 5 + i) % 7] = dayToShow[i];
							activeDaysAvailable = activeDaysAvailable || dayToShow[i];
						}
						if (activeDaysAvailable) {
							if (createNew) {
								OpeningHoursHoursDialogFragment.createInstance(item, positionToAdd, true, 0)
										.show(getFragmentManager(), "TimePickerDialogFragment");
							} else {
								((BasicEditPoiFragment) getParentFragment())
										.setBasicOpeningHoursRule(item, positionToAdd);
							}
						} else {
							Toast.makeText(getContext(), getString(R.string.set_working_days_to_continue), Toast.LENGTH_SHORT).show();
						}
					}

				});

		builder.setNegativeButton(getActivity().getString(R.string.shared_string_cancel), null);
		return builder.create();
	}

	public static OpeningHoursDaysDialogFragment createInstance(
			@NonNull OpeningHoursParser.BasicOpeningHourRule item,
			int positionToAdd) {
		LOG.debug("createInstance(" + "item=" + item + ", positionToAdd=" + positionToAdd + ")");
		OpeningHoursDaysDialogFragment daysDialogFragment = new OpeningHoursDaysDialogFragment();
		Bundle bundle = new Bundle();
		bundle.putSerializable(ITEM, item);
		bundle.putInt(POSITION_TO_ADD, positionToAdd);
		daysDialogFragment.setArguments(bundle);
		return daysDialogFragment;
	}
}