package net.osmand.plus.quickaction;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.mapcontextmenu.editors.icon.EditorIconController;
import net.osmand.plus.mapcontextmenu.editors.icon.data.IconsCategory;
import net.osmand.plus.views.mapwidgets.configure.buttons.MapButtonState;
import net.osmand.plus.views.mapwidgets.configure.buttons.QuickActionButtonState;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

class MapButtonIconController extends EditorIconController {

	public static final String PROCESS_ID = "map_button_select_icon";
	public static final String CUSTOM_KEY = "custom";

	private final MapButtonState buttonState;
	private final ButtonAppearanceParams appearanceParams;

	public MapButtonIconController(@NonNull OsmandApplication app,
	                               @NonNull MapButtonState buttonState,
	                               @NonNull ButtonAppearanceParams appearanceParams) {
		super(app);

		this.buttonState = buttonState;
		this.appearanceParams = appearanceParams;
	}

	@NonNull
	@Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	@Nullable
	@Override
	public String getSelectedIconKey() {
		return appearanceParams.getIconName();
	}

	@Override
	public void setSelectedIconKey(@Nullable String selectedIconKey) {
		appearanceParams.setIconName(selectedIconKey);
	}

	@Override
	protected void initIconCategories() {
		initCustomCategory();
		initLastUsedCategory();
		initAssetsCategories();
		initPoiPoiCategories();
		sortCategories();
	}

	protected void initLastUsedCategory() {
		lastUsedIcons = readLastUsedIcons();
		if (!Algorithms.isEmpty(lastUsedIcons)) {
			categories.add(new IconsCategory(LAST_USED_KEY, app.getString(R.string.shared_string_last_used), lastUsedIcons));
		}
	}

	protected void initCustomCategory() {
		Set<String> iconNames = new LinkedHashSet<>();
		iconNames.add(buttonState.createDefaultAppearanceParams().getIconName());

		if (buttonState instanceof QuickActionButtonState state) {
			for (QuickAction action : state.getQuickActions()) {
				int iconId = action.getIconRes(app);
				if (iconId > 0) {
					iconNames.add(app.getResources().getResourceEntryName(iconId));
				}
			}
		}
		categories.add(new IconsCategory(CUSTOM_KEY, app.getString(R.string.shared_string_custom), new ArrayList<>(iconNames), true));
	}

	public void update() {
		setSelectedCategory(findIconCategory(getSelectedIconKey()));
	}

	public static void onDestroy(@NonNull OsmandApplication app) {
		DialogManager manager = app.getDialogManager();
		manager.unregister(PROCESS_ID);
	}

	@NonNull
	public static MapButtonIconController getInstance(@NonNull OsmandApplication app,
	                                                  @NonNull MapButtonState buttonState,
	                                                  @NonNull ButtonAppearanceParams params,
	                                                  @NonNull Fragment fragment) {
		DialogManager dialogManager = app.getDialogManager();
		MapButtonIconController controller = (MapButtonIconController) dialogManager.findController(PROCESS_ID);
		if (controller == null) {
			controller = new MapButtonIconController(app, buttonState, params);
			controller.init();
			controller.setTargetFragment(fragment);
			dialogManager.register(PROCESS_ID, controller);
		}
		return controller;
	}
}
