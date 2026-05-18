package net.osmand.plus.backup.ui.trash;

import static android.graphics.Typeface.BOLD;
import static net.osmand.plus.base.dialog.data.DialogExtra.BACKGROUND_COLOR;
import static net.osmand.plus.utils.UiUtilities.createSpannableString;

import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.data.DisplayData;
import net.osmand.plus.base.dialog.data.DisplayItem;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogItemClicked;
import net.osmand.plus.base.dialog.interfaces.controller.IDisplayDataProvider;
import net.osmand.plus.settings.bottomsheets.CustomizableOptionsBottomSheet;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;

public class CloudTrashItemMenuController extends BaseDialogController implements IDisplayDataProvider, IDialogItemClicked {

	public static final String PROCESS_ID = "trash_item_options_menu";

	private static final int ACTION_DELETE_ID = 0;
	private static final int ACTION_RESTORE_ID = 1;
	private static final int ACTION_DOWNLOAD_ID = 2;

	private final TrashItem item;
	private final CloudTrashController controller;

	public CloudTrashItemMenuController(@NonNull OsmandApplication app, @NonNull CloudTrashController controller, @NonNull TrashItem item) {
		super(app);
		this.item = item;
		this.controller = controller;
	}

	@NonNull
	@Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	@Nullable
	@Override
	public DisplayData getDisplayData(@NonNull String processId) {
		boolean nightMode = isNightMode();
		int activeColorId = ColorUtilities.getActiveColorId(nightMode);

		DisplayData displayData = new DisplayData();
		displayData.putExtra(BACKGROUND_COLOR, ColorUtilities.getColorWithAlpha(ColorUtilities.getColor(app, activeColorId), 0.3f));

		int iconId = item.getIconId();
		displayData.addDisplayItem(new DisplayItem()
				.setClickable(false)
				.setTitle(item.getName(app))
				.setDescription(item.getDescription(app))
				.setIcon(iconId != -1 ? uiUtilities.getThemedIcon(iconId) : null)
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_72dp)
				.setShowBottomDivider(true, 0));

		if (!item.isLocalDeletion()) {
			displayData.addDisplayItem(new DisplayItem()
					.setTitle(getString(R.string.restore_from_trash))
					.setLayoutId(R.layout.bottom_sheet_item_simple_56dp_padding_32dp)
					.setIcon(uiUtilities.getIcon(R.drawable.ic_action_history, activeColorId))
					.setTag(ACTION_RESTORE_ID));
		}

		displayData.addDisplayItem(new DisplayItem()
				.setTitle(getString(R.string.download_to_device))
				.setLayoutId(R.layout.bottom_sheet_item_simple_56dp_padding_32dp)
				.setIcon(uiUtilities.getIcon(R.drawable.ic_action_device_download, activeColorId))
				.setShowBottomDivider(true, app.getResources().getDimensionPixelSize(R.dimen.bottom_sheet_divider_margin_start))
				.setTag(ACTION_DOWNLOAD_ID));

		displayData.addDisplayItem(new DisplayItem()
				.setTitle(getString(R.string.shared_string_delete_immediately))
				.setLayoutId(R.layout.bottom_sheet_item_simple_56dp_padding_32dp)
				.setIcon(uiUtilities.getIcon(R.drawable.ic_action_delete_dark, R.color.deletion_color_warning))
				.setTag(ACTION_DELETE_ID));

		return displayData;
	}

	@Override
	public void onDialogItemClicked(@NonNull String processId, @NonNull DisplayItem displayItem) {
		Object tag = displayItem.getTag();
		if (tag instanceof Integer) {
			int actionId = (int) tag;
			if (actionId == ACTION_RESTORE_ID) {
				controller.restoreItem(item);
				dismiss();
			} else if (actionId == ACTION_DOWNLOAD_ID) {
				controller.downloadItem(item);
				dismiss();
			} else if (actionId == ACTION_DELETE_ID) {
				showDeleteConfirmationDialog();
			}
		}
	}

	private void showDeleteConfirmationDialog() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			AlertDialogData dialogData = new AlertDialogData(activity, isNightMode())
					.setTitle(getString(R.string.shared_string_delete_item))
					.setNegativeButton(R.string.shared_string_cancel, null)
					.setPositiveButton(R.string.shared_string_delete, ((dialog, which) -> {
						controller.deleteItem(item);
						dismiss();
					}));
			int color = ColorUtilities.getSecondaryTextColor(activity, isNightMode());
			String name = item.getName(app);
			String description = getString(R.string.delete_trash_item_confirmation_desc, name);
			SpannableString spannable = createSpannableString(description, BOLD, name);
			UiUtilities.setSpan(spannable, new ForegroundColorSpan(color), description, description);
			CustomAlert.showSimpleMessage(dialogData, spannable);
		}
	}

	private void dismiss() {
		dialogManager.askDismissDialog(PROCESS_ID);
	}

	public static void showDialog(@NonNull FragmentActivity activity, @NonNull CloudTrashController controller, @NonNull TrashItem item) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();

		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(PROCESS_ID, new CloudTrashItemMenuController(app, controller, item));

		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		CustomizableOptionsBottomSheet.showInstance(fragmentManager, PROCESS_ID, false);
	}
}
