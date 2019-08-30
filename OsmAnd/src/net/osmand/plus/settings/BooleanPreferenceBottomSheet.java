package net.osmand.plus.settings;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.preference.DialogPreference;
import android.view.View;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.BooleanPreference;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

import org.apache.commons.logging.Log;

public class BooleanPreferenceBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = BooleanPreferenceBottomSheet.class.getSimpleName();

	private static final String PREFERENCE_ID = "preference_id";

	private static final Log LOG = PlatformUtil.getLog(BooleanPreferenceBottomSheet.class);

	private SwitchPreferenceEx switchPreference;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		switchPreference = getListPreference();
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		OsmandApplication app = getMyApplication();
		Bundle args = getArguments();
		if (app == null || args == null || switchPreference == null) {
			return;
		}
		String title = switchPreference.getTitle().toString();
		String description = switchPreference.getDescription();

		OsmandPreference preference = app.getSettings().getPreference(switchPreference.getKey());
		if (!(preference instanceof BooleanPreference)) {
			return;
		}

		items.add(new TitleItem(title));

		final OsmandSettings.BooleanPreference pref = (BooleanPreference) preference;
		final String on = getString(R.string.shared_string_on);
		final String off = getString(R.string.shared_string_off);
		boolean checked = pref.get();

		final BottomSheetItemWithCompoundButton[] preferenceBtn = new BottomSheetItemWithCompoundButton[1];
		preferenceBtn[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(checked)
				.setTitle(checked ? on : off)
				.setLayoutId(R.layout.bottom_sheet_item_with_switch)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						boolean newValue = !pref.get();
						if (switchPreference.callChangeListener(newValue)) {
							switchPreference.setChecked(newValue);
							preferenceBtn[0].setTitle(newValue ? on : off);
							preferenceBtn[0].setChecked(newValue);
						}
					}
				})
				.create();
		items.add(preferenceBtn[0]);

		if (description != null) {
			BaseBottomSheetItem preferenceDescription = new BottomSheetItemWithDescription.Builder()
					.setDescription(description)
					.setLayoutId(R.layout.bottom_sheet_item_description_long)
					.create();
			items.add(preferenceDescription);
		}
	}

	private SwitchPreferenceEx getListPreference() {
		if (switchPreference == null) {
			final String key = getArguments().getString(PREFERENCE_ID);
			final DialogPreference.TargetFragment fragment = (DialogPreference.TargetFragment) getTargetFragment();
			switchPreference = (SwitchPreferenceEx) fragment.findPreference(key);
		}
		return switchPreference;
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	public static void showInstance(@NonNull FragmentManager fm, String prefId, Fragment target) {
		try {
			if (fm.findFragmentByTag(BooleanPreferenceBottomSheet.TAG) == null) {
				Bundle args = new Bundle();
				args.putString(PREFERENCE_ID, prefId);
				BooleanPreferenceBottomSheet fragment = new BooleanPreferenceBottomSheet();
				fragment.setArguments(args);
				fragment.setTargetFragment(target, 0);
				fragment.show(fm, BooleanPreferenceBottomSheet.TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}
}