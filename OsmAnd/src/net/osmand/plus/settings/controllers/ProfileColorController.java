package net.osmand.plus.settings.controllers;

import static net.osmand.plus.utils.ColorUtilities.getColor;

import android.content.Context;

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
import net.osmand.plus.card.color.palette.main.data.ColorsCollectionBundle;
import net.osmand.plus.card.color.palette.main.data.DefaultColors;
import net.osmand.plus.card.color.palette.main.data.PredefinedPaletteColor;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.profiles.ProfileIconColors;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
	public static List<PaletteColor> getPredefinedColors(@NonNull Context context, boolean nightMode) {
		List<PaletteColor> predefinedColors = new ArrayList<>();
		for (ProfileIconColors predefinedColor : ProfileIconColors.values()) {
			String id = predefinedColor.name().toLowerCase();
			int colorInt = getColor(context, predefinedColor.getColor(nightMode));
			predefinedColors.add(new PredefinedPaletteColor(id, colorInt, predefinedColor.getName()));
		}
		return predefinedColors;
	}

	@NonNull
	public static ProfileColorController getInstance(
			@NonNull OsmandApplication app, @NonNull ApplicationMode appMode,
			@NonNull OnColorsPaletteListener listener, @ColorInt int selectedColor,
			boolean nightMode
	) {
		OsmandSettings settings = app.getSettings();
		DialogManager dialogManager = app.getDialogManager();
		ProfileColorController controller = (ProfileColorController) dialogManager.findController(PROCESS_ID);
		if (controller == null) {
			ColorsCollectionBundle bundle = new ColorsCollectionBundle();
			bundle.appMode = appMode;
			bundle.predefinedColors = getPredefinedColors(app, nightMode);
			bundle.palettePreference = settings.PROFILE_COLORS_PALETTE;
			ColorsCollection colorsCollection = new ColorsCollection(bundle);
			controller = new ProfileColorController(app, colorsCollection, selectedColor);
			dialogManager.register(PROCESS_ID, controller);
		}
		controller.setPaletteListener(listener);
		return controller;
	}
}
