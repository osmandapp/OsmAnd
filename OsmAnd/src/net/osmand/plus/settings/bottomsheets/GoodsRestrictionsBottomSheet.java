package net.osmand.plus.settings.bottomsheets;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.fragments.ApplyQueryType;
import net.osmand.plus.settings.fragments.OnConfirmPreferenceChange;
import net.osmand.plus.settings.fragments.search.SearchablePreferenceDialog;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GoodsRestrictionsBottomSheet extends BasePreferenceBottomSheet implements SearchablePreferenceDialog {

	public static final String TAG = GoodsRestrictionsBottomSheet.class.getSimpleName();

	private static final String SELECTED_KEY = "selected_key";
	private static final String HAS_CHANGES_TO_APPLY_KEY = "has_changes_to_apply_key";

	private OsmandApplication app;

	private boolean isSelected;
	private boolean hasChangesToApply = false;

	public static GoodsRestrictionsBottomSheet createInstance(final @NonNull Fragment target,
															  final @NonNull Preference preference,
															  final @NonNull ApplicationMode appMode,
															  final boolean usedOnMap,
															  final boolean selected) {
		final Bundle args = new Bundle();
		args.putString(PREFERENCE_ID, preference.getKey());
		args.putBoolean(SELECTED_KEY, selected);
		GoodsRestrictionsBottomSheet fragment = new GoodsRestrictionsBottomSheet();
		fragment.setArguments(args);
		fragment.setPreference(preference);
		fragment.setAppMode(appMode);
		fragment.setUsedOnMap(usedOnMap);
		fragment.setTargetFragment(target, 0);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requiredMyApplication();
		if (savedInstanceState != null) {
			restoreSavedState(savedInstanceState);
		} else if (getArguments() != null) {
			restoreSavedState(getArguments());
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context context = requireContext();
		LayoutInflater inflater = UiUtilities.getInflater(context, nightMode);

		View view = inflater.inflate(R.layout.bottom_sheet_goods_restriction, null);
		setupToggleButtons(view);
		updateView(view);

		items.add(new BaseBottomSheetItem.Builder().setCustomView(view).create());
	}

	private void setupToggleButtons(@NonNull View view) {
		LinearLayout container = view.findViewById(R.id.custom_radio_buttons);
		TextToggleButton radioGroup = new TextToggleButton(app, container, nightMode);
		TextRadioItem btnNo = createToggleButton(radioGroup, false);
		TextRadioItem btnYes = createToggleButton(radioGroup, true);
		radioGroup.setItems(btnNo, btnYes);
		radioGroup.setSelectedItem(isSelected ? btnYes : btnNo);
	}

	private TextRadioItem createToggleButton(@NonNull TextToggleButton group, boolean enabled) {
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
		Fragment target = getTargetFragment();
		if (target instanceof OnConfirmPreferenceChange) {
			OnConfirmPreferenceChange callback = ((OnConfirmPreferenceChange) target);
			callback.onConfirmPreferenceChange(getPrefId(), isSelected, ApplyQueryType.SNACK_BAR);
		}
	}

	@Override
	public void show(final FragmentManager fragmentManager, final OsmandApplication app) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			show(fragmentManager, TAG);
		}
	}

	@Override
	public String getSearchableInfo() {
		return Stream
				.of(
						R.string.routing_attr_goods_restrictions_name,
						R.string.goods_delivery_desc,
						R.string.goods_delivery_desc_2,
						R.string.goods_delivery_desc_3,
						R.string.goods_delivery_desc_4)
				.map(this::getString)
				.collect(Collectors.joining(", "));
	}
}
