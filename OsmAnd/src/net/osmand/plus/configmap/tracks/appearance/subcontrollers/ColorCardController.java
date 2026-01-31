package net.osmand.plus.configmap.tracks.appearance.subcontrollers;

import static net.osmand.shared.gpx.GpxParameter.COLOR;
import static net.osmand.shared.gpx.GpxParameter.COLORING_TYPE;
import static net.osmand.shared.gpx.ColoringPurpose.TRACK;
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
import net.osmand.plus.card.color.palette.gradient.v2.GradientPaletteController;
import net.osmand.plus.card.color.palette.main.ColorsPaletteCard;
import net.osmand.plus.card.color.palette.main.v2.SolidPaletteController;
import net.osmand.plus.palette.controller.BasePaletteController;
import net.osmand.shared.gpx.ColoringPurpose;
import net.osmand.plus.card.color.ColoringStyle;
import net.osmand.plus.card.color.ColoringStyleCardController;
import net.osmand.plus.card.color.IControlsColorProvider;
import net.osmand.plus.card.color.cstyle.ColoringStyleDetailsCard;
import net.osmand.plus.card.color.cstyle.ColoringStyleDetailsCardController;
import net.osmand.plus.card.color.cstyle.IColoringStyleDetailsController;
import net.osmand.plus.card.color.palette.gradient.GradientColorsPaletteCard;
import net.osmand.plus.chooseplan.PromoBannerCard;
import net.osmand.plus.configmap.tracks.appearance.data.AppearanceData;
import net.osmand.shared.palette.data.PaletteUtils;
import net.osmand.shared.palette.domain.PaletteCategory;
import net.osmand.shared.routing.ColoringType;
import net.osmand.plus.track.GpxAppearanceAdapter;
import net.osmand.shared.gpx.GradientScaleType;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class ColorCardController extends ColoringStyleCardController implements IControlsColorProvider {

	private final AppearanceData data;
	private final boolean addUnchanged;

	private BasePaletteController colorsPaletteController;
	private GradientPaletteController gradientPaletteController;
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
	public void onBindCardContent(@NonNull FragmentActivity activity, @NonNull ViewGroup container,
	                              boolean nightMode, boolean usedOnMap) {
		container.removeAllViews();
		ColoringStyle coloringStyle = getSelectedColoringStyle();
		ColoringType coloringType = coloringStyle != null ? coloringStyle.getType() : null;

		if (coloringStyle == null) {
			LayoutInflater inflater = UiUtilities.getInflater(activity, nightMode);
			inflater.inflate(R.layout.list_item_divider_with_padding_basic, container, true);
			container.addView(new DescriptionCard(activity, R.string.unchanged_parameter_summary).build());
		} else if (!isAvailableInSubscription(coloringStyle)) {
			container.addView(new PromoBannerCard(activity).build());
		} else if (ColoringType.Companion.isColorTypeInPurpose(coloringType, ColoringPurpose.TRACK) && coloringType.toGradientScaleType() != null) {
			GradientScaleType gradientScaleType = coloringType.toGradientScaleType();
			container.addView(new GradientColorsPaletteCard(activity, getGradientPaletteController(gradientScaleType)).build());
		} else if (coloringType.isTrackSolid()) {
			container.addView(new ColorsPaletteCard(activity, getColorsPaletteController()).build());
		} else {
			container.addView(new ColoringStyleDetailsCard(activity, getColoringStyleDetailsController()).build());
		}
	}

	@NonNull
	public GradientPaletteController getGradientPaletteController(@NonNull GradientScaleType gradientScaleType) {
		PaletteCategory paletteCategory = gradientScaleType.toPaletteCategory();
		String paletteId = paletteCategory != null ? paletteCategory.getKey() : "";
		if (gradientPaletteController == null) {
			gradientPaletteController = new GradientPaletteController(app, paletteId, null);
		}
		gradientPaletteController.updatePalette(paletteId, PaletteUtils.DEFAULT_NAME);
		gradientPaletteController.setPaletteListener(getExternalListener());
		return gradientPaletteController;
	}

	@NonNull
	public BasePaletteController getColorsPaletteController() {
		if (colorsPaletteController == null) {
			Integer color = data.getParameter(COLOR);
			colorsPaletteController = new SolidPaletteController(app, color, false);
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
			ColoringType type = ColoringType.Companion.requireValueOf(TRACK, coloringType);
			String attribute = ColoringType.Companion.getRouteInfoAttribute(coloringType);
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
		if (!other.isEmpty()) {
			other.get(0).setShowTopDivider(true);
		}
		list.addAll(other);

		return list;
	}

	@Override
	@NonNull
	protected ColoringType[] getSupportedColoringTypes() {
		return ColoringType.Companion.valuesOf(TRACK);
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
