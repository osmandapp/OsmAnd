package net.osmand.plus.settings.bottomsheets;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.fragments.ApplyQueryType;
import net.osmand.plus.settings.fragments.OnConfirmPreferenceChange;
import net.osmand.plus.settings.fragments.OnPreferenceChanged;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.TextViewEx;

import org.apache.commons.logging.Log;

public class CompoundButtonBooleanBottomSheet extends BooleanPreferenceBottomSheet {

	public static final String TAG = CompoundButtonBooleanBottomSheet.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(CompoundButtonBooleanBottomSheet.class);

	@Override
	protected void createBooleanItem(OsmandApplication app, SwitchPreferenceEx switchPreference, OsmandPreference preference) {
		View titleView = UiUtilities.getInflater(app, nightMode).inflate(R.layout.title_with_desc, null);
		String title = switchPreference.getTitle().toString();
		TextView titleTv = titleView.findViewById(R.id.title);
		titleTv.setText(title);

		String description = switchPreference.getDescription();
		TextView descriptionTv = titleView.findViewById(R.id.description);
		if (description != null) {
			descriptionTv.setText(description);
		} else {
			AndroidUiHelper.setVisibility(View.GONE, descriptionTv);
		}

		BaseBottomSheetItem titleItem = new BaseBottomSheetItem.Builder()
				.setCustomView(titleView)
				.create();
		items.add(titleItem);

		CharSequence summaryOn = switchPreference.getSummaryOn();
		CharSequence summaryOff = switchPreference.getSummaryOff();
		String on = summaryOn == null || summaryOn.toString().isEmpty()
				? getString(R.string.shared_string_enabled) : summaryOn.toString();
		String off = summaryOff == null || summaryOff.toString().isEmpty()
				? getString(R.string.shared_string_disabled) : summaryOff.toString();

		boolean checked = switchPreference.isChecked();
		BottomSheetItemWithCompoundButton[] preferenceButtons = new BottomSheetItemWithCompoundButton[2];
		preferenceButtons[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setCustomView(getCustomRadioButtonView(app, on, checked, true))
				.setOnClickListener(v -> onRadioButtonClick(v, preferenceButtons, switchPreference))
				.create();
		preferenceButtons[1] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setCustomView(getCustomRadioButtonView(app, off, checked, false))
				.setOnClickListener(v -> onRadioButtonClick(v, preferenceButtons, switchPreference))
				.create();

		items.add(preferenceButtons[0]);
		items.add(preferenceButtons[1]);
	}

	private void onRadioButtonClick(View v, BottomSheetItemWithCompoundButton[] preferenceBtn, SwitchPreferenceEx preference) {
		boolean newValue = (boolean) v.getTag();
		Fragment targetFragment = getTargetFragment();
		if (targetFragment instanceof OnConfirmPreferenceChange) {
			ApplyQueryType applyQueryType = getApplyQueryType();
			if (applyQueryType == ApplyQueryType.SNACK_BAR) {
				applyQueryType = ApplyQueryType.NONE;
			}
			OnConfirmPreferenceChange confirmationInterface = (OnConfirmPreferenceChange) targetFragment;
			if (confirmationInterface.onConfirmPreferenceChange(preference.getKey(), newValue, applyQueryType)) {
				preference.setChecked(newValue);
				updatePreferenceButtons(preferenceBtn, newValue);
				if (targetFragment instanceof OnPreferenceChanged) {
					((OnPreferenceChanged) targetFragment).onPreferenceChanged(preference.getKey());
				}
			}
		}
	}

	private void updatePreferenceButtons(BottomSheetItemWithCompoundButton[] buttons, boolean checked) {
		for (BottomSheetItemWithCompoundButton button : buttons) {
			updateCustomRadioButtonView(button.getView(), checked);
		}
	}

	public View getCustomRadioButtonView(OsmandApplication app, String title, boolean checked, boolean itemBooleanState) {
		View customView = UiUtilities.getInflater(app, nightMode).inflate(R.layout.dialog_list_item_with_compound_button, null);

		FrameLayout buttonContainer = customView.findViewById(R.id.compound_buttons_container);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
		);
		params.gravity = Gravity.CENTER_VERTICAL;
		int horizontalMargin = app.getResources().getDimensionPixelSize(R.dimen.content_padding_small);
		params.setMargins(horizontalMargin, 0, horizontalMargin, 0);
		buttonContainer.setLayoutParams(params);

		TextViewEx textViewEx = customView.findViewById(R.id.text);
		textViewEx.setText(title);

		RadioButton radioButton = customView.findViewById(R.id.radio);
		radioButton.setClickable(false);
		radioButton.setFocusable(false);
		radioButton.setBackground(null);
		AndroidUiHelper.setVisibility(View.VISIBLE, radioButton);

		LinearLayout buttonView = customView.findViewById(R.id.button);
		buttonView.setMinimumHeight(app.getResources().getDimensionPixelSize(R.dimen.bottom_sheet_list_item_height));
		int color = getAppMode().getProfileColor(nightMode);
		Drawable background = UiUtilities.getColoredSelectableDrawable(app, color, 0.3f);
		AndroidUtils.setBackground(buttonView, background);

		customView.setTag(itemBooleanState);
		updateCustomRadioButtonView(customView, checked);
		return customView;
	}

	public void updateCustomRadioButtonView(View customView, boolean checked) {
		boolean buttonBooleanState = (boolean) customView.getTag();
		RadioButton button = customView.findViewById(R.id.radio);
		button.setChecked(checked == buttonBooleanState);
	}

	public static void showInstance(@NonNull FragmentManager fm, String prefId, Fragment target, boolean usedOnMap,
									@Nullable ApplicationMode appMode, ApplyQueryType applyQueryType,
									boolean profileDependent) {
		try {
			if (fm.findFragmentByTag(TAG) == null) {
				Bundle args = new Bundle();
				args.putString(PREFERENCE_ID, prefId);

				CompoundButtonBooleanBottomSheet fragment = new CompoundButtonBooleanBottomSheet();
				fragment.setArguments(args);
				fragment.setUsedOnMap(usedOnMap);
				fragment.setAppMode(appMode);
				fragment.setApplyQueryType(applyQueryType);
				fragment.setTargetFragment(target, 0);
				fragment.setProfileDependent(profileDependent);
				fragment.show(fm, TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}
}