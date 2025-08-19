package net.osmand.plus.settings.bottomsheets;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.fragments.ApplyQueryType;
import net.osmand.plus.settings.fragments.OnConfirmPreferenceChange;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class HazmatCategoryBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = HazmatCategoryBottomSheet.class.getSimpleName();

	private static final String NAMES_KEY = "names_array_key";
	private static final String VALUES_KEY = "values_array_key";
	private static final String CATEGORY_SELECTED_KEY = "is_any_category_selected_key";
	private static final String SELECTED_ENTRY_INDEX_KEY = "selected_entry_index_key";

	private final List<View> categoriesViews = new ArrayList<>();

	private String[] names;
	private Object[] values;
	private boolean isAnyCategorySelected;
	private int selectedEntryIndex;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			restoreSavedState(savedInstanceState);
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		View view = inflate(R.layout.bottom_sheet_hazmat_category);
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
		radioGroup.setSelectedItem(isAnyCategorySelected ? btnYes : btnNo);
	}

	private TextRadioItem createToggleButton(@NonNull TextToggleButton group, boolean enabled) {
		String title = getString(enabled ? R.string.shared_string_yes : R.string.shared_string_no);
		TextRadioItem item = new TextRadioItem(title);
		item.setOnClickListener((radioItem, view) -> {
			if (isAnyCategorySelected != enabled) {
				isAnyCategorySelected = enabled;
				selectedEntryIndex = 0;
				group.setSelectedItem(item);
				updateView(requireView());
				return true;
			}
			return false;
		});
		return item;
	}

	private void updateView(@NonNull View view) {
		TextView tvDesc = view.findViewById(R.id.description);
		String description = getString(isAnyCategorySelected ? R.string.transport_hazmat_yes_desc : R.string.transport_hazmat_no_desc);
		tvDesc.setText(description);

		View divider = view.findViewById(R.id.divider);
		View categoryHeader = view.findViewById(R.id.category_header);
		LinearLayout llItems = view.findViewById(R.id.items);

		AndroidUiHelper.setVisibility(isAnyCategorySelected ? View.VISIBLE : View.GONE, divider, categoryHeader, llItems);
		setupHazmatCategories(view);
	}

	private void setupHazmatCategories(@NonNull View view) {
		LinearLayout llItems = view.findViewById(R.id.items);
		if (!Algorithms.isEmpty(categoriesViews)) {
			updateRadioButtons();
			return;
		}

		for (int i = 0; i < names.length; i++) {
			View v = inflate(R.layout.bottom_sheet_item_with_radio_btn_left, llItems, false);
			v.setTag(i);

			TextView tvTitle = v.findViewById(R.id.title);
			tvTitle.setText(names[i]);

			int color = appMode.getProfileColor(nightMode);
			CompoundButton cb = v.findViewById(R.id.compound_button);
			UiUtilities.setupCompoundButton(nightMode, color, cb);
			cb.setChecked(i == selectedEntryIndex);

			Drawable background = UiUtilities.getColoredSelectableDrawable(app, color, 0.3f);
			AndroidUtils.setBackground(v, background);

			v.setOnClickListener(_v -> {
				selectedEntryIndex = (int) _v.getTag();
				updateRadioButtons();
				dismiss();
			});

			llItems.addView(v);
			categoriesViews.add(v);
		}
	}

	private void updateRadioButtons() {
		for (int i = 0; i < categoriesViews.size(); i++) {
			View view = categoriesViews.get(i);
			CompoundButton cb = view.findViewById(R.id.compound_button);
			cb.setChecked(i == selectedEntryIndex);
		}
	}

	public void setData(@NonNull String[] names, @NonNull Object[] values, @Nullable Integer selectedEntryIndex) {
		this.names = names;
		this.values = values;
		this.selectedEntryIndex = selectedEntryIndex != null ? selectedEntryIndex : 0;
		this.isAnyCategorySelected = selectedEntryIndex != null;
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	private void restoreSavedState(@NonNull Bundle bundle) {
		names = bundle.getStringArray(NAMES_KEY);
		values = bundle.getStringArray(VALUES_KEY);
		isAnyCategorySelected = bundle.getBoolean(CATEGORY_SELECTED_KEY);
		selectedEntryIndex = bundle.getInt(SELECTED_ENTRY_INDEX_KEY);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putStringArray(NAMES_KEY, names);
		outState.putStringArray(VALUES_KEY, (String[]) values);
		outState.putBoolean(CATEGORY_SELECTED_KEY, isAnyCategorySelected);
		outState.putInt(SELECTED_ENTRY_INDEX_KEY, selectedEntryIndex);
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		super.onDismiss(dialog);
		Activity activity = getActivity();
		if (activity != null && !activity.isChangingConfigurations()) {
			onApply();
		}
	}

	private void onApply() {
		if (getTargetFragment() instanceof OnConfirmPreferenceChange callback) {
			Object newValue = isAnyCategorySelected ? values[selectedEntryIndex] : false;
			callback.onConfirmPreferenceChange(getPrefId(), newValue, ApplyQueryType.SNACK_BAR);
		}
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager,
	                                @NonNull Fragment target, @NonNull String key,
	                                @NonNull ApplicationMode appMode, boolean usedOnMap,
	                                @NonNull String[] names, @NonNull Object[] values,
	                                @Nullable Integer selectedEntryIndex) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			Bundle args = new Bundle();
			args.putString(PREFERENCE_ID, key);
			HazmatCategoryBottomSheet fragment = new HazmatCategoryBottomSheet();
			fragment.setArguments(args);
			fragment.setAppMode(appMode);
			fragment.setUsedOnMap(usedOnMap);
			fragment.setTargetFragment(target, 0);
			fragment.setData(names, values, selectedEntryIndex);
			fragment.show(fragmentManager, TAG);
		}
	}
}
