package net.osmand.plus.mapcontextmenu;

import static net.osmand.plus.views.mapwidgets.TopToolbarController.TopToolbarControllerType.CONTEXT_MENU;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.views.mapwidgets.TopToolbarController;

public class ContextMenuToolbarController extends TopToolbarController {

	private final MenuController controller;

	public ContextMenuToolbarController(@NonNull MenuController controller) {
		super(CONTEXT_MENU);
		this.controller = controller;
		setBgIds(R.color.app_bar_main_light, R.color.app_bar_main_dark,
				R.color.app_bar_main_light, R.color.app_bar_main_dark);
		setBackBtnIconClrIds(R.color.card_and_list_background_light, R.color.card_and_list_background_light);
		setCloseBtnIconClrIds(R.color.card_and_list_background_light, R.color.card_and_list_background_light);
		setTitleTextClrIds(R.color.card_and_list_background_light, R.color.card_and_list_background_light);
	}

	public MenuController getController() {
		return controller;
	}
}
