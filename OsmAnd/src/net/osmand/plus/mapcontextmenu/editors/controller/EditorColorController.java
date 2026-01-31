package net.osmand.plus.mapcontextmenu.editors.controller;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.plus.card.color.palette.main.v2.SolidPaletteController;
import net.osmand.plus.card.color.palette.main.v2.OnColorsPaletteListener;

public class EditorColorController extends SolidPaletteController implements IDialogController {

	private static final String PROCESS_ID = "select_map_point_color";

	private Fragment targetFragment;

	public EditorColorController(@NonNull OsmandApplication app, @ColorInt int selectedColorInt) {
		super(app, selectedColorInt);
	}

	public void setTargetFragment(@NonNull Fragment targetFragment) {
		this.targetFragment = targetFragment;
	}

	@Nullable
	public Fragment getTargetFragment() {
		return targetFragment;
	}

	@Override
	public void onPaletteScreenClosed() {
		if (getTargetFragment() instanceof BaseFullScreenFragment fragment) {
			fragment.updateStatusBar();
		}
	}

	public static void onDestroy(@NonNull OsmandApplication app) {
		DialogManager manager = app.getDialogManager();
		manager.unregister(PROCESS_ID);
	}

	@NonNull
	public static EditorColorController getInstance(@NonNull OsmandApplication app,
													@NonNull Fragment targetFragment,
	                                                @ColorInt int selectedColor) {
		DialogManager dialogManager = app.getDialogManager();
		EditorColorController controller = (EditorColorController) dialogManager.findController(PROCESS_ID);
		if (controller == null) {
			controller = new EditorColorController(app, selectedColor);
			dialogManager.register(PROCESS_ID, controller);
		}
		controller.setTargetFragment(targetFragment);
		if (targetFragment instanceof OnColorsPaletteListener listener) {
			controller.setPaletteListener(listener);
		}
		return controller;
	}
}
