package net.osmand.plus.settings.bottomsheets.displaydata;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface DialogDisplayDataProvider {
	@Nullable
	DialogDisplayData provideDialogDisplayData(@NonNull String dialogId);
}
