package net.osmand.plus.settings.bottomsheets;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.slider.Slider;

import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.fragments.ApplyQueryType;
import net.osmand.plus.settings.fragments.OnPreferenceChanged;
import net.osmand.plus.settings.preferences.ListPreferenceEx;

public class WakeTimeBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = WakeTimeBottomSheet.class.getSimpleName();

	private static final String SELECTED_ENTRY_INDEX_KEY = "selected_entry_index_key";
	private static final String KEEP_SCREEN_ON_ENABLED = "keep_screen_on_enabled";

	private ListPreferenceEx listPreference;

	private View sliderView;

	private int selectedEntryIndex = 1;
	private boolean keepScreenOnEnabled;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context ctx = getContext();
		listPreference = getListPreference();
		if (ctx == null || listPreference == null) {
			return;
		}
		if (savedInstanceState != null) {
			selectedEntryIndex = savedInstanceState.getInt(SELECTED_ENTRY_INDEX_KEY);
			keepScreenOnEnabled = savedInstanceState.getBoolean(KEEP_SCREEN_ON_ENABLED);
		} else {
			int savedValIndex = listPreference.getValueIndex();
			keepScreenOnEnabled = savedValIndex <= 0;
			selectedEntryIndex = savedValIndex > 0 ? savedValIndex : 1;
		}

		items.add(new TitleItem(listPreference.getDialogTitle()));

		BaseBottomSheetItem preferenceDescription = new BottomSheetItemWithDescription.Builder()
				.setDescription(listPreference.getDescription())
				.setLayoutId(R.layout.bottom_sheet_item_descr)
				.create();
		items.add(preferenceDescription);

		String on = getString(R.string.keep_screen_on);
		String off = getString(R.string.keep_screen_on); // also needs to say 'on' the way the dialog is designed.
		BottomSheetItemWithCompoundButton[] preferenceBtn = new BottomSheetItemWithCompoundButton[1];
		preferenceBtn[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(keepScreenOnEnabled)
				.setTitle(keepScreenOnEnabled ? on : off)
				.setLayoutId(R.layout.bottom_sheet_item_with_switch_no_icon)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						keepScreenOnEnabled = !keepScreenOnEnabled;
						preferenceBtn[0].setTitle(keepScreenOnEnabled ? on : off);
						preferenceBtn[0].setChecked(keepScreenOnEnabled);
						AndroidUiHelper.updateVisibility(sliderView, !keepScreenOnEnabled);
						setupHeightAndBackground(getView());
					}
				})
				.create();
		items.add(preferenceBtn[0]);

		DividerItem dividerItem = new DividerItem(ctx);
		int topMargin = getDimensionPixelSize(R.dimen.context_menu_subtitle_margin);
		int startMargin = getDimensionPixelSize(R.dimen.content_padding);
		dividerItem.setMargins(startMargin, topMargin, 0, 0);
		items.add(dividerItem);
		items.add(new DividerSpaceItem(ctx, getDimensionPixelSize(R.dimen.content_padding_small)));

		sliderView = UiUtilities.getInflater(ctx, nightMode).inflate(R.layout.bottom_sheet_item_slider_with_two_text, null);
		AndroidUiHelper.updateVisibility(sliderView, !keepScreenOnEnabled);

		TextView tvSliderTitle = sliderView.findViewById(android.R.id.title);
		tvSliderTitle.setText(getString(R.string.wake_time));

		TextView tvSliderSummary = sliderView.findViewById(android.R.id.summary);
		tvSliderSummary.setText(listPreference.getEntries()[selectedEntryIndex]);

		Slider slider = sliderView.findViewById(R.id.slider);
		slider.setValue(selectedEntryIndex);
		slider.setStepSize(1);
		slider.setValueFrom(1);
		slider.setValueTo(listPreference.getEntryValues().length - 1);
		slider.addOnChangeListener((sliderView, value, fromUser) -> {
			if (fromUser) {
				selectedEntryIndex = (int) value;
				tvSliderSummary.setText(listPreference.getEntries()[selectedEntryIndex]);
			}
		});

		int appModeColor = getAppMode().getProfileColor(nightMode);
		UiUtilities.setupSlider(slider, nightMode, appModeColor, true);

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(sliderView)
				.create());

		BaseBottomSheetItem timeoutDescription = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.screen_timeout_descr, getString(R.string.system_screen_timeout)))
				.setLayoutId(R.layout.bottom_sheet_item_descr)
				.create();
		items.add(timeoutDescription);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		for (BaseBottomSheetItem item : items) {
			if (item instanceof BottomSheetItemWithCompoundButton itemWithCompoundButton) {
				itemWithCompoundButton.getCompoundButton().setSaveEnabled(false);
			}
		}
	}

	@Override
	protected void onRightBottomButtonClick() {
		if (keepScreenOnEnabled) {
			selectedEntryIndex = 0;
		}
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

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(SELECTED_ENTRY_INDEX_KEY, selectedEntryIndex);
		outState.putBoolean(KEEP_SCREEN_ON_ENABLED, keepScreenOnEnabled);
	}

	private ListPreferenceEx getListPreference() {
		return (ListPreferenceEx) getPreference();
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager, String prefId, Fragment target, boolean usedOnMap,
	                                   @Nullable ApplicationMode appMode, ApplyQueryType applyQueryType,
	                                   boolean profileDependent) {
		try {
			if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
				Bundle args = new Bundle();
				args.putString(PREFERENCE_ID, prefId);

				WakeTimeBottomSheet fragment = new WakeTimeBottomSheet();
				fragment.setArguments(args);
				fragment.setUsedOnMap(usedOnMap);
				fragment.setAppMode(appMode);
				fragment.setApplyQueryType(applyQueryType);
				fragment.setTargetFragment(target, 0);
				fragment.setProfileDependent(profileDependent);
				fragment.show(fragmentManager, TAG);
			}
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}
}