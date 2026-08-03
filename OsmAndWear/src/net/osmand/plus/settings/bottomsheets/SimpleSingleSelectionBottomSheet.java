package net.osmand.plus.settings.bottomsheets;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.fragments.ApplyQueryType;
import net.osmand.plus.settings.fragments.OnConfirmPreferenceChange;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class SimpleSingleSelectionBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = SimpleSingleSelectionBottomSheet.class.getSimpleName();

	private static final String TITLE_KEY = "title_key";
	private static final String DESCRIPTION_KEY = "description_key";
	private static final String NAMES_KEY = "names_array_key";
	private static final String VALUES_KEY = "values_array_key";
	private static final String SELECTED_ENTRY_INDEX_KEY = "selected_entry_index_key";

	private OsmandApplication app;
	private ApplicationMode appMode;

	private final List<View> views = new ArrayList<>();
	private LayoutInflater inflater;

	private String title;
	private String description;
	private String[] names;
	private Object[] values;
	private int selectedEntryIndex;

	public static void showInstance(@NonNull FragmentManager fm, @NonNull Fragment target,
	                                @NonNull String key, @NonNull String title, @NonNull String description,
	                                @NonNull ApplicationMode appMode, boolean usedOnMap,
	                                @NonNull String[] names, @NonNull Object[] values,
	                                int selectedIndex) {
		if (AndroidUtils.isFragmentCanBeAdded(fm, TAG)) {
			Bundle args = new Bundle();
			args.putString(PREFERENCE_ID, key);
			SimpleSingleSelectionBottomSheet fragment = new SimpleSingleSelectionBottomSheet();
			fragment.setArguments(args);
			fragment.setAppMode(appMode);
			fragment.setUsedOnMap(usedOnMap);
			fragment.setTargetFragment(target, 0);
			fragment.setParameters(title, description, names, values, selectedIndex);
			fragment.show(fm, TAG);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requiredMyApplication();
		appMode = getAppMode();
		if (savedInstanceState != null) {
			restoreSavedState(savedInstanceState);
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context context = requireContext();
		inflater = UiUtilities.getInflater(context, nightMode);

		View view = inflater.inflate(R.layout.bottom_sheet_simple_single_selection, null);
		TextView tvTitle = view.findViewById(R.id.title);
		tvTitle.setText(title);
		TextView tvDesc = view.findViewById(R.id.description);
		tvDesc.setText(description);
		setupListItems(view);

		items.add(new BaseBottomSheetItem.Builder().setCustomView(view).create());
	}

	private void setupListItems(@NonNull View view) {
		LinearLayout llItems = view.findViewById(R.id.items);

		for (int i = 0; i < names.length; i++) {
			View v = inflater.inflate(R.layout.bottom_sheet_item_with_radio_btn_left, llItems, false);
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
				onApply();
			});

			llItems.addView(v);
			views.add(v);
		}
	}

	private void updateRadioButtons() {
		for (int i = 0; i < views.size(); i++) {
			View view = views.get(i);
			CompoundButton cb = view.findViewById(R.id.compound_button);
			cb.setChecked(i == selectedEntryIndex);
		}
	}

	public void setParameters(@NonNull String title, @NonNull String description, @NonNull String[] names,
	                          @NonNull Object[] values, @Nullable Integer selectedEntryIndex) {
		this.title = title;
		this.description = description;
		this.names = names;
		this.values = values;
		this.selectedEntryIndex = selectedEntryIndex != null ? selectedEntryIndex : 0;
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	private void restoreSavedState(@NonNull Bundle bundle) {
		title = bundle.getString(TITLE_KEY);
		description = bundle.getString(DESCRIPTION_KEY);
		names = bundle.getStringArray(NAMES_KEY);
		values = bundle.getStringArray(VALUES_KEY);
		selectedEntryIndex = bundle.getInt(SELECTED_ENTRY_INDEX_KEY);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(TITLE_KEY, title);
		outState.putString(DESCRIPTION_KEY, description);
		outState.putStringArray(NAMES_KEY, names);
		outState.putStringArray(VALUES_KEY, (String[]) values);
		outState.putInt(SELECTED_ENTRY_INDEX_KEY, selectedEntryIndex);
	}

	private void onApply() {
		Fragment target = getTargetFragment();
		if (target instanceof OnConfirmPreferenceChange) {
			OnConfirmPreferenceChange callback = ((OnConfirmPreferenceChange) target);
			callback.onConfirmPreferenceChange(getPrefId(), values[selectedEntryIndex], ApplyQueryType.SNACK_BAR);
		}
		dismiss();
	}
}
