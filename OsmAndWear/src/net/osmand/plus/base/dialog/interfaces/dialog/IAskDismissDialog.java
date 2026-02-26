package net.osmand.plus.base.dialog.interfaces.dialog;

import androidx.annotation.NonNull;

/**
 * Initiates a request to close the dialog from outside.
 */
public interface IAskDismissDialog extends IDialog {
	void onAskDismissDialog(@NonNull String processId);
}
