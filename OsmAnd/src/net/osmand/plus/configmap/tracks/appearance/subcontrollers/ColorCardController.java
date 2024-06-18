package net.osmand.plus.configmap.tracks.appearance.subcontrollers;

import static net.osmand.gpx.GpxParameter.COLOR;
import static net.osmand.gpx.GpxParameter.COLORING_TYPE;
import static net.osmand.plus.card.color.ColoringPurpose.TRACK;
import static net.osmand.plus.routing.ColoringType.TRACK_SOLID;

import android.util.Pair;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.ColorPalette;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.card.base.multistate.CardState;
import net.osmand.plus.card.base.simple.DescriptionCard;
import net.osmand.plus.card.color.ColoringPurpose;
import net.osmand.plus.card.color.ColoringStyle;
import net.osmand.plus.card.color.ColoringStyleCardController;
import net.osmand.plus.card.color.IControlsColorProvider;
import net.osmand.plus.card.color.cstyle.ColoringStyleDetailsCard;
import net.osmand.plus.card.color.cstyle.ColoringStyleDetailsCardController;
import net.osmand.plus.card.color.cstyle.IColoringStyleDetailsController;
import net.osmand.plus.card.color.palette.gradient.GradientCollection;
import net.osmand.plus.card.color.palette.gradient.GradientColorsPaletteCard;
import net.osmand.plus.card.color.palette.gradient.GradientColorsPaletteController;
import net.osmand.plus.card.color.palette.gradient.PaletteGradientColor;
import net.osmand.plus.card.color.palette.main.ColorsPaletteCard;
import net.osmand.plus.card.color.palette.main.ColorsPaletteController;
import net.osmand.plus.card.color.palette.main.IColorsPaletteController;
import net.osmand.plus.card.color.palette.main.data.ColorsCollection;
import net.osmand.plus.card.color.palette.main.data.ColorsCollectionBundle;
import net.osmand.plus.chooseplan.PromoBannerCard;
import net.osmand.plus.configmap.tracks.appearance.data.AppearanceData;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.GpxAppearanceAdapter;
import net.osmand.plus.track.GradientScaleType;
import net.osmand.plus.track.fragments.controller.TrackColorController;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.router.RouteColorize;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ColorCardController extends ColoringStyleCardController implements IControlsColorProvider {

	private final AppearanceData data;
	private final boolean addUnchanged;

	private IColorsPaletteController colorsPaletteController;
	private GradientColorsPaletteController gradientPaletteController;
	private IColoringStyleDetailsController coloringStyleDetailsController;

	public ColorCardController(@NonNull OsmandApplication app, @NonNull AppearanceData data, boolean addUnchanged) {
		super(app);
		this.data = data;
		this.addUnchanged = addUnchanged;
		this.selectedState = findCardState(getColoringStyle());
	}

	@Override
	protected void onSelectCardState(@NonNull CardState cardState) {
		if (cardState.isOriginal()) {
			selectedState = cardState;
			card.updateSelectedCardState();
			data.resetParameter(COLOR);
			data.resetParameter(COLORING_TYPE);
		} else {
			askSelectColoringStyle((ColoringStyle) cardState.getTag());
		}
	}

	@Override
	protected void onColoringStyleSelected(@Nullable ColoringStyle coloringStyle) {
		super.onColoringStyleSelected(coloringStyle);
		if (coloringStyle != null) {
			IColoringStyleDetailsController styleDetailsController = getColoringStyleDetailsController();
			styleDetailsController.setColoringStyle(coloringStyle);
		}
	}

	@Override
	public void onBindCardContent(@NonNull FragmentActivity activity, @NonNull ViewGroup container, boolean nightMode) {
		container.removeAllViews();
		ColoringStyle coloringStyle = getSelectedColoringStyle();
		ColoringType coloringType = coloringStyle != null ? coloringStyle.getType() : null;

		if (coloringStyle == null) {
			LayoutInflater inflater = UiUtilities.getInflater(activity, nightMode);
			inflater.inflate(R.layout.list_item_divider_with_padding_basic, container, true);
			container.addView(new DescriptionCard(activity, R.string.unchanged_parameter_summary).build());
		} else if (!isAvailableInSubscription(coloringStyle)) {
			container.addView(new PromoBannerCard(activity).build());
		} else if (ColoringType.isColorTypeInPurpose(coloringType, ColoringPurpose.TRACK) && coloringType.toGradientScaleType() != null) {
			GradientScaleType gradientScaleType = coloringType.toGradientScaleType();
			container.addView(new GradientColorsPaletteCard(activity, getGradientPaletteController(gradientScaleType)).build());
		}else if (coloringType.isTrackSolid()) {
			container.addView(new ColorsPaletteCard(activity, getColorsPaletteController()).build());
		} else {
			container.addView(new ColoringStyleDetailsCard(activity, getColoringStyleDetailsController()).build());
		}
	}

	@NonNull
	public GradientColorsPaletteController getGradientPaletteController(@NonNull GradientScaleType gradientScaleType) {
		OsmandSettings settings = app.getSettings();

		RouteColorize.ColorizationType colorizationType = gradientScaleType.toColorizationType();
		Map<String, Pair<ColorPalette, Long>> colorPaletteMap = app.getColorPaletteHelper().getPalletsForType(colorizationType);
		GradientCollection gradientCollection = new GradientCollection(colorPaletteMap, settings.GRADIENT_PALETTES, colorizationType);

		if (gradientPaletteController == null) {
			gradientPaletteController = new GradientColorsPaletteController(app, null);
		}
		gradientPaletteController.updateContent(gradientCollection, PaletteGradientColor.DEFAULT_NAME);
		gradientPaletteController.setPaletteListener(getExternalListener());
		return gradientPaletteController;
	}

	@NonNull
	public IColorsPaletteController getColorsPaletteController() {
		if (colorsPaletteController == null) {
			OsmandSettings settings = app.getSettings();
			ColorsCollectionBundle bundle = new ColorsCollectionBundle();
			bundle.predefinedColors = TrackColorController.getPredefinedColors(app);
			bundle.palettePreference = settings.TRACK_COLORS_PALETTE;
			bundle.customColorsPreference = settings.CUSTOM_TRACK_PALETTE_COLORS;
			Integer color = data.getParameter(COLOR);
			if (color == null) {
				color = bundle.predefinedColors.get(0).getColor();
			}
			ColorsCollection colorsCollection = new ColorsCollection(bundle);
			colorsPaletteController = new ColorsPaletteController(app, colorsCollection, color);
		}
		colorsPaletteController.setPaletteListener(getExternalListener());
		return colorsPaletteController;
	}

	@NonNull
	private IColoringStyleDetailsController getColoringStyleDetailsController() {
		if (coloringStyleDetailsController == null) {
			ColoringStyle selectedStyle = getColoringStyle();
			if (selectedStyle == null) {
				selectedStyle = new ColoringStyle(TRACK_SOLID);
			}
			coloringStyleDetailsController = new ColoringStyleDetailsCardController(app, selectedStyle) {
				@Override
				public boolean shouldShowBottomSpace() {
					return true;
				}
			};
		}
		return coloringStyleDetailsController;
	}

	@Nullable
	private ColoringStyle getColoringStyle() {
		String coloringType = data.getParameter(COLORING_TYPE);
		if (coloringType != null) {
			ColoringType type = ColoringType.requireValueOf(TRACK, coloringType);
			String attribute = ColoringType.getRouteInfoAttribute(coloringType);
			return new ColoringStyle(type, attribute);
		}
		return null;
	}

	@NonNull
	@Override
	protected List<CardState> collectSupportedCardStates() {
		List<CardState> list = new ArrayList<>();

		if (addUnchanged) {
			list.add(new CardState(R.string.shared_string_unchanged));
		}
		list.add(new CardState(R.string.shared_string_original));

		List<CardState> other = super.collectSupportedCardStates();
		if (other.size() > 0) {
			other.get(0).setShowTopDivider(true);
		}
		list.addAll(other);

		return list;
	}

	@Override
	@NonNull
	protected ColoringType[] getSupportedColoringTypes() {
		return ColoringType.valuesOf(TRACK);
	}

	@Override
	public int getSelectedControlsColor() {
		ColoringStyle coloringStyle = getSelectedColoringStyle();
		ColoringType coloringType = coloringStyle != null ? coloringStyle.getType() : null;
		Integer color = null;
		if (coloringType == TRACK_SOLID) {
			color = data.getParameter(COLOR);
		}
		if (color == null) {
			color = GpxAppearanceAdapter.getTrackColor(app);
		}
		return color;
	}
}
