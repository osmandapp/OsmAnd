package net.osmand.plus.mapcontextmenu.editors.icon;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.card.base.simple.DescriptionCard;
import net.osmand.plus.card.icon.IconsPaletteCard;
import net.osmand.plus.configmap.tracks.appearance.favorite.FavoriteAppearanceController;
import net.osmand.plus.mapcontextmenu.editors.icon.data.IconsCategory;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class FavoriteEditorIconController extends EditorIconController {

	private final FavoriteAppearanceController favoriteAppearanceController;

	public static final String ORIGINAL_KEY = "original";
	public static final String PROCESS_ID = "favorite_editor_process_select_icon";

	public FavoriteEditorIconController(@NonNull OsmandApplication app, @NonNull FavoriteAppearanceController favoriteAppearanceController) {
		super(app);
		this.favoriteAppearanceController = favoriteAppearanceController;
	}

	@Override
	protected void initIconCategories() {
		initOriginalCategory();
		super.initIconCategories();
	}

	protected void initOriginalCategory() {
		List<String> iconKeys = new ArrayList<>();
		categories.add(new IconsCategory(ORIGINAL_KEY, app.getString(R.string.shared_string_original), iconKeys, true));
	}

	@Override
	public void setSelectedCategory(@NonNull IconsCategory category) {
		super.setSelectedCategory(category);
		if (ORIGINAL_KEY.equals(category.getKey())) {
			if (iconsPaletteListener != null) {
				iconsPaletteListener.onIconSelectedFromPalette(null);
			}
		} else if (getSelectedIconKey() != null) {
			onIconSelectedFromPalette(getSelectedIconKey(), null);
		} else if (getSelectedIconKey() == null) {
			onIconSelectedFromPalette(favoriteAppearanceController.requireIcon(), null);
			cardController.updateIconsSelection();
		}
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
