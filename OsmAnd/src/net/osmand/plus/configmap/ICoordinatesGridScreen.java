package net.osmand.plus.configmap;

import net.osmand.plus.base.dialog.interfaces.dialog.IDialogNightModeInfoProvider;

public interface ICoordinatesGridScreen extends IDialogNightModeInfoProvider {
	void updateFormatButton();
	void updateZoomLevelsButton();
	void updateLabelsPositionButton();
	void updateGridColorPreview();
}
