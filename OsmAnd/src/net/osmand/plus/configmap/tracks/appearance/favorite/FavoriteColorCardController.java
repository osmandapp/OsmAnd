package net.osmand.plus.configmap.tracks.appearance.favorite;

import static net.osmand.shared.gpx.GpxParameter.COLOR;
import static net.osmand.shared.gpx.GpxParameter.COLORING_TYPE;
import static net.osmand.shared.routing.ColoringType.TRACK_SOLID;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.card.base.multistate.CardState;
import net.osmand.plus.card.base.simple.DescriptionCard;
import net.osmand.plus.card.color.ColoringStyle;
import net.osmand.plus.card.color.ColoringStyleCardController;
import net.osmand.plus.card.color.palette.main.ColorsPaletteCard;
import net.osmand.plus.card.color.palette.main.ColorsPaletteController;
import net.osmand.plus.card.color.palette.main.IColorsPaletteController;
import net.osmand.plus.card.color.palette.main.data.ColorsCollection;
import net.osmand.plus.card.color.palette.main.data.FileColorsCollection;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.configmap.tracks.appearance.data.AppearanceData;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.shared.routing.ColoringType;

import java.util.ArrayList;
import java.util.List;

public class FavoriteColorCardController extends ColoringStyleCardController {

	private final AppearanceData data;

	private IColorsPaletteController colorsPaletteController;
	private final FavoriteAppearanceController favoriteAppearanceController;

	public FavoriteColorCardController(@NonNull OsmandApplication app, @NonNull AppearanceData data, @NonNull FavoriteAppearanceController favoriteAppearanceController) {
		super(app);
		this.data = data;
		this.favoriteAppearanceController = favoriteAppearanceController;
		this.selectedState = findCardState(getColoringStyle());
	}

	@Override
	protected void onSelectCardState(@NonNull CardState cardState) {
		if (cardState.isOriginal()) {
			selectedState = cardState;
			card.updateSelectedCardState();
			data.resetParameter(COLOR);
			data.resetParameter(COLORING_TYPE);
			onColoringStyleSelected(null);
		} else {
			askSelectColoringStyle((ColoringStyle) cardState.getTag());
			PaletteColor selectedCardColor = colorsPaletteController.getSelectedColor();
			if (selectedCardColor != null) {
				getExternalListener().onColorSelectedFromPalette(selectedCardColor);
			}
		}
	}

	@Override
	public void onBindCardContent(@NonNull FragmentActivity activity, @NonNull ViewGroup container,
	                              boolean nightMode, boolean usedOnMap) {
		container.removeAllViews();
		ColoringStyle coloringStyle = getSelectedColoringStyle();

		if (coloringStyle == null) {
			LayoutInflater inflater = UiUtilities.getInflater(activity, nightMode);
			inflater.inflate(R.layout.list_item_divider_with_padding_basic, container, true);
			container.addView(new DescriptionCard(activity, R.string.original_color_description).build());
		} else {
			container.addView(new ColorsPaletteCard(activity, getColorsPaletteController()).build());
		}
	}

	@NonNull
	public IColorsPaletteController getColorsPaletteController() {
		if (colorsPaletteController == null) {
			ColorsCollection colorsCollection = new FileColorsCollection(app);
			Integer color = data.getParameter(COLOR);
			if (color == null) {
				color = favoriteAppearanceController.requireColor();
			}
			colorsPaletteController = new ColorsPaletteController(app, colorsCollection, color);
		}
		colorsPaletteController.setPaletteListener(getExternalListener());
		return colorsPaletteController;
	}

	@Nullable
	private ColoringStyle getColoringStyle() {
		String coloringType = data.getParameter(COLORING_TYPE);
		if (coloringType != null) {
			for (ColoringType type : ColoringType.getEntries()) {
				if (type.getId().equals(coloringType)) {
					return new ColoringStyle(type, null);
				}
			}
		}
		return null;
	}

	@NonNull
	@Override
	protected List<CardState> collectSupportedCardStates() {
		List<CardState> list = new ArrayList<>();

		list.add(new CardState(R.string.shared_string_original));

		List<CardState> other = super.collectSupportedCardStates();
		if (!other.isEmpty()) {
			other.get(0).setShowTopDivider(true);
		}
		list.addAll(other);

		return list;
	}

	@Override
	@NonNull
	protected ColoringType[] getSupportedColoringTypes() {
		return new ColoringType[] {TRACK_SOLID};
	}

	@Override
	@NonNull
	protected List<ColoringStyle> getSupportedColoringStyles() {
		List<ColoringStyle> coloringStyles = new ArrayList<>();
		for (ColoringType coloringType : getSupportedColoringTypes()) {
			if (!coloringType.isRouteInfoAttribute()) {
				coloringStyles.add(new ColoringStyle(coloringType));
			}
		}
		return coloringStyles;
	}
}
