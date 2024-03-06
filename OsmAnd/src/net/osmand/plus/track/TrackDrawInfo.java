package net.osmand.plus.track;

import static net.osmand.plus.configmap.ConfigureMapMenu.CURRENT_TRACK_COLOR_ATTR;
import static net.osmand.plus.configmap.ConfigureMapMenu.CURRENT_TRACK_WIDTH_ATTR;
import static net.osmand.plus.track.fragments.TrackMenuFragment.TRACK_FILE_NAME;
import static net.osmand.gpx.GpxParameter.COLOR;
import static net.osmand.gpx.GpxParameter.COLORING_TYPE;
import static net.osmand.gpx.GpxParameter.JOIN_SEGMENTS;
import static net.osmand.gpx.GpxParameter.SHOW_ARROWS;
import static net.osmand.gpx.GpxParameter.SHOW_START_FINISH;
import static net.osmand.gpx.GpxParameter.SPLIT_INTERVAL;
import static net.osmand.gpx.GpxParameter.SPLIT_TYPE;
import static net.osmand.gpx.GpxParameter.WIDTH;

import android.os.Bundle;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.card.color.ColoringPurpose;
import net.osmand.plus.card.color.ColoringStyle;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.track.helpers.GpxDataItem;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.render.RenderingRulesStorage;
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

	private String filePath;
	private String width;
	private ColoringType coloringType;
	private String routeInfoAttribute;
	private int color;
	private int splitType;
	private double splitInterval;
	private boolean joinSegments;
	private boolean showArrows;
	private boolean showStartFinish = true;

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
		RenderingRulesStorage renderer = app.getRendererRegistry().getCurrentSelectedRenderer();

		width = settings.CURRENT_TRACK_WIDTH.get();
		if (Algorithms.isEmpty(width)) {
			width = getRenderDefaultTrackWidth(renderer);
		}
		color = settings.CURRENT_TRACK_COLOR.get();
		if (color == 0) {
			color = getRenderDefaultTrackColor(renderer);
		}

		coloringType = settings.CURRENT_TRACK_COLORING_TYPE.get();
		routeInfoAttribute = settings.CURRENT_TRACK_ROUTE_INFO_ATTRIBUTE.get();
		showArrows = settings.CURRENT_TRACK_SHOW_ARROWS.get();
		showStartFinish = settings.CURRENT_TRACK_SHOW_START_FINISH.get();
	}

	public void initDefaultTrackParams(@NonNull OsmandApplication app, @NonNull ApplicationMode mode) {
		OsmandSettings settings = app.getSettings();
		RenderingRulesStorage renderer = app.getRendererRegistry().getCurrentSelectedRenderer();
		CommonPreference<String> colorPref = settings.getCustomRenderProperty(CURRENT_TRACK_COLOR_ATTR);

		color = GpxAppearanceAdapter.parseTrackColor(renderer, colorPref.getModeValue(mode));
		width = settings.getCustomRenderProperty(CURRENT_TRACK_WIDTH_ATTR).getModeValue(mode);

		coloringType = ColoringType.requireValueOf(ColoringPurpose.TRACK);
		routeInfoAttribute = ColoringType.getRouteInfoAttribute(null);
	}

	public void updateParams(@NonNull OsmandApplication app, @NonNull GpxDataItem dataItem) {
		OsmandSettings settings = app.getSettings();
		RenderingRulesStorage renderer = app.getRendererRegistry().getCurrentSelectedRenderer();

		width = dataItem.getParameter(WIDTH);
		if (Algorithms.isEmpty(width)) {
			width = settings.getCustomRenderProperty(CURRENT_TRACK_WIDTH_ATTR).get();
		}
		if (Algorithms.isEmpty(width)) {
			width = getRenderDefaultTrackWidth(renderer);
		}
		color = dataItem.getParameter(COLOR);
		if (color == 0) {
			color = GpxAppearanceAdapter.parseTrackColor(renderer, settings.getCustomRenderProperty(CURRENT_TRACK_COLOR_ATTR).get());
		}
		if (color == 0) {
			color = getRenderDefaultTrackColor(renderer);
		}
		coloringType = ColoringType.requireValueOf(ColoringPurpose.TRACK, dataItem.getParameter(COLORING_TYPE));
		routeInfoAttribute = ColoringType.getRouteInfoAttribute(dataItem.getParameter(COLORING_TYPE));
		splitType = dataItem.getParameter(SPLIT_TYPE);
		splitInterval = dataItem.getParameter(SPLIT_INTERVAL);
		joinSegments = dataItem.getParameter(JOIN_SEGMENTS);
		showArrows = dataItem.getParameter(SHOW_ARROWS);
		showStartFinish = dataItem.getParameter(SHOW_START_FINISH);
	}

	@Nullable
	private String getRenderDefaultTrackWidth(@Nullable RenderingRulesStorage renderer) {
		if (renderer != null) {
			RenderingRuleProperty property = renderer.PROPS.getCustomRule(CURRENT_TRACK_WIDTH_ATTR);
			if (property != null && !Algorithms.isEmpty(property.getPossibleValues())) {
				return property.getPossibleValues()[0];
			}
		}
		return "";
	}

	@Nullable
	private int getRenderDefaultTrackColor(@Nullable RenderingRulesStorage renderer) {
		if (renderer != null) {
			RenderingRuleProperty property = renderer.PROPS.getCustomRule(CURRENT_TRACK_COLOR_ATTR);
			if (property != null && !Algorithms.isEmpty(property.getPossibleValues())) {
				return GpxAppearanceAdapter.parseTrackColor(renderer, property.getPossibleValues()[0]);
			}

		}
		return 0;
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

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
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

	public void resetParams(@NonNull OsmandApplication app, @Nullable GPXFile gpxFile) {
		OsmandSettings settings = app.getSettings();
		RenderingRulesStorage renderer = app.getRendererRegistry().getCurrentSelectedRenderer();
		if (isCurrentRecording()) {
			settings.CURRENT_TRACK_COLOR.resetToDefault();
			settings.CURRENT_TRACK_WIDTH.resetToDefault();
			settings.CURRENT_TRACK_COLORING_TYPE.resetToDefault();
			settings.CURRENT_TRACK_ROUTE_INFO_ATTRIBUTE.resetToDefault();
			settings.CURRENT_TRACK_SHOW_ARROWS.resetToDefault();
			settings.CURRENT_TRACK_SHOW_START_FINISH.resetToDefault();
			initCurrentTrackParams(app);
		} else if (isDefaultAppearance()) {
			color = getRenderDefaultTrackColor(renderer);
			width = getRenderDefaultTrackWidth(renderer);
			showArrows = false;
			showStartFinish = true;
			coloringType = ColoringType.requireValueOf(ColoringPurpose.TRACK);
			routeInfoAttribute = ColoringType.getRouteInfoAttribute(null);
		} else if (gpxFile != null) {
			color = gpxFile.getColor(getRenderDefaultTrackColor(renderer));
			width = gpxFile.getWidth(getRenderDefaultTrackWidth(renderer));
			showArrows = gpxFile.isShowArrows();
			showStartFinish = gpxFile.isShowStartFinish();
			splitInterval = gpxFile.getSplitInterval();
			splitType = GpxSplitType.getSplitTypeByName(gpxFile.getSplitType()).getType();
			coloringType = ColoringType.requireValueOf(ColoringPurpose.TRACK, gpxFile.getColoringType());
			routeInfoAttribute = ColoringType.getRouteInfoAttribute(gpxFile.getColoringType());
		}
	}

	private void readBundle(@NonNull Bundle bundle) {
		filePath = bundle.getString(TRACK_FILE_NAME);
		width = bundle.getString(TRACK_WIDTH);
		coloringType = ColoringType.requireValueOf(ColoringPurpose.TRACK, bundle.getString(TRACK_COLORING_TYPE));
		routeInfoAttribute = ColoringType.getRouteInfoAttribute(bundle.getString(TRACK_COLORING_TYPE));
		color = bundle.getInt(TRACK_COLOR);
		splitType = bundle.getInt(TRACK_SPLIT_TYPE);
		splitInterval = bundle.getDouble(TRACK_SPLIT_INTERVAL);
		joinSegments = bundle.getBoolean(TRACK_JOIN_SEGMENTS);
		showArrows = bundle.getBoolean(TRACK_SHOW_ARROWS);
		showStartFinish = bundle.getBoolean(TRACK_SHOW_START_FINISH);
	}

	public void saveToBundle(@NonNull Bundle bundle) {
		bundle.putString(TRACK_FILE_NAME, filePath);
		bundle.putString(TRACK_WIDTH, width);
		bundle.putString(TRACK_COLORING_TYPE, coloringType != null ? coloringType.getName(routeInfoAttribute) : "");
		bundle.putInt(TRACK_COLOR, color);
		bundle.putInt(TRACK_SPLIT_TYPE, splitType);
		bundle.putDouble(TRACK_SPLIT_INTERVAL, splitInterval);
		bundle.putBoolean(TRACK_JOIN_SEGMENTS, joinSegments);
		bundle.putBoolean(TRACK_SHOW_ARROWS, showArrows);
		bundle.putBoolean(TRACK_SHOW_START_FINISH, showStartFinish);
		bundle.putInt(TRACK_APPEARANCE_TYPE, appearanceType);
	}
}