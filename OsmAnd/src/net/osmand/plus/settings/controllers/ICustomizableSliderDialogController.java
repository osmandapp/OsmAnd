package net.osmand.plus.settings.controllers;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.base.dialog.interfaces.controller.IDisplayDataProvider;
import net.osmand.plus.card.base.headed.IHeadedCardController;
import net.osmand.plus.card.base.slider.ISliderCardController;

public interface ICustomizableSliderDialogController
		extends ISliderCardController, IDisplayDataProvider, IHeadedCardController {
	void onApplyChanges();
	void onDestroy(@Nullable FragmentActivity activity);
}
