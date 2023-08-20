package net.osmand.plus.backup.trash.controller;

import static net.osmand.plus.base.dialog.data.DialogExtra.BACKGROUND_COLOR;
import static net.osmand.plus.utils.ColorUtilities.getActiveColor;
import static net.osmand.plus.utils.ColorUtilities.getColor;
import static net.osmand.plus.utils.ColorUtilities.getColorWithAlpha;
import static net.osmand.plus.utils.UiUtilities.createSpannableString;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.backup.trash.TrashUtils;
import net.osmand.plus.backup.trash.data.TrashItem;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.data.DisplayData;
import net.osmand.plus.base.dialog.data.DisplayItem;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogItemClicked;
import net.osmand.plus.base.dialog.interfaces.controller.IDisplayDataProvider;
import net.osmand.plus.settings.bottomsheets.CustomizableOptionsBottomSheet;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;

public class TrashItemMenuController extends BaseDialogController implements IDisplayDataProvider, IDialogItemClicked {

	public static final String PROCESS_ID = "trash_item_options_menu";

	private static final int ACTION_RESTORE_ID = 0;
	private static final int ACTION_DELETE_ID = 1;

	private final TrashUtils trashUtils;
	private final TrashItem trashItem;

	public TrashItemMenuController(@NonNull TrashUtils trashUtils, @NonNull TrashItem trashItem) {
		super(trashUtils.getApp());
		this.trashUtils = trashUtils;
		this.trashItem = trashItem;
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
		UiUtilities iconsCache = app.getUIUtilities();
		DisplayData displayData = new DisplayData();

		int activeColor = getActiveColor(app, nightMode);
		int backgroundColor = getColorWithAlpha(activeColor, 0.3f);
		displayData.putExtra(BACKGROUND_COLOR, backgroundColor);

		// Header
		displayData.addDisplayItem(new DisplayItem()
				.setTitle(trashItem.getName())
				.setDescription(trashItem.getDescription())
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_72dp)
				.setIcon(iconsCache.getThemedIcon(trashItem.getIconId()))
				.setShowBottomDivider(true, 0)
		);

		// Restore
		displayData.addDisplayItem(new DisplayItem()
				.setTitle(getString(R.string.restore_from_trash))
				.setLayoutId(R.layout.bottom_sheet_item_simple_56dp_padding_32dp)
				.setIcon(iconsCache.getPaintedIcon(R.drawable.ic_action_history, activeColor))
				.setTag(ACTION_RESTORE_ID)
		);

		// Delete
		int warningColor = getColor(app, R.color.deletion_color_warning);
		displayData.addDisplayItem(new DisplayItem()
				.setTitle(getString(R.string.shared_string_delete_immediately))
				.setLayoutId(R.layout.bottom_sheet_item_simple_56dp_padding_32dp)
				.setIcon(iconsCache.getPaintedIcon(R.drawable.ic_action_delete_dark, warningColor))
				.setTag(ACTION_DELETE_ID)
		);
		return displayData;
	}

	@Override
	public void onDialogItemClicked(@NonNull String processId, @NonNull DisplayItem item) {
		if (!(item.getTag() instanceof Integer)) {
			return;
		}
		int actionId = (int) item.getTag();
		if (actionId == ACTION_RESTORE_ID) {
			closeDialog();
			trashUtils.restoreFromTrash(trashItem);
		} else if (actionId == ACTION_DELETE_ID) {
			showDeleteConfirmationDialog();
		}
	}

	private void showDeleteConfirmationDialog() {
		Context ctx = getContext();
		if (ctx != null) {
			AlertDialogData dialogData = new AlertDialogData(ctx, isNightMode())
					.setTitle(getString(R.string.shared_string_delete_item))
					.setNegativeButton(R.string.shared_string_cancel, null)
					.setPositiveButton(R.string.shared_string_delete, ((dialog, which) -> {
						closeDialog();
						trashUtils.deleteImmediately(trashItem);
					}));
			String itemName = trashItem.getName();
			String message = getString(R.string.delete_trash_item_confirmation_desc, itemName);
			SpannableString spannableMessage = createSpannableString(message, Typeface.BOLD, itemName);
			CustomAlert.showSimpleMessage(dialogData, spannableMessage);
		}
	}

	private void closeDialog() {
		dialogManager.askDismissDialog(PROCESS_ID);
	}

	public static void showDialog(@NonNull FragmentActivity activity,
	                              @NonNull TrashUtils trashUtils, @NonNull TrashItem trashItem) {
		TrashItemMenuController controller = new TrashItemMenuController(trashUtils, trashItem);

		DialogManager dialogManager = trashUtils.getApp().getDialogManager();
		dialogManager.register(PROCESS_ID, controller);

		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		CustomizableOptionsBottomSheet.showInstance(fragmentManager, PROCESS_ID, false);
	}
}
