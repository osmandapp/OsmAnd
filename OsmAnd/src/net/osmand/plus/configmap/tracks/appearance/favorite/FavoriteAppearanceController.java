package net.osmand.plus.configmap.tracks.appearance.favorite;

import static net.osmand.shared.gpx.GpxParameter.COLOR;
import static net.osmand.shared.gpx.GpxParameter.COLORING_TYPE;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.BackgroundType;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.plus.card.color.ColoringStyle;
import net.osmand.plus.card.color.ColoringStyleCardController.IColorCardControllerListener;
import net.osmand.plus.card.icon.OnIconsPaletteListener;
import net.osmand.plus.configmap.tracks.appearance.data.AppearanceData;
import net.osmand.plus.mapcontextmenu.editors.FavoriteShapesCardController;
import net.osmand.plus.mapcontextmenu.editors.ShapesCard;
import net.osmand.plus.mapcontextmenu.editors.icon.FavoriteEditorIconController;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData.CustomDropDown;
import net.osmand.shared.palette.domain.PaletteItem;
import net.osmand.shared.routing.ColoringType;

public class FavoriteAppearanceController implements IDialogController, IColorCardControllerListener, CardListener, OnIconsPaletteListener<String> {

	public static final String PROCESS_ID = "edit_favorites_default_appearance";

	private final FavoriteColorCardController colorCardController;
	private final FavoriteEditorIconController editorIconController;
	private final FavoriteShapesCardController shapesCardController;

	private final FavoriteGroup favoriteGroup;
	private final DefaultFavoriteListener favoriteListener;

	@Nullable
	private Integer selectedColor;
	@Nullable
	private String selectedIconName;
	@Nullable
	private BackgroundType selectedBackgroundType;

	public FavoriteAppearanceController(@NonNull OsmandApplication app, @NonNull FavoriteGroup favoriteGroup, @NonNull DefaultFavoriteListener favoriteListener) {
		this.favoriteGroup = favoriteGroup;
		this.favoriteListener = favoriteListener;

		checkForSameGroupValues();

		AppearanceData appearanceData = new AppearanceData();
		ColoringType coloringType = selectedColor != null ? ColoringType.TRACK_SOLID : ColoringType.DEFAULT;
		appearanceData.setParameter(COLORING_TYPE, coloringType.getId());
		appearanceData.setParameter(COLOR, requireColor());
		colorCardController = new FavoriteColorCardController(app, appearanceData, this);
		colorCardController.setListener(this);

		editorIconController = new FavoriteEditorIconController(app, this);
		if (selectedIconName != null) {
			editorIconController.setSelectedIconKey(selectedIconName);
		}
		editorIconController.init();
		editorIconController.setIconsPaletteListener(this);
		editorIconController.getCardController().setCustomDropDownSelectorPopup(CustomDropDown.TOP_DROPDOWN);
		editorIconController.getCardController().setLimitHeightSelectorPopup(true);

		shapesCardController = new FavoriteShapesCardController(app, this, selectedBackgroundType != null ? selectedBackgroundType : null);
	}

	private void checkForSameGroupValues() {
		selectedColor = favoriteListener.getOriginalColor();
		selectedIconName = favoriteListener.getOriginalIconKey();
		selectedBackgroundType = favoriteListener.getOriginalShape();

		Integer color = null;
		String icon = null;
		BackgroundType shape = null;

		for (FavouritePoint point : favoriteGroup.getPoints()) {
			if (color == null) {
				color = point.getColor();
			} else if (color != point.getColor()) {
				selectedColor = null;
			}

			if (icon == null) {
				icon = point.getIconName();
			} else if (!icon.equals(point.getIconName())) {
				selectedIconName = null;
			}

			if (shape == null) {
				shape = point.getBackgroundType();
			} else if (shape != point.getBackgroundType()) {
				selectedBackgroundType = null;
			}
		}
	}

	@Override
	public void onColoringStyleSelected(@Nullable ColoringStyle coloringStyle) {
		setColor(coloringStyle != null ? favoriteListener.getOriginalColor() : null);
	}

	@Override
	public void onPaletteItemSelected(@NonNull PaletteItem item) {
		if (item instanceof PaletteItem.Solid solidItem) {
			setColor(solidItem.getColor());
		}
	}

	@Override
	public void onCardPressed(@NonNull BaseCard card) {
		if (card instanceof ShapesCard) {
			BackgroundType type = shapesCardController.getSelectedShape();
			setShape(type);
		}
	}

	@NonNull
	public Integer requireColor() {
		return selectedColor != null ? selectedColor : favoriteListener.getOriginalColor();
	}

	@Nullable
	public Integer getColor() {
		return selectedColor;
	}

	@NonNull
	public BackgroundType requireShape() {
		return selectedBackgroundType != null ? selectedBackgroundType : favoriteListener.getOriginalShape();
	}

	@Nullable
	public BackgroundType getShape() {
		return selectedBackgroundType;
	}

	@Nullable
	public String getIcon() {
		return selectedIconName;
	}

	@NonNull
	public String requireIcon() {
		return selectedIconName != null ? selectedIconName : favoriteListener.getOriginalIconKey();
	}

	public void setColor(@Nullable Integer color) {
		selectedColor = color;
		updateCards();
	}

	public void setShape(@Nullable BackgroundType backgroundType) {
		selectedBackgroundType = backgroundType;
		updateCards();
	}

	public void setIcon(@Nullable String iconName) {
		selectedIconName = iconName;
	}

	private void updateCards() {
		shapesCardController.updateContent();
	}

	@NonNull
	public FavoriteColorCardController getColorCardController() {
		return colorCardController;
	}

	@NonNull
	public FavoriteEditorIconController getIconController() {
		return editorIconController;
	}

	@NonNull
	public FavoriteShapesCardController getShapesController() {
		return shapesCardController;
	}

	@Override
	public void onIconSelectedFromPalette(@Nullable String icon) {
		setIcon(icon);
	}

	public interface DefaultFavoriteListener {
		@NonNull
		String getOriginalIconKey();

		int getOriginalColor();

		@NonNull
		BackgroundType getOriginalShape();
	}
}
