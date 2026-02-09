package net.osmand.plus.track;

import static net.osmand.shared.gpx.ColoringPurpose.TRACK;
import static net.osmand.plus.configmap.ConfigureMapMenu.CURRENT_TRACK_COLOR_ATTR;
import static net.osmand.plus.configmap.ConfigureMapMenu.CURRENT_TRACK_WIDTH_ATTR;
import static net.osmand.plus.track.Gpx3DVisualizationType.FIXED_HEIGHT;
import static net.osmand.plus.track.fragments.TrackMenuFragment.TRACK_FILE_NAME;
import static net.osmand.shared.gpx.GpxParameter.COLOR_PALETTE;
import static net.osmand.shared.gpx.GpxParameter.ADDITIONAL_EXAGGERATION;
import static net.osmand.shared.gpx.GpxParameter.COLOR;
import static net.osmand.shared.gpx.GpxParameter.COLORING_TYPE;
import static net.osmand.shared.gpx.GpxParameter.ELEVATION_METERS;
import static net.osmand.shared.gpx.GpxParameter.JOIN_SEGMENTS;
import static net.osmand.shared.gpx.GpxParameter.SHOW_ARROWS;
import static net.osmand.shared.gpx.GpxParameter.SHOW_START_FINISH;
import static net.osmand.shared.gpx.GpxParameter.SPLIT_INTERVAL;
import static net.osmand.shared.gpx.GpxParameter.SPLIT_TYPE;
import static net.osmand.shared.gpx.GpxParameter.TRACK_3D_LINE_POSITION_TYPE;
import static net.osmand.shared.gpx.GpxParameter.TRACK_3D_WALL_COLORING_TYPE;
import static net.osmand.shared.gpx.GpxParameter.TRACK_VISUALIZATION_TYPE;
import static net.osmand.shared.gpx.GpxParameter.WIDTH;
import net.osmand.shared.routing.Gpx3DWallColorType;

import android.os.Bundle;

import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.card.color.ColoringStyle;
import net.osmand.plus.card.color.palette.gradient.PaletteGradientColor;
import net.osmand.plus.card.color.palette.gradient.PaletteGradientColor;
import net.osmand.shared.routing.ColoringType;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.track.helpers.GpxAppearanceHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.shared.gpx.GpxDataItem;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.util.Algorithms;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class TrackDrawInfo {

	public static final int DEFAULT = 0;
	public static final int GPX_FILE = 1;
	public static final int CURRENT_RECORDING = 2;

	@Retention(RetentionPolicy.SOURCE)
	@IntDef({CURRENT_RECORDING, DEFAULT, GPX_FILE})
	public @interface TrackAppearanceType {
	}

	private static final String TRACK_APPEARANCE_TYPE = "track_appearance_type";
	private static final String TRACK_WIDTH = "track_width";
	private static final String TRACK_COLORING_TYPE = "track_coloring_type";
	private static final String TRACK_COLOR = "track_color";
	private static final String TRACK_SPLIT_TYPE = "track_split_type";
	private static final String TRACK_SPLIT_INTERVAL = "track_split_interval";
	private static final String TRACK_JOIN_SEGMENTS = "track_join_segments";
	private static final String TRACK_SHOW_ARROWS = "track_show_arrows";
	private static final String TRACK_SHOW_START_FINISH = "track_show_start_finish";

	private static final String TRACK_VISUALIZATION_TYPE_KEY = "track_visualization_type";
	private static final String TRACK_WALL_COLOR_TYPE_KEY = "track_wall_color_type";
	private static final String TRACK_LINE_POSITION_TYPE_KEY = "track_line_position_type";
	private static final String ADDITIONAL_EXAGGERATION_KEY = "additional_exaggeration";
	private static final String ELEVATION_METERS_KEY = "elevation_meters";
	private static final String GRADIENT_COLOR_KEY = "gradient_color";

	private String filePath;
	private String width;
	private ColoringType coloringType;
	private String routeInfoAttribute;
	@ColorInt
	@Nullable
	private Integer color;
	private int splitType;
	private double splitInterval;
	private boolean joinSegments;
	private boolean showArrows;
	private boolean showStartFinish = true;
	private Gpx3DVisualizationType trackVisualizationType = Gpx3DVisualizationType.NONE;
	private Gpx3DWallColorType trackWallColorType = Gpx3DWallColorType.NONE;
	private Gpx3DLinePositionType trackLinePositionType = Gpx3DLinePositionType.TOP;
	private float additionalExaggeration = 1f;
	private float elevationMeters = 1000f;
	private String gradientColorName = PaletteGradientColor.DEFAULT_NAME;

	@TrackAppearanceType
	private final int appearanceType;

	public TrackDrawInfo(@NonNull OsmandApplication app, @TrackAppearanceType int appearanceType) {
		this.appearanceType = appearanceType;

		if (appearanceType == CURRENT_RECORDING) {
			initCurrentTrackParams(app);
		} else if (appearanceType == DEFAULT) {
			initDefaultTrackParams(app, app.getSettings().getApplicationMode());
		}
	}

	public TrackDrawInfo(Bundle bundle) {
		readBundle(bundle);
		appearanceType = bundle.getInt(TRACK_APPEARANCE_TYPE);
	}

	public TrackDrawInfo(@NonNull OsmandApplication app, @NonNull String filePath, @Nullable GpxDataItem gpxDataItem) {
		this.appearanceType = GPX_FILE;
		if (gpxDataItem != null) {
			updateParams(app, gpxDataItem);
		}
		this.filePath = filePath;
	}

	private void initCurrentTrackParams(@NonNull OsmandApplication app) {
		OsmandSettings settings = app.getSettings();

		width = settings.CURRENT_TRACK_WIDTH.get();
		color = settings.CURRENT_TRACK_COLOR.get();
		coloringType = settings.CURRENT_TRACK_COLORING_TYPE.get();
		routeInfoAttribute = settings.CURRENT_TRACK_ROUTE_INFO_ATTRIBUTE.get();
		showArrows = settings.CURRENT_TRACK_SHOW_ARROWS.get();
		showStartFinish = settings.CURRENT_TRACK_SHOW_START_FINISH.get();
		additionalExaggeration = settings.CURRENT_TRACK_ADDITIONAL_EXAGGERATION.get();
		elevationMeters = settings.CURRENT_TRACK_ELEVATION_METERS.get();
		trackVisualizationType = Gpx3DVisualizationType.get3DVisualizationType(settings.CURRENT_TRACK_3D_VISUALIZATION_TYPE.get());
		trackWallColorType = Gpx3DWallColorType.Companion.get3DWallColorType(settings.CURRENT_TRACK_3D_WALL_COLORING_TYPE.get());
		trackLinePositionType = Gpx3DLinePositionType.get3DLinePositionType(settings.CURRENT_TRACK_3D_WALL_COLORING_TYPE.get());
	}

	public void initDefaultTrackParams(@NonNull OsmandApplication app, @NonNull ApplicationMode mode) {
		color = GpxAppearanceAdapter.getTrackColor(app);
		width = app.getSettings().getCustomRenderProperty(CURRENT_TRACK_WIDTH_ATTR).getModeValue(mode);
		coloringType = ColoringType.Companion.requireValueOf(TRACK, null);
		routeInfoAttribute = ColoringType.Companion.getRouteInfoAttribute(null);
	}

	public void updateParams(@NonNull OsmandApplication app, @NonNull GpxDataItem item) {
		GpxAppearanceHelper helper = new GpxAppearanceHelper(app);

		width = helper.getParameter(item, WIDTH);
		color = helper.getParameter(item, COLOR);
		String type = helper.getParameter(item, COLORING_TYPE);
		coloringType = ColoringType.Companion.requireValueOf(TRACK, type);
		routeInfoAttribute = ColoringType.Companion.getRouteInfoAttribute(type);
		splitType = helper.requireParameter(item, SPLIT_TYPE);
		splitInterval = helper.requireParameter(item, SPLIT_INTERVAL);
		joinSegments = helper.requireParameter(item, JOIN_SEGMENTS);
		showArrows = helper.requireParameter(item, SHOW_ARROWS);
		showStartFinish = helper.requireParameter(item, SHOW_START_FINISH);
		trackVisualizationType = Gpx3DVisualizationType.get3DVisualizationType(helper.getParameter(item, TRACK_VISUALIZATION_TYPE));
		trackWallColorType = Gpx3DWallColorType.Companion.get3DWallColorType(helper.getParameter(item, TRACK_3D_WALL_COLORING_TYPE));
		trackLinePositionType = Gpx3DLinePositionType.get3DLinePositionType(helper.getParameter(item, TRACK_3D_LINE_POSITION_TYPE));
		additionalExaggeration = ((Double) helper.requireParameter(item, ADDITIONAL_EXAGGERATION)).floatValue();
		elevationMeters = ((Double) helper.requireParameter(item, ELEVATION_METERS)).floatValue();
		gradientColorName = helper.getParameter(item, COLOR_PALETTE);
	}

	@Nullable
	private String getDefaultWidth(@NonNull OsmandSettings settings, @Nullable RenderingRulesStorage renderer) {
		String width = settings.getCustomRenderProperty(CURRENT_TRACK_WIDTH_ATTR).get();
		if (Algorithms.isEmpty(width) && renderer != null) {
			RenderingRuleProperty property = renderer.PROPS.getCustomRule(CURRENT_TRACK_WIDTH_ATTR);
			if (property != null && !Algorithms.isEmpty(property.getPossibleValues())) {
				return property.getPossibleValues()[0];
			}
		}
		return width;
	}

	private int getDefaultColor(@NonNull OsmandSettings settings, @Nullable RenderingRulesStorage renderer) {
		CommonPreference<String> preference = settings.getCustomRenderProperty(CURRENT_TRACK_COLOR_ATTR);
		int color = GpxAppearanceAdapter.parseTrackColor(renderer, preference.get());
		if (color == 0 && renderer != null) {
			RenderingRuleProperty property = renderer.PROPS.getCustomRule(CURRENT_TRACK_COLOR_ATTR);
			if (property != null && !Algorithms.isEmpty(property.getPossibleValues())) {
				return GpxAppearanceAdapter.parseTrackColor(renderer, property.getPossibleValues()[0]);
			}
		}
		return color;
	}

	public String getFilePath() {
		return filePath;
	}

	public String getWidth() {
		return width;
	}

	public void setWidth(String width) {
		this.width = width;
	}

	@NonNull
	public ColoringStyle getColoringStyle() {
		return new ColoringStyle(getColoringType(), getRouteInfoAttribute());
	}

	@NonNull
	public ColoringType getColoringType() {
		return coloringType == null ? ColoringType.TRACK_SOLID : coloringType;
	}

	@Nullable
	public String getColoringTypeName() {
		return getColoringType().getName(routeInfoAttribute);
	}

	public String getRouteInfoAttribute() {
		return routeInfoAttribute;
	}

	public void setColoringStyle(@NonNull ColoringStyle coloringStyle) {
		setColoringType(coloringStyle.getType());
		setRouteInfoAttribute(coloringStyle.getRouteInfoAttribute());
	}

	public void setColoringType(@NonNull ColoringType coloringType) {
		this.coloringType = coloringType;
	}

	public void setRouteInfoAttribute(@Nullable String routeInfoAttribute) {
		this.routeInfoAttribute = routeInfoAttribute;
	}

	@Nullable
	public Integer getColor() {
		return color;
	}

	public void setColor(@Nullable Integer color) {
		this.color = color;
	}

	public int getSplitType() {
		return splitType;
	}

	public void setSplitType(int splitType) {
		this.splitType = splitType;
	}

	public double getSplitInterval() {
		return splitInterval;
	}

	public void setSplitInterval(double splitInterval) {
		this.splitInterval = splitInterval;
	}

	public boolean isJoinSegments() {
		return joinSegments;
	}

	public boolean isShowArrows() {
		return showArrows;
	}

	public void setShowArrows(boolean showArrows) {
		this.showArrows = showArrows;
	}

	public Gpx3DVisualizationType getTrackVisualizationType() {
		return trackVisualizationType;
	}

	public void setTrackVisualizationType(Gpx3DVisualizationType trackVisualizationType) {
		this.trackVisualizationType = trackVisualizationType;
	}

	public Gpx3DWallColorType getTrackWallColorType() {
		return trackWallColorType;
	}

	public void setTrackWallColorType(Gpx3DWallColorType trackWallColorType) {
		this.trackWallColorType = trackWallColorType;
	}

	public Gpx3DLinePositionType getTrackLinePositionType() {
		return trackLinePositionType;
	}

	public void setTrackLinePositionType(Gpx3DLinePositionType trackLinePositionType) {
		this.trackLinePositionType = trackLinePositionType;
	}

	public float getAdditionalExaggeration() {
		return additionalExaggeration;
	}

	public void setAdditionalExaggeration(float additionalExaggeration) {
		this.additionalExaggeration = additionalExaggeration;
	}

	public float getElevationMeters() {
		return elevationMeters;
	}

	public void setElevationMeters(int elevationMeters) {
		this.elevationMeters = elevationMeters;
	}

	public String getGradientColorName() {
		return gradientColorName;
	}

	public void setGradientColorName(String gradientColorName) {
		this.gradientColorName = gradientColorName;
	}

	public void setShowStartFinish(boolean showStartFinish) {
		this.showStartFinish = showStartFinish;
	}

	public boolean isShowStartFinish() {
		return showStartFinish;
	}

	public boolean isCurrentRecording() {
		return appearanceType == CURRENT_RECORDING;
	}

	public boolean isDefaultAppearance() {
		return appearanceType == DEFAULT;
	}

	public boolean isFixedHeight() {
		return trackVisualizationType == FIXED_HEIGHT;
	}

	public void resetParams(@NonNull OsmandApplication app, @Nullable GpxFile gpxFile) {
		OsmandSettings settings = app.getSettings();
		RenderingRulesStorage renderer = app.getRendererRegistry().getCurrentSelectedRenderer();
		if (isCurrentRecording()) {
			settings.CURRENT_TRACK_COLOR.resetToDefault();
			settings.CURRENT_TRACK_WIDTH.resetToDefault();
			settings.CURRENT_TRACK_COLORING_TYPE.resetToDefault();
			settings.CURRENT_GRADIENT_PALETTE.resetToDefault();
			settings.CURRENT_TRACK_ROUTE_INFO_ATTRIBUTE.resetToDefault();
			settings.CURRENT_TRACK_SHOW_ARROWS.resetToDefault();
			settings.CURRENT_TRACK_SHOW_START_FINISH.resetToDefault();
			settings.CURRENT_TRACK_3D_VISUALIZATION_TYPE.resetToDefault();
			initCurrentTrackParams(app);
		} else if (isDefaultAppearance()) {
			color = getDefaultColor(settings, renderer);
			width = getDefaultWidth(settings, renderer);
			showArrows = false;
			showStartFinish = true;
			coloringType = ColoringType.Companion.requireValueOf(TRACK, null);
			gradientColorName = PaletteGradientColor.DEFAULT_NAME;
			gradientColorName = PaletteGradientColor.DEFAULT_NAME;
			routeInfoAttribute = ColoringType.Companion.getRouteInfoAttribute(null);
			trackVisualizationType = Gpx3DVisualizationType.NONE;
			trackWallColorType = Gpx3DWallColorType.NONE;
			trackLinePositionType = Gpx3DLinePositionType.TOP;
		} else if (gpxFile != null) {
			color = gpxFile.getColor(null);
			width = gpxFile.getWidth(null);
			showArrows = gpxFile.isShowArrows();
			showStartFinish = gpxFile.isShowStartFinish();
			splitInterval = gpxFile.getSplitInterval();
			splitType = GpxSplitType.getSplitTypeByName(gpxFile.getSplitType()).getType();
			coloringType = ColoringType.Companion.requireValueOf(TRACK, gpxFile.getColoringType());
			gradientColorName = !Algorithms.isEmpty(gpxFile.getGradientColorPalette()) ? gpxFile.getGradientColorPalette() : PaletteGradientColor.DEFAULT_NAME ;
			routeInfoAttribute = ColoringType.Companion.getRouteInfoAttribute(gpxFile.getColoringType());
			trackVisualizationType = Gpx3DVisualizationType.get3DVisualizationType(gpxFile.get3DVisualizationType());
			trackWallColorType = Gpx3DWallColorType.Companion.get3DWallColorType(gpxFile.get3DWallColoringType());
			trackLinePositionType = Gpx3DLinePositionType.get3DLinePositionType(gpxFile.get3DLinePositionType());
		}
	}

	private void readBundle(@NonNull Bundle bundle) {
		filePath = bundle.getString(TRACK_FILE_NAME);
		width = bundle.getString(TRACK_WIDTH);
		coloringType = ColoringType.Companion.requireValueOf(TRACK, bundle.getString(TRACK_COLORING_TYPE));
		routeInfoAttribute = ColoringType.Companion.getRouteInfoAttribute(bundle.getString(TRACK_COLORING_TYPE));
		color = bundle.containsKey(TRACK_COLOR) ? bundle.getInt(TRACK_COLOR) : null;
		splitType = bundle.getInt(TRACK_SPLIT_TYPE);
		splitInterval = bundle.getDouble(TRACK_SPLIT_INTERVAL);
		joinSegments = bundle.getBoolean(TRACK_JOIN_SEGMENTS);
		showArrows = bundle.getBoolean(TRACK_SHOW_ARROWS);
		showStartFinish = bundle.getBoolean(TRACK_SHOW_START_FINISH);
		trackVisualizationType = AndroidUtils.getSerializable(bundle, TRACK_VISUALIZATION_TYPE_KEY, Gpx3DVisualizationType.class);
		trackWallColorType = AndroidUtils.getSerializable(bundle, TRACK_WALL_COLOR_TYPE_KEY, Gpx3DWallColorType.class);
		trackLinePositionType = AndroidUtils.getSerializable(bundle, TRACK_LINE_POSITION_TYPE_KEY, Gpx3DLinePositionType.class);
		additionalExaggeration = bundle.getFloat(ADDITIONAL_EXAGGERATION_KEY);
		elevationMeters = bundle.getFloat(ELEVATION_METERS_KEY);
		gradientColorName = bundle.getString(GRADIENT_COLOR_KEY);
	}

	public void saveToBundle(@NonNull Bundle bundle) {
		bundle.putString(TRACK_FILE_NAME, filePath);
		bundle.putString(TRACK_WIDTH, width);
		bundle.putString(TRACK_COLORING_TYPE, coloringType != null ? coloringType.getName(routeInfoAttribute) : "");
		bundle.putInt(TRACK_SPLIT_TYPE, splitType);
		bundle.putDouble(TRACK_SPLIT_INTERVAL, splitInterval);
		bundle.putBoolean(TRACK_JOIN_SEGMENTS, joinSegments);
		bundle.putBoolean(TRACK_SHOW_ARROWS, showArrows);
		bundle.putBoolean(TRACK_SHOW_START_FINISH, showStartFinish);
		bundle.putInt(TRACK_APPEARANCE_TYPE, appearanceType);
		bundle.putSerializable(TRACK_VISUALIZATION_TYPE_KEY, trackVisualizationType);
		bundle.putSerializable(TRACK_WALL_COLOR_TYPE_KEY, trackWallColorType);
		bundle.putSerializable(TRACK_LINE_POSITION_TYPE_KEY, trackLinePositionType);
		bundle.putFloat(ADDITIONAL_EXAGGERATION_KEY, trackVisualizationType == null ? 0 : additionalExaggeration);
		bundle.putFloat(ELEVATION_METERS_KEY, elevationMeters);
		bundle.putString(GRADIENT_COLOR_KEY, gradientColorName);

		if (color != null) {
			bundle.putInt(TRACK_COLOR, color);
		}
	}
}