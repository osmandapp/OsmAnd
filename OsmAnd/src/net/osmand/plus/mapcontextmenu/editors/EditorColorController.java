package net.osmand.plus.mapcontextmenu.editors;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.plus.card.color.palette.main.ColorsPaletteController;
import net.osmand.plus.card.color.palette.main.OnColorsPaletteListener;
import net.osmand.plus.card.color.palette.main.data.ColorsCollection;
import net.osmand.plus.card.color.palette.main.data.FileColorsCollection;

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

	@NonNull
	public static EditorColorController getInstance(@NonNull OsmandApplication app,
	                                                @NonNull OnColorsPaletteListener listener,
	                                                @ColorInt int selectedColor) {
		DialogManager dialogManager = app.getDialogManager();
		EditorColorController controller = (EditorColorController) dialogManager.findController(PROCESS_ID);
		if (controller == null) {
			ColorsCollection colorsCollection = new FileColorsCollection(app);
			controller = new EditorColorController(app, colorsCollection, selectedColor);
			dialogManager.register(PROCESS_ID, controller);
		}
		controller.setPaletteListener(listener);
		return controller;
	}
}
