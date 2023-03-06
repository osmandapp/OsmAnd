package net.osmand.plus.base.dialog.interfaces;

import androidx.annotation.NonNull;

import net.osmand.plus.base.dialog.uidata.DialogDisplayItem;

/**
 * Allows the controller to process the result of the user's selection,
 * on dialogs that require single selection of the proposed options.
 */
public interface IDialogItemSelected extends IDialogController {
	void onDialogItemSelected(@NonNull String processId, @NonNull DialogDisplayItem selected);
}
