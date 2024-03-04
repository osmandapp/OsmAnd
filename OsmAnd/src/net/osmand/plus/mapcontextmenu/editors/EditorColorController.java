package net.osmand.plus.mapcontextmenu.editors;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.plus.card.color.palette.main.ColorsPaletteController;
import net.osmand.plus.card.color.palette.main.OnColorsPaletteListener;
import net.osmand.plus.card.color.palette.main.data.ColorsCollection;
import net.osmand.plus.card.color.palette.main.data.ColorsCollectionBundle;
import net.osmand.plus.card.color.palette.main.data.DefaultColors;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.settings.backend.OsmandSettings;

import java.util.Arrays;
import java.util.List;

public class EditorColorController extends ColorsPaletteController implements IDialogController {

	private static final String PROCESS_ID = "select_map_point_color";

	public EditorColorController(@NonNull OsmandApplication app,
	                             @NonNull ColorsCollection colorsCollection,
	                             @ColorInt int selectedColorInt) {
		super(app, colorsCollection, selectedColorInt);
	}

	public static void onDestroy(@NonNull OsmandApplication app) {
		DialogManager manager = app.getDialogManager();
		manager.unregister(PROCESS_ID);
	}

	public static List<PaletteColor> getPredefinedColors() {
		return Arrays.asList(DefaultColors.values());
	}

	@NonNull
	public static EditorColorController getInstance(@NonNull OsmandApplication app,
	                                                @NonNull OnColorsPaletteListener listener,
	                                                @ColorInt int selectedColor) {
		OsmandSettings settings = app.getSettings();
		DialogManager dialogManager = app.getDialogManager();
		EditorColorController controller = (EditorColorController) dialogManager.findController(PROCESS_ID);
		if (controller == null) {
			ColorsCollectionBundle bundle = new ColorsCollectionBundle();
			bundle.predefinedColors = getPredefinedColors();
			bundle.palettePreference = settings.POINT_COLORS_PALETTE;
			bundle.customColorsPreference = settings.CUSTOM_TRACK_PALETTE_COLORS;
			ColorsCollection colorsCollection = new ColorsCollection(bundle);
			controller = new EditorColorController(app, colorsCollection, selectedColor);
			dialogManager.register(PROCESS_ID, controller);
		}
		controller.setPaletteListener(listener);
		return controller;
	}
}
