package net.osmand.plus.settings.bottomsheets;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.fragments.ApplyQueryType;
import net.osmand.plus.settings.fragments.OnConfirmPreferenceChange;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;

public class GoodsRestrictionsBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = GoodsRestrictionsBottomSheet.class.getSimpleName();

	private static final String SELECTED_KEY = "selected_key";
	private static final String HAS_CHANGES_TO_APPLY_KEY = "has_changes_to_apply_key";

	private boolean isSelected;
	private boolean hasChangesToApply = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			restoreSavedState(savedInstanceState);
		} else if (getArguments() != null) {
			restoreSavedState(getArguments());
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		View view = inflate(R.layout.bottom_sheet_goods_restriction);
		setupToggleButtons(view);
		updateView(view);
		items.add(new BaseBottomSheetItem.Builder().setCustomView(view).create());
	}

	private void setupToggleButtons(@NonNull View view) {
		LinearLayout container = view.findViewById(R.id.custom_radio_buttons);
		TextToggleButton radioGroup = new TextToggleButton(app, container, nightMode);
		TextRadioItem btnNo = createToggleButton(false);
		TextRadioItem btnYes = createToggleButton(true);
		radioGroup.setItems(btnNo, btnYes);
		radioGroup.setSelectedItem(isSelected ? btnYes : btnNo);
	}

	@NonNull
	private TextRadioItem createToggleButton(boolean enabled) {
		String title = getString(enabled ? R.string.shared_string_yes : R.string.shared_string_no);
		TextRadioItem item = new TextRadioItem(title);
		item.setOnClickListener((radioItem, view) -> {
			hasChangesToApply = true;
			if (isSelected != enabled) {
				isSelected = enabled;
				updateView(requireView());
				return true;
			}
			return false;
		});
		return item;
	}

	private void updateView(@NonNull View view) {
		View fartherDescription = view.findViewById(R.id.farther_description);
		AndroidUiHelper.setVisibility(isSelected ? View.VISIBLE : View.GONE, fartherDescription);
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	private void restoreSavedState(@NonNull Bundle bundle) {
		isSelected = bundle.getBoolean(SELECTED_KEY);
		hasChangesToApply = bundle.getBoolean(HAS_CHANGES_TO_APPLY_KEY);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(SELECTED_KEY, isSelected);
		outState.putBoolean(HAS_CHANGES_TO_APPLY_KEY, hasChangesToApply);
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		super.onDismiss(dialog);
		Activity activity = getActivity();
		if (hasChangesToApply && activity != null && !activity.isChangingConfigurations()) {
			onApply();
		}
	}

	private void onApply() {
		if (getTargetFragment() instanceof OnConfirmPreferenceChange callback) {
			callback.onConfirmPreferenceChange(getPrefId(), isSelected, ApplyQueryType.SNACK_BAR);
		}
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @NonNull Fragment target,
	                                @NonNull String key, @NonNull ApplicationMode appMode,
	                                boolean usedOnMap, boolean selected) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			Bundle args = new Bundle();
			args.putString(PREFERENCE_ID, key);
			args.putBoolean(SELECTED_KEY, selected);
			GoodsRestrictionsBottomSheet fragment = new GoodsRestrictionsBottomSheet();
			fragment.setArguments(args);
			fragment.setAppMode(appMode);
			fragment.setUsedOnMap(usedOnMap);
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
		}
	}
}
