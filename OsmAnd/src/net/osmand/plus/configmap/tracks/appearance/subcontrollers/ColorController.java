package net.osmand.plus.configmap.tracks.appearance.subcontrollers;

import static net.osmand.plus.routing.ColoringType.TRACK_SOLID;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.card.base.simple.DescriptionCard;
import net.osmand.plus.card.color.ColoringPurpose;
import net.osmand.plus.card.color.ColoringStyle;
import net.osmand.plus.card.color.ColoringStyleCardController;
import net.osmand.plus.card.color.ISelectedColorProvider;
import net.osmand.plus.card.color.cstyle.ColoringStyleDetailsCard;
import net.osmand.plus.card.color.cstyle.ColoringStyleDetailsCardController;
import net.osmand.plus.card.color.cstyle.IColoringStyleDetailsController;
import net.osmand.plus.card.color.palette.main.ColorsPaletteCard;
import net.osmand.plus.card.color.palette.main.ColorsPaletteController;
import net.osmand.plus.card.color.palette.main.IColorsPaletteController;
import net.osmand.plus.card.color.palette.main.data.ColorsCollection;
import net.osmand.plus.card.color.palette.main.data.ColorsCollectionBundle;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.card.color.palette.main.data.PredefinedPaletteColor;
import net.osmand.plus.chooseplan.PromoBannerCard;
import net.osmand.plus.configmap.tracks.appearance.data.AppearanceData;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.AppearanceListItem;
import net.osmand.plus.track.GpxAppearanceAdapter;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class ColorController extends ColoringStyleCardController implements ISelectedColorProvider {

	private IColorsPaletteController colorsPaletteController;
	private IColoringStyleDetailsController coloringStyleDetailsController;
	private final AppearanceData appearanceData;

	public ColorController(@NonNull OsmandApplication app,
	                       @NonNull AppearanceData appearanceData,
	                       @NonNull ColoringStyle selectedColoringStyle) {
		super(app, selectedColoringStyle);
		this.appearanceData = appearanceData;
	}

	@Override
	protected void onColoringStyleSelected(@NonNull ColoringStyle coloringStyle) {
		super.onColoringStyleSelected(coloringStyle);
		IColoringStyleDetailsController styleDetailsController = getColoringStyleDetailsController();
		styleDetailsController.setColoringStyle(coloringStyle);
	}

	@Override
	public void onBindCardContent(@NonNull FragmentActivity activity, @NonNull ViewGroup container, boolean nightMode) {
		container.removeAllViews();
		ColoringStyle coloringStyle = getSelectedColoringStyle();
		ColoringType coloringType = coloringStyle.getType();

		if (coloringType == ColoringType.UNCHANGED) {
			LayoutInflater inflater = UiUtilities.getInflater(activity, nightMode);
			inflater.inflate(R.layout.list_item_divider_with_padding_basic, container, true);
			container.addView(new DescriptionCard(activity, R.string.unchanged_parameter_summary).build());
		} else if (!isAvailableInSubscription(coloringStyle)) {
			container.addView(new PromoBannerCard(activity).build());
		} else if (coloringType.isTrackSolid()) {
			container.addView(new ColorsPaletteCard(activity, getColorsPaletteController()).build());
		} else {
			container.addView(new ColoringStyleDetailsCard(activity, getColoringStyleDetailsController()).build());
		}
	}

	@NonNull
	public IColorsPaletteController getColorsPaletteController() {
		if (colorsPaletteController == null) {
			OsmandSettings settings = app.getSettings();
			ColorsCollectionBundle bundle = new ColorsCollectionBundle();
			bundle.predefinedColors = getPredefinedColors(app);
			bundle.palettePreference = settings.TRACK_COLORS_PALETTE;
			bundle.customColorsPreference = settings.CUSTOM_TRACK_PALETTE_COLORS;
			Integer selectedCustomColor = appearanceData.getCustomColor();
			if (selectedCustomColor == null) {
				selectedCustomColor = bundle.predefinedColors.get(0).getColor();
			}
			ColorsCollection colorsCollection = new ColorsCollection(bundle);
			colorsPaletteController = new ColorsPaletteController(app, colorsCollection, selectedCustomColor);
		}
		colorsPaletteController.setPaletteListener(getControllerListener());
		return colorsPaletteController;
	}

	@NonNull
	private IColoringStyleDetailsController getColoringStyleDetailsController() {
		if (coloringStyleDetailsController == null) {
			ColoringStyle selectedColoringStyle = appearanceData.getColoringStyle();
			if (selectedColoringStyle == null) {
				selectedColoringStyle = new ColoringStyle(TRACK_SOLID);
			}
			coloringStyleDetailsController = new ColoringStyleDetailsCardController(app, selectedColoringStyle) {
				@Override
				public boolean shouldShowBottomSpace() {
					return true;
				}
			};
		}
		return coloringStyleDetailsController;
	}

	@NonNull
	public static List<PaletteColor> getPredefinedColors(@NonNull OsmandApplication app) {
		List<PaletteColor> predefinedColors = new ArrayList<>();
		for (AppearanceListItem item : GpxAppearanceAdapter.getUniqueTrackColorItems(app)) {
			String id = item.getValue();
			int colorInt = item.getColor();
			String name = item.getLocalizedValue();
			predefinedColors.add(new PredefinedPaletteColor(id, colorInt, name));
		}
		return predefinedColors;
	}

	@Override
	protected boolean isDataAvailableForColoringStyle(@NonNull ColoringStyle coloringStyle) {
		return true;
	}

	@Override
	protected ColoringType[] getSupportedColoringTypes() {
		return ColoringType.valuesOf(ColoringPurpose.TRACKS_GROUP);
	}

	@Override
	public int getSelectedColorValue() {
		ColoringStyle coloringStyle = getSelectedColoringStyle();
		ColoringType coloringType = coloringStyle.getType();

		Integer color = null;
		if (coloringType == TRACK_SOLID) {
			color = appearanceData.getCustomColor();
		}
		if (color == null) {
			color = GpxAppearanceAdapter.getTrackColor(app);
		}
		return color;
	}

	@Override
	protected boolean isUsedOnMap() {
		return false;
	}
}
