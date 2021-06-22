package net.osmand.plus.backup.ui;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.settings.backend.ExportSettingsType;

import java.util.ArrayList;
import java.util.List;

public class VersionHistoryClearBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = VersionHistoryClearBottomSheet.class.getSimpleName();
	private static final String DISABLED_TYPES_KEY = "disabled_types_key";

	private final List<ExportSettingsType> types = new ArrayList<>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null && savedInstanceState.containsKey(DISABLED_TYPES_KEY)) {
			List<String> names = savedInstanceState.getStringArrayList(DISABLED_TYPES_KEY);
			for (String name : names) {
				types.add(ExportSettingsType.valueOf(name));
			}
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new SimpleBottomSheetItem.Builder()
				.setTitle(getString(R.string.delete_version_history))
				.setLayoutId(R.layout.bottom_sheet_item_title)
				.create());

		items.add(new LongDescriptionItem.Builder()
				.setDescription(getString(R.string.backup_version_history_delete_descr))
				.setLayoutId(R.layout.bottom_sheet_item_description_long)
				.create());

		items.add(new DividerSpaceItem(getContext(), getResources().getDimensionPixelSize(R.dimen.content_padding_small)));
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		ArrayList<String> names = new ArrayList<>();
		for (ExportSettingsType type : types) {
			names.add(type.name());
		}
		outState.putStringArrayList(DISABLED_TYPES_KEY, names);
	}

	@Override
	protected void onRightBottomButtonClick() {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof VersionHistoryFragment) {
			((VersionHistoryFragment) fragment).deleteHistoryForTypes(types);
		}
		dismiss();
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_delete;
	}

	@Override
	protected DialogButtonType getRightBottomButtonType() {
		return DialogButtonType.SECONDARY_HARMFUL;
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull List<ExportSettingsType> types, Fragment target) {
		if (!manager.isStateSaved()) {
			VersionHistoryClearBottomSheet fragment = new VersionHistoryClearBottomSheet();
			fragment.types.addAll(types);
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}
}