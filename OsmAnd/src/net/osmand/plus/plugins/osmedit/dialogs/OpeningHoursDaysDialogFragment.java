package net.osmand.plus.plugins.osmedit.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseAlertDialogFragment;
import net.osmand.plus.plugins.osmedit.fragments.BasicEditPoiFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.OpeningHoursParser.BasicOpeningHourRule;

import org.apache.commons.logging.Log;

import java.util.Calendar;

public class OpeningHoursDaysDialogFragment extends BaseAlertDialogFragment {

	private static final String TAG = OpeningHoursDaysDialogFragment.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(OpeningHoursDaysDialogFragment.class);
	public static final String POSITION_TO_ADD = "position_to_add";
	public static final String ITEM = "item";

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		updateNightMode();
		AlertDialog.Builder builder = createDialogBuilder();

		Bundle args = requireArguments();
		BasicOpeningHourRule item = AndroidUtils.getSerializable(args, ITEM, BasicOpeningHourRule.class);
		int positionToAdd = args.getInt(POSITION_TO_ADD);

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
		builder.setTitle(getString(R.string.working_days));
		builder.setMultiChoiceItems(daysToShow, dayToShow,
				(dialog, which, isChecked) -> dayToShow[which] = isChecked);

		builder.setPositiveButton(createNew ? R.string.next_proceed : R.string.shared_string_save,
				(dialog, which) -> {
					boolean[] days = item.getDays();
					boolean activeDaysAvailable = false;
					for (int i = 0; i < 7; i++) {
						days[(first + 5 + i) % 7] = dayToShow[i];
						activeDaysAvailable = activeDaysAvailable || dayToShow[i];
					}
					if (activeDaysAvailable) {
						if (createNew) {
							FragmentManager fragmentManager = getFragmentManager();
							if (fragmentManager != null) {
								OpeningHoursHoursDialogFragment.showInstance(
										fragmentManager, item, positionToAdd, true, 0);
							}
						} else {
							((BasicEditPoiFragment) getParentFragment())
									.setBasicOpeningHoursRule(item, positionToAdd);
						}
					} else {
						app.showShortToastMessage(R.string.set_working_days_to_continue);
					}
				});

		builder.setNegativeButton(getString(R.string.shared_string_cancel), null);
		return builder.create();
	}

	public static void showInstance(@NonNull FragmentManager childFragmentManager,
	                                @NonNull BasicOpeningHourRule item, int positionToAdd) {
		if (AndroidUtils.isFragmentCanBeAdded(childFragmentManager, TAG)) {
			LOG.debug("createInstance(" + "item=" + item + ", positionToAdd=" + positionToAdd + ")");
			OpeningHoursDaysDialogFragment fragment = new OpeningHoursDaysDialogFragment();
			Bundle bundle = new Bundle();
			bundle.putSerializable(ITEM, item);
			bundle.putInt(POSITION_TO_ADD, positionToAdd);
			fragment.setArguments(bundle);
			fragment.show(childFragmentManager, TAG);
		}
	}
}