package net.osmand.plus.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.LocationSource;
import net.osmand.plus.settings.fragments.OnPreferenceChanged;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

public class LocationSourceBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = LocationSourceBottomSheet.class.getSimpleName();

	private OsmandApplication app;
	private OsmandSettings settings;
	private LocationSource selectedSource;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		app = requiredMyApplication();
		settings = app.getSettings();
		selectedSource = settings.LOCATION_SOURCE.get();
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		createHeaderItem();
		items.add(new DividerSpaceItem(app, AndroidUtils.dpToPx(app, 12)));
		createSourceItems();
	}

	private void createHeaderItem() {
		String androidApi = LocationSource.ANDROID_API.toHumanString(app);
		String playServices = LocationSource.GOOGLE_PLAY_SERVICES.toHumanString(app);

		BaseBottomSheetItem titleItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.location_source_descr, playServices, playServices, androidApi))
				.setDescriptionMaxLines(Integer.MAX_VALUE)
				.setTitle(getString(R.string.location_source))
				.setLayoutId(R.layout.bottom_sheet_item_title_with_description)
				.create();
		items.add(titleItem);
	}

	private void createSourceItems() {
		int margin = getDimen(R.dimen.content_padding);
		int activeColorId = ColorUtilities.getActiveIconColorId(nightMode);
		int secondaryColorId = ColorUtilities.getSecondaryIconColorId(nightMode);
		LayoutInflater inflater = UiUtilities.getInflater(app, nightMode);

		for (LocationSource source : LocationSource.values()) {
			View view = inflater.inflate(R.layout.bottom_sheet_item_with_radio_btn_left, null);
			View compoundButton = view.findViewById(R.id.compound_button);

			MarginLayoutParams params = (MarginLayoutParams) compoundButton.getLayoutParams();
			AndroidUtils.setMargins(params, 0, 0, margin, 0);

			BottomSheetItemWithCompoundButton[] item = new BottomSheetItemWithCompoundButton[1];
			item[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
					.setChecked(selectedSource == source)
					.setButtonTintList(AndroidUtils.createCheckedColorStateList(app, secondaryColorId, activeColorId))
					.setDescription(getString(R.string.location_source_descr))
					.setTitle(source.toHumanString(app))
					.setCustomView(view)
					.setTag(source)
					.setOnClickListener(v -> sourceSelected(source))
					.create();
			items.add(item[0]);
		}
	}

	private void sourceSelected(@NonNull LocationSource source) {
		if (selectedSource != source) {
			selectedSource = source;
			settings.LOCATION_SOURCE.set(source);
			updateSourceItems();
		}
		Fragment fragment = getTargetFragment();
		if (fragment instanceof OnPreferenceChanged) {
			((OnPreferenceChanged) fragment).onPreferenceChanged(settings.LOCATION_SOURCE.getId());
		}
		dismiss();
	}

	private void updateSourceItems() {
		for (BaseBottomSheetItem item : items) {
			if (item instanceof BottomSheetItemWithCompoundButton) {
				boolean checked = Algorithms.objectEquals(item.getTag(), selectedSource);
				((BottomSheetItemWithCompoundButton) item).setChecked(checked);
			}
		}
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	public static void showInstance(@NonNull FragmentManager manager, @Nullable Fragment target) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			LocationSourceBottomSheet fragment = new LocationSourceBottomSheet();
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}
}