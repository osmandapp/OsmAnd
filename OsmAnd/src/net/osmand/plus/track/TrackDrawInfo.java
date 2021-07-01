package net.osmand.plus.track;

import android.os.Bundle;

import androidx.annotation.NonNull;

import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.util.Algorithms;

import static net.osmand.plus.activities.TrackActivity.CURRENT_RECORDING;
import static net.osmand.plus.activities.TrackActivity.TRACK_FILE_NAME;

public class TrackDrawInfo {

	private static final String TRACK_WIDTH = "track_width";
	private static final String TRACK_GRADIENT_SCALE_TYPE = "track_gradient_scale_type";
	private static final String TRACK_COLOR = "track_color";
	private static final String TRACK_SPLIT_TYPE = "track_split_type";
	private static final String TRACK_SPLIT_INTERVAL = "track_split_interval";
	private static final String TRACK_JOIN_SEGMENTS = "track_join_segments";
	private static final String TRACK_SHOW_ARROWS = "track_show_arrows";
	private static final String TRACK_SHOW_START_FINISH = "track_show_start_finish";

	private String filePath;
	private String width;
	private GradientScaleType gradientScaleType;
	private int color;
	private int[] speedGradientPalette;
	private int[] altitudeGradientPalette;
	private int[] slopeGradientPalette;
	private int splitType;
	private double splitInterval;
	private boolean joinSegments;
	private boolean showArrows;
	private boolean showStartFinish = true;
	private boolean currentRecording;

	public TrackDrawInfo(boolean currentRecording) {
		this.currentRecording = currentRecording;
	}

	public TrackDrawInfo(Bundle bundle) {
		readBundle(bundle);
	}

	public TrackDrawInfo(@NonNull OsmandApplication app, @NonNull GpxDataItem gpxDataItem, boolean currentRecording) {
		filePath = gpxDataItem.getFile().getPath();
		width = gpxDataItem.getWidth();
		color = gpxDataItem.getColor();
		gradientScaleType = gpxDataItem.getGradientScaleType();
		speedGradientPalette = gpxDataItem.getGradientSpeedPalette();
		altitudeGradientPalette = gpxDataItem.getGradientAltitudePalette();
		slopeGradientPalette = gpxDataItem.getGradientSlopePalette();
		splitType = gpxDataItem.getSplitType();
		splitInterval = gpxDataItem.getSplitInterval();
		joinSegments = gpxDataItem.isJoinSegments();
		showArrows = gpxDataItem.isShowArrows();
		showStartFinish = gpxDataItem.isShowStartFinish();
		this.currentRecording = currentRecording;
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

	public GradientScaleType getGradientScaleType() {
		return gradientScaleType;
	}

	public void setGradientScaleType(GradientScaleType gradientScaleType) {
		this.gradientScaleType = gradientScaleType;
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}

	public int[] getGradientPalette(@NonNull GradientScaleType scaleType) {
		if (scaleType == GradientScaleType.SPEED) {
			return speedGradientPalette;
		} else if (scaleType == GradientScaleType.ALTITUDE) {
			return altitudeGradientPalette;
		} else {
			return slopeGradientPalette;
		}
	}

	public int[] getSpeedGradientPalette() {
		return speedGradientPalette;
	}

	public int[] getAltitudeGradientPalette() {
		return altitudeGradientPalette;
	}

	public int[] getSlopeGradientPalette() {
		return slopeGradientPalette;
	}

	public void setSpeedGradientPalette(int[] palette) {
		this.speedGradientPalette = palette;
	}

	public void setAltitudeGradientPalette(int[] palette) {
		this.altitudeGradientPalette = palette;
	}

	public void setSlopeGradientPalette(int[] palette) {
		this.slopeGradientPalette = palette;
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

	private void readBundle(@NonNull Bundle bundle) {
		filePath = bundle.getString(TRACK_FILE_NAME);
		width = bundle.getString(TRACK_WIDTH);
		String gradientScaleTypeName = bundle.getString(TRACK_GRADIENT_SCALE_TYPE);
		if (!Algorithms.isEmpty(gradientScaleTypeName)) {
			gradientScaleType = GradientScaleType.getGradientTypeByName(gradientScaleTypeName);
		}
		color = bundle.getInt(TRACK_COLOR);
		splitType = bundle.getInt(TRACK_SPLIT_TYPE);
		splitInterval = bundle.getDouble(TRACK_SPLIT_INTERVAL);
		joinSegments = bundle.getBoolean(TRACK_JOIN_SEGMENTS);
		showArrows = bundle.getBoolean(TRACK_SHOW_ARROWS);
		showStartFinish = bundle.getBoolean(TRACK_SHOW_START_FINISH);
		currentRecording = bundle.getBoolean(CURRENT_RECORDING);
	}

	protected void saveToBundle(@NonNull Bundle bundle) {
		bundle.putString(TRACK_FILE_NAME, filePath);
		bundle.putString(TRACK_WIDTH, width);
		bundle.putString(TRACK_GRADIENT_SCALE_TYPE, gradientScaleType != null ? gradientScaleType.getTypeName() : "");
		bundle.putInt(TRACK_COLOR, color);
		bundle.putInt(TRACK_SPLIT_TYPE, splitType);
		bundle.putDouble(TRACK_SPLIT_INTERVAL, splitInterval);
		bundle.putBoolean(TRACK_JOIN_SEGMENTS, joinSegments);
		bundle.putBoolean(TRACK_SHOW_ARROWS, showArrows);
		bundle.putBoolean(TRACK_SHOW_START_FINISH, showStartFinish);
		bundle.putBoolean(CURRENT_RECORDING, currentRecording);
	}
}