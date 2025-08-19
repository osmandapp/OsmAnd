package net.osmand.plus.quickaction;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.CallbackWithObject;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.bottomsheets.BasePreferenceBottomSheet;
import net.osmand.plus.settings.enums.CompassMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class SelectMultipleOrientationsBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = SelectMultipleOrientationsBottomSheet.class.getSimpleName();
	public static final String SELECTED_KEYS = "selected_keys";
	public static final String DISABLED_KEYS = "disabled_keys";

	private List<String> selectedModes;
	private List<String> disabledModes;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		if (savedInstanceState != null) {
			readBundle(savedInstanceState);
		} else if (args != null) {
			readBundle(args);
		}
	}

	private void readBundle(Bundle bundle) {
		selectedModes = bundle.getStringArrayList(SELECTED_KEYS);
		disabledModes = bundle.getStringArrayList(DISABLED_KEYS);
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.rotate_map_to)));

		for (CompassMode compassMode : CompassMode.values()){
			adCompassButton(compassMode);
		}
	}

	private void adCompassButton(@NonNull CompassMode compassMode) {
		Context context = requireContext();
		View itemView = inflate(R.layout.bottom_sheet_item_with_descr_and_checkbox_56dp);
		itemView.setMinimumHeight(getDimensionPixelSize(R.dimen.bottom_sheet_large_list_item_height));

		int activeColorId = ColorUtilities.getActiveColorId(nightMode);
		int disableColorId = ColorUtilities.getDefaultIconColorId(nightMode);
		int disableColor = ContextCompat.getColor(context, disableColorId);
		boolean selected = selectedModes.contains(compassMode.getKey());
		boolean disabled = disabledModes.contains(compassMode.getKey());

		TextView tvTitle = itemView.findViewById(R.id.title);
		TextView tvDescription = itemView.findViewById(R.id.description);
		ImageView ivIcon = itemView.findViewById(R.id.icon);
		CompoundButton compoundButton = itemView.findViewById(R.id.compound_button);

		tvTitle.setText(compassMode.getTitleId());
		AndroidUiHelper.updateVisibility(tvDescription, false);

		Drawable drawableIcon;
		if (disabled) {
			drawableIcon = getPaintedIcon(compassMode.getIconId(nightMode), disableColor);
		} else {
			drawableIcon = getIcon(compassMode.getIconId(nightMode));
		}

		ivIcon.setImageDrawable(drawableIcon);
		UiUtilities.setupCompoundButton(nightMode, ColorUtilities.getColor(context,
				disabled ? disableColorId : activeColorId), compoundButton);
		compoundButton.setSaveEnabled(false);
		compoundButton.setChecked(disabled || selected);

		View.OnClickListener l = disabled ? null : v -> {
			String key = compassMode.getKey();
			boolean currentlySelected = selectedModes.contains(key);
			if (currentlySelected) {
				selectedModes.remove(key);
			} else {
				selectedModes.add(key);
			}
			compoundButton.setChecked(!currentlySelected);
		};

		items.add(new BaseBottomSheetItem.Builder()
				.setCustomView(itemView)
				.setOnClickListener(l)
				.create());
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putStringArrayList(SELECTED_KEYS, new ArrayList<>(selectedModes));
		outState.putStringArrayList(DISABLED_KEYS, new ArrayList<>(disabledModes));
	}

	@Override
	protected void onDismissButtonClickAction() {
		super.onDismissButtonClickAction();
		dismiss();
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_apply;
	}

	@Override
	protected void onRightBottomButtonClick() {
		Fragment targetFragment = getTargetFragment();
		if (targetFragment instanceof CallbackWithObject callback) {
			List<String> newSelected = new ArrayList<>();
			for (String mode : selectedModes) {
				if (!disabledModes.contains(mode)) {
					newSelected.add(mode);
				}
			}
			callback.processResult(newSelected);
		}
		dismiss();
	}

	public static void showInstance(@NonNull MapActivity mapActivity, Fragment targetFragment,
	                                @Nullable List<String> selectedModes,
	                                @Nullable List<String> disabledModes,
	                                boolean usedOnMap) {
		FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			SelectMultipleOrientationsBottomSheet fragment = new SelectMultipleOrientationsBottomSheet();
			Bundle args = new Bundle();
			args.putStringArrayList(SELECTED_KEYS, selectedModes != null ?
					new ArrayList<>(selectedModes) : new ArrayList<>());
			args.putStringArrayList(DISABLED_KEYS, disabledModes != null ?
					new ArrayList<>(disabledModes) : new ArrayList<>());
			fragment.setArguments(args);
			fragment.setTargetFragment(targetFragment, 0);
			fragment.setUsedOnMap(usedOnMap);
			fragment.show(fragmentManager, TAG);
		}
	}
}
