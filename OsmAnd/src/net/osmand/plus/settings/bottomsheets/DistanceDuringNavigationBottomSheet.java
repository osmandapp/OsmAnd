package net.osmand.plus.settings.bottomsheets;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.BooleanPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.fragments.ApplyQueryType;
import net.osmand.plus.settings.fragments.OnConfirmPreferenceChange;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public class DistanceDuringNavigationBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = DistanceDuringNavigationBottomSheet.class.getSimpleName();

	private UiUtilities uiUtilities;
	private BooleanPreference preference;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}
		OsmandPreference<?> osmandPreference = app.getSettings().getPreference(getPreference().getKey());
		if (!(osmandPreference instanceof BooleanPreference)) {
			return;
		}
		preference = (BooleanPreference) osmandPreference;
		uiUtilities = app.getUIUtilities();
		items.add(createBottomSheetItem());
	}

	private BaseBottomSheetItem createBottomSheetItem() {
		View rootView = UiUtilities.getInflater(getContext(), nightMode).inflate(R.layout.distance_during_navigation_bottom_sheet, null);
		boolean isPreciseMode = preference.getModeValue(getAppMode());
		ImageView previewIcon = rootView.findViewById(R.id.preview_icon);
		previewIcon.setImageDrawable(uiUtilities.getActiveIcon(isPreciseMode ? R.drawable.ic_action_distance_number_precise : R.drawable.ic_action_distance_number_rounded, nightMode));
		TextView descriptionView = rootView.findViewById(R.id.description);
		descriptionView.setText(getString(R.string.distance_during_navigation_description, getString(R.string.precise), getString(R.string.round_up)));

		setupModes(rootView);

		return new BaseBottomSheetItem.Builder()
				.setCustomView(rootView)
				.create();
	}

	private void setupModes(@NonNull View view) {
		ViewGroup container = view.findViewById(R.id.buttons_container);
		LayoutInflater inflater = UiUtilities.getInflater(getContext(), nightMode);

		for (DistanceDuringNavigationMode mode : DistanceDuringNavigationMode.values()) {
			boolean hasDivider = mode.ordinal() != DistanceDuringNavigationMode.values().length - 1;
			View propertyView = createRadioButton(mode, inflater, container, hasDivider);
			container.addView(propertyView);
		}
	}

	private View createRadioButton(@NonNull DistanceDuringNavigationMode mode, @NonNull LayoutInflater inflater, @Nullable ViewGroup container, boolean hasDivider) {
		View view = inflater.inflate(R.layout.bottom_sheet_item_with_descr_radio_and_icon_btn, container, false);
		TextView title = view.findViewById(R.id.title);
		ImageView iconView = view.findViewById(R.id.icon);
		RadioButton radioButton = view.findViewById(R.id.compound_button);
		boolean isItemEnabled = isModeEnabled(mode);

		title.setText(getString(mode.nameId));
		iconView.setImageDrawable(uiUtilities.getIcon(mode.iconId, isItemEnabled ? ColorUtilities.getActiveIconColorId(nightMode) : ColorUtilities.getDefaultIconColorId(nightMode)));
		radioButton.setChecked(isItemEnabled);

		View button = view.findViewById(R.id.basic_item_body);
		button.setOnClickListener(v -> {
			Fragment fragment = getTargetFragment();
			if (fragment instanceof OnConfirmPreferenceChange) {
				((OnConfirmPreferenceChange) fragment).onConfirmPreferenceChange(getPrefId(), mode == DistanceDuringNavigationMode.PRECISE, ApplyQueryType.SNACK_BAR);
			}
			dismiss();
		});

		AndroidUiHelper.updateVisibility(view.findViewById(R.id.divider_bottom), hasDivider);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.description), false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.end_button), false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.vertical_end_button_divider), false);

		return view;
	}

	private boolean isModeEnabled(DistanceDuringNavigationMode mode) {
		if (mode == DistanceDuringNavigationMode.PRECISE) {
			return preference.getModeValue(getAppMode());
		} else {
			return !preference.getModeValue(getAppMode());
		}
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	public static void showInstance(@NonNull FragmentManager manager, String prefKey, Fragment target,
									@Nullable ApplicationMode appMode, boolean usedOnMap) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			Bundle args = new Bundle();
			args.putString(PREFERENCE_ID, prefKey);
			DistanceDuringNavigationBottomSheet fragment = new DistanceDuringNavigationBottomSheet();
			fragment.setArguments(args);
			fragment.setAppMode(appMode);
			fragment.setUsedOnMap(usedOnMap);
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}

	public enum DistanceDuringNavigationMode {
		PRECISE(R.string.precise, R.drawable.ic_action_distance_number_precise),
		ROUND_UP(R.string.round_up, R.drawable.ic_action_distance_number_rounded);

		@StringRes
		public final int nameId;
		@DrawableRes
		public final int iconId;

		DistanceDuringNavigationMode(int nameId, int iconId) {
			this.nameId = nameId;
			this.iconId = iconId;
		}
	}
}