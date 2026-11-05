package net.osmand.plus.backup.ui;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;

import java.util.ArrayList;
import java.util.List;

public class ClearTypesBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = ClearTypesBottomSheet.class.getSimpleName();

	private static final String CLEAR_TYPE_KEY = "clear_type_key";
	private static final String DISABLED_TYPES_KEY = "disabled_types_key";

	private final List<ExportType> types = new ArrayList<>();
	private BackupClearType clearType;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(DISABLED_TYPES_KEY)) {
				List<String> names = savedInstanceState.getStringArrayList(DISABLED_TYPES_KEY);
				if (names != null) {
					types.addAll(ExportType.valuesOf(names));
				}
			}
			if (savedInstanceState.containsKey(CLEAR_TYPE_KEY)) {
				clearType = BackupClearType.valueOf(savedInstanceState.getString(CLEAR_TYPE_KEY));
			}
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new SimpleBottomSheetItem.Builder()
				.setTitle(getString(clearType.titleId))
				.setLayoutId(R.layout.bottom_sheet_item_title)
				.create());

		items.add(new LongDescriptionItem.Builder()
				.setDescription(getString(clearType.descriptionId))
				.setLayoutId(R.layout.bottom_sheet_item_description_long)
				.create());

		items.add(new DividerSpaceItem(getContext(), getResources().getDimensionPixelSize(R.dimen.content_padding_small)));
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		ArrayList<String> names = new ArrayList<>();
		for (ExportType exportType : types) {
			names.add(exportType.name());
		}
		outState.putStringArrayList(DISABLED_TYPES_KEY, names);
		outState.putString(CLEAR_TYPE_KEY, clearType.name());
	}

	@Override
	protected void onRightBottomButtonClick() {
		Fragment fragment = getTargetFragment();
		if (fragment instanceof OnClearTypesListener) {
			((OnClearTypesListener) fragment).onClearTypesConfirmed(types);
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

	public static void showInstance(@NonNull FragmentManager manager, @NonNull List<ExportType> types,
									@NonNull BackupClearType clearType, @NonNull Fragment target) {
		if (!manager.isStateSaved()) {
			ClearTypesBottomSheet fragment = new ClearTypesBottomSheet();
			fragment.types.addAll(types);
			fragment.clearType = clearType;
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}

	public enum BackupClearType {
		ALL(R.string.backup_delete_types, R.string.backup_delete_types_descr),
		HISTORY(R.string.delete_version_history, R.string.backup_version_history_delete_descr);

		private final int titleId;
		private final int descriptionId;

		BackupClearType(int titleId, int descriptionId) {
			this.titleId = titleId;
			this.descriptionId = descriptionId;
		}

		public int getTitleId() {
			return titleId;
		}

		public int getDescriptionId() {
			return descriptionId;
		}
	}

	public interface OnClearTypesListener {
		void onClearTypesConfirmed(@NonNull List<ExportType> types);
	}
}