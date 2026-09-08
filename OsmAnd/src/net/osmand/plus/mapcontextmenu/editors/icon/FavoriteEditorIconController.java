package net.osmand.plus.mapcontextmenu.editors.icon;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.card.base.simple.DescriptionCard;
import net.osmand.plus.card.icon.IconsPaletteCard;
import net.osmand.plus.configmap.tracks.appearance.favorite.FavoriteAppearanceController;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.mapcontextmenu.editors.icon.data.IconsCategory;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class FavoriteEditorIconController extends EditorIconController {

	@Nullable
	private final FavoriteAppearanceController favoriteAppearanceController;
	@Nullable
	private final String originalIconKey;

	public static final String ORIGINAL_KEY = "original";
	public static final String PROCESS_ID = "favorite_editor_process_select_icon";

	public FavoriteEditorIconController(@NonNull OsmandApplication app, @NonNull FavoriteAppearanceController favoriteAppearanceController) {
		super(app);
		this.favoriteAppearanceController = favoriteAppearanceController;
		this.originalIconKey = null;
	}

	private FavoriteEditorIconController(@NonNull OsmandApplication app, @Nullable String originalIconKey) {
		super(app);
		this.favoriteAppearanceController = null;
		this.originalIconKey = originalIconKey;
	}

	@Override
	protected void initIconCategories() {
		initOriginalCategory();
		super.initIconCategories();
	}

	protected void initOriginalCategory() {
		if (favoriteAppearanceController != null || originalIconKey != null) {
			List<String> iconKeys = new ArrayList<>();
			categories.add(new IconsCategory(ORIGINAL_KEY, app.getString(R.string.shared_string_original), iconKeys, true));
		}
	}

	@Override
	public void setSelectedCategory(@NonNull IconsCategory category) {
		super.setSelectedCategory(category);
		if (ORIGINAL_KEY.equals(category.getKey())) {
			onIconSelectedFromPalette(originalIconKey, null);
		} else if (getSelectedIconKey() != null) {
			onIconSelectedFromPalette(getSelectedIconKey(), null);
		} else {
			onIconSelectedFromPalette(getOriginalIconKey(), null);
			cardController.updateIconsSelection();
		}
	}

	@Nullable
	private String getOriginalIconKey() {
		return favoriteAppearanceController != null ? favoriteAppearanceController.requireIcon() : originalIconKey;
	}

	@Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	@NonNull
	public static FavoriteEditorIconController getInstance(@NonNull OsmandApplication app, @NonNull Fragment targetFragment,
	                                                       @Nullable String originalIconKey, @Nullable String selectedIconKey) {
		DialogManager dialogManager = app.getDialogManager();
		FavoriteEditorIconController controller = (FavoriteEditorIconController) dialogManager.findController(PROCESS_ID);
		if (controller == null) {
			controller = new FavoriteEditorIconController(app, originalIconKey);
			controller.setSelectedIconKey(selectedIconKey);
			controller.init();
			dialogManager.register(PROCESS_ID, controller);
		}
		controller.setTargetFragment(targetFragment);
		return controller;
	}

	public static void onDestroy(@NonNull OsmandApplication app) {
		app.getDialogManager().unregister(PROCESS_ID);
	}

	@NonNull
	@Override
	protected EditorIconCardController createCardController() {
		return new EditorIconCardController(app, this) {

			@Override
			public void onBindCardContent(@NonNull FragmentActivity activity, @NonNull ViewGroup container,
			                              boolean nightMode, boolean usedOnMap) {
				container.removeAllViews();
				if (selectedState.getTag() instanceof IconsCategory iconsCategory && ORIGINAL_KEY.equals(iconsCategory.getKey())) {
					LayoutInflater inflater = UiUtilities.getInflater(activity, nightMode);
					inflater.inflate(R.layout.list_item_divider_with_padding_basic, container, true);
					container.addView(new DescriptionCard(activity, R.string.original_icon_description).build());
				} else {
					paletteController.setIcons(getSelectedCategoryIconKeys());
					paletteController.setSelectedIcon(getSelectedIconKey());
					container.addView(new IconsPaletteCard<>(activity, paletteController, usedOnMap).build());
				}
			}

			@NonNull
			@Override
			public String getCardTitle() {
				return app.getString(R.string.shared_string_icon);
			}
		};
	}
}
