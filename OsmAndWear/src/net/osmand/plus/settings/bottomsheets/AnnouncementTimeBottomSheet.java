package net.osmand.plus.settings.bottomsheets;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.slider.Slider;
import com.google.android.material.slider.Slider.OnChangeListener;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
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

	private OsmandApplication app;
	private AnnounceTimeDistances announceTimeDistances;

	private ListPreferenceEx listPreference;
	private int selectedEntryIndex = -1;

	private TextViewEx tvSeekBarLabel;
	private Slider slider;
	private ImageView ivArrow;
	private TextViewEx tvIntervalsDescr;

	private boolean collapsed = true;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = requiredMyApplication();
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
			Fragment target = getTargetFragment();
			if (target instanceof OnPreferenceChanged) {
				((OnPreferenceChanged) target).onPreferenceChanged(listPreference.getKey());
			}
		}

		dismiss();
	}

	private ListPreferenceEx getListPreference() {
		return (ListPreferenceEx) getPreference();
	}

	private BaseBottomSheetItem createBottomSheetItem() {
		View rootView = UiUtilities.getInflater(getContext(), nightMode)
				.inflate(R.layout.bottom_sheet_announcement_time, null);

		tvSeekBarLabel = rootView.findViewById(R.id.tv_seek_bar_label);
		slider = rootView.findViewById(R.id.arrival_slider);
		ivArrow = rootView.findViewById(R.id.iv_arrow);
		tvIntervalsDescr = rootView.findViewById(R.id.tv_interval_descr);
		int appModeColor = getAppMode().getProfileColor(nightMode);

		slider.setValue(selectedEntryIndex);
		slider.setValueFrom(0);
		slider.setValueTo(listPreference.getEntries().length - 1);
		slider.setStepSize(1);
		slider.addOnChangeListener(new OnChangeListener() {
			@Override
			public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
				int intValue = (int) value;
				if (intValue != selectedEntryIndex) {
					selectedEntryIndex = intValue;
					updateViews();
				}
			}
		});
		UiUtilities.setupSlider(slider, nightMode, appModeColor, true);
		rootView.findViewById(R.id.description_container).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleDescriptionVisibility();
			}
		});

		return new Builder()
				.setCustomView(rootView)
				.create();
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
		try {
			if (!fm.isStateSaved()) {
				Bundle args = new Bundle();
				args.putString(PREFERENCE_ID, prefKey);
				AnnouncementTimeBottomSheet fragment = new AnnouncementTimeBottomSheet();
				fragment.setArguments(args);
				fragment.setAppMode(appMode);
				fragment.setUsedOnMap(usedOnMap);
				fragment.setTargetFragment(target, 0);
				fragment.show(fm, TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}
}