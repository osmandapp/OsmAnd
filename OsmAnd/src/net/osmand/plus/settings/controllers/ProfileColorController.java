package net.osmand.plus.settings.controllers;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.plus.card.color.palette.main.ColorsPaletteController;
import net.osmand.plus.card.color.palette.main.OnColorsPaletteListener;
import net.osmand.plus.card.color.palette.main.data.ColorsCollection;
import net.osmand.plus.card.color.palette.main.data.FileColorsCollection;

public class ProfileColorController extends ColorsPaletteController implements IDialogController {

	private static final String PROCESS_ID = "select_profile_color";

	private ProfileColorController(@NonNull OsmandApplication app,
	                               @NonNull ColorsCollection colorsCollection,
	                               @ColorInt int selectedColor) {
		super(app, colorsCollection, selectedColor);
	}

	@Override
	public int getControlsAccentColor(boolean nightMode) {
		if (selectedPaletteColor != null) {
			return selectedPaletteColor.getColor();
		}
		return super.getControlsAccentColor(nightMode);
	}

	@Override
	public boolean isAccentColorCanBeChanged() {
		return true;
	}

	public void onDestroy(@Nullable FragmentActivity activity) {
		if (activity != null && !activity.isChangingConfigurations()) {
			DialogManager manager = app.getDialogManager();
			manager.unregister(PROCESS_ID);
		}
	}

	@NonNull
	public static ProfileColorController getInstance(@NonNull OsmandApplication app,
	                                                 @NonNull OnColorsPaletteListener listener,
	                                                 @ColorInt int selectedColor) {
		DialogManager dialogManager = app.getDialogManager();
		ProfileColorController controller = (ProfileColorController) dialogManager.findController(PROCESS_ID);
		if (controller == null) {
			ColorsCollection colorsCollection = new FileColorsCollection(app);
			controller = new ProfileColorController(app, colorsCollection, selectedColor);
			dialogManager.register(PROCESS_ID, controller);
		}
		controller.setPaletteListener(listener);
		return controller;
	}
}
