package net.osmand.plus.configmap.tracks.appearance.subcontrollers;

import static net.osmand.plus.routing.ColoringType.TRACK_SOLID;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.card.base.multistate.CardState;
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
import net.osmand.plus.chooseplan.PromoBannerCard;
import net.osmand.plus.configmap.tracks.appearance.data.AppearanceData;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.GpxAppearanceAdapter;
import net.osmand.plus.track.fragments.controller.TrackColorController;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class ColorCardController extends ColoringStyleCardController implements ISelectedColorProvider {

	private IColorsPaletteController colorsPaletteController;
	private IColoringStyleDetailsController coloringStyleDetailsController;
	private final AppearanceData appearanceData;

	public ColorCardController(@NonNull OsmandApplication app,
	                           @NonNull AppearanceData appearanceData) {
		super(app, appearanceData.getColoringStyle());
		this.appearanceData = appearanceData;
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
			bundle.predefinedColors = TrackColorController.getPredefinedColors(app);
			bundle.palettePreference = settings.TRACK_COLORS_PALETTE;
			bundle.customColorsPreference = settings.CUSTOM_TRACK_PALETTE_COLORS;
			Integer selectedCustomColor = appearanceData.getCustomColor();
			if (selectedCustomColor == null) {
				selectedCustomColor = bundle.predefinedColors.get(0).getColor();
			}
			ColorsCollection colorsCollection = new ColorsCollection(bundle);
			colorsPaletteController = new ColorsPaletteController(app, colorsCollection, selectedCustomColor);
		}
		colorsPaletteController.setPaletteListener(getExternalListener());
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
	@Override
	protected List<CardState> collectSupportedCardStates() {
		List<CardState> result = new ArrayList<>();
		result.add(new CardState(R.string.shared_string_unchanged));
		List<CardState> other = super.collectSupportedCardStates();
		if (other.size() > 0) {
			other.get(0).setShowTopDivider(true);
		}
		result.addAll(other);
		return result;
	}

	@Override
	@NonNull
	protected ColoringType[] getSupportedColoringTypes() {
		return ColoringType.valuesOf(ColoringPurpose.TRACK);
	}

	@Override
	public int getSelectedColorValue() {
		ColoringStyle coloringStyle = getSelectedColoringStyle();
		ColoringType coloringType = coloringStyle != null ? coloringStyle.getType() : null;

		Integer color = null;
		if (coloringType == TRACK_SOLID) {
			color = appearanceData.getCustomColor();
		}
		if (color == null) {
			color = GpxAppearanceAdapter.getTrackColor(app);
		}
		return color;
	}
}
