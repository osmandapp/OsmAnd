package net.osmand.plus.base.dialog.interfaces.dialog;

import androidx.annotation.NonNull;

/**
 * Initiates a request to completely refresh the dialog UI from outside.
 */
public interface IAskRefreshDialogCompletely {
	void onAskRefreshDialogCompletely(@NonNull String processId);
}
