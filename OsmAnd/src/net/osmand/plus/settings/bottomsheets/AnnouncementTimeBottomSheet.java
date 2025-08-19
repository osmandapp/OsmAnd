package net.osmand.plus.settings.bottomsheets;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.slider.Slider;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem.Builder;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routing.data.AnnounceTimeDistances;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.fragments.OnPreferenceChanged;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.widgets.TextViewEx;

import org.apache.commons.logging.Log;

import static net.osmand.plus.settings.bottomsheets.SingleSelectPreferenceBottomSheet.SELECTED_ENTRY_INDEX_KEY;


public class AnnouncementTimeBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = AnnouncementTimeBottomSheet.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(AnnouncementTimeBottomSheet.class);

	private AnnounceTimeDistances announceTimeDistances;

	private ListPreferenceEx listPreference;
	private int selectedEntryIndex = -1;

	private TextViewEx tvSeekBarLabel;
	private ImageView ivArrow;
	private TextViewEx tvIntervalsDescr;

	private boolean collapsed = true;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		announceTimeDistances = new AnnounceTimeDistances(getAppMode(), app);

		listPreference = getListPreference();
		if (listPreference == null || listPreference.getEntries() == null ||
				listPreference.getEntryValues() == null) {
			return;
		}
		if (savedInstanceState != null) {
			selectedEntryIndex = savedInstanceState.getInt(SELECTED_ENTRY_INDEX_KEY);
		} else {
			selectedEntryIndex = listPreference.findIndexOfValue(listPreference.getValue());
		}

		items.add(createBottomSheetItem());
	}

	@Override
	public void onResume() {
		super.onResume();
		updateViews();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(SELECTED_ENTRY_INDEX_KEY, selectedEntryIndex);
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	@Override
	protected void onRightBottomButtonClick() {
		Object[] entryValues = listPreference.getEntryValues();
		if (entryValues != null && selectedEntryIndex >= 0) {
			Object value = entryValues[selectedEntryIndex];
			if (listPreference.callChangeListener(value)) {
				listPreference.setValue(value);
			}
			if (getTargetFragment() instanceof OnPreferenceChanged listener) {
				listener.onPreferenceChanged(listPreference.getKey());
			}
		}
		dismiss();
	}

	@Nullable
	private ListPreferenceEx getListPreference() {
		return (ListPreferenceEx) getPreference();
	}

	@NonNull
	private BaseBottomSheetItem createBottomSheetItem() {
		View rootView = inflate(R.layout.bottom_sheet_announcement_time);

		tvSeekBarLabel = rootView.findViewById(R.id.tv_seek_bar_label);
		Slider arrivalSlider = rootView.findViewById(R.id.arrival_slider);
		ivArrow = rootView.findViewById(R.id.iv_arrow);
		tvIntervalsDescr = rootView.findViewById(R.id.tv_interval_descr);
		int appModeColor = getAppMode().getProfileColor(nightMode);

		arrivalSlider.setValue(selectedEntryIndex);
		arrivalSlider.setValueFrom(0);
		arrivalSlider.setValueTo(listPreference.getEntries().length - 1);
		arrivalSlider.setStepSize(1);
		arrivalSlider.addOnChangeListener((slider, value, fromUser) -> {
			int intValue = (int) value;
			if (intValue != selectedEntryIndex) {
				selectedEntryIndex = intValue;
				updateViews();
			}
		});
		UiUtilities.setupSlider(arrivalSlider, nightMode, appModeColor, true);
		rootView.findViewById(R.id.description_container).setOnClickListener(v -> toggleDescriptionVisibility());

		return new Builder().setCustomView(rootView).create();
	}

	private void updateViews() {
		tvSeekBarLabel.setText(listPreference.getEntries()[selectedEntryIndex]);
		float value = (float) listPreference.getEntryValues()[selectedEntryIndex];
		announceTimeDistances.setArrivalDistances(value);
		tvIntervalsDescr.setText(announceTimeDistances.getIntervalsDescription(app));
	}

	private void toggleDescriptionVisibility() {
		collapsed = !collapsed;
		ivArrow.setImageResource(collapsed ? R.drawable.ic_action_arrow_down : R.drawable.ic_action_arrow_up);
		AndroidUiHelper.updateVisibility(tvIntervalsDescr, !collapsed);
	}

	public static void showInstance(@NonNull FragmentManager fm, String prefKey, Fragment target,
	                                @Nullable ApplicationMode appMode, boolean usedOnMap) {
		if (AndroidUtils.isFragmentCanBeAdded(fm, TAG)) {
			Bundle args = new Bundle();
			args.putString(PREFERENCE_ID, prefKey);
			AnnouncementTimeBottomSheet fragment = new AnnouncementTimeBottomSheet();
			fragment.setArguments(args);
			fragment.setAppMode(appMode);
			fragment.setUsedOnMap(usedOnMap);
			fragment.setTargetFragment(target, 0);
			fragment.show(fm, TAG);
		}
	}
}