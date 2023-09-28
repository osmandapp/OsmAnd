package net.osmand.plus.base.dialog.interfaces.controller;

import androidx.annotation.NonNull;

import net.osmand.plus.base.dialog.data.DisplayItem;

/**
 * Allows the controller to process the result of the user's selection,
 * on dialogs that require single selection of the proposed options.
 */
public interface IDialogItemSelected extends IDialogController {
	void onDialogItemSelected(@NonNull String processId, @NonNull DisplayItem selected);
}
