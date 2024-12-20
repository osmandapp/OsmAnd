package net.osmand.plus.base.dialog.interfaces.controller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.base.dialog.data.DisplayData;

/**
 * Provides display data to show on a dialog.
 * You have to create display data and return it in getDialogDisplayData method implementation.
 */
public interface IDisplayDataProvider extends IDialogController {
	@Nullable
	DisplayData getDisplayData(@NonNull String processId);
}
