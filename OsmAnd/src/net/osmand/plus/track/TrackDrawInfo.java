package net.osmand.plus.track;

import static net.osmand.plus.track.fragments.TrackMenuFragment.CURRENT_RECORDING;
import static net.osmand.plus.track.fragments.TrackMenuFragment.TRACK_FILE_NAME;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXFile;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.settings.backend.OsmandSettings;

public class TrackDrawInfo {

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
	private boolean currentRecording;

	public TrackDrawInfo(@NonNull OsmandApplication app, boolean currentRecording) {
		this.currentRecording = currentRecording;
		initCurrentTrackParams(app);
	}

	public TrackDrawInfo(Bundle bundle) {
		readBundle(bundle);
	}

	public TrackDrawInfo(@NonNull String filePath, @Nullable GpxDataItem gpxDataItem, boolean currentRecording) {
		if (gpxDataItem != null) {
			updateParams(gpxDataItem);
		}
		this.filePath = filePath;
		this.currentRecording = currentRecording;
	}

	private void initCurrentTrackParams(@NonNull OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		width = settings.CURRENT_TRACK_WIDTH.get();
		color = settings.CURRENT_TRACK_COLOR.get();
		coloringType = settings.CURRENT_TRACK_COLORING_TYPE.get();
		routeInfoAttribute = settings.CURRENT_TRACK_ROUTE_INFO_ATTRIBUTE.get();
		showArrows = settings.CURRENT_TRACK_SHOW_ARROWS.get();
		showStartFinish = settings.CURRENT_TRACK_SHOW_START_FINISH.get();
	}

	public void updateParams(@NonNull GpxDataItem gpxDataItem) {
		width = gpxDataItem.getWidth();
		color = gpxDataItem.getColor();
		coloringType = ColoringType.getNonNullTrackColoringTypeByName(gpxDataItem.getColoringType());
		routeInfoAttribute = ColoringType.getRouteInfoAttribute(gpxDataItem.getColoringType());
		splitType = gpxDataItem.getSplitType();
		splitInterval = gpxDataItem.getSplitInterval();
		joinSegments = gpxDataItem.isJoinSegments();
		showArrows = gpxDataItem.isShowArrows();
		showStartFinish = gpxDataItem.isShowStartFinish();
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
	public ColoringType getColoringType() {
		return coloringType == null ? ColoringType.TRACK_SOLID : coloringType;
	}

	public String getRouteInfoAttribute() {
		return routeInfoAttribute;
	}

	public void setColoringType(ColoringType coloringType) {
		this.coloringType = coloringType;
	}

	public void setRouteInfoAttribute(String routeInfoAttribute) {
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
		return currentRecording;
	}

	public void resetParams(@NonNull OsmandApplication app, @NonNull GPXFile gpxFile) {
		if (currentRecording) {
			OsmandSettings settings = app.getSettings();
			settings.CURRENT_TRACK_COLOR.resetToDefault();
			settings.CURRENT_TRACK_WIDTH.resetToDefault();
			settings.CURRENT_TRACK_COLORING_TYPE.resetToDefault();
			settings.CURRENT_TRACK_ROUTE_INFO_ATTRIBUTE.resetToDefault();
			settings.CURRENT_TRACK_SHOW_ARROWS.resetToDefault();
			settings.CURRENT_TRACK_SHOW_START_FINISH.resetToDefault();
			initCurrentTrackParams(app);
		} else {
			color = gpxFile.getColor(0);
			width = gpxFile.getWidth(null);
			showArrows = gpxFile.isShowArrows();
			showStartFinish = gpxFile.isShowStartFinish();
			splitInterval = gpxFile.getSplitInterval();
			splitType = GpxSplitType.getSplitTypeByName(gpxFile.getSplitType()).getType();
			coloringType = ColoringType.getNonNullTrackColoringTypeByName(gpxFile.getColoringType());
			routeInfoAttribute = ColoringType.getRouteInfoAttribute(gpxFile.getColoringType());
		}
	}

	private void readBundle(@NonNull Bundle bundle) {
		filePath = bundle.getString(TRACK_FILE_NAME);
		width = bundle.getString(TRACK_WIDTH);
		coloringType = ColoringType.getNonNullTrackColoringTypeByName(bundle.getString(TRACK_COLORING_TYPE));
		routeInfoAttribute = ColoringType.getRouteInfoAttribute(bundle.getString(TRACK_COLORING_TYPE));
		color = bundle.getInt(TRACK_COLOR);
		splitType = bundle.getInt(TRACK_SPLIT_TYPE);
		splitInterval = bundle.getDouble(TRACK_SPLIT_INTERVAL);
		joinSegments = bundle.getBoolean(TRACK_JOIN_SEGMENTS);
		showArrows = bundle.getBoolean(TRACK_SHOW_ARROWS);
		showStartFinish = bundle.getBoolean(TRACK_SHOW_START_FINISH);
		currentRecording = bundle.getBoolean(CURRENT_RECORDING);
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
		bundle.putBoolean(CURRENT_RECORDING, currentRecording);
	}
}