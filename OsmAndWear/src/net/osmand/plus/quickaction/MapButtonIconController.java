package net.osmand.plus.quickaction;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.card.base.simple.DescriptionCard;
import net.osmand.plus.mapcontextmenu.editors.icon.EditorIconCardController;
import net.osmand.plus.mapcontextmenu.editors.icon.EditorIconController;
import net.osmand.plus.mapcontextmenu.editors.icon.data.IconsCategory;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.configure.buttons.MapButtonState;
import net.osmand.plus.views.mapwidgets.configure.buttons.QuickActionButtonState;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

class MapButtonIconController extends EditorIconController {

	public static final String PROCESS_ID = "map_button_select_icon";
	public static final String CUSTOM_KEY = "custom";
	public static final String DYNAMIC_KEY = "use_dynamic_icon";

	private final MapButtonState buttonState;
	private final ButtonAppearanceParams appearanceParams;
	@Nullable
	private IconsCategory dynamicCategory;

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
		askInitDynamicCategory();
		initCustomCategory();
		super.initIconCategories();
	}

	@Override
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

	protected void askInitDynamicCategory() {
//		if (buttonState instanceof QuickActionButtonState state && state.isSingleAction()) {
			String translatedName = app.getString(R.string.shared_string_dynamic);
			dynamicCategory = new IconsCategory(DYNAMIC_KEY, translatedName, new ArrayList<>(), true);
			categories.add(dynamicCategory);
//		}
	}

	public void updateAfterReset() {
		setSelectedCategory(findInitialIconCategory());
	}

	@Override
	public void setSelectedCategory(@NonNull IconsCategory category) {
		super.setSelectedCategory(category);
		if (isDynamicIconCategory(category)) {
			onIconSelectedFromPalette(null, null);
		}
	}

	@NonNull
	@Override
	protected IconsCategory findInitialIconCategory() {
		return findIconCategory(appearanceParams.getIconName());
	}

	@NonNull
	@Override
	protected IconsCategory findIconCategory(@Nullable String iconKey) {
		return iconKey == null && dynamicCategory != null
				? dynamicCategory : super.findIconCategory(iconKey);
	}

	@NonNull
	@Override
	protected EditorIconCardController createCardController() {
		return new EditorIconCardController(app, this) {

			@Override
			public void onBindCardContent(@NonNull FragmentActivity activity, @NonNull ViewGroup container, boolean nightMode, boolean usedOnMap) {
				if (isDynamicIconCategory(selectedCategory)) {
					container.removeAllViews();
					LinearLayout llContainer = createInternalContainer();
					LayoutInflater inflater = UiUtilities.getInflater(activity, nightMode);
					inflater.inflate(R.layout.list_item_divider_with_padding_basic, llContainer, true);
					llContainer.addView(new DescriptionCard(activity, R.string.dynamic_icon_type_summary).build());
					inflater.inflate(R.layout.list_item_divider_basic, llContainer, true);
					container.addView(llContainer);
				} else {
					super.onBindCardContent(activity, container, nightMode, usedOnMap);
				}
			}

			@NonNull
			private LinearLayout createInternalContainer() {
				LinearLayout container = new LinearLayout(app);
				container.setLayoutParams((new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)));
				container.setOrientation(LinearLayout.VERTICAL);
				return container;
			}
		};
	}

	private boolean isDynamicIconCategory(@NonNull IconsCategory category) {
		return Objects.equals(category.getKey(), DYNAMIC_KEY);
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
