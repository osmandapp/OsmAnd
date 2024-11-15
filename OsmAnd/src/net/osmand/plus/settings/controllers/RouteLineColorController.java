package net.osmand.plus.settings.controllers;

import static net.osmand.router.RouteStatisticsHelper.ROUTE_INFO_PREFIX;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.shared.gpx.ColoringPurpose;
import net.osmand.plus.card.color.ColoringStyle;
import net.osmand.plus.card.color.ColoringStyleCardController;
import net.osmand.plus.card.color.IControlsColorProvider;
import net.osmand.plus.card.color.cstyle.ColoringStyleDetailsCard;
import net.osmand.plus.card.color.cstyle.ColoringStyleDetailsCardController;
import net.osmand.plus.card.color.cstyle.IColoringStyleDetailsController;
import net.osmand.plus.card.color.palette.gradient.GradientColorsCollection;
import net.osmand.plus.card.color.palette.gradient.GradientColorsPaletteCard;
import net.osmand.plus.card.color.palette.gradient.GradientColorsPaletteController;
import net.osmand.plus.card.color.palette.main.data.ColorsCollection;
import net.osmand.plus.card.color.palette.main.data.FileColorsCollection;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.card.color.palette.main.data.PaletteMode;
import net.osmand.plus.card.color.palette.moded.ModedColorsPaletteCard;
import net.osmand.plus.card.color.palette.moded.ModedColorsPaletteController;
import net.osmand.plus.card.color.palette.moded.ModedColorsPaletteController.OnPaletteModeSelectedListener;
import net.osmand.plus.chooseplan.PromoBannerCard;
import net.osmand.plus.helpers.DayNightHelper;
import net.osmand.plus.helpers.DayNightHelper.MapThemeProvider;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.shared.routing.ColoringType;
import net.osmand.plus.routing.PreviewRouteLineInfo;
import net.osmand.plus.settings.enums.DayNightMode;
import net.osmand.shared.gpx.GradientScaleType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.layers.PreviewRouteLineLayer;
import net.osmand.shared.routing.RouteColorize;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class RouteLineColorController extends ColoringStyleCardController
		implements MapThemeProvider, IDialogController, IControlsColorProvider {

	public static final String PROCESS_ID = "select_route_line_color";

	private static final int PALETTE_MODE_ID_DAY = 0;
	private static final int PALETTE_MODE_ID_NIGHT = 1;

	private PreviewRouteLineInfo routeLinePreview;

	private ModedColorsPaletteController colorsPaletteController;
	private GradientColorsPaletteController gradientPaletteController;
	private IColoringStyleDetailsController coloringStyleDetailsController;
	private boolean initialNightMode;

	private RouteLineColorController(@NonNull OsmandApplication app, @NonNull ColoringStyle selectedStyle) {
		super(app);
		this.selectedState = findCardState(selectedStyle);
	}

	public void setRouteLinePreview(@NonNull PreviewRouteLineInfo routeLinePreview) {
		this.routeLinePreview = routeLinePreview;
	}

	@NonNull
	public ModedColorsPaletteController getColorsPaletteController() {
		if (colorsPaletteController == null) {
			ColorsCollection colorsCollection = new FileColorsCollection(app);
			colorsPaletteController = new ModedColorsPaletteController(app, colorsCollection) {

				private PaletteMode paletteModeDay;
				private PaletteMode paletteModeNight;

				@Override
				@NonNull
				protected List<PaletteMode> collectAvailablePaletteModes() {
					paletteModeDay = createPaletteMode(false);
					paletteModeNight = createPaletteMode(true);
					return Arrays.asList(paletteModeDay, paletteModeNight);
				}

				@NonNull
				@Override
				protected PaletteMode getInitialPaletteMode() {
					return initialNightMode ? paletteModeNight : paletteModeDay;
				}

				@Override
				protected PaletteColor provideSelectedColorForPaletteMode(@NonNull PaletteMode paletteMode) {
					boolean useNightMap = Objects.equals(paletteMode.getTag(), PALETTE_MODE_ID_NIGHT);
					return collection.findPaletteColor(routeLinePreview.getCustomColor(useNightMap));
				}

				@NonNull
				private PaletteMode createPaletteMode(boolean night) {
					String title = app.getString(night ? R.string.daynight_mode_night : R.string.daynight_mode_day);
					int tag = night ? PALETTE_MODE_ID_NIGHT : PALETTE_MODE_ID_DAY;
					return new PaletteMode(title, tag);
				}

				@Override
				public void onAllColorsScreenClosed() {
					notifyAllColorsScreenClosed();
				}
			};
		}
		colorsPaletteController.setPaletteListener(getExternalListener());
		colorsPaletteController.setPaletteModeSelectedListener((OnPaletteModeSelectedListener) getExternalListener());
		return colorsPaletteController;
	}

	@NonNull
	private IColoringStyleDetailsController getColoringStyleDetailsController() {
		if (coloringStyleDetailsController == null) {
			ColoringStyle selectedColoringStyle = routeLinePreview.getRouteColoringStyle();
			coloringStyleDetailsController = new ColoringStyleDetailsCardController(app, selectedColoringStyle, null) {
				@Nullable
				@Override
				public String getTypeDescription() {
					ColoringType coloringType = coloringStyle.getType();
					String routeInfoAttribute = coloringStyle.getRouteInfoAttribute();
					if (coloringType.isDefault()) {
						String pattern = app.getString(R.string.route_line_use_map_style_color);
						return String.format(pattern, app.getRendererRegistry().getSelectedRendererName());
					} else if (routeInfoAttribute != null) {
						String key = routeInfoAttribute.replaceAll(ROUTE_INFO_PREFIX, "");
						return AndroidUtils.getStringRouteInfoPropertyDescription(app, key);
					} else if (coloringType == ColoringType.ALTITUDE) {
						return app.getString(R.string.route_line_use_coloring_altitude);
					} else if (coloringType == ColoringType.SPEED) {
						return app.getString(R.string.route_line_use_coloring_speed);
					} else if (coloringType == ColoringType.SLOPE) {
						return app.getString(R.string.route_line_use_gradient_coloring);
					}
					return null;
				}

				@Override
				public boolean shouldShowBottomSpace() {
					return true;
				}
			};
		}
		return coloringStyleDetailsController;
	}

	@Override
	public void onBindCardContent(@NonNull FragmentActivity activity, @NonNull ViewGroup container,
	                              boolean nightMode, boolean usedOnMap) {
		container.removeAllViews();
		BaseCard card = getContentCardForSelectedState(activity);
		View cardView = card.getView() != null ? card.getView() : card.build(activity);
		container.addView(cardView);
	}

	@NonNull
	protected BaseCard getContentCardForSelectedState(@NonNull FragmentActivity activity) {
		ColoringStyle coloringStyle = requireSelectedColoringStyle();
		ColoringType coloringType = coloringStyle.getType();
		if (!isAvailableInSubscription(coloringStyle)) {
			return new PromoBannerCard(activity);
		} else if (coloringType.isCustomColor()) {
			return new ModedColorsPaletteCard(activity, getColorsPaletteController());
		} else if (ColoringType.Companion.isColorTypeInPurpose(coloringType, ColoringPurpose.ROUTE_LINE) && coloringType.toGradientScaleType() != null) {
			GradientScaleType gradientScaleType = coloringType.toGradientScaleType();
			return new GradientColorsPaletteCard(activity, getGradientPaletteController(gradientScaleType));
		} else {
			return new ColoringStyleDetailsCard(activity, getColoringStyleDetailsController());
		}
	}

	@NonNull
	public GradientColorsPaletteController getGradientPaletteController(@NonNull GradientScaleType gradientScaleType) {
		RouteColorize.ColorizationType colorizationType = gradientScaleType.toColorizationType();
		GradientColorsCollection gradientCollection = new GradientColorsCollection(app, colorizationType);

		if (gradientPaletteController == null) {
			gradientPaletteController = new GradientColorsPaletteController(app, null) {
				@Override
				public void onAllColorsScreenClosed() {
					notifyAllColorsScreenClosed();
				}
			};
		}
		gradientPaletteController.setPaletteListener(getExternalListener());
		gradientPaletteController.updateContent(gradientCollection, routeLinePreview.getGradientPalette());
		return gradientPaletteController;
	}

	@Nullable
	public GradientColorsPaletteController getGradientPaletteController() {
		return gradientPaletteController;
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
	public boolean shouldShowCardHeader() {
		return false;
	}

	public boolean isSelectedColoringStyleAvailable() {
		return isAvailableInSubscription(requireSelectedColoringStyle());
	}

	@Override
	@NonNull
	protected ColoringType[] getSupportedColoringTypes() {
		return ColoringType.Companion.valuesOf(ColoringPurpose.ROUTE_LINE);
	}

	@Override
	public DayNightMode getMapTheme() {
		ColoringStyle coloringStyle = requireSelectedColoringStyle();
		if (coloringStyle.getType().isCustomColor()) {
			return isNightMap() ? DayNightMode.NIGHT : DayNightMode.DAY;
		}
		return null;
	}

	public boolean isNightMap() {
		ModedColorsPaletteController paletteController = getColorsPaletteController();
		PaletteMode paletteMode = paletteController.getSelectedPaletteMode();
		return Objects.equals(paletteMode.getTag(), PALETTE_MODE_ID_NIGHT);
	}

	public void onDestroy(@Nullable FragmentActivity activity) {
		if (activity != null && !activity.isChangingConfigurations()) {
			DialogManager manager = app.getDialogManager();
			manager.unregister(PROCESS_ID);
		}
	}

	public void onResume() {
		initialNightMode = app.getDaynightHelper().isNightModeForMapControls();
		setMapThemeProvider(this);
	}

	public void onPause() {
		setMapThemeProvider(null);
	}

	private void setMapThemeProvider(@Nullable MapThemeProvider provider) {
		DayNightHelper helper = app.getDaynightHelper();
		helper.setExternalMapThemeProvider(provider);
	}

	@Override
	public int getSelectedControlsColor() {
		PreviewRouteLineLayer layer = app.getOsmandMap().getMapLayers().getPreviewRouteLineLayer();
		return layer.getRouteLineColor(isNightMap());
	}

	private void notifyAllColorsScreenClosed() {
		if (getExternalListener() instanceof IRouteLineColorControllerListener listener) {
			listener.updateStatusBar();
		}
	}

	@NonNull
	public static RouteLineColorController getInstance(@NonNull OsmandApplication app,
	                                                   @NonNull PreviewRouteLineInfo routeLinePreview,
	                                                   @NonNull IRouteLineColorControllerListener listener) {
		DialogManager dialogManager = app.getDialogManager();
		RouteLineColorController controller = (RouteLineColorController) dialogManager.findController(PROCESS_ID);
		if (controller == null) {
			controller = new RouteLineColorController(app, routeLinePreview.getRouteColoringStyle());
			dialogManager.register(PROCESS_ID, controller);
		}
		controller.setRouteLinePreview(routeLinePreview);
		controller.setListener(listener);
		return controller;
	}

	public interface IRouteLineColorControllerListener
			extends IColorCardControllerListener, OnPaletteModeSelectedListener {
		void updateStatusBar();
	}
}
