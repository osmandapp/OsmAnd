package net.osmand.plus.backup.ui;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.utils.AndroidUtils;
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

	private static final String PROCESS_ID_KEY = "process_id";
	private static final String CLEAR_TYPE_KEY = "clear_type_key";
	private static final String DISABLED_TYPES_KEY = "disabled_types_key";

	private final List<ExportType> types = new ArrayList<>();
	private BaseBackupTypesController controller;
	private BackupClearType clearType;
	private String processId;

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
			processId = savedInstanceState.getString(PROCESS_ID_KEY);
		}
		DialogManager dialogManager = app.getDialogManager();
		controller = (BaseBackupTypesController) dialogManager.findController(processId);
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new SimpleBottomSheetItem.Builder()
				.setTitle(getString(clearType.titleId))
				.setLayoutId(R.layout.bottom_sheet_item_title)
				.create());

		String baseDescription = getString(clearType.descriptionId);
		String extendedDescription = getString(R.string.backup_delete_data_warning_extended);
		items.add(new LongDescriptionItem.Builder()
				.setDescription(baseDescription + "\n\n" + extendedDescription)
				.setLayoutId(R.layout.bottom_sheet_item_description_long)
				.create());

		items.add(new DividerSpaceItem(getContext(), getDimensionPixelSize(R.dimen.content_padding_small)));
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
		outState.putString(PROCESS_ID_KEY, processId);
	}

	@Override
	protected void onRightBottomButtonClick() {
		controller.onClearTypesConfirmed(types);
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

	public static void showInstance(@NonNull FragmentActivity activity,
	                                @NonNull BaseBackupTypesController controller,
	                                @NonNull List<ExportType> types) {
		FragmentManager manager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			ClearTypesBottomSheet fragment = new ClearTypesBottomSheet();
			fragment.types.addAll(types);
			fragment.clearType = controller.clearType;
			fragment.processId = controller.getProcessId();
			fragment.show(manager, TAG);
		}
	}

	public interface OnClearTypesListener {
		void onClearTypesConfirmed(@NonNull List<ExportType> types);
	}
}