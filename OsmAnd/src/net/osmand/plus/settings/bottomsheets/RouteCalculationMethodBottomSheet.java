package net.osmand.plus.settings.bottomsheets;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.RouteCalculationMethod;
import net.osmand.plus.settings.fragments.OnPreferenceChanged;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

public class RouteCalculationMethodBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = RouteCalculationMethodBottomSheet.class.getSimpleName();

	private RouteCalculationMethod selectedMethod;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		selectedMethod = settings.ROUTE_CALCULATION_METHOD.getModeValue(getAppMode());
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context ctx = getContext();
		if (ctx == null) {
			return;
		}
		ctx = UiUtilities.getThemedContext(ctx, nightMode);

		items.add(new TitleItem(getString(R.string.route_calculation_method)));

		int normalColor = ColorUtilities.getDefaultIconColor(ctx, nightMode);
		int checkedColor = isProfileDependent()
				? getAppMode().getProfileColor(nightMode)
				: ContextCompat.getColor(ctx, getActiveColorId());

		for (RouteCalculationMethod method : RouteCalculationMethod.values()) {
			BottomSheetItemWithCompoundButton item = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
					.setChecked(selectedMethod == method)
					.setButtonTintList(AndroidUtils.createCheckedColorIntStateList(normalColor, checkedColor))
					.setDescription(method.getDescription(ctx))
					.setDescriptionMaxLines(Integer.MAX_VALUE)
					.setTitle(method.toHumanString(ctx))
					.setTag(method)
					.setLayoutId(R.layout.bottom_sheet_item_with_long_descr_and_left_radio_btn)
					.setOnClickListener(v -> methodSelected(method))
					.create();
			items.add(item);
		}
	}

	private void methodSelected(@NonNull RouteCalculationMethod method) {
		if (selectedMethod != method) {
			selectedMethod = method;
			ListPreferenceEx preference = getListPreference();
			if (preference != null) {
				int value = method.ordinal();
				if (preference.callChangeListener(value)) {
					preference.setValue(value);
				}
			}
			updateMethodItems();
		}
		Fragment target = getTargetFragment();
		if (target instanceof OnPreferenceChanged callback) {
			callback.onPreferenceChanged(settings.ROUTE_CALCULATION_METHOD.getId());
		}
		dismiss();
	}

	private void updateMethodItems() {
		for (BaseBottomSheetItem item : items) {
			if (item instanceof BottomSheetItemWithCompoundButton buttonItem) {
				buttonItem.setChecked(Algorithms.objectEquals(item.getTag(), selectedMethod));
			}
		}
	}

	@Nullable
	private ListPreferenceEx getListPreference() {
		return getPreference() instanceof ListPreferenceEx preference ? preference : null;
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	public static boolean showInstance(@NonNull FragmentManager manager, @NonNull String key,
			@Nullable Fragment target, @Nullable ApplicationMode appMode, boolean profileDependent) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle args = new Bundle();
			args.putString(PREFERENCE_ID, key);

			RouteCalculationMethodBottomSheet fragment = new RouteCalculationMethodBottomSheet();
			fragment.setArguments(args);
			fragment.setAppMode(appMode);
			fragment.setTargetFragment(target, 0);
			fragment.setProfileDependent(profileDependent);
			fragment.show(manager, TAG);
			return true;
		}
		return false;
	}
}
