package net.osmand.plus.base.dialog.interfaces.controller;

import androidx.annotation.NonNull;

import net.osmand.plus.base.dialog.data.DisplayItem;

/**
 * Notifies controller when dialog item is clicked.
 */
public interface IDialogItemClicked extends IDialogController {
	void onDialogItemClicked(@NonNull String processId, @NonNull DisplayItem item);
}
