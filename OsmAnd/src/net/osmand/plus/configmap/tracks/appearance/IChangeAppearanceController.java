package net.osmand.plus.configmap.tracks.appearance;

import androidx.annotation.NonNull;

import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;

public interface IChangeAppearanceController extends IDialogController {

	boolean hasAnyChangesToCommit();

	void onApplyButtonClicked();

	int getEditedItemsCount();

	@NonNull
	String getProcessId();
}
