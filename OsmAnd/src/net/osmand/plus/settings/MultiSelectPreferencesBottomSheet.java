package net.osmand.plus.settings;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.View;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.util.Algorithms;

public class MultiSelectPreferencesBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = MultiSelectPreferencesBottomSheet.class.getSimpleName();

	private static final String TITLE_KEY = "title_key";
	private static final String DESCRIPTION_KEY = "description_key";
	private static final String PREFERENCES_PARAMETERS_KEY = "preferences_parameters_key";

	private String title = "";
	private String description = "";

	private String[] vals = null;
	private OsmandSettings.OsmandPreference<Boolean>[] prefs = null;
	private boolean[] tempPrefs = null;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		final OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(TITLE_KEY)) {
				title = savedInstanceState.getString(PREFERENCES_PARAMETERS_KEY);
			}
			if (savedInstanceState.containsKey(DESCRIPTION_KEY)) {
				description = savedInstanceState.getString(PREFERENCES_PARAMETERS_KEY);
			}
			if (savedInstanceState.containsKey(PREFERENCES_PARAMETERS_KEY)) {
				tempPrefs = savedInstanceState.getBooleanArray(PREFERENCES_PARAMETERS_KEY);
			}
		}

		items.add(new TitleItem(title));

		if (!Algorithms.isEmpty(description)) {
			items.add(new LongDescriptionItem(description));
		}

		tempPrefs = new boolean[prefs.length];

		for (int i = 0; i < prefs.length; i++) {
			String title = vals[i];
			boolean selected = prefs[i].get();
			tempPrefs[i] = selected;

			final int index = i;
			final BottomSheetItemWithCompoundButton[] item = new BottomSheetItemWithCompoundButton[1];
			item[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
					.setChecked(selected)
					.setTitle(title)
					.setLayoutId(R.layout.bottom_sheet_item_with_switch_no_icon)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							boolean checked = !item[0].isChecked();
							item[0].setChecked(checked);
							tempPrefs[index] = checked;
						}
					})
					.setTag(i)
					.create();
			items.add(item[0]);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		for (BaseBottomSheetItem item : items) {
			if (item instanceof BottomSheetItemWithCompoundButton) {
				Integer prefIndex = (Integer) item.getTag();
				((BottomSheetItemWithCompoundButton) item).setChecked(tempPrefs[prefIndex]);
			}
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBooleanArray(PREFERENCES_PARAMETERS_KEY, tempPrefs);
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	@Override
	protected void onRightBottomButtonClick() {
		for (int i = 0; i < prefs.length; i++) {
			prefs[i].set(tempPrefs[i]);
		}
		dismiss();
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager, String title, String description, String[] vals, OsmandSettings.OsmandPreference<Boolean>[] prefs, Fragment target) {
		try {
			Bundle args = new Bundle();

			MultiSelectPreferencesBottomSheet fragment = new MultiSelectPreferencesBottomSheet();
			fragment.title = title;
			fragment.description = description;
			fragment.vals = vals;
			fragment.prefs = prefs;
			fragment.setTargetFragment(target, 0);
			fragment.setArguments(args);
			fragment.show(fragmentManager, TAG);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}
}