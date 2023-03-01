package net.osmand.plus.base.dialog.interfaces;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.base.dialog.uidata.DialogDisplayData;

/**
 * Provides display data to show on a dialog.
 * You have to create display data and return it in getDialogDisplayData method implementation.
 */
public interface IDialogDisplayDataProvider extends IDialogController {
	@Nullable
	DialogDisplayData getDialogDisplayData(@NonNull String processId);
}
