package net.osmand.plus.base.dialog.interfaces.controller;

import androidx.annotation.NonNull;

public interface IDialogProgressChanged extends IDialogController {
	void onDialogProgressChanged(@NonNull String progressTag, int progress);
}
