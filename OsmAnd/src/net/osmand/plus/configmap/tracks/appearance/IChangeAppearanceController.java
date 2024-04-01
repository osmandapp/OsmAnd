package net.osmand.plus.configmap.tracks.appearance;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;

public interface IChangeAppearanceController extends IDialogController {

	boolean hasAnyChangesToCommit();

	void saveChanges(@NonNull FragmentActivity activity);

	int getEditedItemsCount();

	boolean isAppearanceSaved();
}
