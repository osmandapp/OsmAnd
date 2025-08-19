package net.osmand.plus.settings.bottomsheets;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.BooleanPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.fragments.ApplyQueryType;
import net.osmand.plus.settings.fragments.OnConfirmPreferenceChange;
import net.osmand.plus.settings.fragments.OnPreferenceChanged;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

public class BooleanRadioButtonsBottomSheet extends BooleanPreferenceBottomSheet {

	private static final String TAG = BooleanRadioButtonsBottomSheet.class.getSimpleName();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		OsmandPreference preference = settings.getPreference(getPrefId());
		SwitchPreferenceEx switchPreference = getSwitchPreferenceEx();
		if (switchPreference == null || !(preference instanceof BooleanPreference)) {
			return;
		}

		String description = switchPreference.getDescription();
		if (description != null) {
			BaseBottomSheetItem headerItem = new BottomSheetItemWithDescription.Builder()
					.setDescription(description)
					.setTitle(switchPreference.getTitle())
					.setLayoutId(R.layout.title_with_desc)
					.create();
			items.add(headerItem);
		}
		items.add(createRadioButtonItem(switchPreference, true));
		items.add(createRadioButtonItem(switchPreference, false));

		updatePreferenceButtons(switchPreference.isChecked());
	}

	@NonNull
	private BaseBottomSheetItem createRadioButtonItem(@NonNull SwitchPreferenceEx preference, boolean enabled) {
		return new BottomSheetItemWithCompoundButton.Builder()
				.setCustomView(getCustomRadioButtonView(preference, enabled))
				.setOnClickListener(v -> onRadioButtonClick(v, preference)).create();
	}

	@NonNull
	public View getCustomRadioButtonView(@NonNull SwitchPreferenceEx preference, boolean enabled) {
		View view = inflate(R.layout.dialog_list_item_with_compound_button);
		view.setTag(enabled);

		TextView textView = view.findViewById(R.id.text);
		textView.setText(getSummary(preference, enabled));
		textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, getDimensionPixelSize(R.dimen.default_list_text_size));

		int margin = getDimensionPixelSize(R.dimen.content_padding_small);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
		params.gravity = Gravity.CENTER_VERTICAL;
		params.setMargins(margin, 0, margin, 0);
		view.findViewById(R.id.compound_buttons_container).setLayoutParams(params);

		int color = getAppMode().getProfileColor(nightMode);
		LinearLayout button = view.findViewById(R.id.button);
		button.setMinimumHeight(getDimensionPixelSize(R.dimen.bottom_sheet_list_item_height));
		AndroidUtils.setBackground(button, UiUtilities.getColoredSelectableDrawable(app, color, 0.3f));

		AndroidUiHelper.setVisibility(View.VISIBLE, view.findViewById(R.id.radio));

		return view;
	}

	private void onRadioButtonClick(@NonNull View view, @NonNull SwitchPreferenceEx preference) {
		boolean newValue = (boolean) view.getTag();
		Fragment targetFragment = getTargetFragment();
		if (targetFragment instanceof OnConfirmPreferenceChange confirmationInterface) {
			ApplyQueryType applyQueryType = getApplyQueryType();
			if (applyQueryType == ApplyQueryType.SNACK_BAR) {
				applyQueryType = ApplyQueryType.NONE;
			}
			if (confirmationInterface.onConfirmPreferenceChange(preference.getKey(), newValue, applyQueryType)) {
				preference.setChecked(newValue);
				updatePreferenceButtons(newValue);

				if (targetFragment instanceof OnPreferenceChanged listener) {
					listener.onPreferenceChanged(preference.getKey());
				}
			}
		}
		dismiss();
	}

	private void updatePreferenceButtons(boolean checked) {
		for (BaseBottomSheetItem item : items) {
			if (item instanceof BottomSheetItemWithCompoundButton) {
				updateRadioButton(item.getView(), checked);
			}
		}
	}

	public void updateRadioButton(@NonNull View view, boolean checked) {
		boolean buttonBooleanState = (boolean) view.getTag();
		RadioButton button = view.findViewById(R.id.radio);
		button.setChecked(checked == buttonBooleanState);
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull String prefId,
	                                @NonNull ApplyQueryType applyQueryType, @Nullable Fragment target,
	                                @Nullable ApplicationMode appMode, boolean usedOnMap, boolean profileDependent) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle args = new Bundle();
			args.putString(PREFERENCE_ID, prefId);

			BooleanRadioButtonsBottomSheet fragment = new BooleanRadioButtonsBottomSheet();
			fragment.setArguments(args);
			fragment.setUsedOnMap(usedOnMap);
			fragment.setAppMode(appMode);
			fragment.setApplyQueryType(applyQueryType);
			fragment.setTargetFragment(target, 0);
			fragment.setProfileDependent(profileDependent);
			fragment.show(manager, TAG);
		}
	}
}