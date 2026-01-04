package net.osmand.plus.settings.controllers;

import net.osmand.plus.base.dialog.interfaces.controller.IDisplayDataProvider;
import net.osmand.plus.card.base.headed.IHeadedCardController;
import net.osmand.plus.card.base.slider.ISliderCardController;

public interface ICustomizableSliderDialogController
		extends ISliderCardController, IDisplayDataProvider, IHeadedCardController {
	void onDiscardChanges();
	void onApplyChanges();
}
