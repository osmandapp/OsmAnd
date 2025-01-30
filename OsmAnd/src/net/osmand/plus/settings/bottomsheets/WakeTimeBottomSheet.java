package net.osmand.plus.settings.bottomsheets;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;

import com.google.android.material.slider.Slider;

import net.osmand.plus.R;
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
import net.osmand.plus.settings.fragments.search.SearchablePreferenceDialog;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.utils.UiUtilities;

import java.util.Optional;

public class WakeTimeBottomSheet extends BasePreferenceBottomSheet implements SearchablePreferenceDialog {

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

		String on = getKeepScreenOn();
		String off = getKeepScreenOn(); // also needs to say 'on' the way the dialog is designed.
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
		int topMargin = ctx.getResources().getDimensionPixelSize(R.dimen.context_menu_subtitle_margin);
		int startMargin = ctx.getResources().getDimensionPixelSize(R.dimen.content_padding);
		dividerItem.setMargins(startMargin, topMargin, 0, 0);
		items.add(dividerItem);
		items.add(new DividerSpaceItem(ctx, ctx.getResources().getDimensionPixelSize(R.dimen.content_padding_small)));

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
		slider.addOnChangeListener(new Slider.OnChangeListener() {
			@Override
			public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
				if (fromUser) {
					selectedEntryIndex = (int) value;
					tvSliderSummary.setText(listPreference.getEntries()[selectedEntryIndex]);
				}
			}
		});

		int appModeColor = getAppMode().getProfileColor(nightMode);
		UiUtilities.setupSlider(slider, nightMode, appModeColor, true);

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(sliderView)
				.create());

		BaseBottomSheetItem timeoutDescription = new BottomSheetItemWithDescription.Builder()
				.setDescription(getTimeoutDescription())
				.setLayoutId(R.layout.bottom_sheet_item_descr)
				.create();
		items.add(timeoutDescription);
	}

	@NonNull
	private String getKeepScreenOn() {
		return getString(R.string.keep_screen_on);
	}

	@NonNull
	private String getTimeoutDescription() {
		return getString(R.string.screen_timeout_descr, getString(R.string.system_screen_timeout));
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		for (BaseBottomSheetItem item : items) {
			if (item instanceof final BottomSheetItemWithCompoundButton itemWithCompoundButton) {
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

	@NonNull
	public static WakeTimeBottomSheet createInstance(final Preference preference,
													 final Optional<Fragment> target,
													 final boolean usedOnMap,
													 final @Nullable ApplicationMode appMode,
													 final ApplyQueryType applyQueryType,
													 final boolean profileDependent) {
		final WakeTimeBottomSheet bottomSheet = new WakeTimeBottomSheet();
		bottomSheet.setApplyQueryType(applyQueryType);
		bottomSheet.setProfileDependent(profileDependent);
		return BasePreferenceBottomSheetInitializer
				.initialize(bottomSheet)
				.with(Optional.of(preference), appMode, usedOnMap, target);
	}

	@Override
	public void show(final FragmentManager fragmentManager) {
		show(fragmentManager, TAG);
	}

	@Override
	public String getSearchableInfo() {
		return String.join(", ", getKeepScreenOn(), getTimeoutDescription());
	}
}