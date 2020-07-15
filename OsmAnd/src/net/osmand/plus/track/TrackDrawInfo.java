package net.osmand.plus.track;

import net.osmand.plus.GPXDatabase.GpxDataItem;

public class TrackDrawInfo {

	private String filePath;
	private String width;
	private GradientScaleType gradientScaleType;
	private int color;
	private int gradientSpeedColor;
	private int gradientAltitudeColor;
	private int gradientSlopeColor;
	private int splitType;
	private double splitInterval;
	private long fileLastModifiedTime;
	private boolean apiImported;
	private boolean showAsMarkers;
	private boolean joinSegments;
	private boolean showArrows;
	private boolean showStartFinish;

	public TrackDrawInfo(GpxDataItem gpxDataItem) {
		filePath = gpxDataItem.getFile().getPath();
		width = gpxDataItem.getWidth();
		gradientScaleType = gpxDataItem.getGradientScaleType();
		color = gpxDataItem.getColor();
		gradientSpeedColor = gpxDataItem.getGradientSpeedColor();
		gradientAltitudeColor = gpxDataItem.getGradientAltitudeColor();
		gradientSlopeColor = gpxDataItem.getGradientSlopeColor();
		splitType = gpxDataItem.getSplitType();
		splitInterval = gpxDataItem.getSplitInterval();
		fileLastModifiedTime = gpxDataItem.getFileLastModifiedTime();
		apiImported = gpxDataItem.isApiImported();
		showAsMarkers = gpxDataItem.isShowAsMarkers();
		joinSegments = gpxDataItem.isJoinSegments();
		showArrows = gpxDataItem.isShowArrows();
		showStartFinish = gpxDataItem.isShowStartFinish();
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
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

	public int getGradientSpeedColor() {
		return gradientSpeedColor;
	}

	public void setGradientSpeedColor(int gradientSpeedColor) {
		this.gradientSpeedColor = gradientSpeedColor;
	}

	public int getGradientAltitudeColor() {
		return gradientAltitudeColor;
	}

	public void setGradientAltitudeColor(int gradientAltitudeColor) {
		this.gradientAltitudeColor = gradientAltitudeColor;
	}

	public int getGradientSlopeColor() {
		return gradientSlopeColor;
	}

	public void setGradientSlopeColor(int gradientSlopeColor) {
		this.gradientSlopeColor = gradientSlopeColor;
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

	public long getFileLastModifiedTime() {
		return fileLastModifiedTime;
	}

	public void setFileLastModifiedTime(long fileLastModifiedTime) {
		this.fileLastModifiedTime = fileLastModifiedTime;
	}

	public boolean isApiImported() {
		return apiImported;
	}

	public void setApiImported(boolean apiImported) {
		this.apiImported = apiImported;
	}

	public boolean isShowAsMarkers() {
		return showAsMarkers;
	}

	public void setShowAsMarkers(boolean showAsMarkers) {
		this.showAsMarkers = showAsMarkers;
	}

	public boolean isJoinSegments() {
		return joinSegments;
	}

	public void setJoinSegments(boolean joinSegments) {
		this.joinSegments = joinSegments;
	}

	public boolean isShowArrows() {
		return showArrows;
	}

	public void setShowArrows(boolean showArrows) {
		this.showArrows = showArrows;
	}

	public boolean isShowStartFinish() {
		return showStartFinish;
	}

	public void setShowStartFinish(boolean showStartFinish) {
		this.showStartFinish = showStartFinish;
	}
}